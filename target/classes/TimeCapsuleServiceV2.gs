/**
 * TimeCapsuleService V2 - Google Apps Script Backend
 * 
 * Enhanced with:
 * - Users sheet for user management
 * - Friends sheet for friend relationships
 * - Invites sheet for email invitations
 * - CapsuleRecipients sheet for multi-recipient capsules
 * - Secure token handling for invites
 * - Email notifications with surprise mode support
 * 
 * SHEETS REQUIRED:
 * 1. Users: userId, email, displayName, passwordHash, createdAtUtc
 * 2. Friends: requesterUserId, addresseeUserId, status, createdAtUtc, updatedAtUtc
 * 3. Invites: inviteId, inviterUserId, inviteeEmail, tokenHash, status, createdAtUtc, expiresAtUtc, acceptedAtUtc
 * 4. Capsules: id, owner, ownerUserId, unlockTimeEpoch, state, ciphertextBase64, ivBase64, saltBase64, headline, createdAtEpoch, updatedAtEpoch
 * 5. CapsuleRecipients: capsuleId, recipientEmail, recipientUserId, notifyOnCreate, notifyOnUnlock, deliveryStatus, openedAtUtc, createdAtUtc
 * 6. AuditLog: timestampEpoch, action, entityId, oldState, newState, requestId
 */

// ========================
// Configuration
// ========================

var SPREADSHEET_NAME = "TimeCapsuleLedger";
var USERS_SHEET = "Users";
var FRIENDS_SHEET = "Friends";
var INVITES_SHEET = "Invites";
var CAPSULES_SHEET = "Capsules";
var RECIPIENTS_SHEET = "CapsuleRecipients";
var AUDIT_SHEET = "AuditLog";

var INVITE_EXPIRY_DAYS = 7;
var APP_URL = "https://your-app-url.com";  // For invite links

// ========================
// HTTP Entry Point
// ========================

function doPost(e) {
  try {
    var request;
    if (e.postData && e.postData.contents) {
      request = JSON.parse(e.postData.contents);
    } else {
      return jsonResponse({ status: "error", error: "No request body" });
    }
    
    var action = request.action;
    
    switch (action) {
      // User actions
      case "registerOrLogin":
        return handleRegisterOrLogin(request);
      case "searchUsers":
        return handleSearchUsers(request);
      case "getUserByEmail":
        return handleGetUserByEmail(request);
        
      // Friend actions
      case "friendRequest":
        return handleFriendRequest(request);
      case "friendAccept":
        return handleFriendAccept(request);
      case "friendDecline":
        return handleFriendDecline(request);
      case "friendBlock":
        return handleFriendBlock(request);
      case "friendsList":
        return handleFriendsList(request);
      case "friendRequests":
        return handleFriendRequests(request);
        
      // Invite actions
      case "inviteSend":
        return handleInviteSend(request);
      case "inviteAccept":
        return handleInviteAccept(request);
      case "inviteResend":
        return handleInviteResend(request);
      case "invitesList":
        return handleInvitesList(request);
        
      // Capsule actions
      case "create":
        return handleCreate(request);
      case "list":
        return handleList(request);
      case "listSent":
        return handleListSent(request);
      case "listReceived":
        return handleListReceived(request);
      case "open":
        return handleOpen(request);
      case "markRecipientOpened":
        return handleMarkRecipientOpened(request);
        
      default:
        return jsonResponse({ status: "error", error: "Unknown action: " + action });
    }
    
  } catch (error) {
    return jsonResponse({ status: "error", error: error.toString() });
  }
}

function doGet(e) {
  // Handle invite accept via GET with token parameter
  if (e.parameter && e.parameter.token) {
    return handleInviteAcceptPage(e.parameter.token);
  }
  
  return jsonResponse({ 
    status: "ok", 
    message: "TimeCapsule API V2 is running",
    timestamp: Date.now()
  });
}

// ========================
// User Handlers
// ========================

