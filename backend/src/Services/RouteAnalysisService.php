<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Config\Config;
use FloodGuard\Database\Database;
use FloodGuard\Support\CacheStore;
use FloodGuard\Support\HttpClient;

final class RouteAnalysisService
{
    private CacheStore $cache;
    private FloodDataService $floodData;
    private RiskEngine $riskEngine;
    private GeminiService $gemini;
    private WeatherService $weather;
    private \PDO $db;

    public function __construct()
    {
        $this->cache = new CacheStore();
        $this->floodData = new FloodDataService();
        $this->riskEngine = new RiskEngine();
        $this->gemini = new GeminiService();
        $this->weather = new WeatherService();
        $this->db = Database::connection();
    }

    /**
     * @return array<string, mixed>
     */
    public function analyze(
        float $fromLat,
        float $fromLng,
        string $fromName,
        float $toLat,
        float $toLng,
        string $toName,
        string $time,
        string $language = 'en'
    ): array {
        $origin = $this->resolveLocation($fromName, $fromLat, $fromLng);
        $destination = $this->resolveLocation($toName, $toLat, $toLng);
        $cacheKey = sprintf(
            'route:%.5f:%.5f:%.5f:%.5f:%s:%s',
            $origin['lat'],
            $origin['lng'],
            $destination['lat'],
            $destination['lng'],
            $time,
            $language
        );
        $cached = $this->cache->get($cacheKey);
        if ($cached !== null) {
            return $cached;
        }

        $rawRoutes = $this->fetchRoutes(
            $origin['lat'],
            $origin['lng'],
            $destination['lat'],
            $destination['lng']
        );
        $candidates = [];
        foreach ($rawRoutes as $route) {
            $candidates[] = $this->analyzeRouteCandidate($route['label'], $route['points'], $time);
        }
        if (count($candidates) === 0) {
            throw new \RuntimeException('No route could be analyzed.');
        }

        usort(
            $candidates,
            static fn(array $a, array $b): int => ($a['risk_percent'] <=> $b['risk_percent'])
        );

        $best = $candidates[0];
        $alternatives = array_map(
            static fn(array $candidate): array => [
                'route' => $candidate['route'],
                'risk_percent' => $candidate['risk_percent'],
                'recommended_time' => $candidate['recommended_time'],
                'reason' => $candidate['reason'],
            ],
            array_slice($candidates, 1)
        );

        $ai = $this->gemini->routeDecision([
            'language' => $language,
            'from' => $origin,
            'to' => $destination,
            'time' => $time,
            'chosen_route' => $best['route'],
            'base_risk_percent' => $best['risk_percent'],
            'risky_sections' => $best['risky_sections'],
            'alternatives' => $alternatives,
            'routes' => array_map(
                static fn(array $candidate): array => [
                    'route' => $candidate['route'],
                    'risk_percent' => $candidate['risk_percent'],
                    'risk_level' => $candidate['risk_level'],
                    'recommended_time' => $candidate['recommended_time'],
                    'reason' => $candidate['reason'],
                    'risky_sections' => $candidate['risky_sections'],
                ],
                $candidates
            ),
        ]);

        $result = [
            'decision' => (string) ($ai['decision'] ?? ($best['risk_percent'] >= 60 ? 'Unsafe' : 'Safe')),
            'risk_percent' => (int) ($ai['risk_percent'] ?? $best['risk_percent']),
            'recommended_time' => (string) ($ai['recommended_time'] ?? $best['recommended_time']),
            'recommended_action' => (string) ($ai['recommended_action'] ?? $best['recommended_action']),
            'explanation' => (string) ($ai['explanation'] ?? $best['reason']),
            'chosen_route' => (string) ($ai['chosen_route'] ?? $best['route']),
            'resolved_origin' => $origin,
            'resolved_destination' => $destination,
            'risky_sections' => $this->normalizeRiskySections($ai['risky_sections'] ?? null, $best['risky_sections']),
            'alternatives' => $this->normalizeAlternatives($ai['alternatives'] ?? null, $alternatives),
        ];

        $this->saveRoute(
            $origin['name'],
            $destination['name'],
            $result['risk_percent'],
            $best['segment_risks']
        );
        $ttl = (int) Config::get('ROUTE_CACHE_TTL', 300);
        $this->cache->put($cacheKey, $result, $ttl);

        return $result;
    }

