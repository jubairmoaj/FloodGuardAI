<?php

declare(strict_types=1);

namespace FloodGuard\Support;

final class HttpClient
{
    /**
     * @return array{status: int, body: array<string, mixed>}
     */
    public static function get(string $url, array $headers = []): array
    {
        return self::request('GET', $url, null, $headers);
    }

    /**
     * @param array<string, mixed>|null $payload
     * @return array{status: int, body: array<string, mixed>}
     */
    public static function post(string $url, ?array $payload = null, array $headers = []): array
    {
        return self::request('POST', $url, $payload, $headers);
    }

    /**
     * @param array<string, mixed>|null $payload
     * @return array{status: int, body: array<string, mixed>}
     */
    private static function request(string $method, string $url, ?array $payload, array $headers): array
    {
        $ch = curl_init($url);
        if ($ch === false) {
            return ['status' => 0, 'body' => []];
        }

        $requestHeaders = ['Accept: application/json'];
        foreach ($headers as $key => $value) {
            if (is_string($key)) {
                $requestHeaders[] = $key . ': ' . $value;
            } else {
                $requestHeaders[] = (string) $value;
            }
        }

        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 20,
            CURLOPT_CUSTOMREQUEST => $method,
        ]);

        if ($payload !== null) {
            $json = json_encode($payload);
            curl_setopt($ch, CURLOPT_POSTFIELDS, $json ?: '{}');
            $requestHeaders[] = 'Content-Type: application/json';
        }
        curl_setopt($ch, CURLOPT_HTTPHEADER, $requestHeaders);

        $raw = curl_exec($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        curl_close($ch);

        if ($raw === false || $raw === '') {
            return ['status' => $status, 'body' => []];
        }
        $decoded = json_decode($raw, true);
        return ['status' => $status, 'body' => is_array($decoded) ? $decoded : []];
    }
}
