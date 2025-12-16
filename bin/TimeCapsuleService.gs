/**
 * TimeCapsuleService.gs
 * 
 * Google Apps Script backend for the TimeCapsule application.
 * This script provides a simple JSON API for creating, listing, and opening time capsules.
 * 
 * SETUP INSTRUCTIONS:
 * 1. Create a Google Sheet named "TimeCapsuleLedger" with two sheets:
 *    - "Capsules" with columns: id, owner, unlockTimeEpoch, state, ciphertextBase64, ivBase64, saltBase64, headline, createdAtEpoch
 *    - "AuditLog" with columns: timestampEpoch, action, capsuleId, oldState, newState, requestId
 * 2. Open Apps Script from Extensions > Apps Script
 * 3. Paste this entire file into Code.gs
 * 4. Deploy as Web App: Deploy > New Deployment > Web app
 *    - Execute as: Me
 *    - Who has access: Anyone (for testing) or specific users
 * 5. Copy the Web App URL and use it in the Java client
 * 
 * TRUST MODEL:
 * - The server NEVER sees plaintext or passphrase (all encryption is client-side)
 * - Server time (Date.now()) is the authority for determining when a capsule can be opened
 * - The server only stores and returns encrypted data
 */

// ========================
// Configuration
// ========================

// Name of the Google Sheet (must exist in your Drive)
var SPREADSHEET_NAME = "TimeCapsuleLedger";

// Sheet names
var CAPSULES_SHEET = "Capsules";
var AUDIT_SHEET = "AuditLog";

// ========================
// HTTP Entry Point
// ========================

/**
 * Handle POST requests from the Java client.
 * All requests should be JSON with an "action" field.
 */
function doPost(e) {
  try {
    // Parse the incoming JSON
    var request;
    if (e.postData && e.postData.contents) {
      request = JSON.parse(e.postData.contents);
    } else {
      return jsonResponse({ status: "error", error: "No request body" });
    }
    
    // Route based on action
    var action = request.action;
    
    switch (action) {
      case "create":
        return handleCreate(request);
      case "list":
        return handleList(request);
      case "open":
        return handleOpen(request);
      default:
        return jsonResponse({ status: "error", error: "Unknown action: " + action });
    }
    
  } catch (error) {
    return jsonResponse({ status: "error", error: error.toString() });
  }
}

/**
 * Handle GET requests (for testing/health check).
 */
function doGet(e) {
  return jsonResponse({ 
    status: "ok", 
    message: "TimeCapsule API is running",
    timestamp: Date.now()
  });
}

// ========================
// Action Handlers
// ========================

/**
 * Create a new time capsule.
 * 
 * Expected request fields:
 * - owner: string (email)
 * - headline: string (optional)
 * - unlockTimeEpoch: number (milliseconds)
 * - ciphertextBase64: string
 * - ivBase64: string
 * - saltBase64: string
 */
function handleCreate(request) {
  // Validate required fields
  if (!request.owner) {
    return jsonResponse({ status: "error", error: "Missing owner" });
  }
  if (!request.unlockTimeEpoch) {
    return jsonResponse({ status: "error", error: "Missing unlockTimeEpoch" });
  }
  if (!request.ciphertextBase64) {
    return jsonResponse({ status: "error", error: "Missing ciphertextBase64" });
  }
  if (!request.ivBase64 || !request.saltBase64) {
    return jsonResponse({ status: "error", error: "Missing iv or salt" });
  }
  
  // Validate unlock time is in the future
  var now = Date.now();
  var unlockTime = Number(request.unlockTimeEpoch);
  
  if (unlockTime <= now) {
    return jsonResponse({ status: "error", error: "Unlock time must be in the future" });
  }
  
  // Generate a unique ID
  var id = generateId();
  var createdAt = now;
  
  // Get the capsules sheet
  var sheet = getCapsuleSheet();
  
  // Append the new row
  // Columns: id, owner, unlockTimeEpoch, state, ciphertextBase64, ivBase64, saltBase64, headline, createdAtEpoch
  sheet.appendRow([
    id,
    request.owner,
    unlockTime,
    "sealed",
    request.ciphertextBase64,
    request.ivBase64,
    request.saltBase64,
    request.headline || "",
    createdAt
  ]);
  
  // Log the creation
  logAudit("CREATE", id, "", "sealed", "");
  
  return jsonResponse({ status: "ok", id: id });
}

