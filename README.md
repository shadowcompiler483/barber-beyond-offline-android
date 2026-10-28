# BARBER & BEYOND — Official Logo Startup (v1.2 source overlay)

This patch customizes the **v1.2** opening splash screen using the official Barber & Beyond Hair Studio logo supplied by the shop owner.

## Features
- Full official gold-and-black logo (with crown, barber tools and Hair Studio branding) is shown in the opening screen.
- App launcher icon and Android 12+ native startup icon use a focused crop of the same official emblem.
- Gold spinner displays during a real local Room database readiness check, then opens the existing dashboard.
- Original offline records, JSON backup/restore, navigation and reports are unchanged.
- No internet permission was added.

## Install this source overlay using Acode
1. Extract this ZIP to a **new folder** in Acode.
2. Copy its contents (including `.github` and `app`) into the existing Git checkout at `/public/barber-beyond-repo`, preserving folder paths. (Do not delete the existing project.)
3. In Acode Alpine terminal:

   ```sh
   cd /public/barber-beyond-repo
   git add -A
   git commit -m "Use official Barber and Beyond logo on splash"
   git push origin main
   ```

4. On GitHub: Actions > Build Android APK > latest build > Artifacts > download APK ZIP if build succeeds.

**Version identity:** Same `ke.elgonlabs.barberbeyond.v12` identity as the previous, not-yet-installed v1.2 source patch. It is a separate install from v1.1 to protect the existing v1.1 records. On a first v1.2 install, open v1.1 and **Export JSON**, then install v1.2 and **Restore JSON** after checking the backup. Keep v1.1 until every record is verified in v1.2. Since GitHub Actions debug signing varies, do not uninstall a data-bearing v1.2 installation just to replace it; use an explicit backup/migration approach first.

This is an **uncompiled source patch**, not an APK; its build and launch still require testing.
