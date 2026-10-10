<?php
/**
 * Rasta AI - Authentication & Authorization Service
 */

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/storage.php';

class AuthService {

    public static function initBootstrap(): void {
        $users = JsonStorage::read('users', []);
        $adminFound = false;

        foreach ($users as $u) {
            if (strcasecmp($u['username'], 'Malik') === 0) {
                $adminFound = true;
                break;
            }
        }

        if (!$adminFound) {
            // First run: Create administrator Malik with initial password 'asad'
            $adminUser = [
                'id' => 'admin_malik',
                'username' => 'Malik',
                'fullName' => 'Malik Asad',
                'emailOrPhone' => 'admin@rasta.ai',
                'passwordHash' => password_hash('asad', PASSWORD_BCRYPT),
                'role' => 'ADMIN',
                'status' => 'APPROVED',
                'subscriptionPlanId' => 'plan_enterprise',
                'mustChangePassword' => true,
                'createdAt' => time()
            ];

            // Add demo user Shoaib
            $shoaibUser = [
                'id' => 'user_shoaib',
                'username' => 'Shoaib',
                'fullName' => 'Shoaib',
                'emailOrPhone' => 'shoaib@rasta.ai',
                'passwordHash' => password_hash('shoaib123', PASSWORD_BCRYPT),
                'role' => 'USER',
                'status' => 'APPROVED',
                'subscriptionPlanId' => 'plan_pro',
                'mustChangePassword' => false,
                'createdAt' => time()
            ];

            $users[] = $adminUser;
            $users[] = $shoaibUser;
            JsonStorage::write('users', $users);

            JsonStorage::appendLog('audit_logs', [
                'actor' => 'SYSTEM',
                'action' => 'INIT_SETUP',
                'details' => 'Initial administrator Malik and user Shoaib created.'
            ]);
        }
    }

    public static function authenticate(string $username, string $password): ?array {
        self::initBootstrap();
        $users = JsonStorage::read('users', []);

        foreach ($users as $u) {
            if (strcasecmp($u['username'], trim($username)) === 0) {
                if (password_verify($password, $u['passwordHash'])) {
                    if ($u['status'] === 'SUSPENDED') {
                        throw new Exception("Account is suspended by administrator.");
                    }
                    if ($u['status'] === 'DELETED') {
                        throw new Exception("Account has been deleted.");
                    }
                    if ($u['status'] === 'REJECTED') {
                        throw new Exception("Account registration was rejected.");
                    }

                    // Create token session
                    $token = bin2hex(random_bytes(32));
                    $sessions = JsonStorage::read('device_sessions', []);
                    $sessions[$token] = [
                        'userId' => $u['id'],
                        'username' => $u['username'],
                        'role' => $u['role'],
                        'expiresAt' => time() + TOKEN_EXPIRY_SECONDS
                    ];
                    JsonStorage::write('device_sessions', $sessions);

                    JsonStorage::appendLog('audit_logs', [
                        'actor' => $u['username'],
                        'action' => 'LOGIN_SUCCESS',
                        'details' => 'User logged in successfully.'
                    ]);

                    // Remove hash from returned data
                    unset($u['passwordHash']);
                    return [
                        'token' => $token,
                        'user' => $u
                    ];
                }
            }
        }

        JsonStorage::appendLog('security_logs', [
            'actor' => $username,
            'action' => 'LOGIN_FAILED',
            'details' => 'Invalid credentials supplied.'
        ]);

        return null;
    }

    public static function getSessionUser(): ?array {
        $headers = getallheaders();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
        $token = '';
        if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
            $token = $matches[1];
        }

        if (empty($token)) {
            return null;
        }

        $sessions = JsonStorage::read('device_sessions', []);
        if (!isset($sessions[$token])) {
            return null;
        }

        $sess = $sessions[$token];
        if (time() > $sess['expiresAt']) {
            unset($sessions[$token]);
            JsonStorage::write('device_sessions', $sessions);
            return null;
        }

        $users = JsonStorage::read('users', []);
        foreach ($users as $u) {
            if ($u['id'] === $sess['userId']) {
                unset($u['passwordHash']);
                return $u;
            }
        }

        return null;
    }

    public static function requireAdmin(): array {
        $user = self::getSessionUser();
        if (!$user || $user['role'] !== 'ADMIN') {
            json_response(['error' => 'Unauthorized: Administrator access required'], 403);
        }
        return $user;
    }

    public static function requireApprovedUser(): array {
        $user = self::getSessionUser();
        if (!$user) {
            json_response(['error' => 'Authentication required'], 401);
        }
        if ($user['status'] !== 'APPROVED') {
            json_response(['error' => 'Account is pending administrator approval'], 403);
        }
        return $user;
    }
}
