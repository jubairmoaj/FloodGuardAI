<?php

declare(strict_types=1);

date_default_timezone_set('Asia/Dhaka');

spl_autoload_register(static function (string $class): void {
    $prefix = 'FloodGuard\\';
    if (str_starts_with($class, $prefix) === false) {
        return;
    }

    $relativeClass = substr($class, strlen($prefix));
    $relativePath = str_replace('\\', DIRECTORY_SEPARATOR, $relativeClass) . '.php';
    $fullPath = __DIR__ . DIRECTORY_SEPARATOR . $relativePath;

    if (file_exists($fullPath)) {
        require_once $fullPath;
    }
});

FloodGuard\Config\Config::loadEnv(dirname(__DIR__) . '/.env');
