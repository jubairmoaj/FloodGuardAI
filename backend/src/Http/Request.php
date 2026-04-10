<?php

declare(strict_types=1);

namespace FloodGuard\Http;

final class Request
{
    public readonly string $method;
    public readonly string $path;
    public readonly array $query;
    public readonly array $headers;
    public readonly array $body;
    public readonly array $files;
    public readonly string $ip;
    public array $params = [];
    public ?array $user = null;

    public function __construct()
    {
        $this->method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
        $uri = $_SERVER['REQUEST_URI'] ?? '/';
        $this->path = parse_url($uri, PHP_URL_PATH) ?: '/';
        $this->query = $_GET;
        $this->headers = $this->collectHeaders();
        $this->files = $_FILES;
        $this->ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
        $this->body = $this->parseBody();
    }

    public function bearerToken(): ?string
    {
        $header = $this->headers['Authorization'] ?? $this->headers['authorization'] ?? '';
        if (preg_match('/Bearer\s+(.+)/i', $header, $matches) !== 1) {
            return null;
        }
        return trim($matches[1]);
    }

    private function parseBody(): array
    {
        if ($this->method === 'GET' || $this->method === 'DELETE') {
            return [];
        }
        $contentType = $this->headers['Content-Type'] ?? $this->headers['content-type'] ?? '';
        if (str_contains($contentType, 'application/json')) {
            $raw = file_get_contents('php://input');
            if ($raw === false || trim($raw) === '') {
                return [];
            }
            $decoded = json_decode($raw, true);
            return is_array($decoded) ? $decoded : [];
        }
        return $_POST;
    }

    /**
     * @return array<string, string>
     */
    private function collectHeaders(): array
    {
        if (function_exists('getallheaders')) {
            $headers = getallheaders();
            if (is_array($headers)) {
                return $headers;
            }
        }

        $headers = [];
        foreach ($_SERVER as $key => $value) {
            if (!is_string($value)) {
                continue;
            }
            if (str_starts_with($key, 'HTTP_')) {
                $name = str_replace('_', '-', strtolower(substr($key, 5)));
                $headers[$name] = $value;
            }
        }
        if (!isset($headers['authorization']) && isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
            $headers['authorization'] = (string) $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
        }
        return $headers;
    }
}
