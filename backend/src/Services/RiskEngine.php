<?php

declare(strict_types=1);

namespace FloodGuard\Services;

final class RiskEngine
{
    /**
     * @param array<int, float> $hourlyRain
     * @param array<int, int> $historySeverities
     * @param array<int, float> $reportDepthsCm
     * @param array<int, float> $routeSegmentRisks
     * @return array{risk_percent: int, risk_level: string}
     */
    public function compute(
        array $hourlyRain,
        array $historySeverities,
        array $reportDepthsCm,
        array $routeSegmentRisks = []
    ): array {
        $rainScore = $this->rainScore($hourlyRain);
        $historyScore = $this->historyScore($historySeverities);
        $reportsScore = $this->reportsScore($reportDepthsCm);
        $routeScore = $this->routeScore($routeSegmentRisks);

        $weighted = ($rainScore * 0.35) +
            ($historyScore * 0.25) +
            ($reportsScore * 0.25) +
            ($routeScore * 0.15);

        $riskPercent = (int) round(max(0, min(100, $weighted)));
        return [
            'risk_percent' => $riskPercent,
            'risk_level' => $this->riskLevel($riskPercent),
        ];
    }

    public function peakRiskWindow(array $hourlyRain): string
    {
        if (count($hourlyRain) === 0) {
            return 'Unknown';
        }
        $maxIndex = 0;
        $maxValue = $hourlyRain[0];
        foreach ($hourlyRain as $index => $value) {
            if ($value > $maxValue) {
                $maxValue = $value;
                $maxIndex = $index;
            }
        }
        $startHour = (int) date('G') + (int) $maxIndex;
        $endHour = $startHour + 2;
        return sprintf('%02d:00-%02d:00', $startHour % 24, $endHour % 24);
    }

    private function rainScore(array $hourlyRain): float
    {
        if (count($hourlyRain) === 0) {
            return 0;
        }
        $avg = array_sum($hourlyRain) / count($hourlyRain);
        return min(100, $avg * 6.0);
    }

    private function historyScore(array $historySeverities): float
    {
        if (count($historySeverities) === 0) {
            return 25;
        }
        $avg = array_sum($historySeverities) / count($historySeverities);
        return min(100, $avg * 20.0);
    }

    private function reportsScore(array $reportDepthsCm): float
    {
        if (count($reportDepthsCm) === 0) {
            return 15;
        }
        $avgDepth = array_sum($reportDepthsCm) / count($reportDepthsCm);
        return min(100, $avgDepth * 1.8);
    }

    private function routeScore(array $routeSegmentRisks): float
    {
        if (count($routeSegmentRisks) === 0) {
            return 30;
        }
        return min(100, array_sum($routeSegmentRisks) / count($routeSegmentRisks));
    }

    private function riskLevel(int $riskPercent): string
    {
        if ($riskPercent >= 70) {
            return 'High';
        }
        if ($riskPercent >= 40) {
            return 'Medium';
        }
        return 'Low';
    }
}
