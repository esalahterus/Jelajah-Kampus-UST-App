<?php
require_once __DIR__ . '/_layout.php';
require_admin();

page_start('Peserta & Sesi Eksplorasi Aktif', 'tracking');
$r = $db->query("SELECT ms.id, ms.start_time, u.name, st.nim, m.title,
   (SELECT t.latitude  FROM tracking_locations t WHERE t.session_id=ms.id ORDER BY t.recorded_at DESC, t.id DESC LIMIT 1) AS lat,
   (SELECT t.longitude FROM tracking_locations t WHERE t.session_id=ms.id ORDER BY t.recorded_at DESC, t.id DESC LIMIT 1) AS lon,
   (SELECT t.recorded_at FROM tracking_locations t WHERE t.session_id=ms.id ORDER BY t.recorded_at DESC, t.id DESC LIMIT 1) AS last_at
   FROM mission_sessions ms
   JOIN students st ON st.id=ms.student_id
   JOIN users u ON u.id=st.user_id
   JOIN missions m ON m.id=ms.mission_id
   WHERE ms.status='active' ORDER BY ms.id DESC");

echo '<div class="card"><table><tr><th>Mahasiswa</th><th>Misi</th><th>Mulai</th><th>Lokasi terakhir</th><th>Peta</th></tr>';
if ($r->num_rows === 0) echo '<tr><td colspan="5">Tidak ada sesi aktif.</td></tr>';
while ($s = $r->fetch_assoc()) {
    echo '<tr><td>' . e($s['name']) . '<br><small>' . e($s['nim']) . '</small></td><td>' . e($s['title']) . '</td><td>' . e($s['start_time']) . '</td><td>';
    if ($s['lat'] === null) {
        echo 'Lokasi tidak tersedia';
    } else {
        echo e(round((float)$s['lat'], 6)) . ', ' . e(round((float)$s['lon'], 6)) . '<br><small>' . e($s['last_at']) . '</small>';
    }
    echo '</td><td>';
    if ($s['lat'] !== null) {
        $la = rawurlencode((string)$s['lat']);
        $lo = rawurlencode((string)$s['lon']);
        echo '<a class="btn sm" target="_blank" rel="noopener" href="https://www.openstreetmap.org/?mlat=' . $la . '&amp;mlon=' . $lo . '#map=18/' . $la . '/' . $lo . '">OpenStreetMap</a>';
    } else echo '-';
    echo '</td></tr>';
}
echo '</table></div><p><small>Lokasi hanya tercatat selama sesi eksplorasi aktif dan dengan izin pengguna.</small></p>';
page_end();
