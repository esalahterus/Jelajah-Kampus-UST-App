<?php
require_once __DIR__ . '/_layout.php';
require_admin();
check_csrf();

$act = $_POST['act'] ?? '';

if ($act === 'save') {
    $id = (int)($_POST['id'] ?? 0);
    $name = trim($_POST['name'] ?? '');
    $desc = trim($_POST['description'] ?? '');
    $addr = trim($_POST['address'] ?? '');
    $lat = $_POST['latitude'] ?? '';
    $lon = $_POST['longitude'] ?? '';

    if ($name === '' || !is_numeric($lat) || !is_numeric($lon) || abs((float)$lat) > 90 || abs((float)$lon) > 180) {
        flash('Nama wajib diisi dan koordinat harus valid.', 'err');
        redirect('buildings.php' . ($id ? "?edit=$id" : ''));
    }
    $lat = (float)$lat; $lon = (float)$lon;
    if ($id) {
        $s = $db->prepare("UPDATE buildings SET name=?, description=?, address=? WHERE id=?");
        $s->bind_param('sssi', $name, $desc, $addr, $id);
        $s->execute();
        $s = $db->prepare("INSERT INTO locations (building_id, latitude, longitude) VALUES (?,?,?)
                           ON DUPLICATE KEY UPDATE latitude=VALUES(latitude), longitude=VALUES(longitude)");
        $s->bind_param('idd', $id, $lat, $lon);
        $s->execute();
    } else {
        $s = $db->prepare("INSERT INTO buildings (name, description, address) VALUES (?,?,?)");
        $s->bind_param('sss', $name, $desc, $addr);
        $s->execute();
        $id = $db->insert_id;
        $s = $db->prepare("INSERT INTO locations (building_id, latitude, longitude) VALUES (?,?,?)");
        $s->bind_param('idd', $id, $lat, $lon);
        $s->execute();
    }
    flash('Gedung disimpan.');
    redirect("buildings.php?edit=$id");
}

if ($act === 'delete') {
    $id = (int)($_POST['id'] ?? 0);
    $s = $db->prepare("DELETE FROM buildings WHERE id=?");
    $s->bind_param('i', $id);
    $s->execute();
    flash('Gedung dihapus (misi, fasilitas, dan multimedia terkait ikut terhapus).');
    redirect('buildings.php');
}

if ($act === 'fac_add') {
    $bid = (int)($_POST['building_id'] ?? 0);
    $fname = trim($_POST['fname'] ?? '');
    $fdesc = trim($_POST['fdesc'] ?? '');
    if ($fname !== '') {
        $s = $db->prepare("INSERT INTO facilities (building_id, name, description) VALUES (?,?,?)");
        $s->bind_param('iss', $bid, $fname, $fdesc);
        $s->execute();
        flash('Fasilitas ditambahkan.');
    }
    redirect("buildings.php?edit=$bid");
}

if ($act === 'fac_del') {
    $bid = (int)($_POST['building_id'] ?? 0);
    $fid = (int)($_POST['fid'] ?? 0);
    $s = $db->prepare("DELETE FROM facilities WHERE id=? AND building_id=?");
    $s->bind_param('ii', $fid, $bid);
    $s->execute();
    flash('Fasilitas dihapus.');
    redirect("buildings.php?edit=$bid");
}

$editId = isset($_GET['edit']) ? (int)$_GET['edit'] : -1;
page_start('Kelola Gedung & Fasilitas', 'buildings');

if ($editId >= 0) {
    $b = ['id' => 0, 'name' => '', 'description' => '', 'address' => '', 'latitude' => '-7.8060', 'longitude' => '110.3925'];
    if ($editId > 0) {
        $s = $db->prepare("SELECT b.*, l.latitude, l.longitude FROM buildings b LEFT JOIN locations l ON l.building_id=b.id WHERE b.id=?");
        $s->bind_param('i', $editId);
        $s->execute();
        $row = $s->get_result()->fetch_assoc();
        if ($row) $b = array_merge($b, array_filter($row, fn($v) => $v !== null));
    }
    echo '<div class="card"><h2>' . ($b['id'] ? 'Ubah' : 'Tambah') . ' Gedung</h2><form method="post">' . csrf_field()
       . '<input type="hidden" name="act" value="save"><input type="hidden" name="id" value="' . (int)$b['id'] . '">'
       . '<label>Nama gedung</label><input name="name" required maxlength="120" value="' . e($b['name']) . '">'
       . '<label>Deskripsi</label><textarea name="description" rows="3">' . e($b['description']) . '</textarea>'
       . '<label>Alamat</label><input name="address" maxlength="200" value="' . e($b['address']) . '">'
       . '<div class="row2"><div><label>Latitude</label><input name="latitude" required value="' . e($b['latitude']) . '"></div>'
       . '<div><label>Longitude</label><input name="longitude" required value="' . e($b['longitude']) . '"></div></div>'
       . '<small>Tips: buka openstreetmap.org, klik kanan pada titik gedung, pilih "Show address"/"Centre map here" lalu salin koordinatnya.</small><br><br>'
       . '<button>Simpan</button> <a class="btn gray" href="buildings.php">Kembali</a></form></div>';

    if ($b['id']) {
        $bid = (int)$b['id'];
        echo '<div class="card"><h2>Fasilitas</h2><table><tr><th>Nama</th><th>Deskripsi</th><th></th></tr>';
        $r = $db->query("SELECT * FROM facilities WHERE building_id=$bid ORDER BY id");
        while ($f = $r->fetch_assoc()) {
            echo '<tr><td>' . e($f['name']) . '</td><td>' . e($f['description']) . '</td><td>'
               . '<form method="post" onsubmit="return confirm(\'Hapus fasilitas ini?\')">' . csrf_field()
               . '<input type="hidden" name="act" value="fac_del"><input type="hidden" name="building_id" value="' . $bid . '">'
               . '<input type="hidden" name="fid" value="' . (int)$f['id'] . '"><button class="btn red sm">Hapus</button></form></td></tr>';
        }
        echo '</table><form method="post" style="margin-top:12px">' . csrf_field()
           . '<input type="hidden" name="act" value="fac_add"><input type="hidden" name="building_id" value="' . $bid . '">'
           . '<div class="row2"><div><label>Nama fasilitas</label><input name="fname" required maxlength="120"></div>'
           . '<div><label>Deskripsi</label><input name="fdesc"></div></div><button>Tambah fasilitas</button></form></div>';
    }
} else {
    echo '<p><a class="btn" href="buildings.php?edit=0">+ Tambah Gedung</a></p><div class="card"><table><tr><th>ID</th><th>Nama</th><th>Koordinat</th><th>Fasilitas</th><th></th></tr>';
    $r = $db->query("SELECT b.id, b.name, l.latitude, l.longitude, (SELECT COUNT(*) FROM facilities f WHERE f.building_id=b.id) AS nf
                     FROM buildings b LEFT JOIN locations l ON l.building_id=b.id ORDER BY b.id");
    while ($b = $r->fetch_assoc()) {
        echo '<tr><td>' . (int)$b['id'] . '</td><td>' . e($b['name']) . '</td><td>' . e($b['latitude']) . ', ' . e($b['longitude']) . '</td><td>' . (int)$b['nf'] . '</td><td>'
           . '<a class="btn sm" href="buildings.php?edit=' . (int)$b['id'] . '">Ubah</a> '
           . '<form method="post" style="display:inline" onsubmit="return confirm(\'Hapus gedung beserta misi terkait?\')">' . csrf_field()
           . '<input type="hidden" name="act" value="delete"><input type="hidden" name="id" value="' . (int)$b['id'] . '"><button class="btn red sm">Hapus</button></form></td></tr>';
    }
    echo '</table></div>';
}
page_end();