/**
 * List all capsules for a given owner.
 * 
 * Expected request fields:
 * - owner: string (email)
 */
function handleList(request) {
  if (!request.owner) {
    return jsonResponse({ status: "error", error: "Missing owner" });
  }
  
  var sheet = getCapsuleSheet();
  var data = sheet.getDataRange().getValues();
  
  // Skip header row, filter by owner
  var capsules = [];
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var owner = row[1];
    
    if (owner === request.owner) {
      // Return capsule summary (without encrypted content for list view)
      capsules.push({
        id: row[0],
        owner: row[1],
        unlockTimeEpoch: row[2],
        state: row[3],
        headline: row[7],
        createdAtEpoch: row[8]
      });
    }
  }
  
  return jsonResponse({ status: "ok", capsules: capsules });
}

/**
 * Attempt to open a capsule.
 * Uses LockService to prevent race conditions.
 * 
 * Expected request fields:
 * - id: string (capsule ID)
 * - owner: string (email, for verification)
 * - requestId: string (unique request ID for audit)
 */
function handleOpen(request) {
  if (!request.id) {
    return jsonResponse({ status: "error", error: "Missing capsule id" });
  }
  if (!request.owner) {
    return jsonResponse({ status: "error", error: "Missing owner" });
  }
  
  // Use a lock to prevent concurrent modifications
  var lock = LockService.getScriptLock();
  
  try {
    // Wait up to 10 seconds to acquire the lock
    lock.waitLock(10000);
    
    var sheet = getCapsuleSheet();
    var data = sheet.getDataRange().getValues();
    
    // Find the capsule
    var rowIndex = -1;
    var capsuleRow = null;
    
    for (var i = 1; i < data.length; i++) {
      if (data[i][0] === request.id) {
        rowIndex = i + 1;  // 1-indexed for sheet operations
        capsuleRow = data[i];
        break;
      }
    }
    
    if (!capsuleRow) {
      return jsonResponse({ status: "error", error: "Capsule not found" });
    }
    
    // Verify ownership
    if (capsuleRow[1] !== request.owner) {
      return jsonResponse({ status: "error", error: "Access denied" });
    }
    
    var unlockTime = capsuleRow[2];
    var currentState = capsuleRow[3];
    var now = Date.now();
    
    // Check if unlock time has passed
    if (now < unlockTime) {
      return jsonResponse({ 
        status: "notYet", 
        message: "Capsule cannot be opened yet",
        unlockTimeEpoch: unlockTime,
        serverTimeEpoch: now
      });
    }
    
    // If sealed, update state to opened
    if (currentState === "sealed") {
      sheet.getRange(rowIndex, 4).setValue("opened");  // Column 4 is state
      logAudit("OPEN", request.id, "sealed", "opened", request.requestId || "");
    }
    // If already opened, this is idempotent - just return the data
    
    // Return the encrypted content for client-side decryption
    return jsonResponse({
      status: "ok",
      capsule: {
        id: capsuleRow[0],
        owner: capsuleRow[1],
        unlockTimeEpoch: capsuleRow[2],
        state: "opened",
        ciphertextBase64: capsuleRow[4],
        ivBase64: capsuleRow[5],
        saltBase64: capsuleRow[6],
        headline: capsuleRow[7],
        createdAtEpoch: capsuleRow[8]
      }
    });
    
  } finally {
    lock.releaseLock();
  }
}

// ========================
// Background Processing
// ========================

/**
 * Process capsules that are due to be opened.
 * This function can be run on a time-based trigger (e.g., every hour).
 * It marks sealed capsules as opened if their unlock time has passed.
 */
