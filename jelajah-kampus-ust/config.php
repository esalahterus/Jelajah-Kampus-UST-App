<?php
// Konfigurasi koneksi database - default Laragon (root tanpa password).
// Ubah jika Anda menggunakan password MySQL.
const DB_HOST = '127.0.0.1';
const DB_USER = 'root';
const DB_PASS = '';
const DB_NAME = 'jelajah_kampus_ust';

// Batas ukuran upload foto (byte) dan lokasi penyimpanan.
const MAX_UPLOAD_BYTES = 8 * 1024 * 1024;
const UPLOAD_EVIDENCE_DIR = __DIR__ . '/uploads/evidence/';
const UPLOAD_MEDIA_DIR = __DIR__ . '/uploads/media/';

mysqli_report(MYSQLI_REPORT_OFF);
$db = @new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
if ($db->connect_errno) {
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['success' => false, 'message' => 'Koneksi database gagal']);
    exit;
}
$db->set_charset('utf8mb4');
date_default_timezone_set('Asia/Jakarta');
$db->query("SET time_zone = '+07:00'");
