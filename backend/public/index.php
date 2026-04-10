<?php

declare(strict_types=1);

use FloodGuard\Http\Request;
use FloodGuard\Http\Response;
use FloodGuard\Http\Router;
use FloodGuard\Services\AlertService;
use FloodGuard\Services\AuthService;
use FloodGuard\Services\ChatService;
use FloodGuard\Services\MapLayerService;
use FloodGuard\Services\PredictionService;
use FloodGuard\Services\ReportService;
use FloodGuard\Services\RouteAnalysisService;
use FloodGuard\Support\RateLimiter;
use FloodGuard\Support\Validator;

require_once dirname(__DIR__) . '/src/bootstrap.php';

$router = new Router();
$request = new Request();
$auth = new AuthService();
$prediction = new PredictionService();
$routeAnalysis = new RouteAnalysisService();
$reportService = new ReportService();
$alertService = new AlertService();
$chatService = new ChatService();
$mapLayerService = new MapLayerService();
$rateLimiter = new RateLimiter();

$authenticate = static function (Request $request) use ($auth): ?array {
    return $auth->authenticate($request->bearerToken());
};

$limit = static function (Request $request, string $bucket, int $max, int $window) use ($rateLimiter): bool {
    $userPart = $request->user['id'] ?? $request->ip;
    return $rateLimiter->hit($bucket . ':' . $userPart, $max, $window);
};

$router->add('POST', '/api/v1/auth/register', static function (Request $request) use ($auth, $limit): void {
    if (!$limit($request, 'auth-register', 15, 300)) {
        Response::json(['error' => 'Too many requests'], 429);
        return;
    }
    $errors = Validator::required($request->body, ['email', 'password']);
    if ($errors !== []) {
        Response::json(['errors' => $errors], 422);
        return;
    }
    try {
        Response::json($auth->register($request->body), 201);
    } catch (Throwable $throwable) {
        Response::json(['error' => $throwable->getMessage()], 400);
    }
});

$router->add('POST', '/api/v1/auth/login', static function (Request $request) use ($auth, $limit): void {
    if (!$limit($request, 'auth-login', 25, 300)) {
        Response::json(['error' => 'Too many requests'], 429);
        return;
    }
    $errors = Validator::required($request->body, ['email', 'password']);
    if ($errors !== []) {
        Response::json(['errors' => $errors], 422);
        return;
    }
    try {
        Response::json($auth->login($request->body));
    } catch (Throwable $throwable) {
        Response::json(['error' => $throwable->getMessage()], 401);
    }
});

$router->add('POST', '/api/v1/auth/refresh', static function (Request $request) use ($auth): void {
    $errors = Validator::required($request->body, ['refresh_token']);
    if ($errors !== []) {
        Response::json(['errors' => $errors], 422);
        return;
    }
    try {
        Response::json($auth->refresh($request->body));
    } catch (Throwable $throwable) {
        Response::json(['error' => $throwable->getMessage()], 401);
    }
});

$router->add('GET', '/api/v1/me', static function (Request $request): void {
    $user = $request->user ?? [];
    Response::json([
        'id' => (int) ($user['id'] ?? 0),
        'name' => (string) ($user['name'] ?? ''),
        'email' => (string) ($user['email'] ?? ''),
        'language' => (string) ($user['language'] ?? 'en'),
    ]);
}, auth: true);

$router->add('POST', '/api/v1/predictions/location', static function (Request $request) use ($prediction, $limit): void {
    if (!$limit($request, 'prediction', 120, 300)) {
        Response::json(['error' => 'Too many requests'], 429);
        return;
    }
    $location = $request->body['location'] ?? [];
    if (!is_array($location)) {
        Response::json(['error' => 'location object is required'], 422);
        return;
    }
    $lat = (float) ($location['lat'] ?? 0.0);
    $lng = (float) ($location['lng'] ?? 0.0);
    $name = (string) ($location['name'] ?? 'Selected Location');
    $language = (string) ($request->body['language'] ?? 'en');
    Response::json($prediction->predict($lat, $lng, $name, $language));
});

$router->add('POST', '/api/v1/routes/analyze', static function (Request $request) use ($routeAnalysis, $limit): void {
    if (!$limit($request, 'route', 60, 300)) {
        Response::json(['error' => 'Too many requests'], 429);
        return;
    }
    $from = $request->body['from'] ?? [];
    $to = $request->body['to'] ?? [];
    if (!is_array($from) || !is_array($to)) {
        Response::json(['error' => 'from/to are required'], 422);
        return;
    }
    $language = (string) ($request->body['language'] ?? 'en');
    $time = (string) ($request->body['time'] ?? '16:00');
    $result = $routeAnalysis->analyze(
        fromLat: (float) ($from['lat'] ?? 0.0),
        fromLng: (float) ($from['lng'] ?? 0.0),
        fromName: (string) ($from['name'] ?? 'Origin'),
        toLat: (float) ($to['lat'] ?? 0.0),
        toLng: (float) ($to['lng'] ?? 0.0),
        toName: (string) ($to['name'] ?? 'Destination'),
        time: $time,
        language: $language
    );
    Response::json($result);
});

