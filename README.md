# Jelajah Kampus UST App

Aplikasi eksplorasi kampus UST berbasis Android (Java) dengan backend PHP/MySQL (Laragon) dan peta OpenStreetMap (osmdroid).

```
Jelajah-Kampus-UST-App/
├── jelajah-kampus-ust/   -> salin ke Drive Anda\laragon\www\ atau ke htdocs jika menggunakan XAMPP
│   ├── database.sql        skema + data awal (11 tabel, + kolom api_token)
│   ├── config.php          koneksi DB (default Laragon: root, tanpa password)
│   ├── helpers.php         respons JSON, autentikasi token, Haversine
│   ├── api/                REST API untuk aplikasi Android
│   ├── admin/              panel admin web (dashboard, gedung, misi, multimedia, verifikasi bukti, mahasiswa, & peserta aktif)
│   └── uploads/            foto bukti & media
└── JelajahKampusUST/   -> buka folder ini di Android Studio
```

## 1. Menjalankan backend (Laragon atau XAMPP)

1. Salin folder `jelajah-kampus-ust` ke `..\laragon\www\` atau ke `..\htdocs\` jika menggunakan XAMPP.
2. Jalankan Laragon, klik **Start All** (Apache + MySQL).
3. Impor `database.sql` (phpMyAdmin). Database `jelajah_kampus_ust` akan dibuat otomatis.
4. Jika MySQL Anda memakai password, ubah `DB_PASS` di `config.php`.
5. Uji di browser:
   - Panel admin: `http://localhost/jelajah-kampus-ust/admin/login.php`
   - Akun admin: `admin@ustjogja.ac.id` / `admin123`
   - Akun mahasiswa demo (aplikasi): `mhs1@ustjogja.ac.id` / `mahasiswa1`
   - **Ganti password admin dan akun demo sebelum dipakai di luar uji coba.**
6. Untuk unggah video/audio besar lewat admin, naikkan `upload_max_filesize` dan `post_max_size` di `php.ini` Laragon (Menu > PHP > php.ini), lalu restart Apache.

## 2. Menjalankan aplikasi Android

1. Android Studio: **Open** folder `JelajahKampusUST` (tunggu Gradle Sync).
2. Buka `Config.java` dan sesuaikan `BASE_URL`:
   - Emulator: `http://10.0.2.2/jelajah-kampus-ust/` (default di build.gradle.kts: Module :app)
   - HP fisik: `http://IP_LAPTOP/jelajah-kampus-ust/` (satu Wi-Fi)
3. Run. Login dengan akun demo atau daftar akun baru.
4. Emulator: atur lokasi lewat Extended Controls (tiga titik) > Location; kamera Virtual Scene untuk uji foto.

## 3. Endpoint API (`/jelajah-kampus-ust/api/`)

| Endpoint | Metode | Fungsi |
|---|---|---|
| `register.php`, `login.php`, `logout.php` | POST | Akun & token |
| `missions.php` | GET | Daftar misi + status per mahasiswa |
| `buildings.php` | GET | Gedung, koordinat, fasilitas, panduan media |
| `session_start.php`, `session_end.php` | POST | Sesi eksplorasi |
| `tracking_update.php` | POST | Kirim lokasi (sesi aktif) |
| `tracking_list.php` | GET | Peserta aktif di misi yang sama |
| `evidence_upload.php` | POST multipart | Unggah foto bukti (JPG/PNG, maks 8 MB) |
| `profile.php` | GET | Profil, progres, riwayat misi |

Token dikirim lewat header `X-Auth-Token`.

## Author

**Esa Adya Syawal**

GitHub: https://github.com/esalahterus