    /**
     * @return array{lat: float, lng: float, name: string}
     */
    private function resolveLocation(string $name, float $lat, float $lng): array
    {
        $trimmedName = trim($name);
        $apiKey = (string) Config::get('GOOGLE_MAPS_API_KEY', '');

        if ($trimmedName !== '' && $apiKey !== '') {
            $url = sprintf(
                'https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s',
                urlencode($trimmedName),
                urlencode($apiKey)
            );
            $response = HttpClient::get($url);
            $results = $response['body']['results'] ?? [];
            if (is_array($results) && count($results) > 0 && is_array($results[0])) {
                $first = $results[0];
                $location = $first['geometry']['location'] ?? [];
                if (is_array($location)) {
                    return [
                        'lat' => (float) ($location['lat'] ?? $lat),
                        'lng' => (float) ($location['lng'] ?? $lng),
                        'name' => (string) ($first['formatted_address'] ?? $trimmedName),
                    ];
                }
            }
        }

        if ($lat !== 0.0 || $lng !== 0.0) {
            return [
                'lat' => $lat,
                'lng' => $lng,
                'name' => $trimmedName !== '' ? $trimmedName : sprintf('%.5f, %.5f', $lat, $lng),
            ];
        }

        throw new \RuntimeException('Unable to resolve location: ' . ($trimmedName !== '' ? $trimmedName : 'unknown'));
    }

    /**
     * @return array<int, array{label: string, points: array<int, array{lat: float, lng: float}>}>
     */
    private function fetchRoutes(float $fromLat, float $fromLng, float $toLat, float $toLng): array
    {
        $apiKey = (string) Config::get('GOOGLE_MAPS_API_KEY', '');
        if ($apiKey === '') {
            return $this->fallbackRoutes($fromLat, $fromLng, $toLat, $toLng);
        }

        $url = sprintf(
            'https://maps.googleapis.com/maps/api/directions/json?origin=%s,%s&destination=%s,%s&alternatives=true&key=%s',
            $fromLat,
            $fromLng,
            $toLat,
            $toLng,
            urlencode($apiKey)
        );
        $response = HttpClient::get($url);
        $routes = $response['body']['routes'] ?? [];
        if (!is_array($routes) || count($routes) === 0) {
            return $this->fallbackRoutes($fromLat, $fromLng, $toLat, $toLng);
        }

        $results = [];
        foreach (array_slice($routes, 0, 3) as $index => $route) {
            if (!is_array($route)) {
                continue;
            }
            $polyline = (string) ($route['overview_polyline']['points'] ?? '');
            $points = $this->decodePolyline($polyline);
            if (count($points) < 2) {
                continue;
            }
            $results[] = [
                'label' => (string) ($route['summary'] ?? ('Route ' . ($index + 1))),
                'points' => $points,
            ];
        }
        return count($results) > 0 ? $results : $this->fallbackRoutes($fromLat, $fromLng, $toLat, $toLng);
    }

