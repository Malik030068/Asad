<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../storage.php';
require_once __DIR__ . '/../auth.php';

set_json_headers();

$action = $_GET['action'] ?? '';
$input = json_decode(file_get_contents('php://input'), true) ?? [];

switch ($action) {
    case 'login':
        $username = trim($input['username'] ?? '');
        $password = trim($input['password'] ?? '');

        if (empty($username) || empty($password)) {
            json_response(['error' => 'Username and password are required'], 400);
        }

        try {
            $authData = AuthService::authenticate($username, $password);
            if ($authData) {
                json_response([
                    'success' => true,
                    'token' => $authData['token'],
                    'user' => $authData['user']
                ]);
            } else {
                json_response(['error' => 'Invalid username or password'], 401);
            }
        } catch (Exception $e) {
            json_response(['error' => $e->getMessage()], 403);
        }
        break;

    case 'register':
        $fullName = trim($input['fullName'] ?? '');
        $username = trim($input['username'] ?? '');
        $emailOrPhone = trim($input['emailOrPhone'] ?? '');
        $password = trim($input['password'] ?? '');
        $terms = $input['termsAccepted'] ?? false;

        if (!$terms) {
            json_response(['error' => 'Terms and conditions must be accepted'], 400);
        }

        if (empty($fullName) || empty($username) || empty($emailOrPhone) || strlen($password) < 4) {
            json_response(['error' => 'Please fill all fields properly (min 4 char password)'], 400);
        }

        $users = JsonStorage::read('users', []);
        foreach ($users as $u) {
            if (strcasecmp($u['username'], $username) === 0) {
                json_response(['error' => 'Username already exists'], 409);
            }
        }

        $newUser = [
            'id' => 'user_' . bin2hex(random_bytes(4)),
            'username' => $username,
            'fullName' => $fullName,
            'emailOrPhone' => $emailOrPhone,
            'passwordHash' => password_hash($password, PASSWORD_BCRYPT),
            'role' => 'USER',
            'status' => 'PENDING',
            'subscriptionPlanId' => 'plan_free',
            'mustChangePassword' => false,
            'createdAt' => time()
        ];

        $users[] = $newUser;
        JsonStorage::write('users', $users);

        JsonStorage::appendLog('audit_logs', [
            'actor' => $username,
            'action' => 'REGISTRATION',
            'details' => 'New user registered with PENDING status.'
        ]);

        unset($newUser['passwordHash']);
        json_response([
            'success' => true,
            'message' => 'Registration submitted. Awaiting administrator approval.',
            'user' => $newUser
        ], 201);
        break;

    case 'change_password':
        $user = AuthService::getSessionUser();
        if (!$user) {
            json_response(['error' => 'Unauthorized'], 401);
        }

        $newPassword = trim($input['newPassword'] ?? '');
        if (strlen($newPassword) < 4) {
            json_response(['error' => 'Password must be at least 4 characters'], 400);
        }

        $users = JsonStorage::read('users', []);
        foreach ($users as &$u) {
            if ($u['id'] === $user['id']) {
                $u['passwordHash'] = password_hash($newPassword, PASSWORD_BCRYPT);
                $u['mustChangePassword'] = false;
                break;
            }
        }
        JsonStorage::write('users', $users);

        JsonStorage::appendLog('audit_logs', [
            'actor' => $user['username'],
            'action' => 'PASSWORD_CHANGE',
            'details' => 'User changed password.'
        ]);

        json_response(['success' => true, 'message' => 'Password updated successfully']);
        break;

    case 'me':
        $user = AuthService::getSessionUser();
        if (!$user) {
            json_response(['error' => 'Not authenticated'], 401);
        }
        json_response(['user' => $user]);
        break;

    default:
        json_response(['error' => 'Invalid action'], 400);
}
