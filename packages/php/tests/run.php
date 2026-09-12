<?php
require dirname(__DIR__) . '/vendor/autoload.php';

use Tihloh\CodePush\CodePush;
use Tihloh\CodePush\ScanRequest;

function expect(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

$config = CodePush::server('Budget', 'https://example.test/scan', 'POST');
$data = $config->toArray();
expect($data['type'] === 'codepush.server', 'Invalid setup type.');
expect($data['version'] === 1, 'Invalid setup version.');
expect($data['server']['method'] === 'POST', 'Invalid method.');
expect($data['server']['fields'] === [['name' => 'value', 'value' => '{scan}']], 'Default scan field mismatch.');

$config = $config->fields([
    'user_id' => '25',
    'codevalue' => '{scan}',
]);
$data = $config->toArray();
expect(count($data['server']['fields']) === 2, 'Configured fields were not replaced.');
expect($data['server']['fields'][1]['name'] === 'codevalue', 'Configured field name mismatch.');

$config = $config->responseRule('JSON', 'success', 'EQUALS', 'true', 'Notify');
$data = $config->toArray();
expect($data['server']['responseRule']['enabled'] === true, 'Response rule should be enabled.');
expect($data['server']['responseRule']['followUpServerName'] === 'Notify', 'Follow-up server mismatch.');

$request = new ScanRequest(['codevalue' => 'ABC123', 'user_id' => '25'], 'POST');
expect($request->scan('codevalue') === 'ABC123', 'Scan value mismatch.');
expect($request->input('user_id') === '25', 'Dynamic input mismatch.');
expect($request->method() === 'POST', 'Request method mismatch.');

$json = $config->toJson();
expect(json_decode($json, true, 512, JSON_THROW_ON_ERROR)['type'] === 'codepush.server', 'Generated JSON is invalid.');

echo "CodePush PHP tests passed.\n";
