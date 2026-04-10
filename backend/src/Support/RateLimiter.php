<?php

declare(strict_types=1);

namespace FloodGuard\Support;

use FloodGuard\Database\Database;
use PDO;

final class RateLimiter
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::connection();
    }

    public function hit(string $key, int $limit, int $windowSeconds): bool
    {
        $this->cleanup();

        $this->db->beginTransaction();
        try {
            $select = $this->db->prepare('SELECT id, hit_count FROM rate_limits WHERE rate_key = :rate_key LIMIT 1 FOR UPDATE');
            $select->execute(['rate_key' => $key]);
            $row = $select->fetch();

            if (!$row) {
                $insert = $this->db->prepare(
                    'INSERT INTO rate_limits (rate_key, hit_count, window_started_at, expires_at)
                     VALUES (:rate_key, 1, UTC_TIMESTAMP(), DATE_ADD(UTC_TIMESTAMP(), INTERVAL :seconds SECOND))'
                );
                $insert->bindValue('rate_key', $key);
                $insert->bindValue('seconds', $windowSeconds, PDO::PARAM_INT);
                $insert->execute();
                $this->db->commit();
                return true;
            }

            $hitCount = (int) $row['hit_count'];
            if ($hitCount >= $limit) {
                $this->db->commit();
                return false;
            }

            $update = $this->db->prepare('UPDATE rate_limits SET hit_count = hit_count + 1 WHERE id = :id');
            $update->execute(['id' => $row['id']]);
            $this->db->commit();
            return true;
        } catch (\Throwable) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            return false;
        }
    }

    private function cleanup(): void
    {
        $statement = $this->db->prepare('DELETE FROM rate_limits WHERE expires_at < UTC_TIMESTAMP()');
        $statement->execute();
    }
}