function processDueCapsules() {
  var lock = LockService.getScriptLock();
  
  try {
    lock.waitLock(30000);
    
    var sheet = getCapsuleSheet();
    var data = sheet.getDataRange().getValues();
    var now = Date.now();
    var processedCount = 0;
    
    for (var i = 1; i < data.length; i++) {
      var row = data[i];
      var unlockTime = row[2];
      var state = row[3];
      var capsuleId = row[0];
      var ownerEmail = row[1];
      
      // Check if sealed and unlock time has passed
      if (state === "sealed" && now >= unlockTime) {
        var rowIndex = i + 1;  // 1-indexed
        sheet.getRange(rowIndex, 4).setValue("opened");
        
        logAudit("OPEN_DUE", capsuleId, "sealed", "opened", "trigger");
        processedCount++;
        
        // Optionally send email notification to owner
        try {
          MailApp.sendEmail({
            to: ownerEmail,
            subject: "Your TimeCapsule is ready to open!",
            body: "Your time capsule with ID " + capsuleId + " is now ready to be opened.\n\n" +
                  "Log in to the TimeCapsule app to decrypt and read your message."
          });
        } catch (emailError) {
          // Email might fail if not authorized, that's okay
          Logger.log("Email notification failed: " + emailError);
        }
      }
    }
    
    Logger.log("Processed " + processedCount + " due capsule(s)");
    
  } finally {
    lock.releaseLock();
  }
}

/**
 * Install a time-based trigger to process due capsules hourly.
 * Run this function once manually to set up the trigger.
 */
function installTrigger() {
  // Remove any existing triggers first
  var triggers = ScriptApp.getProjectTriggers();
  for (var i = 0; i < triggers.length; i++) {
    if (triggers[i].getHandlerFunction() === "processDueCapsules") {
      ScriptApp.deleteTrigger(triggers[i]);
    }
  }
  
  // Create a new hourly trigger
  ScriptApp.newTrigger("processDueCapsules")
    .timeBased()
    .everyHours(1)
    .create();
    
  Logger.log("Trigger installed: processDueCapsules will run every hour");
}

// ========================
// Helper Functions
// ========================

/**
 * Get the Capsules sheet, creating it if needed.
 */
function getCapsuleSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(CAPSULES_SHEET);
  
  if (!sheet) {
    // Create the sheet with headers
    sheet = ss.insertSheet(CAPSULES_SHEET);
    sheet.appendRow([
      "id", "owner", "unlockTimeEpoch", "state", 
      "ciphertextBase64", "ivBase64", "saltBase64", 
      "headline", "createdAtEpoch"
    ]);
  }
  
  return sheet;
}

/**
 * Get the AuditLog sheet, creating it if needed.
 */
function getAuditSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(AUDIT_SHEET);
  
  if (!sheet) {
    // Create the sheet with headers
    sheet = ss.insertSheet(AUDIT_SHEET);
    sheet.appendRow(["timestampEpoch", "action", "capsuleId", "oldState", "newState", "requestId"]);
  }
  
  return sheet;
}

/**
 * Get the spreadsheet URL. 
 * You can hardcode this or use a search approach.
 */
function getSpreadsheetUrl() {
  // Try to find spreadsheet by name
  var files = DriveApp.getFilesByName(SPREADSHEET_NAME);
  
  if (files.hasNext()) {
    var file = files.next();
    return file.getUrl();
  }
  
  // Create the spreadsheet if it doesn't exist
  var ss = SpreadsheetApp.create(SPREADSHEET_NAME);
  Logger.log("Created spreadsheet: " + ss.getUrl());
  return ss.getUrl();
}

/**
 * Log an action to the audit sheet.
 */
function logAudit(action, capsuleId, oldState, newState, requestId) {
  var sheet = getAuditSheet();
  sheet.appendRow([
    Date.now(),
    action,
    capsuleId,
    oldState,
    newState,
    requestId
  ]);
}

/**
 * Generate a unique capsule ID.
 */
function generateId() {
  // Simple ID: timestamp + random suffix
  return "C" + Date.now() + "_" + Math.random().toString(36).substring(2, 8);
}

/**
 * Create a JSON response for the HTTP handler.
 */
function jsonResponse(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
