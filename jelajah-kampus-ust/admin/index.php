<?php
require_once __DIR__ . '/_layout.php';
require_admin();

function count_of(mysqli $db, string $sql): int { return (int)$db->query($sql)->fetch_assoc()['c']; }

$stats = [
    'Mahasiswa' => count_of($db, "SELECT COUNT(*) c FROM students"),
    'Gedung' => count_of($db, "SELECT COUNT(*) c FROM buildings"),
    'Misi aktif' => count_of($db, "SELECT COUNT(*) c FROM missions WHERE status='active'"),
    'Sesi aktif' => count_of($db, "SELECT COUNT(*) c FROM mission_sessions WHERE status='active'"),
    'Bukti menunggu' => count_of($db, "SELECT COUNT(*) c FROM evidences WHERE verification_status='pending'"),
    'Misi selesai' => count_of($db, "SELECT COUNT(*) c FROM progresses WHERE completion_status='completed'"),
];

page_start('Dashboard', 'index');
echo '<div class="grid">';
foreach ($stats as $label => $n) {
    echo '<div class="card"><div>' . e($label) . '</div><div class="stat">' . $n . '</div></div>';
}
echo '</div>';

$res = $db->query("SELECT u.name, st.nim, m.title, e.verification_status, e.submitted_at
                   FROM evidences e
                   JOIN mission_sessions s ON s.id=e.session_id
                   JOIN students st ON st.id=s.student_id
                   JOIN users u ON u.id=st.user_id
                   JOIN missions m ON m.id=s.mission_id
                   ORDER BY e.id DESC LIMIT 8");
echo '<div class="card"><h2>Aktivitas bukti terbaru</h2><table><tr><th>Mahasiswa</th><th>Misi</th><th>Status</th><th>Waktu</th></tr>';
if ($res->num_rows === 0) echo '<tr><td colspan="4">Belum ada bukti.</td></tr>';
while ($r = $res->fetch_assoc()) {
    echo '<tr><td>' . e($r['name']) . '<br><small>' . e($r['nim']) . '</small></td><td>' . e($r['title']) . '</td>'
       . '<td><span class="tag ' . e($r['verification_status']) . '">' . e($r['verification_status']) . '</span></td>'
       . '<td>' . e($r['submitted_at']) . '</td></tr>';
}
echo '</table></div>';
page_end();
