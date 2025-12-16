# TimeCapsule

A desktop application for creating **time-locked encrypted messages**. Write a message today, lock it until a future date, and share it with friends—no one can read it until the time arrives.

---

## What It Does

TimeCapsule allows users to:

1. **Create encrypted time capsules** — Write a message, set an unlock date, and protect it with a passphrase
2. **Send capsules to friends** — Share capsules with other users who can only open them after the unlock time
3. **Manage a friends list** — Search for users, send friend requests, or invite people via email
4. **Receive and open capsules** — View capsules sent to you and decrypt them when ready

**Key concept:** Even the sender cannot open a capsule early. The server enforces the time lock.

---

## How to Run

### Prerequisites
- **Java 17** or higher installed
- **Internet connection** (required for backend communication)

### Steps

1. **Open a terminal** in the project folder (`TimeCapsule/`)

2. **Run the application:**
   ```bash
   # Windows
   .\mvnw.cmd javafx:run
   
   # Mac/Linux
   ./mvnw javafx:run
   ```

3. **Create an account** — Enter your email, display name, and password

4. **Start using the app:**
   - Click **"+ New Capsule"** to create a time capsule
   - Go to **"Friends"** to add friends
   - Check **"Inbox"** to see capsules sent to you

---

## Testing the Application

### Quick Test (2 minutes)

1. Create an account and log in
2. Click **"+ New Capsule"**
3. Enter:
   - **Headline:** "Test Capsule"
   - **Message:** Any text
   - **Unlock Date:** Set to 1-2 minutes from now (in UTC)
   - **Passphrase:** Any passphrase (remember it!)
4. Click **"Seal & Send Capsule"**
5. Go to **"My Capsules"** — you'll see the capsule is locked
6. Wait until the unlock time passes
7. Click **"Open"** and enter your passphrase
8. The decrypted message appears

### Testing with a Friend

1. Both users create accounts
2. User A goes to **Friends** → searches for User B's email → sends friend request
3. User B accepts the friend request
4. User A creates a capsule and adds User B as a recipient
5. User B sees the capsule in their **Inbox** when unlock time arrives

---

## Advanced Features

### 1. Client-Side Encryption (AES-256-GCM)

All messages are encrypted **on your computer** before being sent to the server. The server never sees your plaintext message or passphrase.

**Implementation:** `src/main/java/timecapsule/crypto/CryptoUtils.java`

```
Passphrase → PBKDF2 (100,000 iterations) → 256-bit AES Key
Message → AES-GCM Encryption → Ciphertext (stored on server)
```

- **PBKDF2** derives a secure key from the user's passphrase
- **AES-GCM** provides both encryption and integrity verification
- **Associated Authenticated Data (AAD)** binds the ciphertext to the owner and unlock time

### 2. Server-Enforced Time Lock

The unlock time is verified by Google's servers, not the client's clock. Users cannot bypass the time lock by changing their local time.

**Backend:** Google Apps Script + Google Sheets

### 3. Asynchronous API Communication

The UI never freezes during network operations. All API calls use Java's `CompletableFuture` for non-blocking requests.

**Implementation:** `src/main/java/timecapsule/api/ApiClient.java`

### 4. Friends System

- Search for existing users by email
- Send/accept/decline friend requests
- Invite non-users via email
- Send capsules to multiple friends at once

### 5. Surprise Mode

When enabled, recipients don't know they received a capsule until it's ready to open. They get notified only at unlock time, not when it's sent.

---

## Project Structure

```
TimeCapsule/
├── pom.xml                           # Maven configuration
├── mvnw.cmd                          # Maven wrapper (Windows)
├── mvnw                              # Maven wrapper (Mac/Linux)
│
└── src/main/java/timecapsule/
    ├── ui/                           # User Interface (JavaFX)
    │   ├── TimeCapsuleApp.java       # Main application
    │   ├── LoginScreen.java          # Login/Register screen
    │   ├── ComposeCapsuleScreen.java # Create new capsules
    │   ├── SentCapsulesScreen.java   # View sent capsules
    │   ├── ReceivedCapsulesScreen.java # Inbox
    │   └── FriendsScreen.java        # Friends management
    │
    ├── crypto/
    │   └── CryptoUtils.java          # AES-GCM encryption
    │
    ├── api/
    │   └── ApiClient.java            # HTTP client for backend
    │
    └── model/                        # Data models
        ├── Capsule.java
        ├── User.java
        ├── Friend.java
        └── ...
```

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Frontend | JavaFX 17 |
| Language | Java 17 |
| Build Tool | Maven |
| Backend | Google Apps Script |
| Database | Google Sheets |
| Encryption | PBKDF2 + AES-GCM |

---

## Security Summary

| Feature | Implementation |
|---------|----------------|
| Message encryption | AES-256-GCM (client-side) |
| Key derivation | PBKDF2 with 100,000 iterations |
| Time verification | Server-side (Google Apps Script) |
| Password storage | SHA-256 hashed before transmission |
| Transport security | HTTPS |

**Zero-knowledge design:** The server stores only encrypted data. It cannot read messages or recover passphrases.

---

## Troubleshooting

**"Cannot find symbol" errors:**
```bash
.\mvnw.cmd clean compile
```

**Application won't start:**
- Ensure Java 17+ is installed: `java -version`
- Ensure you have internet connectivity

**Login fails:**
- Check your internet connection
- The backend may take a few seconds on first request

---

## Author

Ashwin Sharma  , Aarya Shah