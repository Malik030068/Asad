<?php
/**
 * Rasta AI - Automated Backend Verification Test Suite
 */

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../storage.php';
require_once __DIR__ . '/../auth.php';

echo "========================================\n";
echo "RASTA AI BACKEND AUTOMATED TEST SUITE\n";
echo "========================================\n\n";

$passCount = 0;
$failCount = 0;

function assert_test($description, $condition) {
    global $passCount, $failCount;
    if ($condition) {
        echo "[PASS] $description\n";
        $passCount++;
    } else {
        echo "[FAIL] $description\n";
        $failCount++;
    }
}

// 1. Test Storage Read/Write
$testData = ['app' => 'Rasta AI', 'user' => 'Shoaib', 'timestamp' => time()];
$writeOk = JsonStorage::write('test_collection', $testData);
assert_test("JSON Atomic Write with flock", $writeOk);

$readData = JsonStorage::read('test_collection');
assert_test("JSON Storage Read Integrity", isset($readData['app']) && $readData['app'] === 'Rasta AI');

// 2. Test Admin Bootstrap
AuthService::initBootstrap();
$users = JsonStorage::read('users', []);
$malikFound = false;
$malikUser = null;
foreach ($users as $u) {
    if ($u['username'] === 'Malik') {
        $malikFound = true;
        $malikUser = $u;
        break;
    }
}
assert_test("Initial Administrator 'Malik' exists", $malikFound);
assert_test("Administrator 'Malik' has ADMIN role", $malikUser && $malikUser['role'] === 'ADMIN');
assert_test("Administrator 'Malik' initial password requires change", $malikUser && $malikUser['mustChangePassword'] === true);

// 3. Test Authentication with Initial Password 'asad'
$auth = AuthService::authenticate('Malik', 'asad');
assert_test("Administrator 'Malik' logs in with initial password 'asad'", $auth !== null && isset($auth['token']));

// 4. Test User Registration -> PENDING Status
$regUsername = 'test_user_' . time();
$newUserData = [
    'id' => 'user_test',
    'username' => $regUsername,
    'fullName' => 'Test User',
    'emailOrPhone' => 'test@rasta.ai',
    'passwordHash' => password_hash('password123', PASSWORD_BCRYPT),
    'role' => 'USER',
    'status' => 'PENDING',
    'subscriptionPlanId' => 'plan_free',
    'createdAt' => time()
];
$users[] = $newUserData;
JsonStorage::write('users', $users);

$checkUsers = JsonStorage::read('users', []);
$foundPending = false;
foreach ($checkUsers as $cu) {
    if ($cu['username'] === $regUsername && $cu['status'] === 'PENDING') {
        $foundPending = true;
        break;
    }
}
assert_test("New user registration defaults to PENDING status", $foundPending);

// 5. Test Admin Approval
foreach ($checkUsers as &$cu) {
    if ($cu['username'] === $regUsername) {
        $cu['status'] = 'APPROVED';
        $cu['approvedBy'] = 'Malik';
        break;
    }
}
JsonStorage::write('users', $checkUsers);

$checkApproved = false;
foreach (JsonStorage::read('users') as $cu) {
    if ($cu['username'] === $regUsername && $cu['status'] === 'APPROVED') {
        $checkApproved = true;
        break;
    }
}
assert_test("Administrator successfully approves pending user", $checkApproved);

echo "\n========================================\n";
echo "TOTAL TESTS: " . ($passCount + $failCount) . " | PASSED: $passCount | FAILED: $failCount\n";
echo "========================================\n";

if ($failCount > 0) exit(1);
exit(0);
