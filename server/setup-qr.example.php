<?php
// Encode this endpoint's JSON response as the QR text, not the endpoint URL.
// Replace these sample values with your own server-side configuration.
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
echo json_encode([
    'type' => 'codepush.server',
    'version' => 1,
    'server' => [
        'name' => 'Budget',
        'method' => 'POST',
        'url' => 'https://your-server.com/scan_processor.php',
        'fields' => [
            ['name' => 'user_id', 'value' => '42'],
            ['name' => 'codevalue', 'value' => '{scan}'],
            ['name' => 'extra', 'value' => 'receive'],
        ],
    ],
], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
