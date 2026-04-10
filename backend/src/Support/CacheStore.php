<?php

declare(strict_types=1);

namespace FloodGuard\Support;

use FloodGuard\Database\Database;
use PDO;

final class CacheStore
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::connection();
    }

    /**
     * @return array<string, mixed>|null
     */
    public function get(string $cacheKey): ?array
    {
        $statement = $this->db->prepare(
            'SELECT payload FROM cache_entries WHERE cache_key = :cache_key AND expires_at > UTC_TIMESTAMP() LIMIT 1'
        );
        $statement->execute(['cache_key' => $cacheKey]);
        $row = $statement->fetch();
        if (!$row) {
            return null;
        }
        $decoded = json_decode((string) $row['payload'], true);
        return is_array($decoded) ? $decoded : null;
    }

    /**
     * @param array<string, mixed> $payload
     */
    public function put(string $cacheKey, array $payload, int $ttlSeconds): void
    {
        $statement = $this->db->prepare(
            'INSERT INTO cache_entries (cache_key, payload, expires_at)
             VALUES (:cache_key, :payload, DATE_ADD(UTC_TIMESTAMP(), INTERVAL :ttl SECOND))
             ON DUPLICATE KEY UPDATE payload = VALUES(payload), expires_at = VALUES(expires_at)'
        );
        $statement->bindValue('cache_key', $cacheKey);
        $statement->bindValue('payload', json_encode($payload) ?: '{}');
        $statement->bindValue('ttl', $ttlSeconds, PDO::PARAM_INT);
        $statement->execute();
    }
}
