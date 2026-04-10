<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Config\Config;
use FloodGuard\Database\Database;
use RuntimeException;

final class ReportService
{
    private \PDO $db;
    private FloodDataService $floodData;
    private WeatherService $weather;
    private RiskEngine $riskEngine;
    private GeminiService $gemini;

    public function __construct()
    {
        $this->db = Database::connection();
        $this->floodData = new FloodDataService();
        $this->weather = new WeatherService();
        $this->riskEngine = new RiskEngine();
        $this->gemini = new GeminiService();
    }

    /**
     * @param array<string, mixed> $file
     * @return array<string, mixed>
     */
    public function create(?int $userId, float $lat, float $lng, string $note, array $file): array
    {
        if (!isset($file['tmp_name']) || !is_uploaded_file((string) $file['tmp_name'])) {
            throw new RuntimeException('image upload is required');
        }

        $tmpPath = (string) $file['tmp_name'];
        $mime = mime_content_type($tmpPath) ?: '';
        if (!in_array($mime, ['image/jpeg', 'image/png', 'image/webp'], true)) {
            throw new RuntimeException('unsupported image format');
        }
        $hash = hash_file('sha256', $tmpPath) ?: '';
        if ($hash === '') {
            throw new RuntimeException('unable to hash image');
        }

        $existing = $this->findByImageHash($hash);
        if ($existing !== null) {
            return [
                'report_id' => (int) $existing['id'],
                'image_url' => $this->signedImageUrl((int) $existing['id']),
                'analysis' => [
                    'estimated_depth_cm' => (int) $existing['water_level'],
                    'time_to_clear_min' => (int) ($existing['time_to_clear_min'] ?? 0),
                    'risk_level' => (string) ($existing['risk_level'] ?? 'Medium'),
                    'confidence' => (int) ($existing['confidence'] ?? 55),
                ],
            ];
        }

        $storedPath = $this->storeFile($tmpPath, $mime);
        $locationId = $this->floodData->upsertLocation($lat, $lng, 'Reported Location');

        $weather = $this->weather->hourlyRain($lat, $lng);
        $history = $this->floodData->historySeverities($lat, $lng);
        $reports = $this->floodData->reportDepths($lat, $lng);
        $baseRisk = $this->riskEngine->compute($weather['hourly_rain'], $history, $reports);

        $analysis = $this->gemini->imageAnalysis($storedPath, [
            'hourly_rain' => $weather['hourly_rain'],
            'history_severities' => $history,
            'recent_report_depths_cm' => $reports,
            'base_risk_percent' => $baseRisk['risk_percent'],
        ]);

        $insert = $this->db->prepare(
            'INSERT INTO reports (
                user_id, location_id, image_url, image_hash, note, water_level,
                time_to_clear_min, risk_level, confidence, created_at
             ) VALUES (
                :user_id, :location_id, :image_url, :image_hash, :note, :water_level,
                :time_to_clear_min, :risk_level, :confidence, UTC_TIMESTAMP()
             )'
        );
        $insert->execute([
            'user_id' => $userId,
            'location_id' => $locationId,
            'image_url' => $this->relativeUploadPath($storedPath),
            'image_hash' => $hash,
            'note' => $note,
            'water_level' => (string) (int) ($analysis['estimated_depth_cm'] ?? 0),
            'time_to_clear_min' => (int) ($analysis['time_to_clear_min'] ?? 0),
            'risk_level' => (string) ($analysis['risk_level'] ?? 'Medium'),
            'confidence' => (int) ($analysis['confidence'] ?? 55),
        ]);
        $reportId = (int) $this->db->lastInsertId();

