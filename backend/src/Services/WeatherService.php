<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Config\Config;
use FloodGuard\Support\CacheStore;
use FloodGuard\Support\HttpClient;

final class WeatherService
{
    private CacheStore $cache;

    public function __construct()
    {
        $this->cache = new CacheStore();
    }

    /**
     * @return array{hourly_rain: array<int, float>, source: string}
     */
    public function hourlyRain(float $lat, float $lng): array
    {
        $forecast = $this->hourlyForecast($lat, $lng);
        return [
            'hourly_rain' => array_map(
                static fn(array $entry): float => (float) ($entry['rain_mm'] ?? 0.0),
                $forecast['entries']
            ),
            'source' => $forecast['source'],
        ];
    }

    /**
     * @return array{rain_mm: float, forecast_time: string, source: string, hour_index: int, hourly_rain: array<int, float>}
     */
    public function forecastForTime(float $lat, float $lng, string $time): array
    {
        $forecast = $this->hourlyForecast($lat, $lng);
        $entries = $forecast['entries'];
        if (count($entries) === 0) {
            return [
                'rain_mm' => 0.0,
                'forecast_time' => $time,
                'source' => $forecast['source'],
                'hour_index' => 0,
                'hourly_rain' => [],
            ];
        }

        $target = $this->targetTimestamp($time, (int) $entries[0]['timestamp']);
        $closestIndex = 0;
        $closestDistance = PHP_INT_MAX;

        foreach ($entries as $index => $entry) {
            $distance = abs(((int) $entry['timestamp']) - $target);
            if ($distance < $closestDistance) {
                $closestDistance = $distance;
                $closestIndex = (int) $index;
            }
        }

        return [
            'rain_mm' => (float) ($entries[$closestIndex]['rain_mm'] ?? 0.0),
            'forecast_time' => gmdate('H:i', (int) ($entries[$closestIndex]['timestamp'] ?? $target)),
            'source' => $forecast['source'],
            'hour_index' => $closestIndex,
            'hourly_rain' => array_map(
                static fn(array $entry): float => (float) ($entry['rain_mm'] ?? 0.0),
                $entries
            ),
        ];
    }

    /**
     * @return array{entries: array<int, array{timestamp: int, rain_mm: float}>, source: string}
     */
    private function hourlyForecast(float $lat, float $lng): array
    {
        $cacheKey = sprintf('weather:%.4f:%.4f', $lat, $lng);
        $cached = $this->cache->get($cacheKey);
        if ($cached !== null && is_array($cached['entries'] ?? null)) {
            return [
                'entries' => $cached['entries'],
                'source' => 'cache',
            ];
        }

        $apiKey = (string) Config::get('OPENWEATHER_API_KEY', '');
        if ($apiKey === '') {
            return $this->fallbackForecast($cacheKey);
        }

        $url = sprintf(
            'https://api.openweathermap.org/data/3.0/onecall?lat=%s&lon=%s&exclude=minutely,daily,alerts&units=metric&appid=%s',
            $lat,
            $lng,
            $apiKey
        );
        $response = HttpClient::get($url);
        $hourly = $response['body']['hourly'] ?? [];
        if (!is_array($hourly) || count($hourly) === 0) {
            return $this->fallbackForecast($cacheKey);
        }

        $entries = [];
        for ($i = 0; $i < min(12, count($hourly)); $i++) {
            $entry = $hourly[$i];
            if (!is_array($entry)) {
                continue;
            }
            $entries[] = [
                'timestamp' => (int) ($entry['dt'] ?? (time() + ($i * 3600))),
                'rain_mm' => (float) ($entry['rain']['1h'] ?? 0.0),
            ];
        }

        if (count($entries) === 0) {
            return $this->fallbackForecast($cacheKey);
        }

        $ttl = (int) Config::get('WEATHER_CACHE_TTL', 600);
        $this->cache->put($cacheKey, ['entries' => $entries], $ttl);

        return [
            'entries' => $entries,
            'source' => 'openweather',
        ];
    }

    /**
     * @return array{entries: array<int, array{timestamp: int, rain_mm: float}>, source: string}
     */
    private function fallbackForecast(string $cacheKey): array
    {
        $fallback = [2.1, 3.4, 4.6, 6.2, 5.1, 3.8, 2.2, 1.4, 0.9, 0.6, 0.5, 0.3];
        $start = time();
        $entries = [];
        foreach ($fallback as $index => $value) {
            $entries[] = [
                'timestamp' => $start + ($index * 3600),
                'rain_mm' => (float) $value,
            ];
        }
        $ttl = (int) Config::get('WEATHER_CACHE_TTL', 600);
        $this->cache->put($cacheKey, ['entries' => $entries], $ttl);

        return [
            'entries' => $entries,
            'source' => 'fallback',
        ];
    }

    private function targetTimestamp(string $time, int $referenceTimestamp): int
    {
        if (preg_match('/^(\d{1,2}):(\d{2})$/', trim($time), $matches) !== 1) {
            return $referenceTimestamp;
        }

        $hour = max(0, min(23, (int) $matches[1]));
        $minute = max(0, min(59, (int) $matches[2]));
        $baseDate = gmdate('Y-m-d', $referenceTimestamp);
        $target = strtotime(sprintf('%s %02d:%02d:00 UTC', $baseDate, $hour, $minute));
        if ($target === false) {
            return $referenceTimestamp;
        }
        if ($target < ($referenceTimestamp - 3600)) {
            return $target + 86400;
        }
        return $target;
    }
}
