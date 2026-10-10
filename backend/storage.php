<?php
/**
 * Rasta AI - JSON File Persistent Storage Layer
 * Thread-safe with flock() and atomic replacement writes
 */

require_once __DIR__ . '/config.php';

class JsonStorage {

    public static function getFilePath(string $collection): string {
        if (!is_dir(DATA_DIR)) {
            @mkdir(DATA_DIR, 0750, true);
        }
        return DATA_DIR . '/' . preg_replace('/[^a-z0-9_-]/i', '', $collection) . '.json';
    }

    public static function read(string $collection, $default = []): array {
        $path = self::getFilePath($collection);
        if (!file_exists($path)) {
            return $default;
        }

        $fp = fopen($path, 'rb');
        if (!$fp) {
            return $default;
        }

        flock($fp, LOCK_SH);
        $content = stream_get_contents($fp);
        flock($fp, LOCK_UN);
        fclose($fp);

        if (empty($content)) {
            return $default;
        }

        $decoded = json_decode($content, true);
        return is_array($decoded) ? $decoded : $default;
    }

    public static function write(string $collection, array $data): bool {
        $path = self::getFilePath($collection);
        $tempPath = $path . '.tmp.' . bin2hex(random_bytes(6));

        $json = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        if ($json === false) {
            return false;
        }

        $fp = fopen($tempPath, 'wb');
        if (!$fp) {
            return false;
        }

        flock($fp, LOCK_EX);
        fwrite($fp, $json);
        fflush($fp);
        flock($fp, LOCK_UN);
        fclose($fp);

        // Atomic file replacement
        return rename($tempPath, $path);
    }

    public static function appendLog(string $collection, array $entry): void {
        $logs = self::read($collection, []);
        $entry['id'] = $entry['id'] ?? uniqid('log_', true);
        $entry['timestamp'] = $entry['timestamp'] ?? time();
        array_unshift($logs, $entry);
        if (count($logs) > 500) {
            $logs = array_slice($logs, 0, 500);
        }
        self::write($collection, $logs);
    }
}
