# 🕰️ TimeCapsule

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-667eea?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17-007396?style=for-the-badge)
![Google Apps Script](https://img.shields.io/badge/Backend-Google%20Apps%20Script-4285F4?style=for-the-badge&logo=google&logoColor=white)
![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM-34C759?style=for-the-badge&logo=lock&logoColor=white)

### **Write a message today. Unlock it in the future.**

*A beautiful, secure, time-locked messaging application with client-side encryption and server-enforced time locks.*

[Features](#-features) • [How It Works](#-how-it-works) • [Installation](#-installation) • [Usage](#-usage) • [Security](#-security) • [Tech Stack](#-tech-stack)

</div>

---

## 📸 Screenshots

### Main Interface
```
┌──────────────────────────────────────────────────────────────┐
│  ⏳ TimeCapsule                              ⓘ  [user@email] │
│     Seal your memories in time                               │
├──────────────────────────────────────────────────────────────┤
│  🔒 Your Capsules                                            │
│  ┌────────────┬─────────────────┬─────────┬──────────┐       │
│  │ Title      │ Unlock Date     │ Status  │ Time Left│       │
│  ├────────────┼─────────────────┼─────────┼──────────┤       │
│  │ Birthday   │ Dec 25, 2025    │ SEALED  │ 14d 6h   │       │
│  │ New Year   │ Jan 01, 2026    │ SEALED  │ 21d 5h   │       │
│  │ Old Memory │ Dec 01, 2024    │ OPENED  │ ✓ Ready! │       │
│  └────────────┴─────────────────┴─────────┴──────────┘       │
│                                                              │
│     [ Refresh ]    [ + New Capsule ]    [ Open Selected ]    │
│                                                              │
│  ✓ Loaded 3 capsule(s)                                       │
└──────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

### 🔐 Military-Grade Encryption
- **AES-256-GCM** encryption (same as used by banks and governments)
- **PBKDF2** key derivation with 100,000 iterations
- **Client-side encryption** - your message is encrypted BEFORE leaving your computer
- **Zero-knowledge backend** - server never sees your plaintext or passphrase

### ⏰ Tamper-Proof Time Lock
- **Server-enforced time** - Google's cloud servers control when capsules can be opened
- **Immune to clock manipulation** - changing your local time does NOTHING
- **Trusted time authority** - uses `Date.now()` from Google's infrastructure

### 🎨 Premium iOS-Inspired UI
- **Dark mode** with elegant gradients
- **Fluid animations** - hover effects, scale transitions, entrance animations
- **Slot-machine date picker** - Apple-style spinners with scroll wheel support
- **Custom SVG icons** - hand-crafted vector graphics
- **Modern message cards** - beautiful reveal animations when opening capsules

### 📱 User Experience
- **One-click refresh** - instantly sync with server
- **Visual status indicators** - colored badges for sealed/opened states
- **Live countdown** - see exactly how long until unlock
- **Info tooltips** - click ⓘ to learn how everything works

---

## 🔄 How It Works

### The Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           USER'S COMPUTER                               │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                        TimeCapsule App                           │   │
│  │                                                                  │   │
│  │   1. User writes message + sets unlock date + creates passphrase │   │
│  │                              ↓                                   │   │
│  │   2. PBKDF2 derives 256-bit key from passphrase + random salt   │   │
│  │                              ↓                                   │   │
│  │   3. AES-GCM encrypts message with key + random IV              │   │
│  │                              ↓                                   │   │
│  │   4. Only encrypted blob sent to server (plaintext stays local) │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    │ HTTPS (encrypted payload only)     │
└────────────────────────────────────┼────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          GOOGLE CLOUD                                   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Google Apps Script                            │   │
│  │                                                                  │   │
│  │   • Stores encrypted data in Google Sheets                      │   │
│  │   • On open request: checks Date.now() >= unlockTimeEpoch       │   │
│  │   • If too early: returns "Not Yet!" (no data)                  │   │
│  │   • If time passed: returns encrypted blob for client decrypt   │   │
│  │                                                                  │   │
│  │   ⚠️ Server NEVER has: plaintext, passphrase, or decryption key │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Creating a Capsule

1. **Write your message** - anything you want your future self to read
2. **Set unlock date** - use the slot-machine picker to choose when
3. **Create a passphrase** - this NEVER leaves your computer
4. **Click "Seal Capsule"** - message is encrypted locally, then sent to server

### Opening a Capsule

1. **Select the capsule** in the table
2. **Click "Open Selected"**
3. **Server checks time**:
   - ❌ Too early → "Not Yet!" message
   - ✅ Time passed → Returns encrypted data
4. **Enter your passphrase** - decrypts locally on your computer
5. **Read your message!** 🎉

---

## 🚀 Installation

### Prerequisites

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Internet connection** (for server communication)
- **Windows/macOS/Linux** supported

### Quick Start

```bash
# 1. Clone or download the repository
git clone https://github.com/yourusername/TimeCapsule.git
cd TimeCapsule

# 2. Run with Maven Wrapper (no Maven installation needed!)
# Windows:
.\mvnw.cmd javafx:run

# macOS/Linux:
./mvnw javafx:run
```

That's it! The app will compile and launch automatically.

### Alternative: Using Gradle

```bash
# If you prefer Gradle:
./gradlew run
```

---

## 📖 Usage

### Main Window

| Element | Description |
|---------|-------------|
| **⏳ Logo** | Animated pulsing glow effect |
| **ⓘ Info Button** | Click to learn how time-locking works |
| **User Badge** | Shows current configured email |
| **Capsules Table** | Lists all your capsules with status |
| **Refresh Button** | Sync capsules from server |
| **+ New Capsule** | Create a new time-locked message |
| **Open Selected** | Decrypt and read a ready capsule |
| **Status Bar** | Shows current operation status |

### Creating a New Capsule

1. Click **"+ New Capsule"**
2. Fill in the form:
   - **Title** (optional): A name for your capsule
   - **Message**: Your secret message
   - **Unlock Date**: Use spinners or scroll wheel
   - **Passphrase**: Create a strong passphrase
   - **Confirm**: Re-enter passphrase
3. Click **"🔒 Seal Capsule"**

> ⚠️ **IMPORTANT**: Remember your passphrase! It cannot be recovered.

### Opening a Capsule

1. Select a capsule from the table
2. Check the "Time Left" column:
   - If it shows time remaining → Cannot open yet
   - If it shows "✓ Ready!" → Can be opened
3. Click **"Open Selected"**
4. Enter your passphrase
5. Your message appears in a beautiful card!

### What If I Try to Open Early?

You'll see this message:

```
⏰ Not Yet!

This capsule is still time-locked.

The SERVER checks the time, not your computer.
Even if you change your local clock, it won't help!

Please wait until the unlock time passes.
```

---

## 🔒 Security

### Cryptography Details

| Component | Algorithm | Details |
|-----------|-----------|---------|
| **Key Derivation** | PBKDF2WithHmacSHA256 | 100,000 iterations, 16-byte random salt |
| **Encryption** | AES/GCM/NoPadding | 256-bit key, 12-byte random IV |
| **Authentication** | GCM Auth Tag | Prevents tampering, includes email+timestamp as AAD |
| **Encoding** | Base64 | For safe transmission |

### What the Server Stores

```json
{
  "id": "TC-1234567890",
  "owner": "user@example.com",
  "headline": "My Birthday Message",
  "unlockTimeEpoch": 1735689600000,
  "state": "sealed",
  "ciphertextBase64": "encrypted_blob...",
  "ivBase64": "random_iv...",
  "saltBase64": "random_salt..."
}
```

The server NEVER stores:
- ❌ Your plaintext message
- ❌ Your passphrase
- ❌ Your decryption key

### Can I Cheat by Changing My Clock?

**NO.** Here's why:

1. Your computer sends an "open" request to the server
2. Server checks **its own clock**: `Date.now()` (Google's time)
3. Server compares: `serverTime >= unlockTimeEpoch`?
4. If false → Returns "notYet" (no encrypted data given)
5. Your local clock is completely ignored

Even if you:
- Change your system time → Server doesn't care
- Send a fake timestamp → Server ignores it
- Modify the client code → Can't bypass server check

The unlock time is enforced by **Google's servers**, not your computer.

### Trust Model

| Party | Trusts | For |
|-------|--------|-----|
| User | Client app | Correct encryption implementation |
| User | Google | Accurate time, not reading encrypted data |
| Server | Client | Valid public fields (email, unlock time) |
| Server | User | Nothing sensitive (all crypto is client-side) |

---

## 🛠️ Tech Stack

### Frontend (JavaFX Desktop App)

| Technology | Purpose |
|------------|---------|
| **Java 17+** | Core application language |
| **JavaFX 17** | Modern UI framework |
| **Gson 2.10** | JSON serialization |
| **java.security** | Cryptography (built-in) |

### Backend (Google Apps Script)

| Technology | Purpose |
|------------|---------|
| **Google Apps Script** | Serverless JavaScript runtime |
| **Google Sheets** | Database for capsule metadata |
| **LockService** | Prevents race conditions |
| **UrlFetchApp** | (Optional) Webhook notifications |

### Architecture

```
TimeCapsule/
├── pom.xml                              # Maven build config
├── build.gradle                         # Gradle build config (alternative)
├── mvnw.cmd / mvnw                      # Maven wrapper scripts
├── TimeCapsuleService.gs                # Backend code (copy to Apps Script)
├── README.md                            # Quick start guide
├── REPO.md                              # This file - full documentation
│
└── src/main/java/timecapsule/
    ├── ui/
    │   └── TimeCapsuleApp.java          # Main app + all UI code
    ├── crypto/
    │   └── CryptoUtils.java             # PBKDF2 + AES-GCM
    ├── api/
    │   └── ApiClient.java               # Async HTTP client
    └── model/
        ├── Capsule.java                 # Data model
        └── ApiResponse.java             # Response wrapper
```

---

## ⚙️ Configuration

### Changing the Backend URL

Edit `TimeCapsuleApp.java`:

```java
private static final String BACKEND_URL = "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec";
```

### Changing the Owner Email

Edit `TimeCapsuleApp.java`:

```java
private static final String OWNER_EMAIL = "your-email@example.com";
```

### Setting Up Your Own Backend

1. **Create a Google Sheet** named `TimeCapsuleLedger`
2. **Create two sheets inside**:
   - `Capsules` with columns: `id | owner | unlockTimeEpoch | state | ciphertextBase64 | ivBase64 | saltBase64 | headline | createdAtEpoch`
   - `AuditLog` with columns: `timestampEpoch | action | capsuleId | oldState | newState | requestId`
3. **Open Apps Script** (Extensions → Apps Script)
4. **Paste** the contents of `TimeCapsuleService.gs`
5. **Deploy** → New Deployment → Web App
   - Execute as: Me
   - Who has access: Anyone
6. **Copy** the deployment URL to your client config

---

## 🐛 Known Limitations

### Backend Scaling

| Issue | Impact | Workaround |
|-------|--------|------------|
| Script lock is global | ~6 req/min under contention | Use multiple sheets for scaling |
| Full table scan for list | Slow after 1000+ capsules | Add indexing sheet |
| No pagination | Memory issues at scale | Limit capsules per user |
| Email quota | 100/day limit | Use external email service |

### Client Limitations

| Issue | Workaround |
|-------|------------|
| No offline mode | Requires internet connection |
| Single user | Edit config to switch users |
| No auto-refresh | Click Refresh manually |
| No retry logic | Retry failed operations manually |

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

## 📄 License

MIT License - Feel free to use, modify, and distribute.

---

## 💡 FAQ

### Q: What if I forget my passphrase?
**A:** Unfortunately, your message is lost forever. The server only stores encrypted data and cannot decrypt without your passphrase. This is a security feature, not a bug.

### Q: Can the server operator read my messages?
**A:** No. Messages are encrypted on your device before being sent. The server only sees encrypted blobs.

### Q: What happens to my capsules if the server goes down?
**A:** While the server is down, you cannot create new capsules or open existing ones. When it comes back, everything resumes.

### Q: Is there a limit to message length?
**A:** Google Sheets cells have a 50,000 character limit. After Base64 encoding, this means ~35KB of plaintext.

### Q: Can I change the unlock time after creating a capsule?
**A:** No. Once sealed, a capsule's unlock time is permanent.

---

<div align="center">

**Made with ❤️ for your future self**

*TimeCapsule v1.0.0 • Java 17 • JavaFX • Google Apps Script*

⏳ Write today. Read tomorrow. ⏳

</div>
