<?php
/**
 * Rasta AI - Installation Wizard
 */

define('INSTALL_LOCK_FILE', __DIR__ . '/data/installed.lock');

$isLocked = file_exists(INSTALL_LOCK_FILE);
$phpVersionOk = version_compare(PHP_VERSION, '8.0.0', '>=');
$curlOk = extension_loaded('curl');
$jsonOk = extension_loaded('json');
$mbstringOk = extension_loaded('mbstring');
$dataWritable = is_writable(__DIR__) || (file_exists(__DIR__ . '/data') && is_writable(__DIR__ . '/data'));

$message = '';
$installedSuccess = false;

if ($_SERVER['REQUEST_METHOD'] === 'POST' && !$isLocked) {
    if (!$phpVersionOk || !$curlOk || !$jsonOk) {
        $message = "Cannot install: Please ensure all server requirements are satisfied.";
    } else {
        require_once __DIR__ . '/storage.php';
        require_once __DIR__ . '/auth.php';

        AuthService::initBootstrap();

        // Write installation lock file with unique secret
        $secret = bin2hex(random_bytes(24));
        file_put_contents(INSTALL_LOCK_FILE, json_encode([
            'installed_at' => date('c'),
            'secret' => $secret,
            'version' => '1.0.0'
        ]));

        $installedSuccess = true;
        $isLocked = true;
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rasta AI - Backend Installation Wizard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #0B0F19; color: #F1F5F9; font-family: system-ui, -apple-system, sans-serif; }
        .card { background: #111827; border: 1px solid #1E293B; border-radius: 1rem; }
        .badge-ok { background: #059669; }
        .badge-err { background: #DC2626; }
        .btn-cyan { background: #00E5FF; color: #082F49; font-weight: 600; border: none; }
        .btn-cyan:hover { background: #38BDF8; color: #082F49; }
    </style>
</head>
<body class="py-5">
<div class="container" style="max-width: 650px;">
    <div class="text-center mb-4">
        <h1 class="fw-bold" style="letter-spacing: 2px; color: #00E5FF;">RASTA AI</h1>
        <p class="text-secondary">Backend Installation & Configuration Wizard</p>
    </div>

    <div class="card p-4 shadow-lg">
        <?php if ($installedSuccess): ?>
            <div class="alert alert-success">
                <h5 class="fw-bold">✓ Installation Complete!</h5>
                <p>JSON storage initialized and Administrator <strong>Malik</strong> created with initial password <strong>asad</strong>.</p>
                <p class="mb-0">You can now access the Admin Dashboard or link the Android application.</p>
            </div>
            <a href="admin/index.html" class="btn btn-cyan w-100 py-2">Open Web Administrator Portal</a>
        <?php elseif ($isLocked): ?>
            <div class="alert alert-info">
                <h5 class="fw-bold">System Already Installed & Locked</h5>
                <p class="mb-0">Rasta AI backend is active and secured. To reinstall, delete <code>backend/data/installed.lock</code>.</p>
            </div>
            <a href="admin/index.html" class="btn btn-cyan w-100 py-2">Proceed to Admin Dashboard</a>
        <?php else: ?>
            <h5 class="mb-3 fw-bold">System Readiness Checks</h5>
            <ul class="list-group list-group-flush mb-4">
                <li class="list-group-item bg-transparent text-light d-flex justify-content-between align-items-center">
                    PHP Version 8.0+ (Current: <?= PHP_VERSION ?>)
                    <span class="badge <?= $phpVersionOk ? 'badge-ok' : 'badge-err' ?>"><?= $phpVersionOk ? 'Pass' : 'Fail' ?></span>
                </li>
                <li class="list-group-item bg-transparent text-light d-flex justify-content-between align-items-center">
                    cURL Extension
                    <span class="badge <?= $curlOk ? 'badge-ok' : 'badge-err' ?>"><?= $curlOk ? 'Pass' : 'Fail' ?></span>
                </li>
                <li class="list-group-item bg-transparent text-light d-flex justify-content-between align-items-center">
                    JSON Extension
                    <span class="badge <?= $jsonOk ? 'badge-ok' : 'badge-err' ?>"><?= $jsonOk ? 'Pass' : 'Fail' ?></span>
                </li>
                <li class="list-group-item bg-transparent text-light d-flex justify-content-between align-items-center">
                    Storage Directory Permissions
                    <span class="badge <?= $dataWritable ? 'badge-ok' : 'badge-err' ?>"><?= $dataWritable ? 'Writable' : 'Read-only' ?></span>
                </li>
            </ul>

            <div class="card bg-dark border-secondary p-3 mb-4">
                <h6 class="fw-bold text-info">Default Initial Credentials</h6>
                <p class="small text-secondary mb-1">Username: <strong>Malik</strong></p>
                <p class="small text-secondary mb-0">Initial Password: <strong>asad</strong> (Will be prompted to change upon first login)</p>
            </div>

            <form method="POST">
                <button type="submit" class="btn btn-cyan w-100 py-2">Complete Backend Installation</button>
            </form>
        <?php endif; ?>
    </div>
</div>
</body>
</html>
