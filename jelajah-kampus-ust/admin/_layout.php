<?php
// Layout & helper bersama panel admin. Di-include oleh setiap halaman admin.
require_once __DIR__ . '/../config.php';
session_start();

function e($s): string { return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8'); }

function require_admin(): void {
    if (empty($_SESSION['admin_id'])) {
        header('Location: login.php');
        exit;
    }
}

function csrf_token(): string {
    if (empty($_SESSION['csrf'])) $_SESSION['csrf'] = bin2hex(random_bytes(16));
    return $_SESSION['csrf'];
}

function csrf_field(): string {
    return '<input type="hidden" name="csrf" value="' . e(csrf_token()) . '">';
}

function check_csrf(): void {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!hash_equals($_SESSION['csrf'] ?? '', $_POST['csrf'] ?? '')) {
            http_response_code(403);
            exit('Token keamanan tidak valid. Muat ulang halaman.');
        }
    }
}

function flash(string $msg, string $type = 'ok'): void { $_SESSION['flash'] = [$msg, $type]; }

function page_start(string $title, string $active = ''): void {
    $menu = [
        'index' => ['Dashboard', 'index.php'],
        'buildings' => ['Gedung', 'buildings.php'],
        'missions' => ['Misi', 'missions.php'],
        'media' => ['Multimedia', 'media.php'],
        'evidence' => ['Verifikasi Bukti', 'evidence.php'],
        'students' => ['Mahasiswa', 'students.php'],
        'tracking' => ['Peserta Aktif', 'tracking.php'],
    ];
    echo '<!doctype html><html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">';
    echo '<title>' . e($title) . ' - Admin Jelajah Kampus UST</title><style>
    :root{--p:#1b5e20;--bg:#f4f6f4;--line:#dfe5df}
    *{box-sizing:border-box}body{margin:0;font-family:system-ui,Segoe UI,Arial,sans-serif;background:var(--bg);color:#1c241c}
    header{background:var(--p);color:#fff;padding:12px 20px;display:flex;justify-content:space-between;align-items:center}
    nav{background:#fff;border-bottom:1px solid var(--line);padding:0 12px;display:flex;flex-wrap:wrap}
    nav a{padding:12px 14px;color:#33413a;text-decoration:none;font-size:14px;border-bottom:3px solid transparent}
    nav a.on{border-color:var(--p);color:var(--p);font-weight:600}
    main{max-width:1100px;margin:20px auto;padding:0 16px}
    h1{font-size:22px;margin:0 0 14px}h2{font-size:17px}
    .card{background:#fff;border:1px solid var(--line);border-radius:10px;padding:16px;margin-bottom:16px}
    table{width:100%;border-collapse:collapse;font-size:14px}th,td{text-align:left;padding:8px;border-bottom:1px solid var(--line);vertical-align:top}
    th{background:#f0f4f0}
    input,select,textarea{width:100%;padding:8px;border:1px solid #c5cfc5;border-radius:6px;font:inherit;margin:3px 0 10px}
    label{font-size:13px;font-weight:600}
    button,.btn{background:var(--p);color:#fff;border:0;border-radius:6px;padding:8px 14px;cursor:pointer;font:inherit;text-decoration:none;display:inline-block;font-size:14px}
    .btn.red{background:#b3261e}.btn.gray{background:#667}.btn.sm{padding:4px 9px;font-size:12px}
    .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}
    .stat{font-size:28px;font-weight:700;color:var(--p)}
    .msg{padding:10px 14px;border-radius:8px;margin-bottom:14px}.msg.ok{background:#e1f3e2;color:#1b5e20}.msg.err{background:#fde7e5;color:#8c1d18}
    .tag{padding:2px 8px;border-radius:99px;font-size:12px;font-weight:600}
    .pending{background:#fff3cd;color:#7a5a00}.accepted{background:#d9f2dc;color:#1b5e20}.rejected{background:#fde0de;color:#8c1d18}
    img.thumb{max-width:160px;border-radius:6px;border:1px solid var(--line)}
    .row2{display:grid;grid-template-columns:1fr 1fr;gap:12px}@media(max-width:640px){.row2{grid-template-columns:1fr}}
    </style></head><body>';
    echo '<header><strong>Jelajah Kampus UST - Admin</strong><span>' . e($_SESSION['admin_name'] ?? '') . ' &middot; <a style="color:#fff" href="logout.php">Keluar</a></span></header><nav>';
    foreach ($menu as $key => [$label, $href]) {
        echo '<a href="' . $href . '"' . ($key === $active ? ' class="on"' : '') . '>' . e($label) . '</a>';
    }
    echo '</nav><main>';
    if (!empty($_SESSION['flash'])) {
        [$m, $t] = $_SESSION['flash'];
        unset($_SESSION['flash']);
        echo '<div class="msg ' . e($t) . '">' . e($m) . '</div>';
    }
    echo '<h1>' . e($title) . '</h1>';
}

function page_end(): void { echo '</main></body></html>'; }

function redirect(string $to): void { header('Location: ' . $to); exit; }

/** Simpan file media (audio/video) ke uploads/media, kembalikan path relatif atau null. */
function save_media_upload(string $field, string $type, ?string &$error): ?string {
    if (empty($_FILES[$field]['name']) || $_FILES[$field]['error'] === UPLOAD_ERR_NO_FILE) return null;
    $f = $_FILES[$field];
    if ($f['error'] !== UPLOAD_ERR_OK) { $error = 'Upload gagal (cek batas upload_max_filesize di php.ini).'; return null; }
    $ext = strtolower(pathinfo($f['name'], PATHINFO_EXTENSION));
    $ok = $type === 'audio' ? ['mp3', 'wav', 'ogg', 'm4a', 'aac'] : ['mp4', '3gp', 'webm', 'mkv'];
    if (!in_array($ext, $ok, true)) { $error = 'Ekstensi tidak diizinkan untuk ' . $type . ': ' . implode(', ', $ok); return null; }
    if (!is_dir(UPLOAD_MEDIA_DIR)) mkdir(UPLOAD_MEDIA_DIR, 0775, true);
    $name = 'media_' . bin2hex(random_bytes(8)) . '.' . $ext;
    if (!move_uploaded_file($f['tmp_name'], UPLOAD_MEDIA_DIR . $name)) { $error = 'Gagal menyimpan file.'; return null; }
    return 'uploads/media/' . $name;
}
