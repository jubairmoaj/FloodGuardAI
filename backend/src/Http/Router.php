<?php

declare(strict_types=1);

namespace FloodGuard\Http;

use Closure;

final class Router
{
    /**
     * @var array<int, array{method: string, pattern: string, handler: Closure, auth: bool}>
     */
    private array $routes = [];

    public function add(string $method, string $pattern, Closure $handler, bool $auth = false): void
    {
        $this->routes[] = [
            'method' => strtoupper($method),
            'pattern' => $pattern,
            'handler' => $handler,
            'auth' => $auth,
        ];
    }

    public function dispatch(Request $request, callable $authenticate): void
    {
        foreach ($this->routes as $route) {
            if ($route['method'] !== $request->method) {
                continue;
            }

            $params = $this->match($route['pattern'], $request->path);
            if ($params === null) {
                continue;
            }

            if ($route['auth']) {
                $request->user = $authenticate($request);
                if ($request->user === null) {
                    Response::json(['error' => 'Unauthorized'], 401);
                    return;
                }
            }

            $request->params = $params;
            ($route['handler'])($request);
            return;
        }

        Response::json(['error' => 'Not Found'], 404);
    }

    /**
     * @return array<string, string>|null
     */
    private function match(string $pattern, string $path): ?array
    {
        $regex = preg_replace('/\{([a-zA-Z0-9_]+)\}/', '(?P<$1>[^/]+)', $pattern);
        $regex = '#^' . $regex . '$#';
        if ($regex === null || preg_match($regex, $path, $matches) !== 1) {
            return null;
        }

        $params = [];
        foreach ($matches as $key => $value) {
            if (is_string($key)) {
                $params[$key] = $value;
            }
        }
        return $params;
    }
}
