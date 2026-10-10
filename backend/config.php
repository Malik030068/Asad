<?php
/**
 * Rasta AI - System Configuration
 */

define('RASTA_VERSION', '1.0.0');
define('DATA_DIR', __DIR__ . '/data');
define('ALLOW_REGISTRATION', true);
define('TOKEN_EXPIRY_SECONDS', 86400 * 7); // 7 days

// Ensure headers
function set_json_headers() {
    header('Content-Type: application/json; charset=utf-8');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
    if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        http_response_code(200);
        exit;
    }
}

function json_response($data, $status = 200) {
    http_response_code($status);
    echo json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}
