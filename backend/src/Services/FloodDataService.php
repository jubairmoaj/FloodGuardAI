<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Database\Database;
use PDO;

final class FloodDataService
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::connection();
    }

    /**
     * @return array<int, int>
     */
    public function historySeverities(float $lat, float $lng): array
    {
        $statement = $this->db->prepare(
            'SELECT fh.severity
             FROM flood_history fh
             INNER JOIN locations l ON l.id = fh.location_id
             WHERE ABS(l.lat - :lat) <= 0.08
               AND ABS(l.lng - :lng) <= 0.08
             ORDER BY fh.date DESC
             LIMIT 20'
        );
        $statement->execute([
            'lat' => $lat,
            'lng' => $lng,
        ]);
        $rows = $statement->fetchAll();
        return array_map(static fn(array $row): int => (int) $row['severity'], $rows);
    }

    /**
     * @return array<int, float>
     */
    public function reportDepths(float $lat, float $lng): array
    {
        $statement = $this->db->prepare(
            'SELECT CAST(r.water_level AS DECIMAL(10,2)) AS water_level
             FROM reports r
             INNER JOIN locations l ON l.id = r.location_id
             WHERE ABS(l.lat - :lat) <= 0.08
               AND ABS(l.lng - :lng) <= 0.08
               AND r.created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 24 HOUR)
             ORDER BY r.created_at DESC
             LIMIT 30'
        );
        $statement->execute([
            'lat' => $lat,
            'lng' => $lng,
        ]);
        $rows = $statement->fetchAll();
        return array_map(static fn(array $row): float => (float) $row['water_level'], $rows);
    }

    public function upsertLocation(float $lat, float $lng, string $name): int
    {
        $lookup = $this->db->prepare(
            'SELECT id FROM locations WHERE ABS(lat - :lat) < 0.0001 AND ABS(lng - :lng) < 0.0001 LIMIT 1'
        );
        $lookup->execute(['lat' => $lat, 'lng' => $lng]);
        $row = $lookup->fetch();
        if ($row) {
            return (int) $row['id'];
        }

        $insert = $this->db->prepare(
            'INSERT INTO locations (lat, lng, name, created_at) VALUES (:lat, :lng, :name, UTC_TIMESTAMP())'
        );
        $insert->execute([
            'lat' => $lat,
            'lng' => $lng,
            'name' => $name,
        ]);
        return (int) $this->db->lastInsertId();
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    public function latestReportsForMap(int $limit = 200): array
    {
        $statement = $this->db->prepare(
            'SELECT r.id, l.lat, l.lng, r.image_url, r.water_level, r.note, r.created_at
             FROM reports r
             INNER JOIN locations l ON l.id = r.location_id
             ORDER BY r.created_at DESC
             LIMIT :limit'
        );
        $statement->bindValue('limit', $limit, PDO::PARAM_INT);
        $statement->execute();
        return $statement->fetchAll();
    }
}
