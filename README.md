# BARBER & BEYOND Manager — Android v1.3 upgrade

**Offline Android app** powered by ELGON LABS, using Kotlin, Jetpack Compose, and Room/SQLite.

## What changed
- Editable **Services** catalogue. Tap **Load suggested studio prices** once to import a starter list; review each price before real use. Services are saved as normal Room records (`type=Services`) and included in v1.3 JSON backups.
- **Sell** next to any service pre-fills a sales entry; amount stays editable.
- **Appointments** can now have a specific date, time, service, and status (Booked/Completed/Cancelled), stored safely in existing records. Older appointments remain visible as *legacy bookings* and can be edited to add a proper schedule. No notifications or SMS are sent.
- Dashboard shows upcoming bookings and catalogue count.
- Official full logo, launcher icon, gold loading indicator, reports and export/restore are retained.

## Backup compatibility and safety
- **Before switching**: in v1.2 go to Reports > Export JSON and save at least two private copies.
- v1.3 installs separately and does **not** read v1.2 local records automatically. Use Reports > Restore JSON, which **replaces** any current v1.3 records only after confirmation.
- v1.3 JSON backups use schema 2 to include Services, and the v1.3 restore can read either schema 1 (v1.1/v1.2) or schema 2. Old versions may reject schema 2 backups. Export your records before uninstalling any app.
- JSON backups are **unencrypted**, protect them like business data.

## APK builds / signing

The `Build Android APK` workflow runs automatically on pushes to `main` and creates a **PREVIEW DEBUG APK**, installed with app ID `ke.elgonlabs.barberbeyond.manager.preview`. Do **not** depend on preview builds for important records. GitHub Actions debug signing keys are not stable across runners.

For a proper updateable main app, use the separate **manual** `Build signed Barber Beyond release APK` workflow. It produces the app ID `ke.elgonlabs.barberbeyond.manager` and requires FOUR GitHub Actions repository secrets:

- `BB_RELEASE_KEYSTORE_B64`: base64-encoded bytes of a private, locally generated PKCS12 keystore (one line)
- `BB_RELEASE_STORE_PASSWORD`: keystore password
- `BB_RELEASE_KEY_ALIAS`: alias of your key (e.g. `barber-beyond`)
- `BB_RELEASE_KEY_PASSWORD`: private-key password (often same as keystore password for PKCS12)

Generate your signing key **privately on a trusted device**; do NOT commit, post, or share the key, base64 text, password, or secrets. Keep an offline secure backup of the keystore and passwords — losing these means you may not be able to update your signed app. Configure secrets at GitHub repository **Settings → Secrets and variables → Actions → New repository secret**. Only then manually dispatch the signed-release workflow under Actions. Do not install the preview in place of the signed app: they intentionally have different IDs and databases.

A release signed with the *same* keystore and *same* app ID can be installed as an update when `versionCode` increases in later versions. The previously installed v1.2 APK cannot be upgraded in place with this new key; migrate records via JSON instead.

Do NOT enable automatic signed releases until secrets are configured. This sample requires direct user setup for signing and has not been compiled/tested yet.
