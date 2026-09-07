# OTP Bridge (Android/Kotlin)

Dedicated Android relay for the OTP Broker MVP.

## What it does

1. Receives an SMS on a company-managed Android phone.
2. Checks the sender against an exact allowlist.
3. Extracts the last standalone 4–8 digit number as the OTP.
4. Stores only sender + OTP in Android-Keystore-encrypted temporary storage while upload is pending (max 2 minutes).
5. Sends **only** `OTP <code>` plus sender/recipient/device metadata to the broker — never the full SMS body.
6. Broker accepts it only when there is a matching `WAITING` OTP request.

Broker endpoint used:

`POST /v1/ingest/message`

Header:

`X-Source-Key: <SOURCE_API_KEY>`

Payload shape:

```json
{
  "recipient": "+9665XXXXXXXX",
  "sender": "MUQEEM",
  "message": "OTP 483921",
  "source": "android:OTP-PHONE-01"
}
```

## Android 17 / API 37 note

Android 17 adds stronger protection around OTP-containing SMS. Ordinary background SMS receivers can experience delayed OTP delivery. On a **dedicated company phone**, OTP Bridge contains the manifest components needed to request Android's SMS role. Selecting OTP Bridge as the default SMS app is the intended unattended mode for devices where normal `SMS_RECEIVED` delivery is restricted.

Do not use this app to relay personal MFA messages or accounts you are not authorized to automate.

## Build

Recommended: Android Studio Quail 4 (2026.1.4 or newer).

The project uses:

- Android Gradle Plugin 9.4.0
- compileSdk 37
- targetSdk 37
- minSdk 26
- framework Android APIs only; no third-party runtime libraries

Open the folder in Android Studio, allow Gradle sync, then choose **Build > Build APK(s)**.

CLI if Gradle 9.6+ is installed:

```bash
gradle :app:assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

A GitHub Actions workflow is included at `.github/workflows/build-apk.yml` and uploads the debug APK as an artifact.

## Local logic test

```bash
./run-core-tests.sh
```

## First setup on the phone

1. Install the APK on a dedicated company Android phone.
2. Open OTP Bridge.
3. Grant SMS permission.
4. On Android 17+ (or if SMS OTPs are not received), select **Use OTP Bridge as default SMS app**.
5. Enter broker HTTPS URL.
6. Enter the same `SOURCE_API_KEY` configured in the OTP Broker.
7. Set a stable Device ID, e.g. `OTP-PHONE-01`.
8. Set recipient hint exactly as UiPath uses when creating broker requests, or leave it blank on both sides.
9. Add exact sender IDs, one per line.
10. Enable **Forward approved OTPs** and save.
11. Tap **Test broker connection**.

## Security choices

- Release build refuses cleartext HTTP at Android network-security level.
- API key is encrypted using Android Keystore AES/GCM.
- Full SMS body is not sent to the broker.
- Pending OTP retry payload is encrypted and expires after 2 minutes.
- No OTP value is displayed in event history.
- Sender allowlist uses exact case-insensitive matching.
- Broker remains the final gate: without a matching active request, the OTP is rejected.