        return [
            'report_id' => $reportId,
            'image_url' => $this->signedImageUrl($reportId),
            'analysis' => [
                'estimated_depth_cm' => (int) ($analysis['estimated_depth_cm'] ?? 0),
                'time_to_clear_min' => (int) ($analysis['time_to_clear_min'] ?? 0),
                'risk_level' => (string) ($analysis['risk_level'] ?? 'Medium'),
                'confidence' => (int) ($analysis['confidence'] ?? 55),
            ],
        ];
    }

    public function flag(int $reportId, ?int $userId, string $reason): void
    {
        $statement = $this->db->prepare(
            'INSERT INTO report_flags (report_id, user_id, reason, created_at)
             VALUES (:report_id, :user_id, :reason, UTC_TIMESTAMP())'
        );
        $statement->execute([
            'report_id' => $reportId,
            'user_id' => $userId,
            'reason' => $reason !== '' ? $reason : 'abuse',
        ]);
    }

    /**
     * @return array<string, mixed>|null
     */
    public function findImageBySignedToken(int $reportId, string $token): ?array
    {
        if (!$this->validateSignedToken($reportId, $token)) {
            return null;
        }
        $statement = $this->db->prepare('SELECT id, image_url FROM reports WHERE id = :id LIMIT 1');
        $statement->execute(['id' => $reportId]);
        $row = $statement->fetch();
        if (!$row) {
            return null;
        }

        $path = $this->absoluteUploadPath((string) $row['image_url']);
        if (!is_file($path)) {
            return null;
        }
        return [
            'path' => $path,
            'mime' => mime_content_type($path) ?: 'image/jpeg',
        ];
    }

    private function storeFile(string $tmpPath, string $mime): string
    {
        $dir = $this->uploadDirectory();
        if (!is_dir($dir)) {
            mkdir($dir, 0775, true);
        }
        $extension = match ($mime) {
            'image/png' => 'png',
            'image/webp' => 'webp',
            default => 'jpg',
        };
        $filename = date('Ymd_His') . '_' . bin2hex(random_bytes(8)) . '.' . $extension;
        $target = rtrim($dir, '/\\') . DIRECTORY_SEPARATOR . $filename;
        if (!move_uploaded_file($tmpPath, $target)) {
            throw new RuntimeException('failed to store image');
        }
        return $target;
    }

    private function uploadDirectory(): string
    {
        $configured = (string) Config::get('UPLOAD_DIR', '../storage/uploads');
        return realpath(dirname(__DIR__, 2) . '/public') . DIRECTORY_SEPARATOR . trim($configured, '/\\');
    }

    private function relativeUploadPath(string $absolutePath): string
    {
        $publicPath = realpath(dirname(__DIR__, 2) . '/public') ?: '';
        if ($publicPath !== '' && str_starts_with($absolutePath, $publicPath)) {
            return ltrim(substr($absolutePath, strlen($publicPath)), DIRECTORY_SEPARATOR);
        }
        return basename($absolutePath);
    }

    private function absoluteUploadPath(string $relativePath): string
    {
        $publicPath = realpath(dirname(__DIR__, 2) . '/public') ?: dirname(__DIR__, 2) . '/public';
        return rtrim($publicPath, '/\\') . DIRECTORY_SEPARATOR . ltrim($relativePath, '/\\');
    }

    private function signedImageUrl(int $reportId): string
    {
        $expires = time() + 3600;
        $secret = (string) Config::get('APP_SECRET', 'floodguard-secret');
        $signature = hash_hmac('sha256', $reportId . '|' . $expires, $secret);
        $token = base64_encode($expires . ':' . $signature);
        return '/api/v1/reports/' . $reportId . '/image?token=' . urlencode($token);
    }

    private function validateSignedToken(int $reportId, string $token): bool
    {
        $decoded = base64_decode($token, true);
        if ($decoded === false || str_contains($decoded, ':') === false) {
            return false;
        }
        [$expires, $signature] = explode(':', $decoded, 2);
        if ((int) $expires < time()) {
            return false;
        }
        $secret = (string) Config::get('APP_SECRET', 'floodguard-secret');
        $expected = hash_hmac('sha256', $reportId . '|' . $expires, $secret);
        return hash_equals($expected, $signature);
    }

    /**
     * @return array<string, mixed>|null
     */
    private function findByImageHash(string $hash): ?array
    {
        $statement = $this->db->prepare('SELECT * FROM reports WHERE image_hash = :image_hash LIMIT 1');
        $statement->execute(['image_hash' => $hash]);
        $row = $statement->fetch();
        return $row ?: null;
    }
}
