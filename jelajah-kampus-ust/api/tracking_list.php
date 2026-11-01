<?php
require_once __DIR__ . '/../helpers.php';
require_method('GET');

$user = auth_user();
$sid = (int)$user['student_id'];
$mid = (int)input('mission_id');
if ($mid <= 0) fail('mission_id tidak valid');

// Privasi (NFR-05): hanya peserta dengan sesi aktif pada misi ini yang dapat melihat peserta lain.
$stmt = $db->prepare("SELECT id FROM mission_sessions WHERE student_id=? AND mission_id=? AND status='active' LIMIT 1");
$stmt->bind_param('ii', $sid, $mid);
$stmt->execute();
if ($stmt->get_result()->num_rows === 0) fail('Mulai sesi eksplorasi untuk melihat peserta', 403);
$stmt->close();

$sql = "SELECT ms.id AS session_id, st.id AS student_id, u.name, st.nim,
               (SELECT t.latitude  FROM tracking_locations t WHERE t.session_id = ms.id ORDER BY t.recorded_at DESC, t.id DESC LIMIT 1) AS latitude,
               (SELECT t.longitude FROM tracking_locations t WHERE t.session_id = ms.id ORDER BY t.recorded_at DESC, t.id DESC LIMIT 1) AS longitude,
               (SELECT t.recorded_at FROM tracking_locations t WHERE t.session_id = ms.id ORDER BY t.recorded_at DESC, t.id DESC LIMIT 1) AS recorded_at
        FROM mission_sessions ms
        JOIN students st ON st.id = ms.student_id
        JOIN users u ON u.id = st.user_id
        WHERE ms.mission_id = ? AND ms.status = 'active'";
$stmt = $db->prepare($sql);
$stmt->bind_param('i', $mid);
$stmt->execute();
$res = $stmt->get_result();

$participants = [];
$now = time();
while ($p = $res->fetch_assoc()) {
    if ($p['latitude'] === null) {
        $status = 'unavailable';          // Lokasi tidak tersedia
    } elseif ($now - strtotime($p['recorded_at']) > 120) {
        $status = 'inactive';             // Tidak aktif (tidak ada pembaruan > 2 menit)
    } else {
        $status = 'active';
    }
    $participants[] = [
        'session_id' => (int)$p['session_id'],
        'participant_id' => (int)$p['student_id'],
        'name' => $p['name'],
        'nim' => $p['nim'],
        'latitude' => $p['latitude'] !== null ? (float)$p['latitude'] : null,
        'longitude' => $p['longitude'] !== null ? (float)$p['longitude'] : null,
        'recorded_at' => $p['recorded_at'],
        'status' => $status,
        'is_me' => ((int)$p['student_id'] === $sid),
    ];
}
$stmt->close();
json_out(['success' => true, 'participants' => $participants]);
