# BARBER & BEYOND Manager — Offline Android v1.1 (testing)

Offline Android app for Android 11+ using Kotlin, Jetpack Compose, and Room/SQLite.

## v1.1 additions
- Higher-contrast white headings and an always-visible two-row navigation grid.
- Dashboard with today, seven-day and monthly profit estimates, and recent records.
- Time-filtered reports and Cash/M-Pesa sales breakdown.
- Search across names, notes and payment methods.
- Edit records, confirmation before deletion.
- Export JSON backups via Android's Save As document picker.
- Restore JSON backups via Android's file picker, with data validation and explicit replace confirmation. Restore is transactional (all or nothing).

## IMPORTANT: preserve your first app's data

The previous v1.0 APK was debug-signed on an ephemeral GitHub Actions runner; its signing key is not available. A newly-built APK may have a different signature. Trying to replace v1.0 may fail; uninstalling v1.0 would erase its records because Android backup was disabled and the app had no export feature.

**To protect the existing v1.0 records**, v1.1 deliberately has a DIFFERENT Android application ID (`ke.elgonlabs.barberbeyond.v11`) and a distinct launcher name. It installs beside the original app; it does **not** automatically import old records. Keep the original installed until you have verified all records have been transferred manually or by an authorized ADB export. Do not uninstall or clear data on the original app.

Debug builds from different GitHub runs may also have changing signing keys. Before relying on future in-place app updates, set up a stable private Android signing key (GitHub Actions secrets) or export a backup and restore after reinstalling. **Do not place a signing keystore or passwords in this public repository.**

## Backup security

Backups are portable plain JSON. They are NOT encrypted; protect them like business records. Save a backup in two safe locations, check it can be read, and keep it private. Restore REPLACES all v1.1 records with the JSON records after confirmation; it does not merge.

The app has **no internet permission** and does not connect to Supabase, Vercel or M-Pesa systems. Figures reflect manually entered values, not verified payments or complete accounting.

## Build

Push to GitHub main. GitHub Actions builds `app-debug.apk`, available as the artifact `barber-beyond-v1-1-debug-apk` under Actions > run > Artifacts. This is a development/testing build, not a signed production release.
