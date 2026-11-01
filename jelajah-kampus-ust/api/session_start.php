<?php
require_once __DIR__ . '/../helpers.php';
require_method('POST');

$user = auth_user();
$sid = (int)$user['student_id'];
$mid = (int)input('mission_id');
if ($mid <= 0) fail('mission_id tidak valid');

$stmt = $db->prepare("SELECT id FROM missions WHERE id=? AND status='active'");
$stmt->bind_param('i', $mid);
$stmt->execute();
if ($stmt->get_result()->num_rows === 0) fail('Misi tidak ditemukan', 404);
$stmt->close();

// Misi yang sudah selesai tidak dapat dimulai lagi.
$stmt = $db->prepare("SELECT 1 FROM progresses p JOIN mission_sessions s ON s.id=p.session_id
                      WHERE s.student_id=? AND s.mission_id=? AND p.completion_status='completed' LIMIT 1");
$stmt->bind_param('ii', $sid, $mid);
$stmt->execute();
if ($stmt->get_result()->num_rows > 0) fail('Misi ini sudah selesai');
$stmt->close();

// Gunakan sesi aktif yang ada bila sudah dimulai.
$stmt = $db->prepare("SELECT id FROM mission_sessions WHERE student_id=? AND mission_id=? AND status='active' ORDER BY id DESC LIMIT 1");
$stmt->bind_param('ii', $sid, $mid);
$stmt->execute();
$existing = $stmt->get_result()->fetch_assoc();
$stmt->close();
if ($existing) json_out(['success' => true, 'session_id' => (int)$existing['id'], 'message' => 'Sesi dilanjutkan']);

// Satu mahasiswa hanya memiliki satu sesi aktif: akhiri sesi lain.
$stmt = $db->prepare("UPDATE mission_sessions SET status='ended', end_time=NOW() WHERE student_id=? AND status='active'");
$stmt->bind_param('i', $sid);
$stmt->execute();
$stmt->close();

$stmt = $db->prepare("INSERT INTO mission_sessions (student_id, mission_id, start_time, status) VALUES (?, ?, NOW(), 'active')");
$stmt->bind_param('ii', $sid, $mid);
if (!$stmt->execute()) fail('Gagal membuat sesi', 500);
$sessionId = $db->insert_id;
$stmt->close();

$stmt = $db->prepare("INSERT INTO progresses (session_id, completion_status, total_points) VALUES (?, 'in_progress', 0)");
$stmt->bind_param('i', $sessionId);
$stmt->execute();
$stmt->close();

json_out(['success' => true, 'session_id' => (int)$sessionId, 'message' => 'Sesi eksplorasi dimulai']);
