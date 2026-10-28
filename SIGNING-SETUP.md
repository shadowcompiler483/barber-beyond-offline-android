# Barber & Beyond stable APK signing — phone/Acode guide

**Do this only after you have pushed the v1.3 source to GitHub and verified the preview APK compiles.**

A GitHub Actions debug APK is signed with a temporary debug key that can change on every runner. This project creates a separate debug *preview* app and a permanent *signed release* app. To update signed release installs in future, you must preserve the **exact same signing keystore, passwords and app ID**, and increase `versionCode` on each new release.

## 1. Generate a private signing key locally on your Android phone

Inside **Acode Alpine terminal**, install Java's `keytool` (if not installed):

```sh
apk add openjdk17-jdk
```

Create a **private** location in the Acode terminal home (do not use the public Git repository):

```sh
mkdir -p "$HOME/.barber-signing"
chmod 700 "$HOME/.barber-signing"
keytool -genkeypair -v \
  -keystore "$HOME/.barber-signing/barber-beyond-release.p12" \
  -storetype PKCS12 -alias barber-beyond \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -dname 'CN=Barber and Beyond, O=ELGON LABS, C=KE'
```

Choose a strong unique keystore password when prompted. **Do not send your keystore, secret text or passwords to ChatGPT or post them in GitHub files.** For PKCS12, the key password normally matches the keystore password.

## 2. Back up the keystore PRIVATELY

Keep a secure copy of `barber-beyond-release.p12` and the password somewhere other than the phone. This is the signing identity for all future releases. If you lose it, you may lose the ability to deliver in-place updates to the signed app. A private encrypted backup (not the public repo) is strongly recommended. Never commit the key, even if it is base64-encoded.

## 3. Configure GitHub Actions secrets

Open your repo in Chrome: **Settings → Secrets and variables → Actions → New repository secret**. Add four secrets:

- `BB_RELEASE_KEYSTORE_B64` = the **one-line base64** version of the `.p12` keystore, copied privately from your local terminal command below.
- `BB_RELEASE_STORE_PASSWORD` = keystore password.
- `BB_RELEASE_KEY_ALIAS` = `barber-beyond`.
- `BB_RELEASE_KEY_PASSWORD` = private key password (normally same as store password for PKCS12).

Display the keystore's base64 text only when you are ready to copy it into GitHub's secret form:

```sh
base64 -w 0 "$HOME/.barber-signing/barber-beyond-release.p12"
```

Your device screen and clipboard will contain the secret during copying. Do this somewhere private, avoid screenshots, then clear the clipboard. **Base64 does not encrypt the keystore.**

## 4. Generate your first stable signed APK

GitHub → Actions → **Build signed Barber Beyond release APK** → **Run workflow**. When green, download the `barber-beyond-v1-3-SIGNED-release-apk` artifact ZIP and extract/install `app-release.apk`. The stable app ID is `ke.elgonlabs.barberbeyond.manager`.

The auto-build under **Build Android APK** creates `barber-beyond-v1-3-PREVIEW-apk` with app ID `ke.elgonlabs.barberbeyond.manager.preview`. That separate APK is for testing only; it will not update the signed release and its data is isolated.

**Before switching from v1.2:** export its JSON backup, save a spare copy, then use v1.3 Reports → Restore JSON in the new signed app, which replaces existing records after asking for confirmation. Keep v1.2 installed until the restored counts match.
