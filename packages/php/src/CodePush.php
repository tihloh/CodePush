<?php
namespace Tihloh\CodePush;

final class CodePush
{
    public static function server(string $name, string $url, string $method = 'POST'): ServerConfig
    {
        return new ServerConfig($name, $url, $method);
    }

    public static function request(?array $input = null, ?string $method = null): ScanRequest
    {
        return $input === null ? ScanRequest::fromGlobals() : new ScanRequest($input, $method ?? 'POST');
    }
}
