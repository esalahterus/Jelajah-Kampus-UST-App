<?php
require_once __DIR__ . '/../helpers.php';
require_method('POST');

$user = auth_user();
$sid = (int)$user['student_id'];
$sessionId = (int)input('session_id');
$lat = input('latitude');
$lon = input('longitude');

if ($sessionId <= 0 || !is_numeric($lat) || !is_numeric($lon)) fail('Data lokasi tidak valid');
$lat = (float)$lat;
$lon = (float)$lon;
if ($lat < -90 || $lat > 90 || $lon < -180 || $lon > 180) fail('Koordinat di luar jangkauan');

// BR-07: lokasi hanya diperbarui selama sesi aktif milik pengguna.
$stmt = $db->prepare("SELECT id FROM mission_sessions WHERE id=? AND student_id=? AND status='active'");
$stmt->bind_param('ii', $sessionId, $sid);
$stmt->execute();
if ($stmt->get_result()->num_rows === 0) fail('Sesi tidak aktif', 409);
$stmt->close();

$stmt = $db->prepare("INSERT INTO tracking_locations (session_id, latitude, longitude, recorded_at) VALUES (?, ?, ?, NOW())");
$stmt->bind_param('idd', $sessionId, $lat, $lon);
if (!$stmt->execute()) fail('Gagal menyimpan lokasi', 500);
$stmt->close();

// Batasi riwayat: hapus catatan lebih dari 1 hari milik sesi ini.
$db->query("DELETE FROM tracking_locations WHERE session_id = $sessionId AND recorded_at < (NOW() - INTERVAL 1 DAY)");

json_out(['success' => true]);
