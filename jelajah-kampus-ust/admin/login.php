<?php
require_once __DIR__ . '/_layout.php';
$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    check_csrf();
    $email = strtolower(trim($_POST['email'] ?? ''));
    $pass = $_POST['password'] ?? '';

    $stmt = $db->prepare("SELECT id, name, password_hash FROM users WHERE email=? AND role='admin' AND status='active' LIMIT 1");
    $stmt->bind_param('s', $email);
    $stmt->execute();
    $u = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if ($u && password_verify($pass, $u['password_hash'])) {
        session_regenerate_id(true);
        $_SESSION['admin_id'] = (int)$u['id'];
        $_SESSION['admin_name'] = $u['name'];
        redirect('index.php');
    }
    $error = 'Email atau password admin salah.';
}
?><!doctype html><html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Login Admin - Jelajah Kampus UST</title>
<style>body{font-family:system-ui,Arial;background:#e8f0e8;display:flex;min-height:100vh;align-items:center;justify-content:center;margin:0}
form{background:#fff;padding:28px;border-radius:12px;width:min(360px,92vw);box-shadow:0 4px 18px #0002}
h1{font-size:20px;color:#1b5e20;margin:0 0 16px}input{width:100%;padding:10px;margin:4px 0 12px;border:1px solid #bbb;border-radius:6px;box-sizing:border-box}
button{width:100%;padding:10px;background:#1b5e20;color:#fff;border:0;border-radius:6px;font-size:15px;cursor:pointer}.err{color:#b3261e;margin-bottom:10px;font-size:14px}</style></head>
<body><form method="post"><h1>Admin Jelajah Kampus UST</h1>
<?php if ($error) echo '<div class="err">' . e($error) . '</div>'; ?>
<?= csrf_field() ?>
<label>Email</label><input type="email" name="email" required autofocus>
<label>Password</label><input type="password" name="password" required>
<button type="submit">Masuk</button></form></body></html>
