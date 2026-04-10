<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Config\Config;
use FloodGuard\Database\Database;
use FloodGuard\Support\CacheStore;

final class PredictionService
{
    private WeatherService $weather;
    private FloodDataService $floodData;
    private RiskEngine $riskEngine;
    private GeminiService $gemini;
    private CacheStore $cache;
    private \PDO $db;

    public function __construct()
    {
        $this->weather = new WeatherService();
        $this->floodData = new FloodDataService();
        $this->riskEngine = new RiskEngine();
        $this->gemini = new GeminiService();
        $this->cache = new CacheStore();
        $this->db = Database::connection();
    }

    /**
     * @return array<string, mixed>
     */
    public function predict(float $lat, float $lng, string $name, string $language = 'en'): array
    {
        $cacheKey = sprintf('prediction:%.4f:%.4f:%s', $lat, $lng, $language);
        $cached = $this->cache->get($cacheKey);
        if ($cached !== null) {
            return $cached;
        }

        $weather = $this->weather->hourlyRain($lat, $lng);
        $history = $this->floodData->historySeverities($lat, $lng);
        $reports = $this->floodData->reportDepths($lat, $lng);
        $risk = $this->riskEngine->compute(
            hourlyRain: $weather['hourly_rain'],
            historySeverities: $history,
            reportDepthsCm: $reports
        );
        $peak = $this->riskEngine->peakRiskWindow($weather['hourly_rain']);

        $geminiInput = [
            'language' => $language,
            'hourly_rain' => $weather['hourly_rain'],
            'history_severities' => $history,
            'recent_report_depths_cm' => $reports,
            'base_risk_percent' => $risk['risk_percent'],
            'peak_risk_window' => $peak,
        ];
        $ai = $this->gemini->locationPrediction($geminiInput);

        $result = [
            'flood_probability' => (int) ($ai['flood_probability'] ?? $risk['risk_percent']),
            'peak_risk_time' => (string) ($ai['peak_risk_time'] ?? $peak),
            'risk_level' => (string) ($ai['risk_level'] ?? $risk['risk_level']),
            'explanation' => (string) ($ai['explanation'] ?? 'Predicted using weather, history, and reports.'),
            'confidence' => (int) ($ai['confidence'] ?? 60),
        ];

        $this->saveWeatherSnapshot($lat, $lng, $name, $weather['hourly_rain'][0] ?? 0.0);
        $ttl = (int) Config::get('PREDICTION_CACHE_TTL', 900);
        $this->cache->put($cacheKey, $result, $ttl);

        return $result;
    }

    private function saveWeatherSnapshot(float $lat, float $lng, string $name, float $rainMm): void
    {
        $locationId = $this->floodData->upsertLocation($lat, $lng, $name);
        $insert = $this->db->prepare(
            'INSERT INTO weather_data (location_id, rain_mm, time)
             VALUES (:location_id, :rain_mm, UTC_TIMESTAMP())'
        );
        $insert->execute([
            'location_id' => $locationId,
            'rain_mm' => $rainMm,
        ]);
    }
}
