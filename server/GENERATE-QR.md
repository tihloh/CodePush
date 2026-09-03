# Dynamic QR generation

Upload only `generate-qr.php`. It has no hardcoded server configuration.

Supply a complete CodePush setup JSON object on each request:

- POST with `Content-Type: application/json` and the JSON body.
- POST form field `json` containing the JSON string (recommended for opening a result in the browser).
- GET `generate-qr.php?json=URL_ENCODED_JSON` for convenient links/iframes. GET values may appear in browser history and server logs; prefer POST for user-specific configuration.

The response is an HTML page that automatically renders the supplied data as a QR in the browser, with a PNG download button. It is not a server-side PNG API; JavaScript must be enabled. A GET without data shows an empty editor. Invalid data returns HTTP 400 and a JSON error. No data is saved.

Example from your existing PHP page, where `$dynamicJson` is generated from current server/user data:

```php
<form method="post" action="generate-qr.php" target="_blank">
    <input type="hidden" name="json"
           value="<?= htmlspecialchars($dynamicJson, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8') ?>">
    <button type="submit">Generate setup QR</button>
</form>
```

If your data is a PHP array, first use `$dynamicJson = json_encode($yourData, JSON_THROW_ON_ERROR);`.
