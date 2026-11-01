<?php
require_once __DIR__ . '/_layout.php';
require_admin();
check_csrf();

if (($_POST['act'] ?? '') === 'verify') {
    $eid = (int)($_POST['id'] ?? 0);
    $decision = ($_POST['decision'] ?? '') === 'accept' ? 'accepted' : 'rejected';
    $note = trim($_POST['note'] ?? '');
    $admin = (int)$_SESSION['admin_id'];

    $db->begin_transaction();
    $s = $db->prepare("SELECT e.id, e.session_id, m.point_reward
                       FROM evidences e JOIN mission_sessions ms ON ms.id=e.session_id
                       JOIN missions m ON m.id=ms.mission_id
                       WHERE e.id=? AND e.verification_status='pending' FOR UPDATE");
    $s->bind_param('i', $eid);
    $s->execute();
    $ev = $s->get_result()->fetch_assoc();
    $s->close();

    if (!$ev) {
        $db->rollback();
        flash('Bukti tidak ditemukan atau sudah diverifikasi.', 'err');
        redirect('evidence.php');
    }

    $s = $db->prepare("UPDATE evidences SET verification_status=?, verified_by=?, admin_note=? WHERE id=?");
    $s->bind_param('sisi', $decision, $admin, $note, $eid);
    $s->execute();
    $s->close();

    // BR-05: progres selesai hanya setelah bukti diterima.
    if ($decision === 'accepted') {
        $sid = (int)$ev['session_id'];
        $pts = (int)$ev['point_reward'];
        $s = $db->prepare("INSERT INTO progresses (session_id, completion_status, completion_date, total_points) VALUES (?, 'completed', NOW(), ?)
                           ON DUPLICATE KEY UPDATE completion_status='completed', completion_date=NOW(), total_points=VALUES(total_points)");
        $s->bind_param('ii', $sid, $pts);
        $s->execute();
        $s->close();
        // Misi selesai: akhiri sesi dan hentikan pelacakan (BR-10).
        $db->query("UPDATE mission_sessions SET status='ended', end_time=NOW() WHERE id=$sid AND status='active'");
    }
    $db->commit();
    flash($decision === 'accepted' ? 'Bukti diterima, progres misi diperbarui.' : 'Bukti ditolak, mahasiswa dapat mengirim ulang.');
    redirect('evidence.php' . (isset($_GET['f']) ? '?f=' . urlencode($_GET['f']) : ''));
}

$filter = $_GET['f'] ?? 'pending';
if (!in_array($filter, ['pending', 'accepted', 'rejected', 'all'], true)) $filter = 'pending';

page_start('Verifikasi Bukti Kunjungan', 'evidence');
echo '<p>';
foreach (['pending' => 'Menunggu', 'accepted' => 'Diterima', 'rejected' => 'Ditolak', 'all' => 'Semua'] as $k => $l) {
    echo '<a class="btn sm' . ($k === $filter ? '' : ' gray') . '" href="evidence.php?f=' . $k . '">' . $l . '</a> ';
}
echo '</p>';

$where = $filter === 'all' ? '' : "WHERE e.verification_status = '" . $db->real_escape_string($filter) . "'";
$r = $db->query("SELECT e.*, u.name, st.nim, m.title
                 FROM evidences e
                 JOIN mission_sessions ms ON ms.id=e.session_id
                 JOIN students st ON st.id=ms.student_id
                 JOIN users u ON u.id=st.user_id
                 JOIN missions m ON m.id=ms.mission_id
                 $where ORDER BY e.id DESC LIMIT 100");
echo '<div class="card"><table><tr><th>Foto</th><th>Mahasiswa</th><th>Misi</th><th>Status</th><th>Aksi</th></tr>';
if ($r->num_rows === 0) echo '<tr><td colspan="5">Tidak ada data.</td></tr>';
while ($e = $r->fetch_assoc()) {
    echo '<tr><td><a href="../' . e($e['photo_url']) . '" target="_blank"><img class="thumb" src="../' . e($e['photo_url']) . '" alt="bukti"></a></td>'
       . '<td>' . e($e['name']) . '<br><small>' . e($e['nim']) . '</small><br><small>' . e($e['submitted_at']) . '</small></td>'
       . '<td>' . e($e['title']) . '</td>'
       . '<td><span class="tag ' . e($e['verification_status']) . '">' . e($e['verification_status']) . '</span>'
       . ($e['admin_note'] ? '<br><small>' . e($e['admin_note']) . '</small>' : '') . '</td><td>';
    if ($e['verification_status'] === 'pending') {
        echo '<form method="post">' . csrf_field() . '<input type="hidden" name="act" value="verify"><input type="hidden" name="id" value="' . (int)$e['id'] . '">'
           . '<input name="note" placeholder="Catatan (opsional)" maxlength="255">'
           . '<button name="decision" value="accept">Terima</button> <button class="btn red" name="decision" value="reject">Tolak</button></form>';
    } else {
        echo '-';
    }
    echo '</td></tr>';
}
echo '</table></div>';
page_end();
