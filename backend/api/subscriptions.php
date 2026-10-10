<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../storage.php';
require_once __DIR__ . '/../auth.php';

set_json_headers();

$defaultPlans = [
    [
        'id' => 'plan_free',
        'name' => 'Free',
        'priceMonthly' => 0.0,
        'dailyAiQuota' => 25,
        'voiceMinutesPerDay' => 15,
        'description' => '25 AI queries/day & essential device automation.'
    ],
    [
        'id' => 'plan_basic',
        'name' => 'Basic',
        'priceMonthly' => 4.99,
        'dailyAiQuota' => 100,
        'voiceMinutesPerDay' => 60,
        'description' => '100 AI queries/day, contacts search & SMS drafting.'
    ],
    [
        'id' => 'plan_pro',
        'name' => 'Pro',
        'priceMonthly' => 14.99,
        'dailyAiQuota' => 500,
        'voiceMinutesPerDay' => 240,
        'description' => 'High-priority Gemini processing & continuous automation.'
    ],
    [
        'id' => 'plan_premium',
        'name' => 'Premium',
        'priceMonthly' => 29.99,
        'dailyAiQuota' => 2000,
        'voiceMinutesPerDay' => 720,
        'description' => 'Ultra high capacity, priority proxy routing.'
    ],
    [
        'id' => 'plan_enterprise',
        'name' => 'Enterprise',
        'priceMonthly' => 99.99,
        'dailyAiQuota' => 10000,
        'voiceMinutesPerDay' => 2400,
        'description' => 'Unlimited multi-agent assistant support.'
    ]
];

$plans = JsonStorage::read('plans', $defaultPlans);
if (empty($plans)) {
    JsonStorage::write('plans', $defaultPlans);
    $plans = $defaultPlans;
}

$action = $_GET['action'] ?? 'plans';

switch ($action) {
    case 'plans':
        json_response(['plans' => $plans]);
        break;

    case 'my_subscription':
        $user = AuthService::requireApprovedUser();
        $planId = $user['subscriptionPlanId'] ?? 'plan_free';
        $userPlan = null;
        foreach ($plans as $p) {
            if ($p['id'] === $planId) {
                $userPlan = $p;
                break;
            }
        }
        if (!$userPlan) $userPlan = $defaultPlans[0];

        $todayKey = date('Y_z');
        $usage = JsonStorage::read('usage_logs', []);
        $userUsedToday = $usage[$user['id'] . '_' . $todayKey] ?? 0;

        json_response([
            'plan' => $userPlan,
            'usedToday' => $userUsedToday,
            'remainingToday' => max(0, $userPlan['dailyAiQuota'] - $userUsedToday),
            'status' => 'ACTIVE'
        ]);
        break;

    default:
        json_response(['error' => 'Invalid action'], 400);
}