function handleRegisterOrLogin(request) {
  if (!request.email) {
    return jsonResponse({ status: "error", error: "Missing email" });
  }
  
  var sheet = getUsersSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  
  // Check if user exists
  for (var i = 1; i < data.length; i++) {
    if (data[i][1] === request.email) {
      // User exists, verify password if provided
      if (request.passwordHash && data[i][3] !== request.passwordHash) {
        return jsonResponse({ status: "error", error: "Invalid password" });
      }
      
      // Update display name if provided
      if (request.displayName && request.displayName !== data[i][2]) {
        sheet.getRange(i + 1, 3).setValue(request.displayName);
      }
      
      return jsonResponse({
        status: "ok",
        user: {
          userId: data[i][0],
          email: data[i][1],
          displayName: data[i][2] || request.displayName,
          createdAtUtc: data[i][4]
        }
      });
    }
  }
  
  // Create new user
  var userId = "U" + now + "_" + Math.random().toString(36).substring(2, 8);
  
  sheet.appendRow([
    userId,
    request.email,
    request.displayName || "",
    request.passwordHash || "",
    now
  ]);
  
  logAudit("USER_CREATE", userId, "", "created", "");
  
  return jsonResponse({
    status: "ok",
    user: {
      userId: userId,
      email: request.email,
      displayName: request.displayName || "",
      createdAtUtc: now
    }
  });
}

function handleSearchUsers(request) {
  if (!request.query) {
    return jsonResponse({ status: "error", error: "Missing search query" });
  }
  
  var sheet = getUsersSheet();
  var data = sheet.getDataRange().getValues();
  var query = request.query.toLowerCase();
  var results = [];
  
  for (var i = 1; i < data.length; i++) {
    var email = (data[i][1] || "").toLowerCase();
    var displayName = (data[i][2] || "").toLowerCase();
    
    // Skip current user
    if (data[i][0] === request.userId) continue;
    
    if (email.indexOf(query) !== -1 || displayName.indexOf(query) !== -1) {
      results.push({
        userId: data[i][0],
        email: data[i][1],
        displayName: data[i][2]
      });
      
      if (results.length >= 20) break;  // Limit results
    }
  }
  
  return jsonResponse({ status: "ok", users: results });
}

function handleGetUserByEmail(request) {
  if (!request.email) {
    return jsonResponse({ status: "error", error: "Missing email" });
  }
  
  var sheet = getUsersSheet();
  var data = sheet.getDataRange().getValues();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][1] === request.email) {
      return jsonResponse({
        status: "ok",
        user: {
          userId: data[i][0],
          email: data[i][1],
          displayName: data[i][2]
        }
      });
    }
  }
  
  return jsonResponse({ status: "ok", user: null });
}

// ========================
// Friends Handlers
// ========================

function handleFriendRequest(request) {
  if (!request.requesterUserId || !request.addresseeUserId) {
    return jsonResponse({ status: "error", error: "Missing user IDs" });
  }
  
  if (request.requesterUserId === request.addresseeUserId) {
    return jsonResponse({ status: "error", error: "Cannot friend yourself" });
  }
  
  var sheet = getFriendsSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  
  // Check if relationship already exists
  for (var i = 1; i < data.length; i++) {
    var req = data[i][0];
    var addr = data[i][1];
    
    if ((req === request.requesterUserId && addr === request.addresseeUserId) ||
        (req === request.addresseeUserId && addr === request.requesterUserId)) {
      var status = data[i][2];
      if (status === "ACCEPTED") {
        return jsonResponse({ status: "error", error: "Already friends" });
      } else if (status === "PENDING") {
        return jsonResponse({ status: "error", error: "Request already pending" });
      } else if (status === "BLOCKED") {
        return jsonResponse({ status: "error", error: "Cannot send request" });
      }
    }
  }
  
  // Create new friend request
  sheet.appendRow([
    request.requesterUserId,
    request.addresseeUserId,
    "PENDING",
    now,
    now
  ]);
  
  logAudit("FRIEND_REQUEST", request.requesterUserId + "->" + request.addresseeUserId, "", "PENDING", "");
  
  // Send notification email to addressee
  var addresseeEmail = getUserEmailById(request.addresseeUserId);
  var requesterName = getUserDisplayNameById(request.requesterUserId);
  if (addresseeEmail) {
    try {
      MailApp.sendEmail({
        to: addresseeEmail,
        subject: requesterName + " sent you a friend request on TimeCapsule",
        body: requesterName + " wants to be your friend on TimeCapsule!\n\n" +
              "Log in to the app to accept or decline the request."
      });
    } catch (e) {
      Logger.log("Email failed: " + e);
    }
  }
  
  return jsonResponse({ status: "ok", message: "Friend request sent" });
}

