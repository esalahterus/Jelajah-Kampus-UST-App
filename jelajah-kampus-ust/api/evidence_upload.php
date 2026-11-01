<?php
require_once __DIR__ . '/../helpers.php';
require_method('POST');

$user = auth_user();
$sid = (int)$user['student_id'];
$sessionId = (int)input('session_id');
if ($sessionId <= 0) fail('session_id tidak valid');

// BR-02: bukti hanya untuk misi yang sedang diikuti (sesi aktif milik pengguna).
$stmt = $db->prepare("SELECT id FROM mission_sessions WHERE id=? AND student_id=? AND status='active'");
$stmt->bind_param('ii', $sessionId, $sid);
$stmt->execute();
if ($stmt->get_result()->num_rows === 0) fail('Mulai misi terlebih dahulu sebelum mengirim bukti', 409);
$stmt->close();

// Tidak boleh mengirim ulang jika bukti pada sesi ini masih menunggu / sudah diterima.
$stmt = $db->prepare("SELECT verification_status FROM evidences WHERE session_id=? ORDER BY id DESC LIMIT 1");
$stmt->bind_param('i', $sessionId);
$stmt->execute();
$last = $stmt->get_result()->fetch_assoc();
$stmt->close();
if ($last && $last['verification_status'] === 'pending') fail('Bukti sebelumnya masih menunggu verifikasi', 409);
if ($last && $last['verification_status'] === 'accepted') fail('Bukti sudah diterima', 409);

if (!isset($_FILES['photo']) || $_FILES['photo']['error'] !== UPLOAD_ERR_OK) fail('Foto tidak diterima');
$file = $_FILES['photo'];
if ($file['size'] <= 0 || $file['size'] > MAX_UPLOAD_BYTES) fail('Ukuran foto maksimal 8 MB');

$finfo = new finfo(FILEINFO_MIME_TYPE);
$mime = $finfo->file($file['tmp_name']);
$allowed = ['image/jpeg' => 'jpg', 'image/png' => 'png'];
if (!isset($allowed[$mime]) || @getimagesize($file['tmp_name']) === false) fail('Format foto harus JPG atau PNG');

if (!is_dir(UPLOAD_EVIDENCE_DIR)) mkdir(UPLOAD_EVIDENCE_DIR, 0775, true);
$name = 'ev_' . $sessionId . '_' . bin2hex(random_bytes(8)) . '.' . $allowed[$mime];
if (!move_uploaded_file($file['tmp_name'], UPLOAD_EVIDENCE_DIR . $name)) fail('Gagal menyimpan foto', 500);

$path = 'uploads/evidence/' . $name;
$stmt = $db->prepare("INSERT INTO evidences (session_id, photo_url, submitted_at, verification_status) VALUES (?, ?, NOW(), 'pending')");
$stmt->bind_param('is', $sessionId, $path);
if (!$stmt->execute()) {
    @unlink(UPLOAD_EVIDENCE_DIR . $name);
    fail('Gagal menyimpan data bukti', 500);
}
$stmt->close();

json_out(['success' => true, 'message' => 'Bukti terkirim, menunggu verifikasi', 'status' => 'pending']);
