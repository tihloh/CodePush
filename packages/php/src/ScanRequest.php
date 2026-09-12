<?php
namespace Tihloh\CodePush;

use JsonException;
use RuntimeException;

final class ScanRequest
{
    public function __construct(private array $data, private string $method = 'POST')
    {
        $this->method = strtoupper($method);
    }

    public static function fromGlobals(): self
    {
        $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
        if ($method === 'GET') return new self($_GET, 'GET');

        $raw = file_get_contents('php://input');
        if (is_string($raw) && trim($raw) !== '') {
            try {
                $decoded = json_decode($raw, true, 512, JSON_THROW_ON_ERROR);
                if (is_array($decoded)) return new self($decoded, $method);
            } catch (JsonException) {
                // Fall back to ordinary form input below.
            }
        }

        return new self($_POST, $method);
    }

    public function method(): string
    {
        return $this->method;
    }

    public function all(): array
    {
        return $this->data;
    }

    public function has(string $key): bool
    {
        return array_key_exists($key, $this->data);
    }

    public function input(string $key, mixed $default = null): mixed
    {
        return $this->data[$key] ?? $default;
    }

    public function scan(string $key = 'value'): string
    {
        $value = $this->input($key);
        if (!is_scalar($value) || trim((string) $value) === '') throw new RuntimeException("Missing CodePush scan field: {$key}");
        return (string) $value;
    }
}