function handleFriendAccept(request) {
  if (!request.requesterUserId || !request.addresseeUserId) {
    return jsonResponse({ status: "error", error: "Missing user IDs" });
  }
  
  var sheet = getFriendsSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === request.requesterUserId && 
        data[i][1] === request.addresseeUserId &&
        data[i][2] === "PENDING") {
      
      sheet.getRange(i + 1, 3).setValue("ACCEPTED");
      sheet.getRange(i + 1, 5).setValue(now);
      
      logAudit("FRIEND_ACCEPT", request.requesterUserId + "->" + request.addresseeUserId, "PENDING", "ACCEPTED", "");
      
      return jsonResponse({ status: "ok", message: "Friend request accepted" });
    }
  }
  
  return jsonResponse({ status: "error", error: "Friend request not found" });
}

function handleFriendDecline(request) {
  if (!request.requesterUserId || !request.addresseeUserId) {
    return jsonResponse({ status: "error", error: "Missing user IDs" });
  }
  
  var sheet = getFriendsSheet();
  var data = sheet.getDataRange().getValues();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === request.requesterUserId && 
        data[i][1] === request.addresseeUserId &&
        data[i][2] === "PENDING") {
      
      sheet.deleteRow(i + 1);
      logAudit("FRIEND_DECLINE", request.requesterUserId + "->" + request.addresseeUserId, "PENDING", "DELETED", "");
      
      return jsonResponse({ status: "ok", message: "Friend request declined" });
    }
  }
  
  return jsonResponse({ status: "error", error: "Friend request not found" });
}

function handleFriendBlock(request) {
  if (!request.userId || !request.blockUserId) {
    return jsonResponse({ status: "error", error: "Missing user IDs" });
  }
  
  var sheet = getFriendsSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  
  // Find existing relationship
  for (var i = 1; i < data.length; i++) {
    var req = data[i][0];
    var addr = data[i][1];
    
    if ((req === request.userId && addr === request.blockUserId) ||
        (req === request.blockUserId && addr === request.userId)) {
      
      sheet.getRange(i + 1, 3).setValue("BLOCKED");
      sheet.getRange(i + 1, 5).setValue(now);
      
      return jsonResponse({ status: "ok", message: "User blocked" });
    }
  }
  
  // No existing relationship, create blocked entry
  sheet.appendRow([
    request.userId,
    request.blockUserId,
    "BLOCKED",
    now,
    now
  ]);
  
  return jsonResponse({ status: "ok", message: "User blocked" });
}

function handleFriendsList(request) {
  if (!request.userId) {
    return jsonResponse({ status: "error", error: "Missing userId" });
  }
  
  var sheet = getFriendsSheet();
  var data = sheet.getDataRange().getValues();
  var friends = [];
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][2] !== "ACCEPTED") continue;
    
    var friendUserId = null;
    if (data[i][0] === request.userId) {
      friendUserId = data[i][1];
    } else if (data[i][1] === request.userId) {
      friendUserId = data[i][0];
    }
    
    if (friendUserId) {
      var friendInfo = getUserById(friendUserId);
      friends.push({
        requesterUserId: data[i][0],
        addresseeUserId: data[i][1],
        status: data[i][2],
        createdAtUtc: data[i][3],
        updatedAtUtc: data[i][4],
        friendEmail: friendInfo ? friendInfo.email : null,
        friendDisplayName: friendInfo ? friendInfo.displayName : null
      });
    }
  }
  
  return jsonResponse({ status: "ok", friends: friends });
}

function handleFriendRequests(request) {
  if (!request.userId) {
    return jsonResponse({ status: "error", error: "Missing userId" });
  }
  
  var sheet = getFriendsSheet();
  var data = sheet.getDataRange().getValues();
  var requests = [];
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][2] !== "PENDING") continue;
    
    // Include both incoming and outgoing
    if (data[i][0] === request.userId || data[i][1] === request.userId) {
      var friendUserId = data[i][0] === request.userId ? data[i][1] : data[i][0];
      var friendInfo = getUserById(friendUserId);
      
      requests.push({
        requesterUserId: data[i][0],
        addresseeUserId: data[i][1],
        status: data[i][2],
        createdAtUtc: data[i][3],
        updatedAtUtc: data[i][4],
        friendEmail: friendInfo ? friendInfo.email : null,
        friendDisplayName: friendInfo ? friendInfo.displayName : null
      });
    }
  }
  
  return jsonResponse({ status: "ok", requests: requests });
}

// ========================
// Invite Handlers
// ========================

