# Quick server setup

Copy `setup-qr.json`, change the URL/name/fields, and encode the JSON text into a QR code on your website. `setup-qr.example.php` produces a compact example JSON response for your existing QR generator. Neither example sends scans nor connects to the database.

In CodePush 1.6.0+, scan that QR on the normal Scan screen, review the destination, and tap Save. This works with no saved server. Saving adds a new server and selects it. Cancel changes nothing. Configuration QRs are never sent to the selected endpoint and are never automatically fetched from a URL. The same QR is ignored until a different code is scanned or Reset is pressed.

Required envelope: `type` is exactly `codepush.server`, `version` is the number `1`, and `server` is an object with a nonempty `name`, `method` (`GET` or `POST`), and full HTTP(S) `url`.

Optional `fields` is an array of string `name`/`value` pairs, exactly like the manual editor. `{scan}` in a value means the next scanned code. URL placeholders `{name}` or `{{name}}` consume the corresponding field. Unused fields become GET parameters or POST JSON fields. If omitted, fields default to `value={scan}`; an explicit empty array sends no extra fields. The name `scan` is reserved. Names must be unique.

Optional response check inside `server`:

```json
"responseRule": {
  "enabled": true,
  "type": "JSON",
  "selector": "success",
  "operator": "EQUALS",
  "expected": "true",
  "followUpServerName": "Notify"
}
```

Types: AUTO, TEXT, JSON. Operators: EXISTS, EQUALS, CONTAINS. Omit `followUpServerName` for no follow-up request. If supplied, it must match exactly one already-saved server name. Device-specific server IDs cannot be imported. With no responseRule, response checking is disabled.

Use compact JSON and short fields so the QR remains easy to read; actual QR capacity depends on encoding and error correction. Do not embed passwords or secrets. Review every setup QR before saving, especially when it points to a plain HTTP/LAN endpoint. Unknown settings and unsupported versions are rejected rather than silently changing behavior.
