<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Database\Database;

final class ChatService
{
    private GeminiService $gemini;
    private PredictionService $prediction;
    private \PDO $db;

    public function __construct()
    {
        $this->gemini = new GeminiService();
        $this->prediction = new PredictionService();
        $this->db = Database::connection();
    }

    /**
     * @param array<string, mixed> $context
     * @return array<string, mixed>
     */
    public function ask(string $question, array $context, string $language): array
    {
        $location = $context['location'] ?? null;
        $predictionContext = [];
        if (is_array($location)) {
            $lat = (float) ($location['lat'] ?? 0.0);
            $lng = (float) ($location['lng'] ?? 0.0);
            $name = (string) ($location['name'] ?? 'Selected Location');
            if ($lat !== 0.0 || $lng !== 0.0) {
                $predictionContext = $this->prediction->predict($lat, $lng, $name, $language);
            }
        }

        $response = $this->gemini->chatAnswer($question, [
            'language' => $language,
            'prediction' => $predictionContext,
            'context' => $context,
        ]);
        $this->audit('chat', [
            'question' => $question,
            'language' => $language,
            'context' => $context,
            'response' => $response,
        ]);

        return [
            'answer' => (string) ($response['answer'] ?? 'Please avoid flood-prone routes right now.'),
            'risk_level' => (string) ($response['risk_level'] ?? ($predictionContext['risk_level'] ?? 'Medium')),
            'recommended_action' => (string) ($response['recommended_action'] ?? 'Monitor live flood alerts.'),
        ];
    }

    /**
     * @param array<string, mixed> $payload
     */
    private function audit(string $type, array $payload): void
    {
        $statement = $this->db->prepare(
            'INSERT INTO ai_audit_logs (event_type, payload, created_at)
             VALUES (:event_type, :payload, UTC_TIMESTAMP())'
        );
        $statement->execute([
            'event_type' => $type,
            'payload' => json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) ?: '{}',
        ]);
    }
}
