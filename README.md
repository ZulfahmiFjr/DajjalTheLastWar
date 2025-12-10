# 👁️ Dajjal: The Last War 🏔️

![Java](https://img.shields.io/badge/Language-Java-orange) ![Framework](https://img.shields.io/badge/Framework-LibGDX-red) ![Backend](https://img.shields.io/badge/Backend-SpringBoot-green) ![Platform](https://img.shields.io/badge/Platform-Desktop%20%7C%20Web-lightgrey) ![Status](https://img.shields.io/badge/Status-In%20Development-blue)

> **Proyek Akhir Pemrograman Berorientasi Objek (OOP) - Teknik Komputer UI 2025/2026** > _Game FPS 3D survival tentang pertempuran terakhir melawan kejahatan tertinggi di akhir zaman: **Dajjal**._

## 🔗 Download & Play

Game ini disubmit untuk memenuhi tugas akhir dan dapat dimainkan melalui **itch.io**:
👉 **[Link Menuju Halaman Game di itch.io]** (Akan diupdate pada tanggal 21 Des 2025)

---

## 👥 Tim Pengembang (Kelompok 7)

| Nama Anggota                   | NPM        | Role  |
| :----------------------------- | :--------- | :---- |
| **Caesar Nur Falah Widiyanto** | 2406487052 | _TBA_ |
| **Marshal Aufa Diliyana**      | 2406346913 | _TBA_ |
| **Muhammad Hashif Jade**       | 2406396786 | _TBA_ |
| **Zulfahmi Fajri**             | 2406345425 | _TBA_ |

## 👨‍🏫 Aslab Pendamping

| Nama Asisten [Kode]     | NPM        |
| :---------------------- | :--------- |
| **Shafwan Hasyim [SH]** | 2306209113 |

---

## 📜 Latar Belakang & Lore

Game ini berlatar di akhir zaman. Player berperan sebagai satu-satunya harapan umat manusia yang harus mendaki **Gunung Fitnah**. Misi utamanya adalah bertahan hidup dari serangan gelombang **Yajuj & Majuj** di sepanjang jalan spiral gunung, mengumpulkan persenjataan yang semakin canggih, dan menghadapi pertarungan terakhir melawan **Dajjal** di puncak gunung.

## 🛠️ Tech Stack

- **Core Game:** Java & LibGDX (OpenGL ES).
- **Backend Services:** Spring Boot (Authentication & Score Leaderboard).
- **Architecture:** MVC (Model-View-Controller) dengan penerapan 10 Design Patterns.
- **Build Tool:** Gradle.

---

## ✅ Development Roadmap (TODO List)

Berikut adalah status pengerjaan proyek kami sejauh ini:

### 🌍 World Generation & Environment

- [x] **Procedural Terrain:** Implementasi Perlin Noise untuk kontur gunung acak.
- [x] **Spiral Road Logic:** Algoritma _Newton's Iteration_ untuk jalan spiral yang mulus dari kaki hingga puncak.
- [x] **Vegetation System:** Spawn 600+ pohon dengan logika _Spatial Filtering_ (tidak tumbuh di jalan/puncak) & _Slope Alignment_ (kemiringan pohon disesuaikan dengan lokasi spawn).
- [x] **Visual Polish:** Transparansi daun (Blending Attribute) & Relative path textures.
- [ ] **Skybox & Fog:** Menambahkan atmosfer mencekam dan batas pandang seperti akhir zaman.

### 🎮 Player Mechanics & Physics

- [x] **Movement System:** WASD Movement dengan _Substepping_ (4 steps) untuk mencegah tembus tebing.
- [x] **Camera System:** FPS Camera dengan batasan _pitch_ dan anti snap logic.
- [x] **Jump & Gravity:** Hitungan fisika lompatan dan deteksi tanah.
- [ ] **Tree Collision:** Optimasi deteksi tabrakan pohon menggunakan _Grid Partitioning_ (Next Priority).
- [ ] **Weapon System:** Implementasi _Strategy Pattern_ untuk variasi senjata (Pistol, Shotgun, AK47).

### 👾 AI & Enemies

- [ ] **Yajuj Majuj:** Implementasi _Factory Method_ untuk spawn musuh dengan tingkat kesulitan bertingkat sesuai ketinggian gunung.
- [ ] **Dajjal (Final Boss):**
  - [ ] 3D Model Loading (`.g3db`/`.g3dj`).
  - [ ] **Boss Animations:** Implementasi `AnimationController` untuk status _Idle_, _Attack_, _Hit_, dan _Die_.
  - [ ] Boss AI Pattern & Phase logic.
- [ ] **Loot Drop System:** Musuh menjatuhkan senjata atau alat lain saat dibunuh.

### 🔌 Backend & System

- [ ] **Score System:** Mengirim data _wave_ tertinggi ke database setelah _Game Over_.
- [ ] **Authentication:** Login & Register user sebelum masuk main menu.
- [ ] **Menu & UI:** Main Menu, HUD (Health/Ammo), dan Pause Screen.

---

## 🏗️ Implementasi Design Patterns

Sesuai ketentuan tugas akhir, kami menerapkan 10 Design Pattern berikut untuk memastikan kode yang clean dan scalable serta optimal:

| Pattern                | Kategori   | Implementasi Rencana                                                                 |
| :--------------------- | :--------- | :----------------------------------------------------------------------------------- |
| **1. Singleton**       | Creational | Mengelola `AssetManager` dan `MusicManager` agar hanya ada satu instance.            |
| **2. Factory Method**  | Creational | `EnemyFactory` untuk spawn variasi musuh (Easy, Medium, Hard Yajuj).                 |
| **3. Object Pool**     | Creational | Manajemen memori untuk _Bullets_ dan _Particles_ agar performa stabil (GC friendly). |
| **4. Command**         | Behavioral | `InputHandler` untuk membungkus aksi player (Jump, Shoot, Reload).                   |
| **5. Observer**        | Behavioral | Sistem HUD (UI) yang memantau status Player (Darah berkurang -> UI update).          |
| **6. State**           | Behavioral | Mengatur status Player (`Idle`, `Run`, `Shoot`) atau Game (`Menu`, `Play`, `Pause`). |
| **7. Strategy**        | Behavioral | Logika senjata (`WeaponStrategy`), membedakan perilaku Pistol vs Shotgun.            |
| **8. Template Method** | Behavioral | Algoritma loading level atau inisialisasi AI musuh.                                  |
| **9. Facade**          | Structural | `WorldRenderer` untuk menyederhanakan kompleksitas rendering 3D di `Main` class.     |
| **10. Game Loop**      | Pattern    | Custom update loop untuk memisahkan logic physics dan rendering.                     |

---
