<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../storage.php';
require_once __DIR__ . '/../auth.php';

set_json_headers();

$user = AuthService::requireApprovedUser();
$input = json_decode(file_get_contents('php://input'), true) ?? [];
$prompt = trim($input['prompt'] ?? '');
$userLanguage = trim($input['language'] ?? 'Roman Urdu');

if (empty($prompt)) {
    json_response(['error' => 'Prompt cannot be empty'], 400);
}

// Check and enforce quota
$plans = JsonStorage::read('plans', []);
$planId = $user['subscriptionPlanId'] ?? 'plan_free';
$dailyLimit = 25;
foreach ($plans as $p) {
    if ($p['id'] === $planId) {
        $dailyLimit = $p['dailyAiQuota'];
        break;
    }
}

$todayKey = date('Y_z');
$usage = JsonStorage::read('usage_logs', []);
$usageKey = $user['id'] . '_' . $todayKey;
$usedToday = $usage[$usageKey] ?? 0;

if ($usedToday >= $dailyLimit) {
    json_response([
        'error' => 'Daily AI request quota reached for your plan. Please upgrade your subscription.'
    ], 429);
}

// Get AI provider settings
$aiConfig = JsonStorage::read('ai_providers', [
    'provider' => 'Google Gemini',
    'model' => 'gemini-2.5-flash',
    'apiKey' => getenv('GEMINI_API_KEY') ?: ''
]);

$apiKey = $aiConfig['apiKey'] ?? '';

// Wake phrase check
$cleanPrompt = strtolower($prompt);
if ($cleanPrompt === 'rasta ai' || $cleanPrompt === 'hey rasta' || $cleanPrompt === 'hi rasta') {
    $wakeResponse = ($userLanguage === 'Urdu') 
        ? "جی شعیب! راستہ اے آئی حاضر ہے۔ بتائیے، میں آپ کے لیے کیا کروں؟"
        : "Ji Shoaib, Rasta AI hazir hai. Bataiye, main aap ke liye kya karun?";
    json_response([
        'success' => true,
        'response' => $wakeResponse
    ]);
}

if (empty($apiKey)) {
    json_response([
        'error' => 'Gemini API Key is not configured on the server. Please configure it in the Admin AI Provider settings.'
    ], 503);
}

// Call Gemini API
$url = "https://generativelanguage.googleapis.com/v1beta/models/{$aiConfig['model']}:generateContent?key=" . urlencode($apiKey);

$systemInstruction = "You are Rasta AI, an intelligent personal AI mobile assistant. The user's name is Shoaib. Introduce yourself as Rasta AI when greeting. Naturally address the user as Shoaib, but do not repeat the name unnaturally in every sentence. Respond in the requested language ($userLanguage). Keep answers concise, helpful, and natural for mobile.";

$payload = [
    'contents' => [
        [
            'role' => 'user',
            'parts' => [
                ['text' => $prompt]
            ]
        ]
    ],
    'systemInstruction' => [
        'parts' => [
            ['text' => $systemInstruction]
        ]
    ],
    'generationConfig' => [
        'temperature' => 0.7,
        'maxOutputTokens' => 800
    ]
];

$ch = curl_init($url);
curl_setopt_array($ch, [
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($payload),
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
    CURLOPT_TIMEOUT => 30
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

if ($curlError) {
    json_response(['error' => "Network error connecting to AI provider: $curlError"], 502);
}

if ($httpCode !== 200) {
    json_response(['error' => "AI provider returned HTTP $httpCode: $response"], 502);
}

$respData = json_decode($response, true);
$replyText = $respData['candidates'][0]['content']['parts'][0]['text'] ?? '';

if (empty($replyText)) {
    json_response(['error' => 'No content returned from AI provider'], 502);
}

// Increment quota
$usage[$usageKey] = $usedToday + 1;
JsonStorage::write('usage_logs', $usage);

json_response([
    'success' => true,
    'response' => $replyText,
    'remainingToday' => max(0, $dailyLimit - ($usedToday + 1))
]);
