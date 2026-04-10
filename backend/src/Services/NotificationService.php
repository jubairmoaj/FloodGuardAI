<?php

declare(strict_types=1);

namespace FloodGuard\Services;

use FloodGuard\Config\Config;
use FloodGuard\Support\HttpClient;

final class NotificationService
{
    /**
     * @param array<int, string> $deviceTokens
     */
    public function push(array $deviceTokens, string $title, string $body, array $data = []): bool
    {
        $serverKey = (string) Config::get('FCM_SERVER_KEY', '');
        if ($serverKey === '' || count($deviceTokens) === 0) {
            return false;
        }

        $success = true;
        foreach ($deviceTokens as $token) {
            $response = HttpClient::post(
                'https://fcm.googleapis.com/fcm/send',
                [
                    'to' => $token,
                    'notification' => [
                        'title' => $title,
                        'body' => $body,
                    ],
                    'data' => $data,
                    'priority' => 'high',
                ],
                [
                    'Authorization' => 'key=' . $serverKey,
                ]
            );
            if ($response['status'] < 200 || $response['status'] >= 300) {
                $success = false;
            }
        }

        return $success;
    }
}
