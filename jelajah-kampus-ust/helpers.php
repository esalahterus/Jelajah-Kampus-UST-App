<?php
require_once __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

function json_out(array $data, int $code = 200): void {
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function fail(string $message, int $code = 400): void {
    json_out(['success' => false, 'message' => $message], $code);
}

function require_method(string $method): void {
    if ($_SERVER['REQUEST_METHOD'] !== $method) {
        fail('Metode tidak diizinkan', 405);
    }
}

function input(string $key, string $default = ''): string {
    return isset($_POST[$key]) ? trim((string)$_POST[$key]) : (isset($_GET[$key]) ? trim((string)$_GET[$key]) : $default);
}

/** Alamat dasar server, dipakai untuk membentuk URL foto/media absolut. */
function base_url(): string {
    $scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
    $dir = rtrim(str_replace('\\', '/', dirname(dirname($_SERVER['SCRIPT_NAME']))), '/');
    return $scheme . '://' . $_SERVER['HTTP_HOST'] . $dir . '/';
}

function absolute_url(?string $path): string {
    if (!$path) return '';
    if (preg_match('#^https?://#i', $path)) return $path;
    return base_url() . ltrim($path, '/');
}

/** Ambil pengguna dari token (header X-Auth-Token atau parameter token). */
function auth_user(bool $requireStudent = true): array {
    global $db;
    $token = $_SERVER['HTTP_X_AUTH_TOKEN'] ?? input('token');
    if ($token === '') fail('Belum login', 401);

    $stmt = $db->prepare("SELECT u.id, u.name, u.email, u.role, u.status, s.id AS student_id, s.nim, s.program_study
                          FROM users u LEFT JOIN students s ON s.user_id = u.id
                          WHERE u.api_token = ? LIMIT 1");
    $stmt->bind_param('s', $token);
    $stmt->execute();
    $user = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if (!$user || $user['status'] !== 'active') fail('Sesi tidak valid, silakan login ulang', 401);
    if ($requireStudent && $user['student_id'] === null) fail('Fitur ini hanya untuk mahasiswa', 403);
    return $user;
}

function user_payload(array $u): array {
    return [
        'id' => (int)$u['id'],
        'name' => $u['name'],
        'email' => $u['email'],
        'nim' => $u['nim'] ?? '',
        'program_study' => $u['program_study'] ?? '',
    ];
}

/** Jarak (meter) antara dua koordinat - rumus Haversine. */
function distance_m(float $lat1, float $lon1, float $lat2, float $lon2): float {
    $r = 6371000;
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat / 2) ** 2 + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLon / 2) ** 2;
    return $r * 2 * atan2(sqrt($a), sqrt(1 - $a));
}
