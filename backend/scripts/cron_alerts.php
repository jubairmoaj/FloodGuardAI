<?php

declare(strict_types=1);

use FloodGuard\Services\AlertService;

require_once dirname(__DIR__) . '/src/bootstrap.php';

$service = new AlertService();
$result = $service->runThresholdChecks();

echo json_encode([
    'status' => 'ok',
    'checked' => $result['checked'],
    'triggered' => $result['triggered'],
    'ran_at' => gmdate('Y-m-d\TH:i:s\Z'),
], JSON_UNESCAPED_SLASHES) . PHP_EOL;
