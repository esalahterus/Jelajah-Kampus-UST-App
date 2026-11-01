<?php
require_once __DIR__ . '/_layout.php';
require_admin();
check_csrf();

$act = $_POST['act'] ?? '';

if ($act === 'save') {
    $id = (int)($_POST['id'] ?? 0);
    $bid = (int)($_POST['building_id'] ?? 0);
    $title = trim($_POST['title'] ?? '');
    $desc = trim($_POST['description'] ?? '');
    $pts = (int)($_POST['point_reward'] ?? 0);
    $status = ($_POST['status'] ?? '') === 'inactive' ? 'inactive' : 'active';

    // BR-03: setiap misi harus memiliki lokasi tujuan (gedung).
    if ($title === '' || $bid <= 0 || $pts < 0) {
        flash('Judul dan gedung tujuan wajib diisi.', 'err');
        redirect('missions.php' . ($id ? "?edit=$id" : '?edit=0'));
    }
    if ($id) {
        $s = $db->prepare("UPDATE missions SET building_id=?, title=?, description=?, point_reward=?, status=? WHERE id=?");
        $s->bind_param('issisi', $bid, $title, $desc, $pts, $status, $id);
    } else {
        $s = $db->prepare("INSERT INTO missions (building_id, title, description, point_reward, status) VALUES (?,?,?,?,?)");
        $s->bind_param('issis', $bid, $title, $desc, $pts, $status);
    }
    $s->execute();
    flash('Misi disimpan.');
    redirect('missions.php');
}

if ($act === 'delete') {
    $id = (int)($_POST['id'] ?? 0);
    $s = $db->prepare("DELETE FROM missions WHERE id=?");
    $s->bind_param('i', $id);
    $s->execute();
    flash('Misi dihapus.');
    redirect('missions.php');
}

$editId = isset($_GET['edit']) ? (int)$_GET['edit'] : -1;
page_start('Kelola Misi', 'missions');

if ($editId >= 0) {
    $m = ['id' => 0, 'building_id' => 0, 'title' => '', 'description' => '', 'point_reward' => 20, 'status' => 'active'];
    if ($editId > 0) {
        $s = $db->prepare("SELECT * FROM missions WHERE id=?");
        $s->bind_param('i', $editId);
        $s->execute();
        $row = $s->get_result()->fetch_assoc();
        if ($row) $m = $row;
    }
    echo '<div class="card"><form method="post">' . csrf_field() . '<input type="hidden" name="act" value="save"><input type="hidden" name="id" value="' . (int)$m['id'] . '">'
       . '<label>Judul misi</label><input name="title" required maxlength="150" value="' . e($m['title']) . '">'
       . '<label>Deskripsi</label><textarea name="description" rows="4">' . e($m['description']) . '</textarea>'
       . '<label>Gedung tujuan</label><select name="building_id" required><option value="">- pilih -</option>';
    $r = $db->query("SELECT id, name FROM buildings ORDER BY name");
    while ($b = $r->fetch_assoc()) {
        echo '<option value="' . (int)$b['id'] . '"' . ((int)$b['id'] === (int)$m['building_id'] ? ' selected' : '') . '>' . e($b['name']) . '</option>';
    }
    echo '</select><div class="row2"><div><label>Poin penyelesaian</label><input type="number" min="0" name="point_reward" value="' . (int)$m['point_reward'] . '"></div>'
       . '<div><label>Status</label><select name="status"><option value="active"' . ($m['status'] === 'active' ? ' selected' : '') . '>Aktif</option>'
       . '<option value="inactive"' . ($m['status'] === 'inactive' ? ' selected' : '') . '>Nonaktif</option></select></div></div>'
       . '<button>Simpan</button> <a class="btn gray" href="missions.php">Kembali</a></form></div>';
} else {
    echo '<p><a class="btn" href="missions.php?edit=0">+ Tambah Misi</a></p><div class="card"><table><tr><th>ID</th><th>Judul</th><th>Gedung</th><th>Poin</th><th>Status</th><th></th></tr>';
    $r = $db->query("SELECT m.*, b.name AS bname FROM missions m JOIN buildings b ON b.id=m.building_id ORDER BY m.id");
    while ($m = $r->fetch_assoc()) {
        echo '<tr><td>' . (int)$m['id'] . '</td><td>' . e($m['title']) . '</td><td>' . e($m['bname']) . '</td><td>' . (int)$m['point_reward'] . '</td><td>' . e($m['status']) . '</td><td>'
           . '<a class="btn sm" href="missions.php?edit=' . (int)$m['id'] . '">Ubah</a> '
           . '<form method="post" style="display:inline" onsubmit="return confirm(\'Hapus misi ini?\')">' . csrf_field()
           . '<input type="hidden" name="act" value="delete"><input type="hidden" name="id" value="' . (int)$m['id'] . '"><button class="btn red sm">Hapus</button></form></td></tr>';
    }
    echo '</table></div>';
}
page_end();
