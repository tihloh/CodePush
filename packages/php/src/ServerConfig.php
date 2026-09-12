<?php
namespace Tihloh\CodePush;

use InvalidArgumentException;

final class ServerConfig
{
    private string $name;
    private string $method;
    private string $url;
    private array $fields = [];
    private ?array $responseRule = null;

    public function __construct(string $name, string $url, string $method = 'POST')
    {
        $name = trim($name);
        $url = trim($url);
        $method = strtoupper(trim($method));
        if ($name === '') throw new InvalidArgumentException('Server name is required.');
        if (!filter_var($url, FILTER_VALIDATE_URL) || !in_array(parse_url($url, PHP_URL_SCHEME), ['http', 'https'], true)) {
            throw new InvalidArgumentException('Server URL must be a valid HTTP or HTTPS URL.');
        }
        if (!in_array($method, ['GET', 'POST'], true)) throw new InvalidArgumentException('Method must be GET or POST.');
        $this->name = $name;
        $this->url = $url;
        $this->method = $method;
    }

    public function field(string $name, string $value): self
    {
        $name = trim($name);
        if ($name === '' || $name === 'scan') throw new InvalidArgumentException('Field name is invalid or reserved.');
        foreach ($this->fields as $field) {
            if ($field['name'] === $name) throw new InvalidArgumentException("Duplicate field: {$name}");
        }
        $clone = clone $this;
        $clone->fields[] = ['name' => $name, 'value' => $value];
        return $clone;
    }

    public function fields(array $fields): self
    {
        $clone = clone $this;
        $clone->fields = [];
        foreach ($fields as $name => $value) {
            if (is_int($name) && is_array($value)) {
                if (!array_key_exists('name', $value) || !array_key_exists('value', $value)) throw new InvalidArgumentException('Each field requires name and value.');
                $clone = $clone->field((string) $value['name'], (string) $value['value']);
            } else {
                $clone = $clone->field((string) $name, (string) $value);
            }
        }
        return $clone;
    }

    public function responseRule(string $type, string $selector, string $operator, string $expected = '', ?string $followUpServerName = null): self
    {
        $type = strtoupper(trim($type));
        $operator = strtoupper(trim($operator));
        if (!in_array($type, ['AUTO', 'TEXT', 'JSON'], true)) throw new InvalidArgumentException('Response rule type must be AUTO, TEXT or JSON.');
        if (!in_array($operator, ['EXISTS', 'EQUALS', 'CONTAINS'], true)) throw new InvalidArgumentException('Response rule operator must be EXISTS, EQUALS or CONTAINS.');
        $clone = clone $this;
        $clone->responseRule = [
            'enabled' => true,
            'type' => $type,
            'selector' => $selector,
            'operator' => $operator,
            'expected' => $expected,
        ];
        if ($followUpServerName !== null && trim($followUpServerName) !== '') $clone->responseRule['followUpServerName'] = trim($followUpServerName);
        return $clone;
    }

    public function toArray(): array
    {
        $server = [
            'name' => $this->name,
            'method' => $this->method,
            'url' => $this->url,
            'fields' => $this->fields,
        ];
        if ($this->responseRule !== null) $server['responseRule'] = $this->responseRule;
        return ['type' => 'codepush.server', 'version' => 1, 'server' => $server];
    }

    public function toJson(int $flags = 0): string
    {
        $json = json_encode($this->toArray(), JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR | $flags);
        return $json;
    }
}