function handleInviteSend(request) {
  if (!request.inviterUserId || !request.inviteeEmail) {
    return jsonResponse({ status: "error", error: "Missing required fields" });
  }
  
  // Check if email belongs to existing user
  var existingUser = getUserByEmail(request.inviteeEmail);
  if (existingUser) {
    // Create friend request instead
    return handleFriendRequest({
      requesterUserId: request.inviterUserId,
      addresseeUserId: existingUser.userId
    });
  }
  
  var sheet = getInvitesSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  var expiresAt = now + (INVITE_EXPIRY_DAYS * 24 * 60 * 60 * 1000);
  
  // Check for existing pending invite
  for (var i = 1; i < data.length; i++) {
    if (data[i][1] === request.inviterUserId && 
        data[i][2] === request.inviteeEmail &&
        data[i][4] === "SENT") {
      
      if (data[i][6] > now) {
        return jsonResponse({ status: "error", error: "Invite already pending" });
      }
      // Expired, will create new one
    }
  }
  
  // Generate secure token
  var token = generateSecureToken();
  var tokenHash = hashToken(token);
  var inviteId = "INV" + now + "_" + Math.random().toString(36).substring(2, 8);
  
  sheet.appendRow([
    inviteId,
    request.inviterUserId,
    request.inviteeEmail,
    tokenHash,
    "SENT",
    now,
    expiresAt,
    ""  // acceptedAtUtc
  ]);
  
  // Send invite email
  var inviterName = getUserDisplayNameById(request.inviterUserId);
  var inviteLink = APP_URL + "?token=" + token;
  
  try {
    MailApp.sendEmail({
      to: request.inviteeEmail,
      subject: inviterName + " invited you to TimeCapsule",
      htmlBody: "<h2>" + inviterName + " invited you to TimeCapsule!</h2>" +
                "<p>TimeCapsule lets you create encrypted time-locked messages for friends.</p>" +
                (request.message ? "<p><em>\"" + request.message + "\"</em></p>" : "") +
                "<p><a href='" + inviteLink + "' style='display:inline-block;padding:12px 24px;background:#667eea;color:white;text-decoration:none;border-radius:8px;'>Accept Invite</a></p>" +
                "<p style='color:#888;font-size:12px;'>This invite expires in " + INVITE_EXPIRY_DAYS + " days.</p>"
    });
  } catch (e) {
    Logger.log("Invite email failed: " + e);
  }
  
  logAudit("INVITE_SEND", inviteId, "", "SENT", "");
  
  return jsonResponse({ 
    status: "ok", 
    inviteId: inviteId,
    message: "Invite sent to " + request.inviteeEmail
  });
}

function handleInviteAccept(request) {
  if (!request.token) {
    return jsonResponse({ status: "error", error: "Missing token" });
  }
  
  var tokenHash = hashToken(request.token);
  var sheet = getInvitesSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][3] === tokenHash && data[i][4] === "SENT") {
      var inviteId = data[i][0];
      var inviterUserId = data[i][1];
      var inviteeEmail = data[i][2];
      var expiresAt = data[i][6];
      
      // Check expiry
      if (now > expiresAt) {
        sheet.getRange(i + 1, 5).setValue("EXPIRED");
        return jsonResponse({ status: "error", error: "Invite has expired" });
      }
      
      // Create or get user
      var userResponse = handleRegisterOrLogin({
        email: request.email || inviteeEmail,
        displayName: request.displayName || "",
        passwordHash: request.passwordHash || ""
      });
      
      var userResult = JSON.parse(userResponse.getContent());
      if (userResult.status !== "ok") {
        return jsonResponse({ status: "error", error: "Failed to create user" });
      }
      
      var newUserId = userResult.user.userId;
      
      // Mark invite as accepted
      sheet.getRange(i + 1, 5).setValue("ACCEPTED");
      sheet.getRange(i + 1, 8).setValue(now);
      
      // Create friendship (auto-accepted)
      var friendsSheet = getFriendsSheet();
      friendsSheet.appendRow([
        inviterUserId,
        newUserId,
        "ACCEPTED",
        now,
        now
      ]);
      
      // Update any pending capsule recipients for this email
      updatePendingRecipients(inviteeEmail, newUserId);
      
      logAudit("INVITE_ACCEPT", inviteId, "SENT", "ACCEPTED", "");
      
      return jsonResponse({ 
        status: "ok", 
        message: "Welcome to TimeCapsule!",
        user: userResult.user
      });
    }
  }
  
  return jsonResponse({ status: "error", error: "Invalid or expired invite token" });
}

