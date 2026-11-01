-- Jelajah Kampus UST - skema database (MySQL / MariaDB, Laragon)
-- Import via phpMyAdmin isi file ini.

CREATE DATABASE IF NOT EXISTS jelajah_kampus_ust CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jelajah_kampus_ust;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS tracking_locations, progresses, evidences, mission_sessions,
    media_guides, missions, facilities, locations, students, buildings, users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('student','admin') NOT NULL DEFAULT 'student',
  status ENUM('active','inactive') NOT NULL DEFAULT 'active',
  api_token VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE students (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  nim VARCHAR(30) NOT NULL UNIQUE,
  program_study VARCHAR(100) NOT NULL DEFAULT 'Informatika',
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE buildings (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  description TEXT,
  address VARCHAR(200)
) ENGINE=InnoDB;

CREATE TABLE locations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  building_id INT NOT NULL UNIQUE,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE facilities (
  id INT AUTO_INCREMENT PRIMARY KEY,
  building_id INT NOT NULL,
  name VARCHAR(120) NOT NULL,
  description TEXT,
  FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE missions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  building_id INT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  point_reward INT NOT NULL DEFAULT 10,
  status ENUM('active','inactive') NOT NULL DEFAULT 'active',
  FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE media_guides (
  id INT AUTO_INCREMENT PRIMARY KEY,
  building_id INT NOT NULL,
  title VARCHAR(150) NOT NULL,
  media_type ENUM('audio','video') NOT NULL,
  media_url VARCHAR(255) NOT NULL,
  FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE mission_sessions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  mission_id INT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NULL,
  status ENUM('active','ended') NOT NULL DEFAULT 'active',
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE evidences (
  id INT AUTO_INCREMENT PRIMARY KEY,
  session_id INT NOT NULL,
  photo_url VARCHAR(255) NOT NULL,
  submitted_at DATETIME NOT NULL,
  verification_status ENUM('pending','accepted','rejected') NOT NULL DEFAULT 'pending',
  verified_by INT NULL,
  admin_note VARCHAR(255) NULL,
  FOREIGN KEY (session_id) REFERENCES mission_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE progresses (
  id INT AUTO_INCREMENT PRIMARY KEY,
  session_id INT NOT NULL UNIQUE,
  completion_status ENUM('in_progress','completed') NOT NULL DEFAULT 'in_progress',
  completion_date DATETIME NULL,
  total_points INT NOT NULL DEFAULT 0,
  FOREIGN KEY (session_id) REFERENCES mission_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE tracking_locations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  session_id INT NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  recorded_at DATETIME NOT NULL,
  INDEX idx_session_time (session_id, recorded_at),
  FOREIGN KEY (session_id) REFERENCES mission_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- DATA:
-- Admin: admin@ustjogja.ac.id / admin123 | Mahasiswa demo: mhs1@ustjogja.ac.id / mahasiswa1, mhs2@ustjogja.ac.id / mahasiswa2
INSERT INTO users (name, email, password_hash, role) VALUES
('Administrator', 'admin@ustjogja.ac.id', '$2y$10$DIyARZVXNQn7oriN9iOd.OCpuJavegzKp1SaFN0y3LEIz9Rq9qW7y', 'admin'),
('Mahasiswa Demo 1', 'mhs1@ustjogja.ac.id', '$2y$10$nzMMUivL2r19S3BzQi79XOMObiaxCrCz1tOJXiJsSasYdd2CNPhKG', 'student'),
('Mahasiswa Demo 2', 'mhs2@ustjogja.ac.id', '$2y$10$bJx7n9VqVRWR9S4Qg8B/QOonIIpKt72lFfFNjxHLnmHGIo0uNNjmC', 'student');

INSERT INTO students (user_id, nim, program_study) VALUES
(2, '2026018001', 'Informatika'),
(3, '2026018002', 'Informatika');

INSERT INTO buildings (name, description, address) VALUES
('Gedung Fakultas Teknik', 'Gedung perkuliahan Fakultas Teknik, termasuk Program Studi Informatika. Terdapat ruang kelas, ruang dosen, dan ruang diskusi mahasiswa.', 'Kampus UST, Yogyakarta'),
('Perpustakaan', 'Perpustakaan pusat dengan koleksi buku, jurnal, dan ruang baca. Mahasiswa dapat meminjam buku dan mengakses referensi akademik.', '59VJ+GF4, Jl. Batikan No.2, Tempel, Wirogunan, Kec. Mergangsan, Kota Yogyakarta, Daerah Istimewa Yogyakarta 55167'),
('Gedung Pusat', 'Pusat administrasi universitas: kantor rektor, biro akademik, dan layanan administrasi mahasiswa.', 'Jl. Batikan, UH-III Jl. Tuntungan No.1043, Tahunan, Kec. Umbulharjo, Kota Yogyakarta, Daerah Istimewa Yogyakarta 55167'),
('Gedung Fakultas Ekonomi', 'Gedung perkuliahan Fakultas Ekonomi. Terdapat ruang kelas, ruang dosen, dan ruang diskusi mahasiswa.', 'Jl. Kusumanegara No.157, Muja Muju, Kec. Umbulharjo, Kota Yogyakarta, Daerah Istimewa Yogyakarta 55165');

INSERT INTO locations (building_id, latitude, longitude) VALUES
(1, -7.8007482, 110.3877684),
(2, -7.8062481, 110.3811597),
(3, -7.8086984, 110.3817199),
(4, -7.801661, 110.3923413);

INSERT INTO facilities (building_id, name, description) VALUES
(1, 'Ruang Kelas', 'Ruang kuliah ber-AC dengan proyektor.'),
(1, 'Ruang Diskusi', 'Area belajar bersama mahasiswa.'),
(1, 'Laboratorium Komputer', 'Ruang praktikum komputer untuk kegiatan pembelajaran.'),
(2, 'Ruang Baca', 'Ruang baca tenang ber-AC dengan meja individu.'),
(3, 'Biro Akademik', 'Layanan administrasi akademik mahasiswa.'),
(3, 'Ruang Kelas', 'Ruang kuliah ber-AC dengan proyektor.'),
(3, 'Auditorium', 'Ruang untuk seminar dan kegiatan besar.'),
(4, 'Ruang Kelas', 'Ruang kuliah ber-AC dengan proyektor.'),
(4, 'Ruang Diskusi', 'Area belajar bersama mahasiswa.'),
(4, 'Ruang Ki Sarino', 'Ruang untuk kuliah umum, rapat, seminar, workshop, dan kegiatan akademik.');

INSERT INTO missions (building_id, title, description, point_reward) VALUES
(1, 'Misi 01 - Mengenal Gedung Fakultas Teknik', 'Kunjungi Gedung Fakultas Teknik, kenali ruang kelas dan ruang diskusi, lalu ambil foto di depan gedung sebagai bukti kunjungan.', 25),
(2, 'Misi 02 - Mengunjungi Perpustakaan', 'Datangi perpustakaan, lihat ruang baca, lalu ambil foto bukti kunjungan.', 25),
(3, 'Misi 03 - Mengenal Gedung Pusat', 'Kunjungi Gedung Pusat dan unggah foto sebagai bukti kunjungan di area Auditorium.', 25),
(4, 'Misi 04 - Mengunjungi Gedung Fakultas Ekonomi', 'Kunjungi Gedung Fakultas Ekonomi, kenali ruang kelas dan ruang diskusi, lalu ambil foto di depan gedung sebagai bukti kunjungan.', 25);