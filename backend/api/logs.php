<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../storage.php';
require_once __DIR__ . '/../auth.php';

set_json_headers();

AuthService::requireAdmin();

$type = $_GET['type'] ?? 'audit';
$collection = ($type === 'security') ? 'security_logs' : 'audit_logs';
$logs = JsonStorage::read($collection, []);

json_response(['logs' => $logs]);