    /**
     * @return array<int, array{label: string, points: array<int, array{lat: float, lng: float}>}>
     */
    private function fallbackRoutes(float $fromLat, float $fromLng, float $toLat, float $toLng): array
    {
        return [
            [
                'label' => 'Primary Corridor',
                'points' => [
                    ['lat' => $fromLat, 'lng' => $fromLng],
                    ['lat' => ($fromLat + $toLat) / 2 + 0.012, 'lng' => ($fromLng + $toLng) / 2 + 0.006],
                    ['lat' => $toLat, 'lng' => $toLng],
                ],
            ],
            [
                'label' => 'Alternative 1',
                'points' => [
                    ['lat' => $fromLat, 'lng' => $fromLng],
                    ['lat' => ($fromLat + $toLat) / 2 - 0.011, 'lng' => ($fromLng + $toLng) / 2 + 0.004],
                    ['lat' => $toLat, 'lng' => $toLng],
                ],
            ],
            [
                'label' => 'Alternative 2',
                'points' => [
                    ['lat' => $fromLat, 'lng' => $fromLng],
                    ['lat' => ($fromLat + $toLat) / 2 + 0.003, 'lng' => ($fromLng + $toLng) / 2 - 0.009],
                    ['lat' => $toLat, 'lng' => $toLng],
                ],
            ],
        ];
    }

    /**
     * @param array<int, array{lat: float, lng: float}> $points
     * @return array{
     *   route: string,
     *   risk_percent: int,
     *   risk_level: string,
     *   recommended_time: string,
     *   recommended_action: string,
     *   reason: string,
     *   risky_sections: array<int, array<string, mixed>>,
     *   segment_risks: array<int, float>
     * }
     */
    private function analyzeRouteCandidate(string $label, array $points, string $time): array
    {
        $sampledPoints = $this->sampleRoutePoints($points, 450.0, 10);
        $segmentRisks = [];
        $riskySections = [];
        $hourlyRain = [];
        $historyAverages = [];
        $reportAverages = [];
        $forecastByHour = [];

        foreach ($sampledPoints as $index => $point) {
            $forecast = $this->weather->forecastForTime($point['lat'], $point['lng'], $time);
            $history = $this->floodData->historySeverities($point['lat'], $point['lng']);
            $reports = $this->floodData->reportDepths($point['lat'], $point['lng']);
            $local = $this->riskEngine->compute(
                hourlyRain: [$forecast['rain_mm']],
                historySeverities: $history,
                reportDepthsCm: $reports
            );

            $historyAverage = count($history) > 0 ? (array_sum($history) / count($history)) : 0.0;
            $reportAverage = count($reports) > 0 ? (array_sum($reports) / count($reports)) : 0.0;

            $hourlyRain[] = $forecast['rain_mm'];
            $historyAverages[] = (int) round($historyAverage);
            $reportAverages[] = $reportAverage;
            $segmentRisks[] = (float) $local['risk_percent'];

            foreach (($forecast['hourly_rain'] ?? []) as $hourIndex => $rainValue) {
                $forecastByHour[$hourIndex][] = (float) $rainValue;
            }

            if ($local['risk_percent'] >= 60) {
                $riskySections[] = [
                    'label' => 'Point ' . ($index + 1),
                    'lat' => $point['lat'],
                    'lng' => $point['lng'],
                    'risk_percent' => (int) $local['risk_percent'],
                    'risk_level' => (string) $local['risk_level'],
                    'rain_mm' => (float) $forecast['rain_mm'],
                    'forecast_time' => (string) $forecast['forecast_time'],
                    'history_severity_avg' => round($historyAverage, 2),
                    'report_depth_avg_cm' => round($reportAverage, 2),
                ];
            }
        }

        $routeRisk = $this->riskEngine->compute(
            hourlyRain: $hourlyRain,
            historySeverities: $historyAverages,
            reportDepthsCm: $reportAverages,
            routeSegmentRisks: $segmentRisks
        );
        $recommendedTime = $this->recommendedTimeFromForecast($forecastByHour, $time, $routeRisk['risk_percent']);
        $reason = $this->buildRouteReason($routeRisk['risk_level'], $riskySections, $recommendedTime, $time);

        return [
            'route' => $label,
            'risk_percent' => (int) $routeRisk['risk_percent'],
            'risk_level' => (string) $routeRisk['risk_level'],
            'recommended_time' => $recommendedTime,
            'recommended_action' => $routeRisk['risk_percent'] >= 60
                ? 'Avoid this route during the requested time and consider a safer alternative.'
                : 'Travel with caution and monitor live flood alerts.',
            'reason' => $reason,
            'risky_sections' => array_slice($riskySections, 0, 5),
            'segment_risks' => $segmentRisks,
        ];
    }