function handleInviteResend(request) {
  if (!request.inviteId || !request.inviterUserId) {
    return jsonResponse({ status: "error", error: "Missing required fields" });
  }
  
  var sheet = getInvitesSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === request.inviteId && 
        data[i][1] === request.inviterUserId &&
        data[i][4] === "SENT") {
      
      var inviteeEmail = data[i][2];
      var expiresAt = now + (INVITE_EXPIRY_DAYS * 24 * 60 * 60 * 1000);
      
      // Generate new token
      var newToken = generateSecureToken();
      var newTokenHash = hashToken(newToken);
      
      sheet.getRange(i + 1, 4).setValue(newTokenHash);
      sheet.getRange(i + 1, 7).setValue(expiresAt);
      
      // Resend email
      var inviterName = getUserDisplayNameById(request.inviterUserId);
      var inviteLink = APP_URL + "?token=" + newToken;
      
      try {
        MailApp.sendEmail({
          to: inviteeEmail,
          subject: "Reminder: " + inviterName + " invited you to TimeCapsule",
          htmlBody: "<h2>" + inviterName + " is still waiting for you on TimeCapsule!</h2>" +
                    "<p><a href='" + inviteLink + "' style='display:inline-block;padding:12px 24px;background:#667eea;color:white;text-decoration:none;border-radius:8px;'>Accept Invite</a></p>"
        });
      } catch (e) {
        Logger.log("Resend email failed: " + e);
      }
      
      return jsonResponse({ status: "ok", message: "Invite resent" });
    }
  }
  
  return jsonResponse({ status: "error", error: "Invite not found" });
}

function handleInvitesList(request) {
  if (!request.userId) {
    return jsonResponse({ status: "error", error: "Missing userId" });
  }
  
  var sheet = getInvitesSheet();
  var data = sheet.getDataRange().getValues();
  var invites = [];
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][1] === request.userId) {
      invites.push({
        inviteId: data[i][0],
        inviterUserId: data[i][1],
        inviteeEmail: data[i][2],
        status: data[i][4],
        createdAtUtc: data[i][5],
        expiresAtUtc: data[i][6],
        acceptedAtUtc: data[i][7] || null
      });
    }
  }
  
  return jsonResponse({ status: "ok", invites: invites });
}

// ========================
// Capsule Handlers
// ========================

function handleCreate(request) {
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
  
  var now = Date.now();
  var unlockTime = Number(request.unlockTimeEpoch);
  
  if (unlockTime <= now) {
    return jsonResponse({ status: "error", error: "Unlock time must be in the future" });
  }
  
  var id = "C" + now + "_" + Math.random().toString(36).substring(2, 8);
  var sheet = getCapsuleSheet();
  
  // Columns: id, owner, ownerUserId, unlockTimeEpoch, state, ciphertextBase64, ivBase64, saltBase64, headline, createdAtEpoch, updatedAtEpoch
  sheet.appendRow([
    id,
    request.owner,
    request.ownerUserId || "",
    unlockTime,
    "sealed",
    request.ciphertextBase64,
    request.ivBase64,
    request.saltBase64,
    request.headline || "",
    now,
    now
  ]);
  
  // Handle recipients
  if (request.recipients && request.recipients.length > 0) {
    var recipientsSheet = getRecipientsSheet();
    
    for (var i = 0; i < request.recipients.length; i++) {
      var r = request.recipients[i];
      var recipientUserId = "";
      
      // Try to find userId if email belongs to existing user
      if (r.recipientEmail) {
        var existingUser = getUserByEmail(r.recipientEmail);
        if (existingUser) {
          recipientUserId = existingUser.userId;
        }
      }
      
      // Columns: capsuleId, recipientEmail, recipientUserId, notifyOnCreate, notifyOnUnlock, deliveryStatus, openedAtUtc, createdAtUtc
      recipientsSheet.appendRow([
        id,
        r.recipientEmail || "",
        recipientUserId || r.recipientUserId || "",
        r.notifyOnCreate ? "TRUE" : "FALSE",
        r.notifyOnUnlock !== false ? "TRUE" : "FALSE",
        "NONE",
        "",
        now
      ]);
      
      // Send creation notification if notifyOnCreate is true
      if (r.notifyOnCreate && r.recipientEmail) {
        try {
          var senderName = getUserDisplayNameById(request.ownerUserId) || request.owner;
          MailApp.sendEmail({
            to: r.recipientEmail,
            subject: senderName + " sent you a TimeCapsule!",
            body: senderName + " has sent you a time capsule that will unlock on " + 
                  new Date(unlockTime).toLocaleString() + ".\n\n" +
                  "Title: " + (request.headline || "(No title)") + "\n\n" +
                  "Log in to TimeCapsule to see it when it unlocks!"
          });
          
          // Update delivery status
          var lastRow = recipientsSheet.getLastRow();
          recipientsSheet.getRange(lastRow, 6).setValue("CREATED_EMAIL_SENT");
        } catch (e) {
          Logger.log("Creation notification failed: " + e);
        }
      }
    }
  }
  
  logAudit("CREATE", id, "", "sealed", "");
  
  return jsonResponse({ status: "ok", id: id });
}

