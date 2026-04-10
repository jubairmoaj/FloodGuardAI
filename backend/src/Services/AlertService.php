<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Database\Database;

final class AlertService
{
    private \PDO $db;
    private FloodDataService $floodData;
    private PredictionService $prediction;
    private NotificationService $notifications;

    public function __construct()
    {
        $this->db = Database::connection();
        $this->floodData = new FloodDataService();
        $this->prediction = new PredictionService();
        $this->notifications = new NotificationService();
    }

    /**
     * @return array<string, mixed>
     */
    public function create(int $userId, float $lat, float $lng, string $name, int $threshold): array
    {
        $locationId = $this->floodData->upsertLocation($lat, $lng, $name);
        $statement = $this->db->prepare(
            'INSERT INTO alerts (user_id, location_id, threshold, enabled, created_at, updated_at)
             VALUES (:user_id, :location_id, :threshold, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP())'
        );
        $statement->execute([
            'user_id' => $userId,
            'location_id' => $locationId,
            'threshold' => max(10, min(95, $threshold)),
        ]);
        return $this->list($userId);
    }

    /**
     * @return array<string, mixed>
     */
    public function list(int $userId): array
    {
        $statement = $this->db->prepare(
            'SELECT a.id, l.name AS location, a.threshold, a.enabled
             FROM alerts a
             INNER JOIN locations l ON l.id = a.location_id
             WHERE a.user_id = :user_id
             ORDER BY a.created_at DESC'
        );
        $statement->execute(['user_id' => $userId]);
        return [
            'alerts' => array_map(static fn(array $row): array => [
                'id' => (int) $row['id'],
                'location' => (string) $row['location'],
                'threshold' => (int) $row['threshold'],
                'enabled' => (bool) $row['enabled'],
            ], $statement->fetchAll()),
        ];
    }

    /**
     * @return array<string, mixed>
     */
    public function delete(int $userId, int $alertId): array
    {
        $statement = $this->db->prepare('DELETE FROM alerts WHERE id = :id AND user_id = :user_id');
        $statement->execute([
            'id' => $alertId,
            'user_id' => $userId,
        ]);
        return $this->list($userId);
    }

    /**
     * @return array<string, mixed>
     */
    public function history(int $userId): array
    {
        $statement = $this->db->prepare(
            'SELECT ae.id, ae.alert_id, ae.message, ae.triggered_at
             FROM alert_events ae
             INNER JOIN alerts a ON a.id = ae.alert_id
             WHERE a.user_id = :user_id
             ORDER BY ae.triggered_at DESC
             LIMIT 100'
        );
        $statement->execute(['user_id' => $userId]);
        return [
            'events' => array_map(static fn(array $row): array => [
                'id' => (int) $row['id'],
                'alert_id' => (int) $row['alert_id'],
                'message' => (string) $row['message'],
                'triggered_at' => (string) $row['triggered_at'],
            ], $statement->fetchAll()),
        ];
    }

    public function registerDeviceToken(int $userId, string $token, string $platform): void
    {
        $statement = $this->db->prepare(
            'INSERT INTO device_tokens (user_id, device_token, platform, created_at)
             VALUES (:user_id, :device_token, :platform, UTC_TIMESTAMP())
             ON DUPLICATE KEY UPDATE platform = VALUES(platform)'
        );
        $statement->execute([
            'user_id' => $userId,
            'device_token' => $token,
            'platform' => $platform,
        ]);
    }

    /**
     * @return array<string, int>
     */
    public function runThresholdChecks(): array
    {
        $alerts = $this->db->query(
            'SELECT a.id, a.user_id, a.threshold, l.lat, l.lng, l.name
             FROM alerts a
             INNER JOIN locations l ON l.id = a.location_id
             WHERE a.enabled = 1'
        );
        $rows = $alerts ? $alerts->fetchAll() : [];

        $triggered = 0;
        foreach ($rows as $row) {
            $prediction = $this->prediction->predict(
                lat: (float) $row['lat'],
                lng: (float) $row['lng'],
                name: (string) $row['name'],
                language: 'en'
            );
            $probability = (int) ($prediction['flood_probability'] ?? 0);
            $threshold = (int) $row['threshold'];
            if ($probability < $threshold) {
                continue;
            }
            if ($this->isRecentlyTriggered((int) $row['id'])) {
                continue;
            }

            $message = sprintf(
                '%s flood probability is %d%% (threshold: %d%%).',
                (string) $row['name'],
                $probability,
                $threshold
            );
            $this->recordEvent((int) $row['id'], $message);
            $deviceTokens = $this->tokensForUser((int) $row['user_id']);
            $this->notifications->push(
                deviceTokens: $deviceTokens,
                title: 'FloodGuard Alert',
                body: $message,
                data: [
                    'alert_id' => (string) $row['id'],
                    'flood_probability' => (string) $probability,
                ]
            );
            $triggered++;
        }

        return [
            'checked' => count($rows),
            'triggered' => $triggered,
        ];
    }

    private function isRecentlyTriggered(int $alertId): bool
    {
        $statement = $this->db->prepare(
            'SELECT id FROM alert_events
             WHERE alert_id = :alert_id
               AND triggered_at > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 MINUTE)
             LIMIT 1'
        );
        $statement->execute(['alert_id' => $alertId]);
        return (bool) $statement->fetch();
    }

    private function recordEvent(int $alertId, string $message): void
    {
        $statement = $this->db->prepare(
            'INSERT INTO alert_events (alert_id, message, triggered_at)
             VALUES (:alert_id, :message, UTC_TIMESTAMP())'
        );
        $statement->execute([
            'alert_id' => $alertId,
            'message' => $message,
        ]);
    }

    /**
     * @return array<int, string>
     */
    private function tokensForUser(int $userId): array
    {
        $statement = $this->db->prepare(
            'SELECT device_token FROM device_tokens WHERE user_id = :user_id ORDER BY created_at DESC LIMIT 20'
        );
        $statement->execute(['user_id' => $userId]);
        $rows = $statement->fetchAll();
        return array_values(array_unique(array_map(static fn(array $row): string => (string) $row['device_token'], $rows)));
    }
}
