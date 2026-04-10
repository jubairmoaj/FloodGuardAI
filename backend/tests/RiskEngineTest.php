<?php

declare(strict_types=1);

require_once dirname(__DIR__) . '/src/bootstrap.php';

use FloodGuard\Services\RiskEngine;

$engine = new RiskEngine();
$resultLow = $engine->compute([0.5, 0.3], [1, 1], [3, 4], [10, 12]);
$resultHigh = $engine->compute([12, 10, 11], [5, 4], [45, 36], [80, 85, 79]);

if ($resultLow['risk_percent'] >= $resultHigh['risk_percent']) {
    throw new RuntimeException('Risk engine should rank high-risk scenario above low-risk scenario.');
}

if ($resultHigh['risk_level'] !== 'High') {
    throw new RuntimeException('High scenario should classify as High risk.');
}

echo "RiskEngineTest passed\n";
