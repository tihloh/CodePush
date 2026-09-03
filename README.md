# CodePush

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