$router->add('GET', '/api/v1/map/layers', static function (Request $request) use ($mapLayerService): void {
    Response::json($mapLayerService->layers());
});

$router->add('POST', '/api/v1/reports', static function (Request $request) use ($reportService, $limit): void {
    if (!$limit($request, 'reports', 20, 300)) {
        Response::json(['error' => 'Too many requests'], 429);
        return;
    }
    $file = $request->files['image'] ?? null;
    if (!is_array($file)) {
        Response::json(['error' => 'image file is required'], 422);
        return;
    }
    try {
        $result = $reportService->create(
            userId: $request->user['id'] ?? null,
            lat: (float) ($request->body['lat'] ?? 0.0),
            lng: (float) ($request->body['lng'] ?? 0.0),
            note: (string) ($request->body['note'] ?? ''),
            file: $file
        );
        Response::json($result, 201);
    } catch (Throwable $throwable) {
        Response::json(['error' => $throwable->getMessage()], 400);
    }
});

$router->add('GET', '/api/v1/reports/{id}/image', static function (Request $request) use ($reportService): void {
    $reportId = (int) ($request->params['id'] ?? 0);
    $token = (string) ($request->query['token'] ?? '');
    $file = $reportService->findImageBySignedToken($reportId, $token);
    if ($file === null) {
        Response::json(['error' => 'Invalid or expired token'], 403);
        return;
    }
    header('Content-Type: ' . $file['mime']);
    readfile($file['path']);
});

$router->add('POST', '/api/v1/reports/{id}/flag', static function (Request $request) use ($reportService): void {
    $reportId = (int) ($request->params['id'] ?? 0);
    $reason = (string) ($request->body['reason'] ?? 'abuse');
    $reportService->flag($reportId, $request->user['id'] ?? null, $reason);
    Response::json(['status' => 'ok'], 201);
}, auth: true);

$router->add('POST', '/api/v1/alerts', static function (Request $request) use ($alertService): void {
    $location = $request->body['location'] ?? [];
    if (!is_array($location)) {
        Response::json(['error' => 'location is required'], 422);
        return;
    }
    $response = $alertService->create(
        userId: (int) ($request->user['id'] ?? 0),
        lat: (float) ($location['lat'] ?? 0.0),
        lng: (float) ($location['lng'] ?? 0.0),
        name: (string) ($location['name'] ?? 'Alert Location'),
        threshold: (int) ($request->body['threshold'] ?? 60)
    );
    Response::json($response, 201);
}, auth: true);

$router->add('GET', '/api/v1/alerts', static function (Request $request) use ($alertService): void {
    Response::json($alertService->list((int) ($request->user['id'] ?? 0)));
}, auth: true);

$router->add('DELETE', '/api/v1/alerts/{id}', static function (Request $request) use ($alertService): void {
    $id = (int) ($request->params['id'] ?? 0);
    Response::json($alertService->delete((int) ($request->user['id'] ?? 0), $id));
}, auth: true);

$router->add('GET', '/api/v1/alerts/history', static function (Request $request) use ($alertService): void {
    Response::json($alertService->history((int) ($request->user['id'] ?? 0)));
}, auth: true);

$router->add('POST', '/api/v1/device-tokens', static function (Request $request) use ($alertService): void {
    $token = (string) ($request->body['device_token'] ?? '');
    if ($token === '') {
        Response::json(['error' => 'device_token is required'], 422);
        return;
    }
    $platform = (string) ($request->body['platform'] ?? 'android');
    $alertService->registerDeviceToken((int) ($request->user['id'] ?? 0), $token, $platform);
    Response::json(['alerts' => []], 201);
}, auth: true);

$router->add('POST', '/api/v1/chat/ask', static function (Request $request) use ($chatService, $limit): void {
    if (!$limit($request, 'chat', 90, 300)) {
        Response::json(['error' => 'Too many requests'], 429);
        return;
    }
    $question = (string) ($request->body['question'] ?? '');
    if ($question === '') {
        Response::json(['error' => 'question is required'], 422);
        return;
    }
    $context = $request->body['context'] ?? [];
    $language = (string) ($request->body['language'] ?? 'en');
    Response::json($chatService->ask($question, is_array($context) ? $context : [], $language));
});

$router->dispatch($request, $authenticate);
