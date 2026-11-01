<?php
require_once __DIR__ . '/_layout.php';
require_admin();
check_csrf();

$act = $_POST['act'] ?? '';
if ($act === 'toggle') {
    $uid = (int)($_POST['user_id'] ?? 0);
    $s = $db->prepare("UPDATE users SET status = IF(status='active','inactive','active'), api_token = NULL WHERE id=? AND role='student'");
    $s->bind_param('i', $uid);
    $s->execute();
    flash('Status akun diperbarui.');
    redirect('students.php');
}
if ($act === 'reset') {
    $uid = (int)($_POST['user_id'] ?? 0);
    $new = 'mhs' . random_int(100000, 999999);
    $hash = password_hash($new, PASSWORD_DEFAULT);
    $s = $db->prepare("UPDATE users SET password_hash=?, api_token=NULL WHERE id=? AND role='student'");
    $s->bind_param('si', $hash, $uid);
    $s->execute();
    flash('Password baru: ' . $new . ' (catat dan berikan ke mahasiswa).');
    redirect('students.php');
}
if ($act === 'delete') {
    $uid = (int)($_POST['user_id'] ?? 0);
    $s = $db->prepare("DELETE FROM users WHERE id=? AND role='student'");
    $s->bind_param('i', $uid);
    $s->execute();
    flash('Mahasiswa dihapus.');
    redirect('students.php');
}

page_start('Data Mahasiswa', 'students');
$r = $db->query("SELECT u.id, u.name, u.email, u.status, st.nim, st.program_study,
                 (SELECT COUNT(*) FROM progresses p JOIN mission_sessions s ON s.id=p.session_id
                   WHERE s.student_id=st.id AND p.completion_status='completed') AS done
                 FROM users u JOIN students st ON st.user_id=u.id ORDER BY u.id DESC");
echo '<div class="card"><table><tr><th>Nama</th><th>NIM</th><th>Prodi</th><th>Misi selesai</th><th>Status</th><th></th></tr>';
while ($u = $r->fetch_assoc()) {
    $uid = (int)$u['id'];
    echo '<tr><td>' . e($u['name']) . '<br><small>' . e($u['email']) . '</small></td><td>' . e($u['nim']) . '</td><td>' . e($u['program_study']) . '</td><td>' . (int)$u['done'] . '</td><td>' . e($u['status']) . '</td><td>'
       . '<form method="post" style="display:inline">' . csrf_field() . '<input type="hidden" name="user_id" value="' . $uid . '"><button class="btn sm gray" name="act" value="toggle">Aktif/Nonaktif</button> '
       . '<button class="btn sm" name="act" value="reset" onclick="return confirm(\'Reset password mahasiswa ini?\')">Reset password</button> '
       . '<button class="btn sm red" name="act" value="delete" onclick="return confirm(\'Hapus mahasiswa beserta seluruh datanya?\')">Hapus</button></form></td></tr>';
}
echo '</table></div>';
page_end();
