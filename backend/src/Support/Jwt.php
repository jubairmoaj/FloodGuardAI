<?php

declare(strict_types=1);

namespace FloodGuard\Support;

use FloodGuard\Config\Config;

final class Jwt
{
    public static function encode(array $payload, int $ttlSeconds): string
    {
        $secret = (string) Config::get('APP_SECRET', 'floodguard-secret');
        $header = ['alg' => 'HS256', 'typ' => 'JWT'];
        $issuedAt = time();
        $claims = array_merge($payload, [
            'iat' => $issuedAt,
            'exp' => $issuedAt + $ttlSeconds,
        ]);

        $headerEncoded = self::base64UrlEncode(json_encode($header) ?: '{}');
        $payloadEncoded = self::base64UrlEncode(json_encode($claims) ?: '{}');
        $signature = hash_hmac('sha256', "$headerEncoded.$payloadEncoded", $secret, true);
        $signatureEncoded = self::base64UrlEncode($signature);

        return "$headerEncoded.$payloadEncoded.$signatureEncoded";
    }

    public static function decode(string $jwt): ?array
    {
        $parts = explode('.', $jwt);
        if (count($parts) !== 3) {
            return null;
        }
        [$header, $payload, $signature] = $parts;
        $secret = (string) Config::get('APP_SECRET', 'floodguard-secret');
        $expected = self::base64UrlEncode(hash_hmac('sha256', "$header.$payload", $secret, true));
        if (!hash_equals($expected, $signature)) {
            return null;
        }
        $decodedPayload = json_decode(self::base64UrlDecode($payload), true);
        if (!is_array($decodedPayload)) {
            return null;
        }
        $exp = (int) ($decodedPayload['exp'] ?? 0);
        if ($exp <= time()) {
            return null;
        }
        return $decodedPayload;
    }

    private static function base64UrlEncode(string $value): string
    {
        return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $value): string
    {
        $remainder = strlen($value) % 4;
        if ($remainder > 0) {
            $value .= str_repeat('=', 4 - $remainder);
        }
        return base64_decode(strtr($value, '-_', '+/')) ?: '';
    }
}