function handleList(request) {
  if (!request.owner) {
    return jsonResponse({ status: "error", error: "Missing owner" });
  }
  
  var sheet = getCapsuleSheet();
  var data = sheet.getDataRange().getValues();
  var capsules = [];
  
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var owner = row[1];
    
    if (owner === request.owner) {
      capsules.push({
        capsuleId: row[0],
        ownerEmail: row[1],
        ownerUserId: row[2],
        unlockAtUtc: row[3],
        state: row[4],
        headline: row[8],
        createdAtUtc: row[9],
        updatedAtUtc: row[10]
      });
    }
  }
  
  return jsonResponse({ status: "ok", capsules: capsules });
}

function handleListSent(request) {
  return handleList(request);  // Same as list for owner
}

function handleListReceived(request) {
  if (!request.userId && !request.email) {
    return jsonResponse({ status: "error", error: "Missing userId or email" });
  }
  
  var recipientsSheet = getRecipientsSheet();
  var recipientsData = recipientsSheet.getDataRange().getValues();
  var capsulesSheet = getCapsuleSheet();
  var capsulesData = capsulesSheet.getDataRange().getValues();
  
  // Build capsule lookup map
  var capsuleMap = {};
  for (var i = 1; i < capsulesData.length; i++) {
    var row = capsulesData[i];
    capsuleMap[row[0]] = {
      capsuleId: row[0],
      ownerEmail: row[1],
      ownerUserId: row[2],
      unlockAtUtc: row[3],
      state: row[4],
      ciphertextBase64: row[5],
      ivBase64: row[6],
      saltBase64: row[7],
      headline: row[8],
      createdAtUtc: row[9]
    };
  }
  
  var receivedCapsules = [];
  
  for (var i = 1; i < recipientsData.length; i++) {
    var r = recipientsData[i];
    var matches = (r[2] === request.userId) || (r[1] === request.email);
    
    if (matches) {
      var capsule = capsuleMap[r[0]];
      if (capsule) {
        // Get sender info
        var ownerInfo = getUserById(capsule.ownerUserId);
        
        receivedCapsules.push({
          capsuleId: capsule.capsuleId,
          ownerEmail: capsule.ownerEmail,
          ownerDisplayName: ownerInfo ? ownerInfo.displayName : null,
          unlockAtUtc: capsule.unlockAtUtc,
          state: capsule.state,
          headline: capsule.headline,
          createdAtUtc: capsule.createdAtUtc,
          recipientOpenedAt: r[6] || null,
          isSurprise: r[3] !== "TRUE"  // notifyOnCreate false = surprise
        });
      }
    }
  }
  
  return jsonResponse({ status: "ok", capsules: receivedCapsules });
}

function handleOpen(request) {
  if (!request.id) {
    return jsonResponse({ status: "error", error: "Missing capsule id" });
  }
  
  var lock = LockService.getScriptLock();
  
  try {
    lock.waitLock(10000);
    
    var sheet = getCapsuleSheet();
    var data = sheet.getDataRange().getValues();
    
    var rowIndex = -1;
    var capsuleRow = null;
    
    for (var i = 1; i < data.length; i++) {
      if (data[i][0] === request.id) {
        rowIndex = i + 1;
        capsuleRow = data[i];
        break;
      }
    }
    
    if (!capsuleRow) {
      return jsonResponse({ status: "error", error: "Capsule not found" });
    }
    
    // Check if user is owner or recipient
    var isOwner = capsuleRow[1] === request.owner || capsuleRow[2] === request.userId;
    var isRecipient = checkIsRecipient(request.id, request.userId, request.owner);
    
    if (!isOwner && !isRecipient) {
      return jsonResponse({ status: "error", error: "Access denied" });
    }
    
    var unlockTime = capsuleRow[3];
    var currentState = capsuleRow[4];
    var now = Date.now();
    
    if (now < unlockTime) {
      return jsonResponse({ 
        status: "notYet", 
        message: "Capsule cannot be opened yet",
        unlockTimeEpoch: unlockTime,
        serverTimeEpoch: now
      });
    }
    
    if (currentState === "sealed") {
      sheet.getRange(rowIndex, 5).setValue("opened");
      sheet.getRange(rowIndex, 11).setValue(now);
      logAudit("OPEN", request.id, "sealed", "opened", request.requestId || "");
    }
    
    return jsonResponse({
      status: "ok",
      capsule: {
        capsuleId: capsuleRow[0],
        ownerEmail: capsuleRow[1],
        ownerUserId: capsuleRow[2],
        unlockAtUtc: capsuleRow[3],
        state: "opened",
        ciphertextBase64: capsuleRow[5],
        ivBase64: capsuleRow[6],
        saltBase64: capsuleRow[7],
        headline: capsuleRow[8],
        createdAtUtc: capsuleRow[9]
      }
    });
    
  } finally {
    lock.releaseLock();
  }
}