    /**
     * @param array<int, array{lat: float, lng: float}> $points
     * @return array<int, array{lat: float, lng: float}>
     */
    private function sampleRoutePoints(array $points, float $stepMeters, int $maxSamples): array
    {
        if (count($points) <= 2) {
            return $points;
        }

        $sampled = [$points[0]];
        $distanceAccum = 0.0;
        for ($i = 1; $i < count($points) - 1; $i++) {
            $distanceAccum += $this->distanceMeters($points[$i - 1], $points[$i]);
            if ($distanceAccum >= $stepMeters) {
                $sampled[] = $points[$i];
                $distanceAccum = 0.0;
            }
            if (count($sampled) >= ($maxSamples - 1)) {
                break;
            }
        }
        $sampled[] = $points[count($points) - 1];
        return $sampled;
    }

    /**
     * @param array<int, array<int, float>> $forecastByHour
     */
    private function recommendedTimeFromForecast(array $forecastByHour, string $requestedTime, int $currentRisk): string
    {
        if ($currentRisk < 60 || count($forecastByHour) === 0) {
            return $requestedTime;
        }

        $bestIndex = 0;
        $bestRain = PHP_FLOAT_MAX;
        foreach ($forecastByHour as $index => $values) {
            if (count($values) === 0) {
                continue;
            }
            $average = array_sum($values) / count($values);
            if ($average < $bestRain) {
                $bestRain = $average;
                $bestIndex = (int) $index;
            }
        }

        return $this->shiftTime($requestedTime, $bestIndex);
    }

    private function shiftTime(string $time, int $hours): string
    {
        if (preg_match('/^(\d{1,2}):(\d{2})$/', trim($time), $matches) !== 1) {
            return $time;
        }

        $hour = (int) $matches[1];
        $minute = (int) $matches[2];
        $shifted = (($hour + $hours) % 24 + 24) % 24;
        return sprintf('%02d:%02d', $shifted, $minute);
    }

    /**
     * @param array<int, array<string, mixed>> $riskySections
     */
    private function buildRouteReason(string $riskLevel, array $riskySections, string $recommendedTime, string $requestedTime): string
    {
        if ($riskLevel === 'High') {
            if ($recommendedTime !== $requestedTime) {
                return sprintf(
                    'High flood risk was detected at %d sampled route points. A later departure around %s looks safer.',
                    count($riskySections),
                    $recommendedTime
                );
            }
            return sprintf(
                'High flood risk was detected at %d sampled route points along this route.',
                count($riskySections)
            );
        }
        if ($riskLevel === 'Medium') {
            return 'Moderate flood risk is present on parts of this route. Travel carefully and avoid low-lying roads.';
        }
        return 'This route currently shows lower flood risk than the alternatives analyzed.';
    }

    /**
     * @param mixed $value
     * @param array<int, array<string, mixed>> $fallback
     * @return array<int, array<string, mixed>>
     */
    private function normalizeRiskySections(mixed $value, array $fallback): array
    {
        if (!is_array($value)) {
            return $fallback;
        }

        $normalized = [];
        foreach ($value as $entry) {
            if (!is_array($entry)) {
                continue;
            }
            $normalized[] = [
                'label' => (string) ($entry['label'] ?? 'Risk point'),
                'lat' => (float) ($entry['lat'] ?? 0.0),
                'lng' => (float) ($entry['lng'] ?? 0.0),
                'risk_percent' => (int) ($entry['risk_percent'] ?? 0),
                'risk_level' => (string) ($entry['risk_level'] ?? 'Medium'),
                'rain_mm' => (float) ($entry['rain_mm'] ?? 0.0),
                'forecast_time' => (string) ($entry['forecast_time'] ?? ''),
                'history_severity_avg' => (float) ($entry['history_severity_avg'] ?? 0.0),
                'report_depth_avg_cm' => (float) ($entry['report_depth_avg_cm'] ?? 0.0),
            ];
        }

        return count($normalized) > 0 ? $normalized : $fallback;
    }

