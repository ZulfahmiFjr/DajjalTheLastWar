# 👁️ Dajjal: The Last War 🏔️
![picture dajjal](https://raw.githubusercontent.com/imhotepes/ProgDas/main/caches/6657f8e1436d0638ba11a780410e13da6ff614df0183f3907f56ca30b0c132c6.png)

![Java](https://img.shields.io/badge/Language-Java-orange) ![Framework](https://img.shields.io/badge/Framework-LibGDX-red) ![Backend](https://img.shields.io/badge/Backend-SpringBoot-green) ![Platform](https://img.shields.io/badge/Platform-Desktop%20%7C%20Web-lightgrey) ![Status](https://img.shields.io/badge/Status-In%20Development-blue)

> **Proyek Akhir Pemrograman Berorientasi Objek (OOP) - Teknik Komputer UI 2025/2026** > _Game FPS 3D survival tentang pertempuran terakhir melawan kejahatan tertinggi di akhir zaman: **Dajjal**._

## 🔗 Download & Play

Game ini disubmit buat menuhin tugas akhir dan sekarang udah bisa kalian download atau mainin langsung lewat **itch.io**:
👉 **[Klik di sini buat main Dajjal: The Last War](https://mrshlaf.itch.io/dajjal-the-last-war)**

---

## 👥 Tim Pengembang (Kelompok 7)

| Nama Anggota                   | NPM        | Role                                                                                                                                                                                                                                     |
| :----------------------------- | :--------- |:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Caesar Nur Falah Widiyanto** | 2406487052 | **HUD Specialist:** Ngurusin tampilan informasi layar pas main, juga ngurusin bar darah player, stamina, nyawa boss, sampe info stage biar pemain tau kondisi lagi aman atau sekarat.                                                    |
| **Marshal Aufa Diliyana**      | 2406346913 | **Backend & UI System Architect:** Ngurusin backend game sama logika alur permainan, bikin sistem login yang aman ke server, sampe bikin UI screen login dan menu beserta transisi animasinya yang mulus.                                |
| **Muhammad Hashif Jade**       | 2406396786 | **Combat Engineer:** Ngurusin di mekanik senjata buat lawan entitynya kyak animasi nembak dan reload, bikin kontrol karakter responsif, itungan damage pas, dan hitbox peluru yang akurat biar aksi tembak tembakan kerasa dapet banget. |
| **Zulfahmi Fajri**             | 2406345425 | **Lead Programmer & 3D Artist:** Ngurusin otak teknis game ini, ngeintegrasiin semua sistem, ngerancang algoritma inti gameplay sekaligus yang bikin model 3D dan visual dunia game biar keliatan cakep dan realistis.                   |

## 👨‍🏫 Aslab Pendamping

| Nama Aslab [Kode Aslab] | NPM        |
| :---------------------- | :--------- |
| **Shafwan Hasyim [SH]** | 2306209113 |

---

## 📜 Latar Belakang & Lore

Jadi game ini tuh settingnya pas lagi akhir zaman banget, kita bakal main jadi satu satunya harapan buat nyelametin umat manusia yang tersisa. Misi kita sebenernya simpel tapi berat, yaitu harus mendaki "Gunung Fitnah" yang jalannya muter spiral ke atas kayak obat nyamuk.

Perjalanan kita gak bakal mulus karena bakal dibagi jadi beberapa fase sesuai ketinggian gunungnya. Pas masih di **Kaki Gunung (Stage 1)**, mungkin rasanya masih kyak pemanasan, musuhnya cuma dikit dan jalannya juga santai, tapi begitu mulai nanjak masuk ke **Fase Pertengahan (Stage 2 & 3)**, mulai kerasa deh tuh bedanya, musuh jadi makin alot darahnya dan larinya makin kenceng.

Nahh pas udah **Agak Tinggi (Stage 4 & 5)**, di sini skill kita beneran diuji, musuhnya gak cuma makin tebel, tapi munculnya makin cepet kyak gak dikasih napas, makin banyak juga. Puncaknya ada di **Stage 6 (Neraka)**, ini bener bener chaos parah, bayangin aja musuhnya rame banget sampe 40 biji, darahnya 3 kali lipat lebih tebel, larinya ngebut, dan sekali pukul damagenya sakitnyaa minta ampun. Kalo kita sukses ngelewatin neraka itu, baru deh kita bakal duel satu lawan satu ngadepin bos terakhirnya, si **Dajjal**.

## 🛠️ Tech Stack

- **Core Game:** Java & LibGDX (OpenGL ES).
- **Backend Services:** Spring Boot (Authentication & Score Leaderboard).
- **Architecture:** MVC (Model-View-Controller) dengan penerapan 10 Design Patterns.
- **Build Tool:** Gradle.

---

## ✅ Development Roadmap (TODO List)

Ini progres pengerjaan game kami sejauh ini. Alhamdulillah semua fitur utama udah **Selesai (Done)** dan siap dimainkan:

### 🌍 World Generation & Environment

- [x] **Procedural Terrain:** Bikin gunung acak otomatis pake _Perlin Noise_, jadi bentuk gunungnya gak monoton.
- [x] **Spiral Road Logic:** Algoritma _Newton's Iteration_ buat bikin jalanan spiral yang mulus dari bawah sampe puncak gunung.
- [x] **Vegetation System:** Naro 600+ pohon pake logika pinter (_Spatial Filtering_), jadi pohon gak bakal numbuh ngehalangin jalan atau numpuk di arena boss.
- [x] **Visual Polish:** Daun pohon transparan (_Blending_) biar realistis, sama tekstur jalan yang nyatu sama tanah.
- [x] **Atmosphere & Fog:** Kabut prosedural dan langit dinamis (efek petir pas lawan Dajjal) yang diurus sama `WorldRenderer` biar suasana makin mencekam.

### 🎮 Player Mechanics & Physics

- [x] **Movement System:** Gerakan player (WASD) udah rapi pake _Command Pattern_ (`InputHandler`), jadi kodingannya bersih dan responsif.
- [x] **Camera System:** Kamera FPS yang gak bikin pusing, udah ada batesan nengok atas-bawah (_pitch clamping_).
- [x] **Jump & Gravity:** Fisika lompat dan jatuh yang pas, gak melayang layang kayak kapas.
- [x] **Tree Collision Optimization:** Tabrakan sama ribuan pohon biar gak ngelag karena udah pake sistem kotak kotak (_Spatial Grid_). CPU jadi enteng!
- [x] **Weapon System:** Sistem senjata canggih (_Strategy Pattern_). Bisa ganti ganti dari Pistol (ada varian Zippy, Chunky, Scoped) sampe AK-47 dengan efek _recoil_ dan _reload_ yang beda.

### 👾 AI & Enemies

- [x] **Yajuj Majuj:** Musuh spawn otomatis pake _Factory Method_. Makin tinggi nanjak gunung, musuhnya makin kuat dan ganas dan juga makin banyak.
- [x] **Dajjal (Final Boss):**
    - [x] Model 3D Dajjal dimuat rapi pake `ResourceManager`.
    - [x] **Boss Animations:** Animasi Dajjal lengkap (Diem, Lari, Mukul) diatur pake controller khusus.
    - [x] **Boss Logic:** AI Dajjal pinter, bisa ngunci posisi player pas mau mukul dan punya fase ngamuk.
- [x] **Loot Drop System:** Musuh yang mati bakal ngedrop koin, darah, atau peluru (gacha dikit lahh probabilitasnya).
- [x] **Particle System:** Efek darah muncrat pas musuh mati udah pake _Object Pool_, jadi memori aman walau bantai banyak musuh sekaligus.

### 🔌 Backend & System

- [x] **Authentication:** Login & Register aman, data user langsung konek ke backend Spring Boot.
- [x] **Score System:** Duit/Koin yang didapet di game langsung disimpen ke server database, jadi gak bakal ilang kalo game ditutup, ataupun tiba tiba putus koneksi servernya.
- [x] **Black Market (Shop):** Toko dalem game buat beli senjata atau upgrade pistol pake koin hasil ngebunuh musuh.
- [x] **Asset Management:** Semua gambar, model 3D, dan suara dimuat di awal pake `ResourceManager` (Singleton), loading sekali doang biar pas main lancar jaya.
- [x] **UI & HUD:** Tampilan darah, peluru, info wave, dan menu pause yang informatif dan gampang dibaca.

---

## 🏗️ Implementasi Design Patterns

Sesuai ketentuan tugas akhir, kami nerapin 10 Design Pattern berikut buat mastiin kode yang clean dan scalable serta optimal:

| Pattern                | Kategori   | Implementasi Rencana                                                                                                                                                                                                                              |
| :--------------------- | :--------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **1. Singleton**       | Creational | Diterapin di ResourceManager. Jadi semua aset kyak model 3D, tekstur, sama suara dimuat sekali doang di memori, class lain tinggal panggil instance ini, gak perlu loading ulang yang bikin berat.                                                |
| **2. Factory Method**  | Creational | Pake di EnemyFactory. Ini tuh pabriknya musuh, game gak perlu tau cara bikin Yajuj, Majuj, atau Dajjal. Tinggal minta ke factory, dia yang urus detail spawnnya di koordinat mana.                                                                |
| **3. Object Pool**     | Creational | Diterapin di ParticleSystem buat efek darah (BloodParticle). Daripada bikin objek baru tiap musuh mati trus dihapus, karna akan bikin sampah memori, mending pake sistem kolam. Partikel yang udah ilang dipake ulang, jadi hemat memori bangett! |
| **4. Command**         | Behavioral | Ada di InputHandler sama GameCommands. Tombol keyboard (WASD, Spasi, Shift) dibungkus jadi objek perintah, jadi logic gerak gak numpuk di controller pake if-else panjang, tapi kepisah rapi per aksinya.                                         |
| **5. Observer**        | Behavioral | Dipake di ItemManager (ItemListener) sama PlayerController (WarningListener). Sistemnya kyak langganan info, kalo player dapet koin atau nabrak tembok, sistem ini bakal ngasih tau HUD buat update tampilan skor/warning.                        |
| **6. State**           | Behavioral | Ini otak dari AI musuh di BaseEnemy. Musuh punya status yang jelas, lagi Emerge (keluar tanah), Chase (ngejar), Attack (mukul), atau Die, perilakunya jadi berubah drastis tergantung statusnya sekarang lagi apa.                                |
| **7. Strategy**        | Behavioral | Diterapin di sistem senjata (Firearm, Pistol, AkRifle). Tiap senjata punya strategi nembak sendiri, pistol logicnya single shot, AK-47 auto. Kita bisa ganti strategi nembak (ganti senjata) secara realtime.                                     |
| **8. Template Method** | Behavioral | Ada di StageConfigs. Kita punya cetakan dasar level (BaseStage), nah anak anaknya (Stage 1 sampe 6) tinggal ngisi detail bedanya, kyak jumlah musuh atau multiplier darahnya, kerangkanya tetep ikut induknya.                                    |
| **9. Facade**          | Structural | Ini ada di WorldRenderer. GameScreen kita gak perlu pusing mikirin ribetnya OpenGL, setting kabut, lighting, atau batch drawing, cukup panggil satu fungsi render(), si facade ini yang ngurus semua keruwetan di belakang layar.                 |
| **10. Game Loop**      | Pattern    | Ada di GameScreen. Kita pisahin tegas antara updateGameLogic() buat mikir fisika/AI sama render() buat gambar, jadi game jalan mulus, logic gak keganggu sama urusan gambar menggambar.                                                           |

---
