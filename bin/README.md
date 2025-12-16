# TimeCapsule

A simple "write now, read later" application that lets you create encrypted time-locked messages. Write a message today, set an unlock date, and the message can only be decrypted after that date passes according to server time.

## What It Does

1. You write a message and choose when it can be opened
2. The message is encrypted **locally** on your computer using a passphrase you choose
3. The encrypted data is stored on a Google Sheet (the server never sees your actual message)
4. When you try to open a capsule, the server checks if the unlock time has passed
5. If yes, the encrypted data is returned and you decrypt it locally with your passphrase

**Key point:** The server never sees your passphrase or your plaintext message. Encryption happens entirely on your computer.

## Architecture

```
┌─────────────────────┐         HTTPS/JSON          ┌─────────────────────┐
│   JavaFX Client     │ ◄────────────────────────►  │  Google Apps Script │
│                     │                              │    Web App          │
│  - Encrypt locally  │                              │  - Check time       │
│  - Decrypt locally  │                              │  - Store encrypted  │
│  - Show UI          │                              │    data in Sheet    │
└─────────────────────┘                              └─────────────────────┘
                                                              │
                                                              ▼
                                                     ┌─────────────────────┐
                                                     │   Google Sheet      │
                                                     │  "TimeCapsuleLedger"│
                                                     │                     │
                                                     │  - Capsules sheet   │
                                                     │  - AuditLog sheet   │
                                                     └─────────────────────┘
```

## Crypto Primitives Used

This project uses **only standard Java cryptography** - no external libraries:

- **Key Derivation:** PBKDF2WithHmacSHA256 (100,000 iterations)
  - Turns your passphrase + random salt into an AES key
  
- **Encryption:** AES/GCM/NoPadding (256-bit key)
  - Authenticated encryption that protects both confidentiality and integrity
  - Random 12-byte IV for each message

- **Associated Data:** Owner email + unlock timestamp
  - Bound to the ciphertext so the capsule can't be reassigned to a different owner or time

## Trust Model

- **Server time is the authority:** The Google Apps Script backend uses `Date.now()` to decide if a capsule can be opened. If you try to open early, you get a "not yet" response.
  
- **Server never sees plaintext:** All encryption/decryption happens in the Java client. The server only stores Base64-encoded ciphertext, IV, and salt.

- **Passphrase stays local:** Your passphrase is never sent to the server. Without it, the encrypted data is useless.

- **Idempotent opens:** Opening an already-opened capsule just returns the same encrypted data again. No harm in retrying.

---

## Setup Instructions

### Step 1: Create the Google Sheet

1. Go to [Google Sheets](https://sheets.google.com) and create a new spreadsheet
2. Name it exactly: **TimeCapsuleLedger**
3. Rename the first sheet tab to **Capsules**
4. Add these column headers in row 1:
   ```
   id | owner | unlockTimeEpoch | state | ciphertextBase64 | ivBase64 | saltBase64 | headline | createdAtEpoch
   ```
5. Create a second sheet tab named **AuditLog**
6. Add these column headers in row 1:
   ```
   timestampEpoch | action | capsuleId | oldState | newState | requestId
   ```

### Step 2: Deploy the Apps Script Backend

1. In your Google Sheet, go to **Extensions → Apps Script**
2. Delete any existing code in `Code.gs`
3. Copy the entire contents of `TimeCapsuleService.gs` and paste it in
4. Save the project (Ctrl+S) - name it "TimeCapsule Backend"
5. Click **Deploy → New Deployment**
6. Click the gear icon next to "Select type" and choose **Web app**
7. Set these options:
   - Description: "TimeCapsule API v1"
   - Execute as: **Me**
   - Who has access: **Anyone** (for testing) or **Anyone with Google Account**
8. Click **Deploy**
9. Click **Authorize access** and follow the prompts
10. **Copy the Web App URL** - you'll need this for the Java client

### Step 3: (Optional) Set Up Automatic Processing

If you want capsules to automatically become "opened" when their time passes:

1. In the Apps Script editor, run the `installTrigger` function once
2. This creates an hourly trigger that marks due capsules as opened
3. Owners will receive email notifications when their capsules are ready

### Step 4: Configure the Java Client

1. Open `src/main/java/timecapsule/ui/TimeCapsuleApp.java`
2. Find these lines near the top and update them:
   ```java
   private static final String BACKEND_URL = "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec";
   private static final String OWNER_EMAIL = "your.email@example.com";
   ```
3. Replace with your actual Web App URL and email

### Step 5: Build and Run

**With Maven:**
```bash
cd TimeCapsule
mvn clean javafx:run
```

**Or build a JAR:**
```bash
mvn clean package
java -jar target/timecapsule-1.0-SNAPSHOT.jar
```

---

## Quick Manual Test

1. **Create a test capsule:**
   - Click "New Capsule"
   - Enter a message like "Hello, future me!"
   - Set the unlock time to 2-3 minutes from now
   - Enter a passphrase (remember it!)
   - Click "Save & Seal"

2. **Try to open early:**
   - Click "Refresh" to see your new capsule (state: "sealed")
   - Select it and click "Open Selected"
   - Enter your passphrase
   - You should see "Not Yet!" - the server rejected the early open

3. **Wait for unlock time:**
   - Watch the "Time Remaining" column
   - When it shows "Ready!", click "Open Selected" again
   - Enter your passphrase
   - Your decrypted message appears!

4. **Check the Google Sheet:**
   - Open your TimeCapsuleLedger spreadsheet
   - You'll see your capsule row with state changed to "opened"
   - Check AuditLog for the CREATE and OPEN entries

---

## Project Structure

```
TimeCapsule/
├── pom.xml                          # Maven build configuration
├── TimeCapsuleService.gs            # Google Apps Script (copy to Apps Script)
├── README.md                        # This file
└── src/main/java/timecapsule/
    ├── model/
    │   ├── Capsule.java             # Data model for capsule
    │   └── ApiResponse.java         # Response wrapper
    ├── crypto/
    │   └── CryptoUtils.java         # PBKDF2 + AES-GCM encryption
    ├── api/
    │   └── ApiClient.java           # HTTP client for backend
    └── ui/
        └── TimeCapsuleApp.java      # JavaFX main application
```

## Troubleshooting

**"No capsules loaded":**
- Make sure BACKEND_URL is correct (the full URL from deployment)
- Make sure OWNER_EMAIL matches an email you used to create capsules

**Network errors:**
- Check your internet connection
- Make sure the Apps Script is deployed and accessible
- Try the deployment URL in your browser - you should see `{"status":"ok","message":"TimeCapsule API is running"}`

**Decryption failed:**
- Double-check you're using the exact same passphrase
- Passphrases are case-sensitive

**Apps Script errors:**
- Check the Apps Script execution log: Extensions → Apps Script → Executions
- Make sure the spreadsheet exists and has the right sheet names

---

## Design Notes for Instructors

This project demonstrates:

- **Standard Java crypto APIs:** `javax.crypto.*` for PBKDF2 and AES-GCM
- **Async programming:** `CompletableFuture` for background HTTP calls
- **JavaFX fundamentals:** TableView, dialogs, Platform.runLater for UI updates
- **Clean separation:** Model/API/Crypto/UI packages
- **Basic security:** Client-side encryption, server just tracks time and stores blobs

The crypto is intentionally simple and uses only built-in Java primitives. No external crypto libraries are required.
