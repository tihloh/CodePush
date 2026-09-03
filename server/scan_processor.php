<?php

header('Content-Type: application/json');

// Your existing database connection must create $mysqli.
require 'config.php';

// Accept GET, normal POST, or JSON POST.
$data = $_REQUEST;
if (str_contains($_SERVER['CONTENT_TYPE'] ?? '', 'application/json')) {
    $data = json_decode(file_get_contents('php://input'), true) ?? [];
}

$user_id   = (int) ($data['user_id'] ?? 0);
$codevalue = trim($data['codevalue'] ?? '');
$extra     = trim($data['extra'] ?? '');

if (!$user_id || $codevalue === '') {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'user_id and codevalue are required'
    ]);
    exit;
}

$scancode = $extra === '' ? $codevalue : $codevalue . '_' . $extra;

$stmt = $mysqli->prepare('UPDATE tblemps SET scancode = ? WHERE id = ?');
$stmt->bind_param('si', $scancode, $user_id);
$stmt->execute();

echo json_encode([
    'success' => true,
    'user_id' => $user_id,
    'scancode' => $scancode
]);

$stmt->close();
$mysqli->close();

