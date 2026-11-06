# Firebase Setup Guide

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Enter project name: `ludo-game` (or your choice)
4. Disable Google Analytics (optional for this project)
5. Click **"Create project"**

## Step 2: Add Android App

1. In Firebase Console, click **"Add app"** → **Android**
2. Enter package name: `com.ludo.game`
3. Enter app nickname: `Ludo Game`
4. Click **"Register app"**

## Step 3: Download Config File

1. Download `google-services.json`
2. Place it in: `app/google-services.json`

```
ludo/
├── app/
│   ├── google-services.json  ← Place here
│   ├── build.gradle.kts
│   └── src/
```

## Step 4: Enable Authentication

1. Go to **Authentication** → **Sign-in method**
2. Enable **Anonymous** sign-in
3. Click **Save**

## Step 5: Create Realtime Database

1. Go to **Realtime Database** → **Create Database**
2. Select location closest to your users
3. Start in **Test mode** (we'll add rules later)
4. Click **Enable**

## Step 6: Set Database Rules

Go to **Realtime Database** → **Rules** and paste:

```json
{
  "rules": {
    "rooms": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "gameStates": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null && (
          !data.exists() || 
          data.child('currentTurnPlayerId').val() === auth.uid
        )"
      }
    },
    "roomCodes": {
      ".read": "auth != null",
      "$code": {
        ".write": "auth != null && !data.exists()"
      }
    }
  }
}
```

Click **Publish**.

## Step 7: Verify Setup

Your `google-services.json` should look like:

```json
{
  "project_info": {
    "project_number": "123456789",
    "project_id": "your-project-id",
    "storage_bucket": "your-project-id.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789:android:abc123",
        "android_client_info": {
          "package_name": "com.ludo.game"
        }
      },
      ...
    }
  ]
}
```

## Step 8: Build & Run

1. Open project in Android Studio
2. Sync Gradle files
3. Run on device/emulator
4. Check Firebase Console → Authentication for new anonymous user

## Troubleshooting

### Error: "Default FirebaseApp is not initialized"
- Ensure `google-services.json` is in `app/` folder
- Verify package name matches in Firebase Console

### Error: "Permission denied"
- Check Realtime Database rules
- Verify Authentication is enabled

### Error: "Network error"
- Check internet connection
- Verify Firebase project is not on Spark plan limits

## Database Structure

```
├── rooms/
│   └── {roomId}/
│       ├── roomCode: "ABC123"
│       ├── hostId: "user123"
│       ├── status: "WAITING"
│       └── players/
│           └── {playerId}/
│               ├── name: "Player 1"
│               ├── color: "RED"
│               └── isOnline: true
│
├── gameStates/
│   └── {roomId}/
│       ├── currentTurnPlayerId: "user123"
│       ├── diceValue: 6
│       ├── canRollDice: false
│       └── tokens/
│           └── {playerId}/
│               └── {tokenId}/
│                   ├── position: 12
│                   └── state: "ON_BOARD"
│
└── roomCodes/
    └── ABC123: "{roomId}"
```

## Next Steps

After Firebase setup:
1. Build the APK: `./gradlew assembleDebug`
2. Install on device
3. Create a room and share the code!
