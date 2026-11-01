<?php
require_once __DIR__ . '/../helpers.php';
require_method('GET');

$user = auth_user();
$sid = (int)$user['student_id'];

// Riwayat misi selesai (BR-09)
$stmt = $db->prepare("SELECT m.id AS mission_id, m.title, p.completion_date, p.total_points
                      FROM progresses p
                      JOIN mission_sessions s ON s.id = p.session_id
                      JOIN missions m ON m.id = s.mission_id
                      WHERE s.student_id = ? AND p.completion_status = 'completed'
                      ORDER BY p.completion_date DESC");
$stmt->bind_param('i', $sid);
$stmt->execute();
$res = $stmt->get_result();
$history = [];
$points = 0;
while ($r = $res->fetch_assoc()) {
    $points += (int)$r['total_points'];
    $history[] = [
        'mission_id' => (int)$r['mission_id'],
        'title' => $r['title'],
        'completion_date' => $r['completion_date'],
        'points' => (int)$r['total_points'],
    ];
}
$stmt->close();

$total = (int)$db->query("SELECT COUNT(*) c FROM missions WHERE status='active'")->fetch_assoc()['c'];

json_out([
    'success' => true,
    'user' => user_payload($user),
    'completed_count' => count($history),
    'total_missions' => $total,
    'total_points' => $points,
    'history' => $history,
]);
