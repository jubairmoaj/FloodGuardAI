<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Config\Config;
use FloodGuard\Support\HttpClient;

final class GeminiService
{
    /**
     * @param array<string, mixed> $input
     * @return array<string, mixed>
     */
    public function routeDecision(array $input): array
    {
        $prompt = <<<TXT
You are a flood risk and travel safety AI.
Analyze this JSON payload and respond with strict JSON only.

{$this->toJson($input)}

Output schema:
{
  "decision":"Safe|Unsafe",
  "risk_percent":0,
  "recommended_time":"HH:MM",
  "recommended_action":"text",
  "explanation":"text",
  "chosen_route":"text",
  "risky_sections":[{"label":"text","lat":0,"lng":0,"risk_percent":0,"risk_level":"Low|Medium|High","rain_mm":0,"forecast_time":"HH:MM","history_severity_avg":0,"report_depth_avg_cm":0}],
  "alternatives":[{"route":"text","risk_percent":0,"recommended_time":"HH:MM","reason":"text"}]
}
TXT;
        $fallback = [
            'decision' => ($input['base_risk_percent'] ?? 0) >= 60 ? 'Unsafe' : 'Safe',
            'risk_percent' => (int) ($input['base_risk_percent'] ?? 35),
            'recommended_time' => (string) ($input['routes'][0]['recommended_time'] ?? '19:00'),
            'recommended_action' => 'Prefer higher roads and avoid low-lying intersections.',
            'explanation' => 'This decision is based on route-point rainfall, nearby flood history, and recent flood reports.',
            'chosen_route' => (string) ($input['chosen_route'] ?? ''),
            'risky_sections' => is_array($input['risky_sections'] ?? null) ? $input['risky_sections'] : [],
            'alternatives' => is_array($input['alternatives'] ?? null) ? $input['alternatives'] : [],
        ];
        return $this->generateJson($prompt, $fallback);
    }

    /**
     * @param array<string, mixed> $context
     * @return array<string, mixed>
     */
    public function locationPrediction(array $context): array
    {
        $prompt = <<<TXT
You are a flood prediction AI.
Use this data and return strict JSON:
{$this->toJson($context)}

Output schema:
{
  "flood_probability":0,
  "peak_risk_time":"HH:MM-HH:MM",
  "risk_level":"Low|Medium|High",
  "explanation":"text",
  "confidence":0
}
TXT;
        $baseRisk = (int) ($context['base_risk_percent'] ?? 30);
        $fallback = [
            'flood_probability' => $baseRisk,
            'peak_risk_time' => (string) ($context['peak_risk_window'] ?? '16:00-19:00'),
            'risk_level' => $baseRisk >= 70 ? 'High' : ($baseRisk >= 40 ? 'Medium' : 'Low'),
            'explanation' => 'Risk is estimated from rainfall, flood history, and nearby user reports.',
            'confidence' => 68,
        ];
        return $this->generateJson($prompt, $fallback);
    }

    /**
     * @param array<string, mixed> $context
     * @return array<string, mixed>
     */
    public function chatAnswer(string $question, array $context): array
    {
        $prompt = <<<TXT
You are a flood safety assistant.
Question: {$question}
Context JSON: {$this->toJson($context)}

Return strict JSON:
{
  "answer":"text",
  "risk_level":"Low|Medium|High",
  "recommended_action":"text"
}
TXT;

        $fallback = [
            'answer' => 'Current conditions indicate caution. Check route conditions before travel.',
            'risk_level' => $context['risk_level'] ?? 'Medium',
            'recommended_action' => 'Delay travel in low-lying areas and monitor local alerts.',
        ];
        return $this->generateJson($prompt, $fallback);
    }

