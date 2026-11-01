<?php
require_once __DIR__ . '/../helpers.php';
require_method('POST');

$user = auth_user(false);
$stmt = $db->prepare("UPDATE users SET api_token = NULL WHERE id = ?");
$stmt->bind_param('i', $user['id']);
$stmt->execute();
$stmt->close();

// Akhiri sesi eksplorasi yang masih aktif (BR-10).
if ($user['student_id'] !== null) {
    $stmt = $db->prepare("UPDATE mission_sessions SET status='ended', end_time=NOW() WHERE student_id=? AND status='active'");
    $stmt->bind_param('i', $user['student_id']);
    $stmt->execute();
    $stmt->close();
}
json_out(['success' => true, 'message' => 'Logout berhasil']);
