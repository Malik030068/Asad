<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../storage.php';
require_once __DIR__ . '/../auth.php';

set_json_headers();

$admin = AuthService::requireAdmin();
$action = $_GET['action'] ?? 'list';
$input = json_decode(file_get_contents('php://input'), true) ?? [];

$users = JsonStorage::read('users', []);

switch ($action) {
    case 'list':
        $sanitized = array_map(function($u) {
            unset($u['passwordHash']);
            return $u;
        }, $users);
        json_response(['users' => $sanitized]);
        break;

    case 'approve':
        $userId = $input['userId'] ?? '';
        foreach ($users as &$u) {
            if ($u['id'] === $userId) {
                $u['status'] = 'APPROVED';
                $u['approvedBy'] = $admin['username'];
                $u['approvedAt'] = time();
                JsonStorage::write('users', $users);
                JsonStorage::appendLog('audit_logs', [
                    'actor' => $admin['username'],
                    'action' => 'USER_APPROVED',
                    'details' => "Approved user {$u['username']}"
                ]);
                unset($u['passwordHash']);
                json_response(['success' => true, 'user' => $u]);
            }
        }
        json_response(['error' => 'User not found'], 404);
        break;

    case 'reject':
        $userId = $input['userId'] ?? '';
        foreach ($users as &$u) {
            if ($u['id'] === $userId) {
                $u['status'] = 'REJECTED';
                JsonStorage::write('users', $users);
                JsonStorage::appendLog('audit_logs', [
                    'actor' => $admin['username'],
                    'action' => 'USER_REJECTED',
                    'details' => "Rejected user {$u['username']}"
                ]);
                unset($u['passwordHash']);
                json_response(['success' => true, 'user' => $u]);
            }
        }
        json_response(['error' => 'User not found'], 404);
        break;

    case 'suspend':
        $userId = $input['userId'] ?? '';
        foreach ($users as &$u) {
            if ($u['id'] === $userId) {
                if ($u['role'] === 'ADMIN') {
                    json_response(['error' => 'Cannot suspend administrator'], 400);
                }
                $u['status'] = 'SUSPENDED';
                JsonStorage::write('users', $users);
                JsonStorage::appendLog('audit_logs', [
                    'actor' => $admin['username'],
                    'action' => 'USER_SUSPENDED',
                    'details' => "Suspended user {$u['username']}"
                ]);
                unset($u['passwordHash']);
                json_response(['success' => true, 'user' => $u]);
            }
        }
        json_response(['error' => 'User not found'], 404);
        break;

    case 'reactivate':
        $userId = $input['userId'] ?? '';
        foreach ($users as &$u) {
            if ($u['id'] === $userId) {
                $u['status'] = 'APPROVED';
                JsonStorage::write('users', $users);
                JsonStorage::appendLog('audit_logs', [
                    'actor' => $admin['username'],
                    'action' => 'USER_REACTIVATED',
                    'details' => "Reactivated user {$u['username']}"
                ]);
                unset($u['passwordHash']);
                json_response(['success' => true, 'user' => $u]);
            }
        }
        json_response(['error' => 'User not found'], 404);
        break;

    case 'delete':
        $userId = $input['userId'] ?? '';
        $found = false;
        $filtered = [];
        foreach ($users as $u) {
            if ($u['id'] === $userId) {
                if ($u['username'] === 'Malik') {
                    json_response(['error' => 'Cannot delete primary admin Malik'], 400);
                }
                $found = true;
                JsonStorage::appendLog('audit_logs', [
                    'actor' => $admin['username'],
                    'action' => 'USER_DELETED',
                    'details' => "Deleted user {$u['username']}"
                ]);
            } else {
                $filtered[] = $u;
            }
        }
        if ($found) {
            JsonStorage::write('users', $filtered);
            json_response(['success' => true, 'message' => 'User deleted successfully']);
        }
        json_response(['error' => 'User not found'], 404);
        break;

    case 'assign_plan':
        $userId = $input['userId'] ?? '';
        $planId = $input['planId'] ?? 'plan_free';
        foreach ($users as &$u) {
            if ($u['id'] === $userId) {
                $u['subscriptionPlanId'] = $planId;
                JsonStorage::write('users', $users);
                JsonStorage::appendLog('audit_logs', [
                    'actor' => $admin['username'],
                    'action' => 'PLAN_ASSIGNED',
                    'details' => "Assigned plan {$planId} to {$u['username']}"
                ]);
                unset($u['passwordHash']);
                json_response(['success' => true, 'user' => $u]);
            }
        }
        json_response(['error' => 'User not found'], 404);
        break;

    default:
        json_response(['error' => 'Invalid action'], 400);
}
