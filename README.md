# Barber & Beyond Manager — Offline Android v1.4

**TILLERN LABS** • Black-and-gold, offline-first business manager for Android 11+.

## v1.4: streamlined studio operations

- Redesigned **Dashboard** with the studio logo, a dark gold-accented overview, current sales, and a compact, usable mobile layout.
- Every displayed action is wired to a real interaction: **New sale** opens the sale editor; **Expense** opens the expense editor; **Service prices** opens the catalogue; **Business reports** opens the date-filtered reports; **Export backup** opens Android's Save File dialog.
- Main navigation: **Dashboard / Sales / Expenses / Services / Reports**. All tabs work and show their corresponding data.
- Removed **Customers** and **Bookings** from the screens, editors, navigation, recent activity, and dashboard calculations. The historical records are NOT deleted; legacy categories remain accepted in JSON backup/restore so existing backups cannot silently lose data. Old customer/booking records are no longer editable in the app.
- Existing sales, expenses, services, cash/M-Pesa labels, search, edit, delete confirmation, and JSON backup/restore remain.
- The official Barber & Beyond Hair Studio splash logo remains unchanged.

## App identities and safe data migration

- v1.3 preview was a separate app. **Do not uninstall it or clear its storage.**
- v1.4 debug preview uses `ke.elgonlabs.barberbeyond.manager.preview14`. It installs alongside the v1.3 preview, with an independent blank local database, so experimentation cannot overwrite current records.
- Signed v1.4 release uses **`ke.elgonlabs.barberbeyond.manager`**, which will remain the stable application ID for future signed releases. Version code is 5. Only future builds with the **same permanent key** and increasing versionCode can update it in place.
- Before migrating records: export JSON from the installed app, keep two secure copies, then use the new app's Reports → Restore JSON. **Restoring replaces all records** after confirmation, including historical types hidden by this UI. Check totals and counts before uninstalling any prior app.
- The app does not have Internet permission. JSON exports are unencrypted; keep them private.

## Build from GitHub Actions

1. Upload this overlay on top of your existing GitHub repo, preserving paths.
2. **Build Android APK** runs automatically on push to `main`. Its ZIP artifact is `barber-beyond-v1-4-PREVIEW-apk` and contains a separate **testing** APK.
3. For the stable main app, visit Actions → **Build signed Barber Beyond release APK** → Run workflow. When signing succeeds, the artifact is `barber-beyond-v1-4-SIGNED-release-apk`, containing `app-release.apk`.
4. Check build logs before installing anything. This is source code; APK compilation has **not** been verified for v1.4 at the time of packaging.

## Signing: root-cause diagnostics and safe compatibility conversion

Build #3/#4 of v1.3 failed `TEST A` because Java could not read the `.p12` using GitHub secrets. The v1.4 signed workflow:

1. Validates that all four `BB_RELEASE_*` secrets are populated and the keystore decodes.
2. Uses OpenSSL to verify whether the actual keystore can be unlocked with the configured **store password** (`TEST A`). If this fails, recheck the Base64 and password in GitHub. **Do not regenerate the key.**
3. Only after TEST A passes, converts the **same existing key/certificate** to a Java-compatible PKCS12 encoding temporarily on the GitHub Actions runner and checks that Java can read its alias and private key (`TEST B`). This is not a new signing identity.
4. Signs with the converted file and verifies the final APK signature. Signing material is never added to the repository or uploaded as an artifact. Temporary unencrypted PEM material exists only briefly on the isolated runner and is deleted after the conversion.

See `SIGNING-SETUP.md` for phone-based steps. **Never share the `.p12`, secret values, decoded private key, or passwords in a chat or public issue.** The v1.3 preview has a different application ID, and cannot be updated in place by the signed release.

## Limitations

- Still a small offline manager, not a replacement for formal accounting software.
- Offline records are private to each app installation. No M-Pesa API connection, verified payment processing, or cloud sync.
- Continue to save JSON backups regularly; clearing app storage or uninstalling an installation can erase its local records.
