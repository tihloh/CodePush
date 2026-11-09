# CodePush

Quick server setup: encode the JSON in `server/setup-qr.json` into a QR, scan it, and review/save the imported server. See `server/SETUP-QR.md` for the template and optional response checks.

A native Android scanner that sends QR/barcode contents to configurable HTTP endpoints.

## Features

- Continuous on-device scanning for QR, Data Matrix, PDF417, Aztec, UPC/EAN, Code 39/93/128, ITF, and Codabar.
- Remembers the last scan for the current app session and silently ignores an identical next scan. Restarting the app or tapping **Reset** clears this memory.
- Stores multiple GET and POST server configurations locally on the device.
- Supports `{variable}` and `{{variable}}` placeholders in URLs.
- Dynamic fields fill matching placeholders; unused fields become GET query parameters or a POST JSON body.
- `{scan}` can be used inside field values. `value={scan}` is included in a new server by default.
- Optional plain-text or JSON response checks (`exists`, `equals`, `contains`).
- A matching response can trigger one follow-up HTTP request to another saved server.
- No accounts, authentication, analytics, or cloud persistence.

## Build

Open the project in a current Android Studio release, let Gradle sync, then run the `app` configuration on an Android 6.0+ device. Camera hardware is optional at install time but required to scan.

The app permits both HTTPS and cleartext HTTP because local/LAN servers are a core use case. For production deployments, consider replacing `usesCleartextTraffic="true"` with a restricted Network Security Configuration.

## Request examples

For `GET https://example.test/items/{location}` with fields `location=warehouse-a` and `value={scan}`, a scan of `ABC123` sends:

```text
GET https://example.test/items/warehouse-a?value=ABC123
```

For `POST https://example.test/scan` with fields `code={scan}` and `station=front-desk`, it sends:

```json
{"code":"ABC123","station":"front-desk"}
```

## Stable Android releases

CodePush publishes **stable, versioned release APKs only** using
`.github/workflows/android-release.yml`. Debug APKs and prereleases are not
published by this workflow.

Each new stable release must increment **both** `versionCode` and
`versionName` in `app/build.gradle.kts`. A push to `main` that changes
Android application/build files runs tests, builds a signed release APK, and
publishes `v<versionName>` with these assets:

- `CodePush-<versionName>.apk`
- `CodePush-<versionName>.apk.sha256`

Existing versions are never overwritten. If the release tag already exists,
the workflow skips publishing until the app version is incremented.

### Required one-time GitHub Actions secrets

Configure these under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `CODEPUSH_KEYSTORE_BASE64` | Base64-encoded stable Android release keystore (entire JKS/PKCS12 file, without line breaks) |
| `CODEPUSH_KEYSTORE_PASSWORD` | Keystore password |
| `CODEPUSH_KEY_ALIAS` | Alias of the signing key |
| `CODEPUSH_KEY_PASSWORD` | Signing key password |

For example, to encode an *existing* release keystore on Linux:
`base64 -w 0 release.jks`.
On Windows PowerShell:
`[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))`.

**Keep the original keystore and passwords backed up securely.** Do not
commit them to Git. Never replace the signing key between releases: Android
requires the same application ID and compatible signing identity to update
an existing installation without uninstalling it. An existing debug-signed
installation generally cannot be updated in place with the new
release-signed APK; preserve/export configurations before switching.

If signing secrets are missing, the build fails rather than publishing
an unsigned or debug-signed artifact. The app's **About → App updates**
checks the latest stable release, ignoring prereleases.
