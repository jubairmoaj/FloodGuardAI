<?php

declare(strict_types=1);

namespace FloodGuard\Support;

final class Validator
{
    /**
     * @param array<string, mixed> $payload
     * @param array<int, string> $required
     * @return array<int, string>
     */
    public static function required(array $payload, array $required): array
    {
        $errors = [];
        foreach ($required as $field) {
            $value = $payload[$field] ?? null;
            if ($value === null || (is_string($value) && trim($value) === '')) {
                $errors[] = "$field is required";
            }
        }
        return $errors;
    }
}
