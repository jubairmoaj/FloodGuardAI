<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Database\Database;
use FloodGuard\Support\Jwt;
use RuntimeException;

final class AuthService
{
    private \PDO $db;

    public function __construct()
    {
        $this->db = Database::connection();
    }

    /**
     * @param array<string, mixed> $payload
     * @return array<string, mixed>
     */
    public function register(array $payload): array
    {
        $email = strtolower(trim((string) ($payload['email'] ?? '')));
        $password = (string) ($payload['password'] ?? '');
        $name = trim((string) ($payload['name'] ?? 'FloodGuard User'));
        if ($email === '' || $password === '') {
            throw new RuntimeException('email and password are required');
        }

        $existing = $this->db->prepare('SELECT id FROM users WHERE email = :email LIMIT 1');
        $existing->execute(['email' => $email]);
        if ($existing->fetch()) {
            throw new RuntimeException('email already registered');
        }

        $insert = $this->db->prepare(
            'INSERT INTO users (name, email, password_hash, language, created_at, updated_at)
             VALUES (:name, :email, :password_hash, :language, UTC_TIMESTAMP(), UTC_TIMESTAMP())'
        );
        $insert->execute([
            'name' => $name,
            'email' => $email,
            'password_hash' => password_hash($password, PASSWORD_BCRYPT),
            'language' => 'en',
        ]);
        $id = (int) $this->db->lastInsertId();

        return $this->issueTokens($this->findUserById($id));
    }

    /**
     * @param array<string, mixed> $payload
     * @return array<string, mixed>
     */
    public function login(array $payload): array
    {
        $email = strtolower(trim((string) ($payload['email'] ?? '')));
        $password = (string) ($payload['password'] ?? '');

        $statement = $this->db->prepare('SELECT * FROM users WHERE email = :email LIMIT 1');
        $statement->execute(['email' => $email]);
        $user = $statement->fetch();
        if (!$user || !password_verify($password, (string) $user['password_hash'])) {
            throw new RuntimeException('invalid credentials');
        }

        return $this->issueTokens($user);
    }

    /**
     * @param array<string, mixed> $payload
     * @return array<string, mixed>
     */
    public function refresh(array $payload): array
    {
        $token = (string) ($payload['refresh_token'] ?? '');
        if ($token === '') {
            throw new RuntimeException('refresh_token is required');
        }
        $tokenHash = hash('sha256', $token);
        $statement = $this->db->prepare(
            'SELECT * FROM refresh_tokens WHERE token_hash = :token_hash AND revoked_at IS NULL AND expires_at > UTC_TIMESTAMP() LIMIT 1'
        );
        $statement->execute(['token_hash' => $tokenHash]);
        $refresh = $statement->fetch();
        if (!$refresh) {
            throw new RuntimeException('invalid refresh token');
        }

        $user = $this->findUserById((int) $refresh['user_id']);
        if (!$user) {
            throw new RuntimeException('user not found');
        }

        $revoke = $this->db->prepare('UPDATE refresh_tokens SET revoked_at = UTC_TIMESTAMP() WHERE id = :id');
        $revoke->execute(['id' => $refresh['id']]);

        return $this->issueTokens($user);
    }

    /**
     * @return array<string, mixed>|null
     */
    public function authenticate(?string $accessToken): ?array
    {
        if ($accessToken === null || $accessToken === '') {
            return null;
        }
        $claims = Jwt::decode($accessToken);
        if ($claims === null) {
            return null;
        }
        $userId = (int) ($claims['sub'] ?? 0);
        return $this->findUserById($userId);
    }

    /**
     * @param array<string, mixed> $user
     * @return array<string, mixed>
     */
    private function issueTokens(array $user): array
    {
        $userId = (int) $user['id'];
        $accessToken = Jwt::encode([
            'sub' => $userId,
            'email' => $user['email'],
        ], 3600);
        $refreshToken = bin2hex(random_bytes(32));
        $tokenHash = hash('sha256', $refreshToken);

        $insert = $this->db->prepare(
            'INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at)
             VALUES (:user_id, :token_hash, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 DAY), UTC_TIMESTAMP())'
        );
        $insert->execute([
            'user_id' => $userId,
            'token_hash' => $tokenHash,
        ]);

        return [
            'access_token' => $accessToken,
            'refresh_token' => $refreshToken,
            'user' => $this->publicUser($user),
        ];
    }

    /**
     * @return array<string, mixed>|null
     */
    private function findUserById(int $id): ?array
    {
        $statement = $this->db->prepare('SELECT * FROM users WHERE id = :id LIMIT 1');
        $statement->execute(['id' => $id]);
        $row = $statement->fetch();
        return $row ?: null;
    }

    /**
     * @param array<string, mixed> $user
     * @return array<string, mixed>
     */
    private function publicUser(array $user): array
    {
        return [
            'id' => (int) $user['id'],
            'name' => (string) $user['name'],
            'email' => (string) $user['email'],
            'language' => (string) ($user['language'] ?? 'en'),
        ];
    }
}
