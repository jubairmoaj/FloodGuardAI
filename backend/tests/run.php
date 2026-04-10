<?php

declare(strict_types=1);

$tests = [
    __DIR__ . '/RiskEngineTest.php',
    __DIR__ . '/JwtLifecycleTest.php',
];

foreach ($tests as $test) {
    passthru('php ' . escapeshellarg($test), $status);
    if ($status !== 0) {
        exit($status);
    }
}

echo "All backend tests passed\n";
