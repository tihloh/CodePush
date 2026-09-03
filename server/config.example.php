<?php

$mysqli = new mysqli(
    'localhost',       // database host
    'database_user',   // database username
    'database_pass',   // database password
    'database_name'    // database name
);

if ($mysqli->connect_error) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database connection failed']);
    exit;
}