function handleMarkRecipientOpened(request) {
  if (!request.capsuleId) {
    return jsonResponse({ status: "error", error: "Missing capsuleId" });
  }
  
  var sheet = getRecipientsSheet();
  var data = sheet.getDataRange().getValues();
  var now = Date.now();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === request.capsuleId && 
        (data[i][2] === request.userId || data[i][1] === request.email)) {
      
      if (!data[i][6]) {  // Only set if not already opened
        sheet.getRange(i + 1, 7).setValue(now);
      }
      return jsonResponse({ status: "ok" });
    }
  }
  
  return jsonResponse({ status: "error", error: "Recipient not found" });
}

// ========================
// Background Processing
// ========================

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
      var capsuleId = row[0];
      var unlockTime = row[3];
      var state = row[4];
      
      if (state === "sealed" && now >= unlockTime) {
        var rowIndex = i + 1;
        sheet.getRange(rowIndex, 5).setValue("opened");
        sheet.getRange(rowIndex, 11).setValue(now);
        
        // Send unlock notifications to recipients
        sendUnlockNotifications(capsuleId, row);
        
        logAudit("OPEN_DUE", capsuleId, "sealed", "opened", "trigger");
        processedCount++;
      }
    }
    
    Logger.log("Processed " + processedCount + " due capsule(s)");
    
  } finally {
    lock.releaseLock();
  }
}

function sendUnlockNotifications(capsuleId, capsuleRow) {
  var recipientsSheet = getRecipientsSheet();
  var data = recipientsSheet.getDataRange().getValues();
  var senderName = getUserDisplayNameById(capsuleRow[2]) || capsuleRow[1];
  var headline = capsuleRow[8] || "(Untitled)";
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === capsuleId && data[i][4] === "TRUE") {  // notifyOnUnlock
      var recipientEmail = data[i][1];
      
      if (recipientEmail && data[i][5] !== "UNLOCK_EMAIL_SENT") {
        try {
          MailApp.sendEmail({
            to: recipientEmail,
            subject: "Your TimeCapsule from " + senderName + " is ready!",
            body: "The time capsule '" + headline + "' from " + senderName + " is now ready to open!\n\n" +
                  "Log in to TimeCapsule to decrypt and read your message."
          });
          
          recipientsSheet.getRange(i + 1, 6).setValue("UNLOCK_EMAIL_SENT");
        } catch (e) {
          Logger.log("Unlock notification failed for " + recipientEmail + ": " + e);
        }
      }
    }
  }
}

function installTrigger() {
  var triggers = ScriptApp.getProjectTriggers();
  for (var i = 0; i < triggers.length; i++) {
    if (triggers[i].getHandlerFunction() === "processDueCapsules") {
      ScriptApp.deleteTrigger(triggers[i]);
    }
  }
  
  ScriptApp.newTrigger("processDueCapsules")
    .timeBased()
    .everyHours(1)
    .create();
    
  Logger.log("Trigger installed: processDueCapsules will run every hour");
}

// ========================
// Helper Functions
// ========================

function getUsersSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(USERS_SHEET);
  
  if (!sheet) {
    sheet = ss.insertSheet(USERS_SHEET);
    sheet.appendRow(["userId", "email", "displayName", "passwordHash", "createdAtUtc"]);
  }
  
  return sheet;
}

function getFriendsSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(FRIENDS_SHEET);
  
  if (!sheet) {
    sheet = ss.insertSheet(FRIENDS_SHEET);
    sheet.appendRow(["requesterUserId", "addresseeUserId", "status", "createdAtUtc", "updatedAtUtc"]);
  }
  
  return sheet;
}

function getInvitesSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(INVITES_SHEET);
  
  if (!sheet) {
    sheet = ss.insertSheet(INVITES_SHEET);
    sheet.appendRow(["inviteId", "inviterUserId", "inviteeEmail", "tokenHash", "status", "createdAtUtc", "expiresAtUtc", "acceptedAtUtc"]);
  }
  
  return sheet;
}

function getCapsuleSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(CAPSULES_SHEET);
  
  if (!sheet) {
    sheet = ss.insertSheet(CAPSULES_SHEET);
    sheet.appendRow([
      "id", "owner", "ownerUserId", "unlockTimeEpoch", "state", 
      "ciphertextBase64", "ivBase64", "saltBase64", 
      "headline", "createdAtEpoch", "updatedAtEpoch"
    ]);
  }
  
  return sheet;
}

function getRecipientsSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(RECIPIENTS_SHEET);
  
  if (!sheet) {
    sheet = ss.insertSheet(RECIPIENTS_SHEET);
    sheet.appendRow([
      "capsuleId", "recipientEmail", "recipientUserId", 
      "notifyOnCreate", "notifyOnUnlock", "deliveryStatus", 
      "openedAtUtc", "createdAtUtc"
    ]);
  }
  
  return sheet;
}

function getAuditSheet() {
  var ss = SpreadsheetApp.openByUrl(getSpreadsheetUrl());
  var sheet = ss.getSheetByName(AUDIT_SHEET);
  
  if (!sheet) {
    sheet = ss.insertSheet(AUDIT_SHEET);
    sheet.appendRow(["timestampEpoch", "action", "entityId", "oldState", "newState", "requestId"]);
  }
  
  return sheet;
}

function getSpreadsheetUrl() {
  var files = DriveApp.getFilesByName(SPREADSHEET_NAME);
  
  if (files.hasNext()) {
    return files.next().getUrl();
  }
  
  var ss = SpreadsheetApp.create(SPREADSHEET_NAME);
  Logger.log("Created spreadsheet: " + ss.getUrl());
  return ss.getUrl();
}

function logAudit(action, entityId, oldState, newState, requestId) {
  var sheet = getAuditSheet();
  sheet.appendRow([Date.now(), action, entityId, oldState, newState, requestId]);
}

function getUserById(userId) {
  var sheet = getUsersSheet();
  var data = sheet.getDataRange().getValues();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === userId) {
      return {
        userId: data[i][0],
        email: data[i][1],
        displayName: data[i][2]
      };
    }
  }
  return null;
}

function getUserByEmail(email) {
  var sheet = getUsersSheet();
  var data = sheet.getDataRange().getValues();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][1] === email) {
      return {
        userId: data[i][0],
        email: data[i][1],
        displayName: data[i][2]
      };
    }
  }
  return null;
}

function getUserEmailById(userId) {
  var user = getUserById(userId);
  return user ? user.email : null;
}

function getUserDisplayNameById(userId) {
  var user = getUserById(userId);
  return user ? (user.displayName || user.email) : null;
}

function checkIsRecipient(capsuleId, userId, email) {
  var sheet = getRecipientsSheet();
  var data = sheet.getDataRange().getValues();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === capsuleId) {
      if (data[i][2] === userId || data[i][1] === email) {
        return true;
      }
    }
  }
  return false;
}

function updatePendingRecipients(email, userId) {
  var sheet = getRecipientsSheet();
  var data = sheet.getDataRange().getValues();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][1] === email && !data[i][2]) {
      sheet.getRange(i + 1, 3).setValue(userId);
    }
  }
}

function generateSecureToken() {
  var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
  var token = '';
  for (var i = 0; i < 32; i++) {
    token += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return token;
}

function hashToken(token) {
  var rawHash = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, token);
  var hash = '';
  for (var i = 0; i < rawHash.length; i++) {
    var byte = rawHash[i];
    if (byte < 0) byte += 256;
    var hex = byte.toString(16);
    if (hex.length === 1) hex = '0' + hex;
    hash += hex;
  }
  return hash;
}

function jsonResponse(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

function handleInviteAcceptPage(token) {
  var html = "<html><head><title>Accept TimeCapsule Invite</title></head>" +
             "<body style='font-family:sans-serif;max-width:400px;margin:50px auto;text-align:center;'>" +
             "<h1>Welcome to TimeCapsule!</h1>" +
             "<p>You've been invited to join TimeCapsule.</p>" +
             "<p>Please open the TimeCapsule app and enter this token to complete signup:</p>" +
             "<code style='display:block;padding:10px;background:#f0f0f0;border-radius:4px;'>" + token + "</code>" +
             "</body></html>";
  return HtmlService.createHtmlOutput(html);
}
