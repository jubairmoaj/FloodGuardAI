<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Database\Database;

final class MapLayerService
{
    private \PDO $db;
    private FloodDataService $floodData;

    public function __construct()
    {
        $this->db = Database::connection();
        $this->floodData = new FloodDataService();
    }

    /**
     * @return array<string, mixed>
     */
    public function layers(): array
    {
        $floodZones = [];
        $safeZones = [];
        $reports = $this->floodData->latestReportsForMap(300);

        $history = $this->db->query(
            'SELECT fh.severity, l.lat, l.lng
             FROM flood_history fh
             INNER JOIN locations l ON l.id = fh.location_id
             ORDER BY fh.date DESC
             LIMIT 80'
        );
        $rows = $history ? $history->fetchAll() : [];
        foreach ($rows as $idx => $row) {
            $severity = (int) $row['severity'];
            $polygon = [
                'id' => 'zone-' . $idx,
                'risk_level' => $severity >= 4 ? 'High' : 'Low',
                'points' => $this->square((float) $row['lat'], (float) $row['lng'], $severity >= 4 ? 0.0035 : 0.0026),
            ];
            if ($severity >= 4) {
                $floodZones[] = $polygon;
            } else {
                $safeZones[] = $polygon;
            }
        }

        return [
            'flood_zones' => $floodZones,
            'safe_zones' => $safeZones,
            'reports' => array_map(static function (array $report): array {
                return [
                    'id' => (int) $report['id'],
                    'lat' => (float) $report['lat'],
                    'lng' => (float) $report['lng'],
                    'image_url' => $report['image_url'],
                    'water_level' => $report['water_level'],
                    'note' => $report['note'],
                    'created_at' => $report['created_at'],
                ];
            }, $reports),
        ];
    }

    /**
     * @return array<int, array{lat: float, lng: float}>
     */
    private function square(float $lat, float $lng, float $radius): array
    {
        return [
            ['lat' => $lat + $radius, 'lng' => $lng - $radius],
            ['lat' => $lat + $radius, 'lng' => $lng + $radius],
            ['lat' => $lat - $radius, 'lng' => $lng + $radius],
            ['lat' => $lat - $radius, 'lng' => $lng - $radius],
        ];
    }
}
