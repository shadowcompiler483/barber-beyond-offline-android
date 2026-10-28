# BARBER & BEYOND Manager — Offline Android (starter)

An offline-first **native Android** starter for Android 11+ (API 30), built with Kotlin, Jetpack Compose and Room/SQLite. The database is created on the phone; the app has no internet permission and does not connect to Supabase or Vercel.

## Included
- Black-and-gold ELGON LABS branding
- Local records for sales, expenses, customers and appointments
- Cash/M-Pesa labels (manual entry only; no payment verification)
- Dashboard and all-time profit calculations
- Local record deletion
- No online account or login

## Build
1. Open this folder in Android Studio (with Android SDK 35 and JDK 17 installed).
2. Allow Gradle to sync (requires internet once to fetch dependencies).
3. Build > Build APK(s). Install the debug APK on your Android 11+ phone.
4. Turn off mobile data/Wi-Fi and test recording and reopening entries.

## Important limitations
This is an **initial functional source project**, not a verified compiled APK. The build has not been tested in this environment because Android SDK/Gradle are unavailable. Do not use it as your sole business ledger yet.

Not yet implemented: encrypted PIN, edit existing records, robust date-filtered reports, CSV/JSON backup and restore, automatic backups, appointment reminders, multi-device transfer, and migration from Supabase. Deleting a record is immediate. Keep using the existing online app until the offline app has been built, tested, and a reliable backup workflow is added.

Records reside in the app's private local SQLite database. Uninstalling or clearing app data destroys them. Backups are deliberately disabled at the Android manifest level until a tested explicit export/import feature is added.
