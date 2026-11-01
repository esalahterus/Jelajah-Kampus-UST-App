<?php
require_once __DIR__ . '/../helpers.php';
require_method('POST');

$email = strtolower(input('email'));
$password = $_POST['password'] ?? '';
if ($email === '' || $password === '') fail('Email dan password wajib diisi');

$stmt = $db->prepare("SELECT u.id, u.name, u.email, u.password_hash, u.role, u.status, s.nim, s.program_study
                      FROM users u LEFT JOIN students s ON s.user_id = u.id
                      WHERE u.email = ? LIMIT 1");
$stmt->bind_param('s', $email);
$stmt->execute();
$u = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$u || !password_verify($password, $u['password_hash'])) fail('Email atau password salah', 401);
if ($u['status'] !== 'active') fail('Akun tidak aktif', 403);
if ($u['role'] !== 'student') fail('Akun administrator login melalui panel admin web', 403);

$token = bin2hex(random_bytes(32));
$stmt = $db->prepare("UPDATE users SET api_token = ? WHERE id = ?");
$stmt->bind_param('si', $token, $u['id']);
$stmt->execute();
$stmt->close();

json_out(['success' => true, 'token' => $token, 'user' => user_payload($u)]);