    /**
     * @param array<string, mixed> $context
     * @return array<string, mixed>
     */
    public function imageAnalysis(string $imagePath, array $context): array
    {
        $key = (string) Config::get('GEMINI_API_KEY', '');
        $model = (string) Config::get('GEMINI_MODEL', 'gemini-2.0-flash');
        if ($key === '' || !is_file($imagePath)) {
            return $this->fallbackVision($context);
        }

        $url = sprintf(
            'https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s',
            urlencode($model),
            urlencode($key)
        );
        $imageData = base64_encode(file_get_contents($imagePath) ?: '');
        if ($imageData === '') {
            return $this->fallbackVision($context);
        }

        $prompt = <<<TXT
You are a flood analysis AI.
Estimate water depth from visible objects.
Use this context: {$this->toJson($context)}

Return strict JSON:
{
  "estimated_depth_cm":0,
  "time_to_clear_min":0,
  "risk_level":"Low|Medium|High",
  "confidence":0
}
TXT;

        $payload = [
            'contents' => [[
                'parts' => [
                    ['text' => $prompt],
                    [
                        'inline_data' => [
                            'mime_type' => 'image/jpeg',
                            'data' => $imageData,
                        ],
                    ],
                ],
            ]],
            'generationConfig' => [
                'temperature' => 0.2,
                'maxOutputTokens' => 350,
            ],
        ];

        $response = HttpClient::post($url, $payload);
        $jsonText = $this->extractGeminiText($response['body']);
        $parsed = $this->extractFirstJsonObject($jsonText);
        if (is_array($parsed)) {
            return $this->normalizeVision($parsed, $context);
        }
        return $this->fallbackVision($context);
    }

    /**
     * @param array<string, mixed> $fallback
     * @return array<string, mixed>
     */
    private function generateJson(string $prompt, array $fallback): array
    {
        $key = (string) Config::get('GEMINI_API_KEY', '');
        $model = (string) Config::get('GEMINI_MODEL', 'gemini-2.0-flash');
        if ($key === '') {
            return $fallback;
        }

        $url = sprintf(
            'https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s',
            urlencode($model),
            urlencode($key)
        );

        $payload = [
            'contents' => [[
                'parts' => [
                    ['text' => $prompt],
                ],
            ]],
            'generationConfig' => [
                'temperature' => 0.2,
                'maxOutputTokens' => 600,
            ],
        ];

        for ($attempt = 0; $attempt < 3; $attempt++) {
            $response = HttpClient::post($url, $payload);
            $jsonText = $this->extractGeminiText($response['body']);
            $parsed = $this->extractFirstJsonObject($jsonText);
            if (is_array($parsed)) {
                return $parsed;
            }
        }
        return $fallback;
    }

    /**
     * @param array<string, mixed> $body
     */
    private function extractGeminiText(array $body): string
    {
        $candidates = $body['candidates'] ?? [];
        if (!is_array($candidates) || count($candidates) === 0) {
            return '';
        }
        $first = $candidates[0]['content']['parts'][0]['text'] ?? '';
        return is_string($first) ? trim($first) : '';
    }

    /**
     * @return array<string, mixed>|null
     */
    private function extractFirstJsonObject(string $text): ?array
    {
        if ($text === '') {
            return null;
        }

        $clean = trim($text);
        if (str_starts_with($clean, '```')) {
            $clean = preg_replace('/^```(?:json)?|```$/m', '', $clean) ?? $clean;
        }
        $firstBrace = strpos($clean, '{');
        $lastBrace = strrpos($clean, '}');
        if ($firstBrace === false || $lastBrace === false || $lastBrace <= $firstBrace) {
            return null;
        }

        $json = substr($clean, $firstBrace, $lastBrace - $firstBrace + 1);
        $decoded = json_decode($json, true);
        return is_array($decoded) ? $decoded : null;
    }

    /**
     * @param array<string, mixed> $context
     * @return array<string, mixed>
     */
    private function fallbackVision(array $context): array
    {
        $baseRisk = (int) ($context['base_risk_percent'] ?? 45);
        $depth = max(8, min(65, (int) round($baseRisk * 0.45)));
        return [
            'estimated_depth_cm' => $depth,
            'time_to_clear_min' => max(30, (int) round($depth * 4.5)),
            'risk_level' => $baseRisk >= 70 ? 'High' : ($baseRisk >= 40 ? 'Medium' : 'Low'),
            'confidence' => 55,
        ];
    }

    /**
     * @param array<string, mixed> $parsed
     * @param array<string, mixed> $context
     * @return array<string, mixed>
     */
    private function normalizeVision(array $parsed, array $context): array
    {
        $fallback = $this->fallbackVision($context);
        return [
            'estimated_depth_cm' => (int) ($parsed['estimated_depth_cm'] ?? $fallback['estimated_depth_cm']),
            'time_to_clear_min' => (int) ($parsed['time_to_clear_min'] ?? $fallback['time_to_clear_min']),
            'risk_level' => (string) ($parsed['risk_level'] ?? $fallback['risk_level']),
            'confidence' => (int) ($parsed['confidence'] ?? $fallback['confidence']),
        ];
    }

    /**
     * @param array<string, mixed> $value
     */
    private function toJson(array $value): string
    {
        return json_encode($value, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) ?: '{}';
    }
}
