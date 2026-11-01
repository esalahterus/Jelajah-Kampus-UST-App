<?php
require_once __DIR__ . '/../helpers.php';
require_method('GET');

$user = auth_user();
$sid = (int)$user['student_id'];

$sql = "SELECT m.id, m.title, m.description, m.point_reward, m.building_id,
               b.name AS building_name, b.description AS building_description,
               l.latitude, l.longitude
        FROM missions m
        JOIN buildings b ON b.id = m.building_id
        LEFT JOIN locations l ON l.building_id = b.id
        WHERE m.status = 'active'
        ORDER BY m.id";
$res = $db->query($sql);
$missions = [];

while ($m = $res->fetch_assoc()) {
    $mid = (int)$m['id'];

    // Sesi aktif milik mahasiswa untuk misi ini
    $stmt = $db->prepare("SELECT id FROM mission_sessions WHERE student_id=? AND mission_id=? AND status='active' ORDER BY id DESC LIMIT 1");
    $stmt->bind_param('ii', $sid, $mid);
    $stmt->execute();
    $active = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    // Misi selesai jika ada progres completed pada salah satu sesi (BR-05)
    $stmt = $db->prepare("SELECT 1 FROM progresses p JOIN mission_sessions s ON s.id=p.session_id
                          WHERE s.student_id=? AND s.mission_id=? AND p.completion_status='completed' LIMIT 1");
    $stmt->bind_param('ii', $sid, $mid);
    $stmt->execute();
    $completed = $stmt->get_result()->num_rows > 0;
    $stmt->close();

    // Bukti terakhir
    $stmt = $db->prepare("SELECT e.id, e.photo_url, e.submitted_at, e.verification_status, e.admin_note
                          FROM evidences e JOIN mission_sessions s ON s.id=e.session_id
                          WHERE s.student_id=? AND s.mission_id=? ORDER BY e.id DESC LIMIT 1");
    $stmt->bind_param('ii', $sid, $mid);
    $stmt->execute();
    $ev = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if ($completed) $status = 'completed';
    elseif ($ev && $ev['verification_status'] === 'pending') $status = 'pending';
    elseif ($ev && $ev['verification_status'] === 'rejected' && !$active) $status = 'rejected';
    elseif ($active) $status = ($ev && $ev['verification_status'] === 'rejected') ? 'rejected' : 'in_progress';
    else $status = 'not_started';

    $missions[] = [
        'id' => $mid,
        'title' => $m['title'],
        'description' => $m['description'],
        'point_reward' => (int)$m['point_reward'],
        'building_id' => (int)$m['building_id'],
        'building_name' => $m['building_name'],
        'latitude' => $m['latitude'] !== null ? (float)$m['latitude'] : null,
        'longitude' => $m['longitude'] !== null ? (float)$m['longitude'] : null,
        'status' => $status,
        'active_session_id' => $active ? (int)$active['id'] : 0,
        'evidence' => $ev ? [
            'photo_url' => absolute_url($ev['photo_url']),
            'submitted_at' => $ev['submitted_at'],
            'verification_status' => $ev['verification_status'],
            'admin_note' => $ev['admin_note'] ?? '',
        ] : null,
    ];
}
json_out(['success' => true, 'missions' => $missions]);
