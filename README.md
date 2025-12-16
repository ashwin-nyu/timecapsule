# ⏳ TimeCapsule

<div align="center">

![TimeCapsule](https://img.shields.io/badge/TimeCapsule-v1.0-667eea?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-17-007396?style=for-the-badge)
![Google Apps Script](https://img.shields.io/badge/Apps%20Script-Backend-4285F4?style=for-the-badge&logo=google)

**Seal your memories in time. Write a message today, unlock it in the future.**

</div>

---

## ✨ What to Expect

TimeCapsule is a beautiful, iOS-inspired desktop application that lets you create encrypted time-locked messages. Here's what you'll experience:

### 🎨 Premium UI/UX
- **Dark Mode Interface** - Sleek, modern design with glassmorphism effects
- **Fluid Animations** - Smooth hover effects, scale transitions, and entrance animations
- **Custom SVG Icons** - Hand-crafted vector icons for all actions
- **Responsive Layout** - Adapts beautifully to different window sizes

### 🔐 Security Features
- **Client-Side Encryption** - Your message never leaves your computer unencrypted
- **Zero-Knowledge Backend** - Server only stores encrypted blobs, never sees your data
- **Passphrase Protection** - Each capsule is locked with your unique passphrase

### ⏰ Time-Lock Mechanism
- **Server-Verified Time** - Google's servers determine when capsules can be opened
- **Countdown Display** - See exactly how long until your capsule unlocks
- **Email Notifications** - Get notified when your capsule is ready (optional)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER'S COMPUTER                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              JavaFX Desktop Application                  │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │    │
│  │  │    UI    │  │  Crypto  │  │   API    │  │  Model  │ │    │
│  │  │ Package  │  │ Package  │  │ Package  │  │ Package │ │    │
│  │  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │    │
│  │       │              │              │                   │    │
│  │       └──────────────┴──────────────┘                   │    │
│  │                      │                                   │    │
│  │           ┌──────────▼──────────┐                       │    │
│  │           │  PBKDF2 + AES-GCM   │ ◄── Encryption        │    │
│  │           │   (Client-Side)     │     happens here!     │    │
│  │           └──────────┬──────────┘                       │    │
│  └──────────────────────┼──────────────────────────────────┘    │
│                         │ HTTPS (encrypted payload)              │
└─────────────────────────┼───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     GOOGLE CLOUD                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Google Apps Script Web App                  │    │
│  │                                                          │    │
│  │  • doPost() - Handle API requests                       │    │
│  │  • LockService - Prevent race conditions                │    │
│  │  • Date.now() - Trusted time authority                  │    │
│  │  • processDueCapsules() - Background trigger            │    │
│  │                                                          │    │
│  └────────────────────────┬────────────────────────────────┘    │
│                           │                                      │
│  ┌────────────────────────▼────────────────────────────────┐    │
│  │              Google Sheets Database                      │    │
│  │                                                          │    │
│  │  📋 Capsules Sheet                                       │    │
│  │  ├── id, owner, unlockTimeEpoch, state                  │    │
│  │  ├── ciphertextBase64, ivBase64, saltBase64             │    │
│  │  └── headline, createdAtEpoch                           │    │
│  │                                                          │    │
│  │  📋 AuditLog Sheet                                       │    │
│  │  └── timestampEpoch, action, capsuleId, states...       │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Frontend** | JavaFX 17 | Desktop UI with animations |
| **Language** | Java 17+ | Main application logic |
| **Build Tool** | Maven 3.9 | Dependency management |
| **Backend** | Google Apps Script | Serverless API |
| **Database** | Google Sheets | Capsule storage |
| **Encryption** | PBKDF2 + AES-GCM | Client-side crypto |
| **JSON** | Gson 2.10 | Serialization |

---

## 🔒 Cryptography

### Key Derivation
```
Passphrase → PBKDF2WithHmacSHA256 (100,000 iterations) → 256-bit AES Key
                    ↑
              Random Salt (16 bytes)
```

### Encryption
```
Plaintext → AES/GCM/NoPadding → Ciphertext + Auth Tag
                 ↑
          Random IV (12 bytes)
          Associated Data (email + timestamp)
```

### Trust Model
- ✅ Server **NEVER** sees plaintext or passphrase
- ✅ Server time (`Date.now()`) controls unlock
- ✅ Capsule data bound to owner via AAD
- ✅ Idempotent opens (safe to retry)

---

## ⚠️ Known Issues & Scaling Considerations

### Backend Bugs / Limitations

| # | Issue | Description | Impact |
|---|-------|-------------|--------|
| 1 | **Script Lock Contention** | `LockService.getScriptLock()` is script-wide, not per-capsule. High concurrent requests will serialize, causing 10s timeouts. | 🔴 High - Limits to ~6 req/min under contention |
| 2 | **No Rate Limiting** | No request throttling per user/IP. Malicious actors can spam create requests filling the sheet. | 🔴 High - DoS vulnerability |
| 3 | **Full Table Scan** | `handleList()` and `handleOpen()` iterate all rows. O(n) complexity degrades with capsule count. | 🟡 Medium - Slow after 1000+ capsules |
| 4 | **No Pagination** | List returns ALL capsules for an owner. Large responses may timeout or fail. | 🟡 Medium - Memory issues at scale |
| 5 | **Email Quota** | `MailApp.sendEmail()` has daily quota limits (100/day for free). Bulk notifications will fail silently. | 🟡 Medium - Notifications may not send |

### Client Bugs / Improvements Needed

| # | Issue | Description |
|---|-------|-------------|
| 1 | **No Retry Logic** | Network failures don't auto-retry, user must manually refresh |
| 2 | **No Offline Mode** | App is unusable without internet connection |
| 3 | **Time Sync** | Client shows "Ready!" based on local time, but server may disagree |
| 4 | **No Auto-Refresh** | User must manually click Refresh to update table |
| 5 | **Single User** | Email is hardcoded, no login/account switching |

---

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Internet connection
- Google Account (for backend)

### 1. Clone & Configure

```bash
# The app is already configured with:
BACKEND_URL = "https://script.google.com/macros/s/AKfycby.../exec"
OWNER_EMAIL = "sharmaashwin4000@gmail.com"
```

### 2. Run the Application

```bash
cd TimeCapsule
.\mvnw.cmd javafx:run    # Windows
./mvnw javafx:run        # Mac/Linux
```

### 3. Create Your First Capsule

1. Click **"+ New Capsule"**
2. Enter a title and message
3. Set unlock date/time (must be in future)
4. Create a passphrase (remember it!)
5. Click **"Seal Capsule"**

### 4. Try Opening Early

- Select your capsule and click **"Open Selected"**
- You'll see "Not Yet!" - the server rejected it

### 5. Wait & Open

- When "Time Left" shows "Ready!", try again
- Enter your passphrase to decrypt

---

## 📁 Project Structure

```
TimeCapsule/
├── pom.xml                          # Maven config
├── build.gradle                     # Gradle config (alternative)
├── mvnw.cmd                         # Maven wrapper (Windows)
├── README.md                        # This file
├── TimeCapsuleService.gs            # Backend (copy to Apps Script)
│
└── src/main/java/timecapsule/
    ├── ui/
    │   └── TimeCapsuleApp.java      # Main app with iOS-style UI
    ├── crypto/
    │   └── CryptoUtils.java         # PBKDF2 + AES-GCM
    ├── api/
    │   └── ApiClient.java           # Async HTTP client
    └── model/
        ├── Capsule.java             # Data model
        └── ApiResponse.java         # Response wrapper
```

---

## 🎨 UI Features

### Custom SVG Icons
- **Refresh** - Circular sync arrows
- **Add** - Rounded plus sign
- **Unlock** - Open padlock
- **Capsule** - Hourglass/capsule hybrid
- **Sealed** - Closed padlock

### Animations
- Entrance fade + slide
- Button hover scale (1.03x)
- Button press shrink (0.95x)
- Icon glow pulse
- Row hover highlight

### Color Palette
| Color | Hex | Usage |
|-------|-----|-------|
| Primary Start | `#667eea` | Gradient buttons |
| Primary End | `#764ba2` | Gradient buttons |
| Success | `#34C759` | Open status, unlock button |
| Warning | `#FF9500` | Sealed status |
| Background | `#1C1C1E` | Main background |
| Text Primary | `#FFFFFF` | Main text |
| Text Secondary | `#8E8E93` | Labels, hints |

---

## 🔧 Backend Setup (If Making Your Own)

### 1. Create Google Sheet
Name: `TimeCapsuleLedger`

**Capsules sheet columns:**
```
id | owner | unlockTimeEpoch | state | ciphertextBase64 | ivBase64 | saltBase64 | headline | createdAtEpoch
```

**AuditLog sheet columns:**
```
timestampEpoch | action | capsuleId | oldState | newState | requestId
```

### 2. Deploy Apps Script
1. Extensions → Apps Script
2. Paste `TimeCapsuleService.gs`
3. Deploy → New Deployment → Web App
4. Execute as: Me, Access: Anyone
5. Copy deployment URL

### 3. Install Trigger (Optional)
Run `installTrigger()` once to enable hourly auto-processing of due capsules.

---

## 📝 License

MIT License - Feel free to modify and use for educational purposes.

---

<div align="center">

**Made with ❤️ for the future you**

*TimeCapsule v1.0 • Java 17 • JavaFX • Google Apps Script*

</div>
