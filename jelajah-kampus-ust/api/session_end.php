<?php
require_once __DIR__ . '/../helpers.php';
require_method('POST');

$user = auth_user();
$sid = (int)$user['student_id'];
$sessionId = (int)input('session_id');
if ($sessionId <= 0) fail('session_id tidak valid');

$stmt = $db->prepare("UPDATE mission_sessions SET status='ended', end_time=NOW() WHERE id=? AND student_id=? AND status='active'");
$stmt->bind_param('ii', $sessionId, $sid);
$stmt->execute();
$affected = $stmt->affected_rows;
$stmt->close();

json_out(['success' => true, 'message' => $affected > 0 ? 'Sesi diakhiri, pelacakan dihentikan' : 'Sesi sudah berakhir']);
