<?php
require_once __DIR__ . '/_layout.php';
require_admin();
check_csrf();

$act = $_POST['act'] ?? '';

if ($act === 'add') {
    $bid = (int)($_POST['building_id'] ?? 0);
    $title = trim($_POST['title'] ?? '');
    $type = ($_POST['media_type'] ?? '') === 'video' ? 'video' : 'audio';
    $url = trim($_POST['media_url'] ?? '');
    $err = null;

    $path = save_media_upload('file', $type, $err);
    if ($err) { flash($err, 'err'); redirect('media.php'); }
    if ($path === null) {
        // Tanpa upload: gunakan URL eksternal (harus http/https).
        if (!preg_match('#^https?://#i', $url)) { flash('Unggah file atau isi URL http(s).', 'err'); redirect('media.php'); }
        $path = $url;
    }
    if ($title === '' || $bid <= 0) { flash('Judul dan gedung wajib diisi.', 'err'); redirect('media.php'); }

    $s = $db->prepare("INSERT INTO media_guides (building_id, title, media_type, media_url) VALUES (?,?,?,?)");
    $s->bind_param('isss', $bid, $title, $type, $path);
    $s->execute();
    flash('Panduan multimedia ditambahkan.');
    redirect('media.php');
}

if ($act === 'delete') {
    $id = (int)($_POST['id'] ?? 0);
    $s = $db->prepare("SELECT media_url FROM media_guides WHERE id=?");
    $s->bind_param('i', $id);
    $s->execute();
    $row = $s->get_result()->fetch_assoc();
    if ($row && strpos($row['media_url'], 'uploads/media/') === 0) {
        @unlink(UPLOAD_MEDIA_DIR . basename($row['media_url']));
    }
    $s = $db->prepare("DELETE FROM media_guides WHERE id=?");
    $s->bind_param('i', $id);
    $s->execute();
    flash('Panduan dihapus.');
    redirect('media.php');
}

page_start('Panduan Multimedia', 'media');
echo '<div class="card"><h2>Tambah audio / video</h2><form method="post" enctype="multipart/form-data">' . csrf_field() . '<input type="hidden" name="act" value="add">'
   . '<div class="row2"><div><label>Gedung</label><select name="building_id" required><option value="">- pilih -</option>';
$r = $db->query("SELECT id, name FROM buildings ORDER BY name");
while ($b = $r->fetch_assoc()) echo '<option value="' . (int)$b['id'] . '">' . e($b['name']) . '</option>';
echo '</select></div><div><label>Jenis</label><select name="media_type"><option value="audio">Audio</option><option value="video">Video</option></select></div></div>'
   . '<label>Judul</label><input name="title" required maxlength="150">'
   . '<div class="row2"><div><label>Unggah file (mp3/wav/ogg/m4a atau mp4/3gp/webm)</label><input type="file" name="file"></div>'
   . '<div><label>atau URL media (http/https)</label><input name="media_url" placeholder="https://..."></div></div>'
   . '<button>Tambah</button></form></div>';

echo '<div class="card"><table><tr><th>Gedung</th><th>Judul</th><th>Jenis</th><th>Sumber</th><th></th></tr>';
$r = $db->query("SELECT g.*, b.name AS bname FROM media_guides g JOIN buildings b ON b.id=g.building_id ORDER BY g.id DESC");
while ($g = $r->fetch_assoc()) {
    echo '<tr><td>' . e($g['bname']) . '</td><td>' . e($g['title']) . '</td><td>' . e($g['media_type']) . '</td><td><small>' . e($g['media_url']) . '</small></td><td>'
       . '<form method="post" onsubmit="return confirm(\'Hapus panduan ini?\')">' . csrf_field()
       . '<input type="hidden" name="act" value="delete"><input type="hidden" name="id" value="' . (int)$g['id'] . '"><button class="btn red sm">Hapus</button></form></td></tr>';
}
echo '</table></div>';
page_end();