    /**
     * @param mixed $value
     * @param array<int, array<string, mixed>> $fallback
     * @return array<int, array<string, mixed>>
     */
    private function normalizeAlternatives(mixed $value, array $fallback): array
    {
        if (!is_array($value)) {
            return $fallback;
        }

        $normalized = [];
        foreach ($value as $entry) {
            if (!is_array($entry)) {
                continue;
            }
            $normalized[] = [
                'route' => (string) ($entry['route'] ?? 'Alternative'),
                'risk_percent' => (int) ($entry['risk_percent'] ?? 0),
                'recommended_time' => (string) ($entry['recommended_time'] ?? ''),
                'reason' => (string) ($entry['reason'] ?? ''),
            ];
        }

        return count($normalized) > 0 ? $normalized : $fallback;
    }

    /**
     * @param array{lat: float, lng: float} $a
     * @param array{lat: float, lng: float} $b
     */
    private function distanceMeters(array $a, array $b): float
    {
        $earthRadius = 6371000.0;
        $dLat = deg2rad($b['lat'] - $a['lat']);
        $dLng = deg2rad($b['lng'] - $a['lng']);
        $lat1 = deg2rad($a['lat']);
        $lat2 = deg2rad($b['lat']);

        $haversine = sin($dLat / 2) ** 2 + cos($lat1) * cos($lat2) * sin($dLng / 2) ** 2;
        return 2 * $earthRadius * asin(min(1, sqrt($haversine)));
    }

    /**
     * @return array<int, array{lat: float, lng: float}>
     */
    private function decodePolyline(string $encoded): array
    {
        if ($encoded === '') {
            return [];
        }

        $points = [];
        $index = 0;
        $lat = 0;
        $lng = 0;
        $length = strlen($encoded);

        while ($index < $length) {
            $result = 0;
            $shift = 0;
            do {
                $b = ord($encoded[$index++]) - 63;
                $result |= ($b & 0x1f) << $shift;
                $shift += 5;
            } while ($b >= 0x20 && $index < $length);
            $deltaLat = (($result & 1) ? ~($result >> 1) : ($result >> 1));
            $lat += $deltaLat;

            $result = 0;
            $shift = 0;
            do {
                $b = ord($encoded[$index++]) - 63;
                $result |= ($b & 0x1f) << $shift;
                $shift += 5;
            } while ($b >= 0x20 && $index < $length);
            $deltaLng = (($result & 1) ? ~($result >> 1) : ($result >> 1));
            $lng += $deltaLng;

            $points[] = ['lat' => $lat / 1e5, 'lng' => $lng / 1e5];
        }
        return $points;
    }

    /**
     * @param array<int, float> $segmentRisks
     */
    private function saveRoute(string $source, string $destination, int $riskScore, array $segmentRisks): void
    {
        $insert = $this->db->prepare(
            'INSERT INTO routes (source, destination, risk_score, created_at)
             VALUES (:source, :destination, :risk_score, UTC_TIMESTAMP())'
        );
        $insert->execute([
            'source' => $source,
            'destination' => $destination,
            'risk_score' => $riskScore,
        ]);
        $routeId = (int) $this->db->lastInsertId();

        $segmentInsert = $this->db->prepare(
            'INSERT INTO route_segments (route_id, segment_index, risk_score, created_at)
             VALUES (:route_id, :segment_index, :risk_score, UTC_TIMESTAMP())'
        );
        foreach ($segmentRisks as $index => $segmentRisk) {
            $segmentInsert->execute([
                'route_id' => $routeId,
                'segment_index' => $index,
                'risk_score' => (int) round($segmentRisk),
            ]);
        }
    }
}
