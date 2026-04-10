<?php

declare(strict_types=1);

require_once dirname(__DIR__) . '/src/bootstrap.php';

use FloodGuard\Support\Jwt;

$token = Jwt::encode(['sub' => 42, 'email' => 'test@example.com'], 120);
$decoded = Jwt::decode($token);

if (!is_array($decoded)) {
    throw new RuntimeException('JWT should decode successfully.');
}

if ((int) ($decoded['sub'] ?? 0) !== 42) {
    throw new RuntimeException('JWT sub claim mismatch.');
}

echo "JwtLifecycleTest passed\n";
