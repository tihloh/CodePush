# CodePush for PHP

`Tihloh\CodePush` provides small, framework-independent helpers for integrating the CodePush Android scanner with a PHP application.

It intentionally does **not** create routes, databases, users, or authentication. CodePush can send scans to any configured GET or POST endpoint, so this package focuses on two reusable jobs:

- generate valid `codepush.server` setup JSON for QR enrollment;
- read incoming scan data consistently from GET, JSON POST, or ordinary POST requests.

The Android app remains the protocol authority. Application-specific meaning stays in your own endpoint/controller.

## Install

During development from the CodePush source repository:

```json
{
  "repositories": [
    {
      "type": "path",
      "url": "/path/to/CodePush/packages/php"
    }
  ],
  "require": {
    "tihloh/codepush": "dev-main"
  }
}
```

After the distribution repository is released through Packagist:

```bash
composer require tihloh/codepush
```

Requires PHP 8.1+.

## Generate a CodePush setup QR payload

```php
<?php
require __DIR__ . '/vendor/autoload.php';

use Tihloh\CodePush\CodePush;

$config = CodePush::server(
    name: 'Albay Budget',
    url: 'https://example.com/budget',
    method: 'GET'
)->fields([
    'ext' => 'scannedcode',
    'user_id' => '25',
    'codevalue' => '{scan}',
    'extra' => 'encodeq',
]);

$json = $config->toJson();
```

The result follows the setup format understood by CodePush 1.6.0+:

```json
{
  "type": "codepush.server",
  "version": 1,
  "server": {
    "name": "Albay Budget",
    "method": "GET",
    "url": "https://example.com/budget",
    "fields": [
      {"name":"ext","value":"scannedcode"},
      {"name":"user_id","value":"25"},
      {"name":"codevalue","value":"{scan}"},
      {"name":"extra","value":"encodeq"}
    ]
  }
}
```

A new `ServerConfig` defaults to CodePush's standard `value={scan}` field. Calling `fields([...])` replaces that default. Use `withoutFields()` when you deliberately want an explicit empty field list.

Optional response checks are supported:

```php
$config = $config->responseRule(
    type: 'JSON',
    selector: 'success',
    operator: 'EQUALS',
    expected: 'true',
    followUpServerName: 'Notify'
);
```

Supported types are `AUTO`, `TEXT`, and `JSON`. Supported operators are `EXISTS`, `EQUALS`, and `CONTAINS`.

## Receive a scan

CodePush does not require a special fixed endpoint path. Point a saved CodePush server configuration to any route in your application.

For the default `value={scan}` field:

```php
use Tihloh\CodePush\CodePush;

$request = CodePush::request();
$scan = $request->scan();
```

For a custom field such as `codevalue={scan}`:

```php
$scan = CodePush::request()->scan('codevalue');
```

You can also access all configured dynamic values:

```php
$request = CodePush::request();
$userId = $request->input('user_id');
$extra = $request->input('extra');
$data = $request->all();
```

`CodePush::request()` reads GET parameters for GET requests, JSON bodies for POST-style requests, and falls back to ordinary form POST data.

## Framework integration

If your framework has already parsed the request body, pass the array directly:

```php
$request = CodePush::request($requestBody, 'POST');
$scan = $request->scan('codevalue');
```

This package does not decide what a scanned value means. Routing a document, finding an OBR, recording attendance, updating inventory, or any other action belongs to your application.

## Development

```bash
composer install
composer check
```

The source of truth lives in `tihloh/CodePush/packages/php`. The `tihloh/codepush-php` repository is intended only as the Composer distribution mirror for Packagist.
