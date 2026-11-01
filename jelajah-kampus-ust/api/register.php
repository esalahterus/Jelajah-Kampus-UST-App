<?php
require_once __DIR__ . '/../helpers.php';
require_method('POST');

$name = input('name');
$email = strtolower(input('email'));
$nim = input('nim');
$prodi = input('program_study', 'Informatika');
$password = $_POST['password'] ?? '';

if ($name === '' || $email === '' || $nim === '' || $password === '') fail('Semua field wajib diisi');
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) fail('Format email tidak valid');
if (!preg_match('/^[0-9A-Za-z]{5,30}$/', $nim)) fail('NIM harus 5-30 karakter huruf/angka');
if (strlen($password) < 6) fail('Password minimal 6 karakter');
if (mb_strlen($name) > 100) fail('Nama terlalu panjang');

$stmt = $db->prepare("SELECT 1 FROM users WHERE email = ? UNION SELECT 1 FROM students WHERE nim = ?");
$stmt->bind_param('ss', $email, $nim);
$stmt->execute();
if ($stmt->get_result()->num_rows > 0) fail('Email atau NIM sudah terdaftar');
$stmt->close();

$hash = password_hash($password, PASSWORD_DEFAULT);
$db->begin_transaction();
$stmt = $db->prepare("INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'student')");
$stmt->bind_param('sss', $name, $email, $hash);
$ok = $stmt->execute();
$uid = $db->insert_id;
$stmt->close();

if ($ok) {
    $stmt = $db->prepare("INSERT INTO students (user_id, nim, program_study) VALUES (?, ?, ?)");
    $stmt->bind_param('iss', $uid, $nim, $prodi);
    $ok = $stmt->execute();
    $stmt->close();
}
if (!$ok) {
    $db->rollback();
    fail('Gagal menyimpan data', 500);
}
$db->commit();
json_out(['success' => true, 'message' => 'Registrasi berhasil, silakan login']);
