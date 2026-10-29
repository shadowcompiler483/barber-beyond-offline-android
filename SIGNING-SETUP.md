# Barber & Beyond: permanent APK signing (v1.4)

The signed Android app uses `ke.elgonlabs.barberbeyond.manager` (versionCode 5), independent of debug previews. Preserve its package ID and the SAME private signing key for all future in-place releases.

## Existing private key

Your OpenSSL-created key is at `$HOME/.barber-signing/barber-beyond-release.p12` in the Acode Alpine environment where it was generated. **Keep this original keystore. Do not regenerate it to fix a failed build.** Save a protected copy in a second location before changing anything. Do not commit it to GitHub or copy it into a publicly shared directory.

## Four GitHub Secrets

GitHub repository → Settings → Secrets and variables → Actions:

- `BB_RELEASE_KEYSTORE_B64`: full, single-line base64 output of the **original** `.p12` (`base64 -w 0 "$HOME/.barber-signing/barber-beyond-release.p12"`)
- `BB_RELEASE_STORE_PASSWORD`: original OpenSSL **Export Password** for the `.p12`
- `BB_RELEASE_KEY_ALIAS`: `barber-beyond`
- `BB_RELEASE_KEY_PASSWORD`: same Export Password for the PKCS12 key

The earlier encrypted PEM passphrase is not necessarily the export password. Never post these values or their encoded form in ChatGPT, screenshots, issues, or code. Base64 is **not encryption**.

## What the v1.4 signed workflow checks

`TEST A`: Can OpenSSL open the uploaded keystore using the GitHub store-password secret?

- If FAILED: wrong/mismatched password, Base64 truncation, or wrong `.p12` file. Verify the original locally with `openssl pkcs12 -info -noout -in "$HOME/.barber-signing/barber-beyond-release.p12"` and update the two relevant GitHub secrets privately. Do not regenerate a different key.
- If PASSED: the original keystore and store password are readable. The workflow temporarily repackages the SAME private key/certificate using compatibility PKCS12 algorithms for Java 17. This does not change the signing certificate.

`TEST B`: Does Java read the compatible keystore and unlock its alias/private key?

- If FAILED: check alias and `BB_RELEASE_KEY_PASSWORD`; if Java still cannot unlock it, inspect only the sanitized error without revealing secrets.
- If PASSED: Gradle signs the release APK; Android's `apksigner` then verifies it.

## Obtain the signed APK

GitHub Actions → **Build signed Barber Beyond release APK** → **Run workflow** → green result → Artifacts → `barber-beyond-v1-4-SIGNED-release-apk` → extract and install `app-release.apk`.

Do not uninstall your current v1.3 debug app. Export a JSON backup before installing v1.4; after successfully installing the signed release, open **Reports → Restore JSON** and verify records. That signed app is independent from the preview app. Future signed releases will update it only if the signing certificate and app ID remain unchanged and `versionCode` increases.
