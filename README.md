# Personal Life Todo List — Versi Android Native (Kotlin)

Proyek Android Studio lengkap dari aplikasi Personal Life Todo List yang
sebelumnya dibuat versi web di Muse. Ini **kode sumber**, bukan APK jadi:
buka proyek ini di Android Studio, lalu build APK sendiri (langkah di bawah).

## Apa yang sudah termasuk

- **One Task at a Time** — tugas kedua tidak bisa dimulai selama ada tugas aktif.
- **Alarm native terjadwal** (`AlarmManager.setExactAndAllowWhileIdle`) untuk:
  1. **Alarm berbunyi tepat pada jam mulai tugas** (lihat "Perbaikan alarm"
     di bawah — sekarang benar-benar berdering, bukan hanya notifikasi),
  2. Pengingat setiap 30 menit selama tugas dikerjakan,
  3. **Pengingat terlambat setiap 30 menit** untuk tugas yang jam mulainya
     sudah lewat tapi belum dikerjakan (lihat "Perbaikan alarm terlambat"
     di bawah — ini perbaikan terbaru),
  4. Alarm akhir istirahat (maksimal 15 menit).
  Alarm berdering lewat layanan `AlarmRingService` + layar full-screen di atas
  lock screen, dan dijadwalkan ulang otomatis setelah HP restart
  (`BootReceiver`).
- **Keputusan yang sudah disepakati tetap berlaku:**
  - Tugas aktif yang lewat deadline **tetap biru + badge merah "Terlambat"**.
  - Timer fokus **tetap jalan** saat alarm pengingat berbunyi (tidak dijeda).
  - Setelah alarm istirahat, pengerjaan lanjut hanya setelah tombol
    **Kembali Bekerja** ditekan.
  - Tombol **Hapus** (tugas belum dimulai), **Batalkan** (tugas aktif), dan
    **Hapus dari Riwayat**.
- **Warna status dominan:** badge status blok solid pekat dengan teks putih
  besar tebal, garis warna lebar di sisi kiri kartu, badan kartu ber-tint
  warna status — sama seperti versi web terakhir.
- **Perulangan:** Tidak berulang / Setiap hari / Setiap minggu / Setiap bulan,
  dengan opsi tanggal berakhir dan pratinjau jumlah jadwal. Setiap tanggal
  menjadi tugas terpisah (label "Berulang Harian/Mingguan/Bulanan").
  Tanpa tanggal berakhir: 28 harian, 12 mingguan, 6 bulanan.
- **Dashboard bisa di-tap:** bar/kartu tugas di Beranda dan daftar Tugas masuk
  ke **Halaman Fokus** — ada tombol **Mulai** besar, timer fokus,
  Istirahat / Selesai / Batalkan, dan ringkasan untuk tugas selesai.
- **Menu Tugas sekarang kalender sederhana:** satu kalender bulanan, tanggal
  bertanda titik bila ada tugasnya; **tap tanggalnya** dan daftar tugas pada
  tanggal itu muncul di bawah kalender (lihat "Menu Tugas jadi kalender").
- **Jadwal Hari Ini di Beranda sudah dirapikan:** status cukup ditunjukkan
  **simbol lingkaran** (tanpa pill teks panjang), dan **kalender dirapikan**
  dengan sel yang lebih tinggi (lihat "Rapikan Jadwal + kalender" di bawah).
- **Timer muncul di notifikasi:** selama tugas dikerjakan, ada notifikasi
  menetap berisi **timer fokus yang berjalan**; saat istirahat berubah jadi
  **hitung mundur sisa istirahat** (lihat "Timer di notifikasi" di bawah).
- **Statistik** harian / mingguan / bulanan + **Riwayat**, dan **Profil** dengan
  tema Terang / Gelap / Ikuti Sistem serta status izin alarm.
- Data tersimpan lokal di HP (database Room). Aplikasi mulai kosong, sama
  seperti versi web.

## Rapikan Jadwal + kalender & timer di notifikasi (update ini)

Permintaan Gusti: *"1. jadwal di dashboard kurang rapi. tolong dirapikan.
mungkin lebih simpel tanda selesai dan belum dikerjakan diganti simbol saja
2. kalender juga dirapikan. bentuk bisa panjang kebawah jika memang kurang
muat 3. aku ingin timer juga muncul di bagian notifikasi"*

Ketiganya sudah dikerjakan:

1. **Jadwal Hari Ini (Beranda) dirapikan.** Penyebab berantakannya (terlihat
   di screenshot): pill teks status yang panjang ("Belum Dikerjakan")
   memakan hampir separuh lebar bar, jadi judul terpotong ("Bac...",
   "Try ...") dan teks "Tap untuk buka Fokus" membungkus satu kata per
   baris menjulang ke bawah. Sekarang:
   - Status diganti **simbol saja**: lingkaran 32.dp berwarna solid sesuai
     status dengan ikon putih di tengah — ✓ hijau = Selesai, ▶ biru =
     Sedang Dikerjakan, ◷ merah = Belum Dikerjakan, lonceng merah tua =
     Terlambat, ⊘ abu = Dibatalkan. Artinya dijelaskan **legenda** kecil di
     bawah judul "Jadwal Hari Ini".
   - Teks "Tap untuk buka Fokus" **dihapus dari bar**. Bar-nya sendiri
     **tetap bisa di-tap** untuk masuk Halaman Fokus (ditandai panah › di
     kanan), jadi judul sekarang utuh dan bar jauh lebih ringkas: jam,
     judul 1 baris, tanggal + deadline 1 baris, simbol, panah.
   - Kartu daftar Tugas dan Riwayat **tidak diubah** — di sana pill teks
     status tetap tampil seperti sebelumnya.
2. **Kalender (tab Tugas) dirapikan.** Penyebabnya: sel tanggal memakai
   lingkaran sebesar sel, jadi titik penanda + jumlah tugas terjepit di
   tepi bawah lingkaran dan tampil berantakan (pada tanggal terpilih
   malah terpotong lingkaran birunya). Sekarang tiap sel **tinggi tetap
   58.dp** — kalender memang jadi lebih panjang ke bawah, seperti yang
   diizinkan — angka tanggal di dalam **lingkaran kecil 38.dp**, dan titik
   penanda + jumlah tugas punya **baris sendiri setinggi 12.dp** di
   bawah angka yang selalu disediakan, jadi semua angka sejajar rapi.
   Di bawah grid ada legenda titik: biru = ada tugas, merah = ada yang
   terlambat, angka kecil = jumlah tugas.
3. **Timer muncul di notifikasi.** Ditambah `FocusTimerService`: selama
   tugas dikerjakan, muncul **satu notifikasi menetap** di panel notifikasi
   (dan lock screen) berisi judul tugas + **timer fokus yang berjalan**
   (menghitung naik dari total waktu fokus) + deadline. Timer dirender
   sistem Android (chronometer), jadi tetap akurat walau aplikasi ditutup.
   Saat **Istirahat**, notifikasi yang sama berubah menjadi **hitung
   mundur sisa waktu istirahat** (dari 15:00). **Tap notifikasinya** membuka
   Halaman Fokus tugas itu. Notifikasi **hilang sendiri** saat tugas
   selesai / dibatalkan / dihapus, dan **dipulihkan otomatis** setelah HP
   restart atau aplikasi dibuka lagi bila tugasnya masih berjalan.
   Notifikasi ini tanpa suara/getar (channel khusus prioritas rendah),
   terpisah dari notifikasi alarm yang berdering.

**Cara pasang:** seperti biasa — hapus folder proyek lama, ekstrak ZIP ini
ke folder bersih, Gradle Sync, Build APK. Database tidak berubah, jadi
**tidak perlu uninstall** aplikasi di HP; install menimpa versi lama dan
tugas-tugasmu tetap ada. Setelah install, buka aplikasinya sekali supaya
alarm terjadwal ulang dan timer notifikasi tersinkron.

**Cara uji:**
- Buka Beranda: bar Jadwal Hari Ini sekarang ringkas — judul tidak
  terpotong, tidak ada tulisan yang membungkus ke bawah, status berupa
  lingkaran simbol berwarna di kanan. Tap bar-nya tetap masuk Fokus.
- Buka tab Tugas: kalender tampil lebih tinggi dan rapi; titik + jumlah
  tugas berada tepat di bawah angka tanggal, tidak terpotong lingkaran.
- Mulai sebuah tugas, lalu tarik panel notifikasi dari atas: ada
  notifikasi menetap berisi judul tugas dan timer yang berjalan.
  Tekan Istirahat: timer berubah jadi hitungan mundur sisa istirahat.
  Selesaikan tugas: notifikasi timer hilang.

## Perbaikan bug: tampilan kacau saat alarm tugas berikutnya muncul (update ini)

Laporan Gusti: *"ada bug saat mengerjakan tugas. kondisi ini terjadi saat
mengerjakan tugas, namun alarm mulai tugas berikutnya muncul. jadi tampilan
error seperti di gambar."*

Pada screenshot, kartu tugas yang sedang dikerjakan ("Labex", Sedang
Dikerjakan + Terlambat) rusak: ada **kolom huruf vertikal** di tepi kanan
kartu ("…ai te rl a m b at d ar i ja d w al"), ruang kosong besar, dan timer
terdorong jauh ke bawah. Kolom huruf itu adalah teks **"Mulai terlambat
dari jadwal"** yang hancur. Penyebabnya ada dua lapis:

1. **Tata letak (yang terlihat di screenshot).** Catatan "Mulai terlambat
   dari jadwal" dulu ditaruh **sebaris** dengan chip "Sedang Dikerjakan"
   dan badge "Terlambat". Dua pill itu saja hampir memenuhi lebar kartu,
   jadi sisa lebar untuk teks catatan tinggal beberapa dp. Teks terpaksa
   membungkus **1–2 huruf per baris**, barisnya menjulang sangat tinggi,
   chip terdorong ke tengah kartu (barisnya di-center vertikal) dan semua
   isi di bawahnya terdorong ke bawah.
   **Perbaikan:** catatan itu sekarang **baris sendiri selebar kartu, di
   bawah badge** — di hero Beranda, di Halaman Fokus, dan di kartu daftar
   Tugas (tiga tempat yang memakai pola baris yang sama). Teks chip/badge
   juga dikunci maksimal 1 baris. Warna, urutan, dan isi lain tidak berubah
   (tetap sama dengan versi web).
2. **Alarm tugas berikutnya saat tugas lain sedang dikerjakan.** Dulu alarm
   jam mulai tugas berikutnya tetap berdering **layar-penuh** menutupi
   pekerjaan yang sedang berjalan — padahal tombol "Mulai Tugas Sekarang"
   di layar alarm itu pun terblokir aturan One Task at a Time (dan
   pemblokirannya diam saja, tanpa pesan).
   **Perbaikan:** selama ada tugas lain yang sedang dikerjakan/istirahat,
   alarm jam mulai/pengingat terlambat tugas berikutnya **ditunda**: cukup
   **notifikasi biasa** ("Tugas Berikutnya Sudah Waktunya", tanpa dering
   looping dan tanpa layar penuh) yang menyebut tugas yang sedang
   dikerjakan, dan rantai pengingat tiap 30 menit tetap berjalan — begitu
   tugas yang berjalan selesai, pengingat berikutnya **berdering penuh
   seperti biasa**. Bila layar alarm tetap terbuka dan tombol "Mulai Tugas
   Sekarang" ditekan saat tugas lain aktif, sekarang muncul pesan tugas
   mana yang harus diselesaikan dulu, lalu kembali ke Beranda.
   Sekalian diperketat: alarm yang status tugasnya sudah tidak cocok
   (tugas sudah dimulai manual/selesai/dibatalkan/dihapus sejak alarm
   dijadwalkan) sekarang **tidak berdering sama sekali**.

**Cara pasang:** seperti update sebelumnya — hapus folder proyek lama,
ekstrak ZIP ini ke folder bersih, Gradle Sync, Build APK. Database tidak
berubah, jadi **tidak perlu uninstall** aplikasi di HP; install menimpa
versi lama dan tugas-tugasmu tetap ada. Buka aplikasinya sekali setelah
install supaya alarm terjadwal ulang dengan aturan baru.

**Cara uji:**
- Buat tugas, mulai **setelah** jam mulainya (supaya tercatat "Mulai
  terlambat dari jadwal"), lalu buka Beranda / Halaman Fokus: catatan itu
  harus tampil **utuh satu baris** di bawah badge, tanpa kolom huruf
  vertikal, dan timer langsung di bawah judul seperti biasa.
- Selagi tugas itu berjalan, buat tugas lain yang jam mulainya tiba saat
  itu juga (atau sudah dekat): yang muncul hanya **notifikasi biasa**,
  layar fokus tidak tertutup alarm. Selesaikan tugas yang berjalan, lalu
  pada pengingat 30 menit berikutnya tugas yang tertunda itu berdering
  penuh seperti biasa.

## Perbaikan alarm terlambat: sekarang berbunyi lagi tiap 30 menit (update ini)

Keluhan Gusti: *"kok aplikasinya 30 menit terlambat mengerjakan tugas tidak
muncul alarm lagi, harusnya ada."*

Betul — itu memang kekurangan versi lama, dan penyebabnya ada dua, dua-duanya
di rantai penjadwalan untuk tugas yang **belum dimulai** (PENDING):

1. Alarm jam mulai hanya dijadwalkan **satu kali**. Setelah berbunyi (dan
   misalnya dimatikan), tidak ada kode yang menjadwalkan alarm berikutnya —
   `AlarmReceiver` versi lama hanya menjadwalkan ulang pengingat untuk tugas
   yang sedang dikerjakan (`REMINDER_30`), tidak untuk tugas yang belum
   dimulai. Jadi sesudah itu tugas terlambat berjam-jam pun diam saja.
2. Bila penjadwalan ulang terjadi saat jam mulai **sudah lewat** (sesudah HP
   restart, atau tugas dibuat sudah terlambat), waktu picunya ada di masa
   lalu dan dibuang oleh penjadwal — hasilnya juga tidak ada alarm sama sekali.

Perbaikannya:

- Jenis alarm baru **`LATE_REMINDER` — "Tugas Belum Dikerjakan!"** — berdering
  dengan cara yang sama seperti alarm jam mulai (nada alarm looping + getar +
  layar full-screen, tombol **Mulai Tugas Sekarang** / **Matikan Alarm**).
- Untuk tugas yang belum dimulai dan jam mulainya sudah lewat, pengingat
  dijadwalkan pada **kelipatan 30 menit berikutnya dari jam mulai**
  (mulai+30, +60, +90, ...). Setiap kali pengingat berbunyi,
  `AlarmReceiver` menjadwalkan kelipatan berikutnya selama tugas masih
  belum dimulai — jadi alarm **muncul lagi tiap 30 menit** persis seperti
  yang diminta, dan **berhenti sendiri** begitu tugas dimulai, selesai,
  dibatalkan, atau dihapus.
- Mematikan alarm di layar alarm hanya menghentikan dering kali itu;
  pengingat 30 menit berikutnya tetap terjadwal (sudah dipasang oleh
  receiver saat alarm berbunyi, bukan tergantung tombol yang ditekan).
- Pengingat 30 menit untuk tugas yang **sedang dikerjakan** tidak berubah
  perilakunya, hanya diperketat: siklus berikutnya kini hanya dijadwalkan
  selama tugas benar-benar masih berjalan (bukan saat istirahat).
- Tampilan aplikasi tidak berubah gara-gara perbaikan ini, kecuali menu
  Tugas di bawah ini.

**Cara uji:** buat tugas yang mulai **2 menit dari sekarang**, kunci layar,
biarkan alarm jam mulai berbunyi lalu tekan **Matikan Alarm** (jangan mulai
tugasnya). Sekitar **30 menit setelah jam mulai**, alarm harus **berbunyi
lagi** dengan layar "Tugas Belum Dikerjakan!". Ulangi: ia berbunyi lagi tiap
30 menit sampai tugas dimulai. Bila mau cepat, buat tugas yang jam mulainya
sudah lewat 25 menit dari sekarang — pengingat pertamanya berbunyi ±5 menit
lagi (di kelipatan 30 menit dari jam mulai).

## Menu Tugas jadi kalender (update ini)

Permintaan Gusti: *"untuk menu tugas kita persimple dengan tampilan
kalender. tapi saat kita tap tanggalnya akan muncul daftar tugasnya."*

Sudah dibuat seperti itu:

- Tab **Tugas** sekarang hanya berisi **satu kalender bulanan** yang ringkas:
  judul bulan + tombol **< / >** untuk pindah bulan, kepala hari
  Sen–Min, dan tanggal-tanggalnya. Filter pill lama
  (Semua / Belum Dimulai / Berjalan / Selesai) **dihapus**.
- Tanggal yang ada tugasnya ditandai **titik** di bawah angkanya, plus
  jumlah tugasnya bila lebih dari satu. Titik berwarna **merah** bila di
  tanggal itu ada tugas terlambat yang belum dikerjakan. Tanggal terpilih
  dilingkari warna aksen; hari ini diberi latar biru muda.
- **Tap tanggal** → di bawah kalender muncul tanggal lengkapnya
  ("Jumat, 7 November 2026"), jumlah tugas, dan **daftar tugas pada tanggal
  itu** urut jam mulai, memakai kartu yang sama seperti sebelumnya — jadi
  tombol **Fokus / Mulai / Ubah / Hapus / Batalkan** tetap lengkap.
  Tanggal tanpa tugas menampilkan keterangan kosong. Tombol **Hari ini**
  muncul bila tanggal terpilih bukan hari ini, untuk melompat kembali.
- Beranda, Halaman Fokus, Tambah Tugas, Statistik, dan Profil **tidak
  berubah** — tetap sama dengan versi web seperti update sebelumnya.

**Cara pasang update ini:** seperti biasa — hapus folder proyek lama,
ekstrak ZIP ini ke folder bersih, Gradle Sync, Build APK, lalu install.
Update kali ini tidak mengubah database, jadi **tidak perlu uninstall**
aplikasi di HP; install menimpa versi lama dan tugas-tugasmu tetap ada.
(Setelah install, buka aplikasi sekali supaya semua alarm — termasuk
pengingat terlambat yang baru — terjadwal ulang dengan aturan baru.)

## Update tampilan: disamakan dengan versi web

Sesuai permintaan Gusti, tampilan Android di update ini ditulis ulang mengikuti
versi web di Muse (kode `App.tsx` + `theme.css`), bukan lagi gaya Material 3
bawaan. Yang disamakan:

- **Warna persis web, terang & gelap:** latar `#EEF2F9` / `#0A1122`, permukaan
  `#FFFFFF` / `#131F3A`, surface2 `#E3EAF6` / `#1B2C50`, teks `#14213D` /
  `#EAF0FD`, dim `#5B6B8C` / `#93A5C9`, border `#D3DCEE` / `#27395F`, aksen
  `#2563EB` / `#5B8CFF`, dan tint redBg/greenBg/blueBg/amberBg yang sama.
  **Dynamic color Android dimatikan** — di versi lama fitur itu menimpa palet
  web dari wallpaper HP, salah satu penyebab tampilan terasa beda.
- **Warna status solid web:** Selesai `#15803D`, Dibatalkan `#475569`,
  Sedang Dikerjakan `#2563EB`, Belum Dikerjakan `#DC2626`, Terlambat
  `#991B1B` (terang) / `#B91C1C` (gelap) — chip pill berikon dengan teks putih
  14.sp ExtraBold dan bayangan, sama seperti chip web.
- **Beranda, urutan sama dengan web:** sapaan + tanggal panjang + tombol
  lonceng bundar putih; kartu hero tugas aktif (latar blueBg, bingkai aksen
  2.dp, timer fokus 48.sp + progress bar siklus 30 menit, tombol Istirahat
  putih dan Selesai hijau) atau kartu Tugas Berikutnya (Buka Halaman Fokus +
  Mulai Tugas Ini); Ringkasan Hari Ini 3 kotak dengan angka di tengah;
  **Jadwal Hari Ini sekarang bar ringkas seperti web** (jam 56.dp, judul,
  "Tap untuk buka Fokus", chip + panah) — bukan kartu tugas penuh seperti
  sebelumnya; ditutup kartu Streak dan Tips singkat (teks tips sama).
- **Tab Tugas:** saat itu filter berbentuk pill web (aksen saat terpilih),
  dan kartu tugas punya tombol aksi di dalam kartu seperti web
  (Fokus / Mulai / Ubah / Hapus untuk yang belum dimulai,
  Lihat Timer Fokus / Batalkan untuk yang berjalan,
  Fokus / Hapus dari Riwayat untuk yang selesai).
  *(Di update terbaru tab ini sudah diganti kalender bulanan — lihat
  "Menu Tugas jadi kalender" di atas. Kartu + tombol aksinya tetap sama,
  kini tampil per tanggal yang di-tap.)*
- **Halaman Fokus:** kartu rounded-3xl berbingkai seperti web, kotak
  surface2 berisi jam mulai besar untuk tugas yang belum dimulai, tombol
  Mulai besar 18.sp, dan ringkasan dua kotak untuk tugas selesai.
- **Tambah/Ubah Tugas:** label di atas kolom, kotak rounded-xl berbingkai
  border (fokus = aksen), tanggal & waktu mulai/deadline berdampingan,
  Perulangan di dalam kartu berbingkai dengan pratinjau jumlah jadwal,
  tombol "Buat Tugas" / "Simpan Perubahan".
- **Statistik:** pemilih periode 3 kolom, grid metrik 2 kolom (6 kartu,
  termasuk Frekuensi penundaan dan Persentase penyelesaian), progress bar
  hijau, lalu Riwayat — sama seperti web.
- **Profil:** judul "Profil & Pengaturan", kartu-kartu putih rounded-3xl:
  Nama sapaan (sekarang bisa diisi; sapaan di Beranda memakai nama ini),
  Tema tampilan Sistem/Terang/Gelap, Notifikasi & Alarm, Aturan aplikasi.
- **Navigasi bawah persis web:** bar putih bergaris atas, label 12.sp,
  dan tombol **+** bundar 56.dp yang menonjol ke atas di tengah
  (sebelumnya NavigationBar Material 3 dengan indikator pill).
- Layar alarm full-screen ikut diberi latar tint yang sama dengan overlay
  alarm web (merah muda / biru muda / kuning muda sesuai jenis alarm).

Batas kejujuran soal "sama persis": susunan layar, warna, ukuran teks,
radius, dan tombol sudah disamakan, tetapi hasil render tidak bisa identik
sampai ke piksel karena web dirender browser (CSS) dan Android dirender
Jetpack Compose. Dua perbedaan kecil yang disengaja: (1) font web
Plus Jakarta Sans tidak dibundel di proyek ini (file font-nya tidak tersedia
offline), jadi Android memakai font Sans bawaan HP dengan bobot yang sama;
(2) deadline di Android selalu ada (database native memakainya untuk alarm
tugas aktif), jadi formulir selalu menampilkan tanggal/waktu deadline,
sedangkan di web deadline bisa dimatikan lewat centang. Fungsi, alarm, dan
data tidak berubah dari versi sebelumnya — proyek ini tetap **belum
dikompilasi di sini** (tidak ada Android SDK di lingkungan pembuatan), jadi
Gradle Sync + Build di Android Studio tetap menjadi verifikasi sebenarnya.

## Perbaikan alarm: sekarang benar-benar BERBUNYI (update ini)

Keluhan Gusti: *"alarm saat tugas sudah masuk jamnya maka dia akan berbunyi.
tadi yang ku lihat hanya sebuah notifikasi dan bukan itu yang aku inginkan."*

Betul — itu bug versi lama, dan penyebabnya jelas:

- **Penyebab:** versi lama hanya mengandalkan suara notification channel.
  Suara channel hanya diputar **sekali** (bunyi singkat), lalu berhenti.
  Tidak ada kode yang memutar nada alarm secara berulang, jadi yang terlihat
  memang hanya notifikasi biasa — bukan alarm yang berdering seperti weker.
- **Perbaikan:** ditambah `AlarmRingService`, foreground service dering yang
  dipanggil `AlarmReceiver` tepat pada jam yang dijadwalkan:
  - memutar **nada alarm bawaan HP secara berulang (looping)** lewat
    `MediaPlayer` (`USAGE_ALARM`) sampai dimatikan pengguna,
  - **getar berulang** + WakeLock supaya dering tidak terhenti Doze,
  - notifikasi full-screen (channel baru `alarm_ring_channel_v2`, sengaja
    tanpa suara channel agar tidak dobel dengan dering service) dan layar
    alarm yang **menyalakan layar & muncul di atas lock screen**,
  - tombol **Matikan Alarm** juga tersedia langsung pada notifikasinya,
  - dering **berhenti otomatis setelah 5 menit** bila tidak ada yang
    menekan apa pun, supaya tidak berbunyi selamanya.
- **Alarm jam mulai sekarang eksplisit:** jenis alarm lama `OVERDUE` diganti
  nama menjadi `START_TIME` — **"Waktunya Mulai Tugas!"** — tetap terjadwal
  tepat pada `scheduledStart` tugas yang belum dimulai (kode/requestCode sama,
  jadi alarm lama tetap bisa dibatalkan). Di layar alarmnya sekarang ada
  tombol **Mulai Tugas Sekarang** (dering berhenti + tugas langsung dimulai
  dan membuka aplikasi) di samping **Matikan Alarm** (dering berhenti saja).
  Pengingat 30 menit dan alarm akhir istirahat ikut memakai dering yang sama;
  tombol di layar alarm (Matikan Alarm / Kembali Bekerja) menghentikan
  deringnya.
- **Tampilan aplikasi tidak berubah** — tetap sama dengan versi web seperti
  update sebelumnya; yang berubah hanya perilaku bunyi alarm.

**Cara pasang update ini di HP (penting):** karena ada channel notifikasi
baru dan layanan baru, sebaiknya **hapus dulu aplikasi lama dari HP**
(uninstall), lalu install APK hasil build baru ini, buka sekali, dan berikan
lagi izin notifikasi + alarm eksak + abaikan optimasi baterai di tab Profil.
Data tugas lama di HP ikut terhapus saat uninstall — buat ulang tugas ujinya.

Uji alarmnya: buat tugas yang mulai **2 menit dari sekarang**, tutup
aplikasi, kunci layar. Tepat pada jamnya HP harus **berdering terus**
(nada alarm + getar) dengan layar alarm full-screen — bukan hanya muncul
notifikasi — sampai kamu menekan **Matikan Alarm** atau
**Mulai Tugas Sekarang**. Bila HP dalam mode Jangan Ganggu (DND) atau mode
senyap total, Android sendiri bisa menahan dering; matikan DND saat menguji.

## Perbaikan build (update ini)

Error terbaru yang muncul di Build Output:

> Unresolved reference 'androidx' — Screens.kt baris 188, 338, dan 424
> (`:app:compileDebugKotlin` gagal, 3 errors)

sudah diperbaiki. Penjelasannya:

- **Penyebab:** tiga tempat di `Screens.kt` (tombol lonceng di Beranda, pill
  filter di daftar Tugas, dan tombol Kembali di Halaman Fokus) memakai
  `.androidx.compose.foundation.clickable { ... }` langsung di tengah rantai
  `Modifier`. Penulisan nama lengkap seperti itu tidak dikenal Kotlin sebagai
  fungsi `Modifier.clickable`, jadi dianggap referensi yang tidak ada.
- **Perbaikan:** ditambahkan `import androidx.compose.foundation.clickable`
  di `Screens.kt`, dan ketiga tempat itu diubah menjadi `.clickable { ... }`
  biasa — sama seperti yang sudah dipakai di file lain proyek ini.
- **Tampilan tidak berubah sama sekali:** fungsi, urutan, warna, dan perilaku
  ketuk ketiga tombol itu persis sama seperti sebelumnya (versi yang sudah
  disamakan dengan web). Yang berubah hanya cara penulisan kodenya.

Error lama yang muncul di Build Output:

> Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required when compose is enabled.

sudah diperbaiki. Penjelasannya:

- **Penyebab:** mulai Kotlin 2.0, Compose Compiler terpisah dari compiler Kotlin.
  Versi sebelumnya masih memakai cara lama (`composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }`),
  jadi Android Studio menolak build.
- **Perbaikan:** plugin `org.jetbrains.kotlin.plugin.compose` versi `2.0.20`
  (sama persis dengan versi Kotlin `2.0.20`) sudah ditambahkan di
  `build.gradle.kts` (root) dan `app/build.gradle.kts`, dan blok
  `composeOptions` lama sudah dihapus. Semua versi lain tidak berubah
  (AGP 8.5.2, KSP 2.0.20-1.0.25, Compose BOM 2024.09.00, Gradle 8.7),
  karena urutan versi itu memang harus cocok satu sama lain.

**Penting sebelum build ulang:** hapus folder proyek lama yang error itu, lalu
ekstrak ZIP baru ini ke lokasi yang baru/bersih — jangan ditimpa di atas folder
lama, supaya tidak ada sisa file/config cache yang lama. Setelah itu buka folder
baru tersebut di Android Studio dan lanjutkan langkah di bawah.

## Cara build jadi APK

Syarat: komputer dengan **Android Studio** (Ladybug atau lebih baru) dan
JDK 17 (sudah termasuk di Android Studio).

1. Ekstrak ZIP ini, lalu di Android Studio pilih **File → Open** dan arahkan ke
   folder `PersonalLifeTodoList` (folder yang berisi `settings.gradle.kts`).
2. Tunggu **Gradle Sync** selesai (butuh internet sekali untuk mengunduh
   Gradle 8.7 dan dependency: Compose, Room, DataStore).
   - Bila Android Studio menawarkan membuat/memperbarui Gradle Wrapper,
     setujui saja.
3. Hubungkan HP Android lewat kabel USB (aktifkan USB Debugging), atau pakai
   emulator.
4. Klik **Run ▶** untuk langsung memasang versi debug ke HP.
5. Untuk file APK yang bisa dibagikan/diarsipkan:
   **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
   Hasilnya ada di `app/build/outputs/apk/debug/app-debug.apk`.
   (APK debug bisa langsung diinstal; untuk Play Store perlu bundle release
   yang ditandatangani dengan keystore sendiri lewat
   **Build → Generate Signed App Bundle / APK**.)

### Kalau masih error

1. **Gradle Sync** dulu: klik ikon gajah Sync di Android Studio, atau
   **File → Sync Project with Gradle Files**, lalu ulangi **Build APK(s)**.
   Kadang error pertama hilang sendiri setelah sync bersih.
2. Kalau errornya berubah menjadi soal versi Kotlin / AGP / JDK (misalnya
   Android Studio menawarkan upgrade), **jangan upgrade versi apa pun** —
   jangan klik tawaran upgrade AGP/Kotlin dan jangan ubah angka versi di
   `build.gradle.kts`. Rangkaian versi di proyek ini (Kotlin 2.0.20 +
   Compose plugin 2.0.20 + AGP 8.5.2 + KSP 2.0.20-1.0.25) memang dirancang
   cocok satu sama lain; menaikkan satu saja bisa memunculkan error baru.
3. Kalau tetap gagal, **kirim teks error lengkapnya, bukan cuma screenshot
   panelnya.** Caranya: di panel **Build Output**, klik kanan pada baris error
   merah → **Copy**, atau klik ikon **Download info / Export** (ikon panah
   ke bawah di sisi kanan panel Build) untuk menyimpan seluruh log, lalu
   kirim teks/file log itu. Screenshot yang terpotong hanya menampilkan
   sebagian error, jadi penyebab pastinya sulit dipastikan.

## Izin yang harus diberikan di HP (penting untuk alarm)

Saat pertama dibuka, aplikasi meminta izin notifikasi. Setelah itu, buka tab
**Profil** di aplikasi dan aktifkan:

1. **Aktifkan Alarm Eksak** — tanpa ini Android boleh menggeser waktu alarm.
2. **Izinkan Abaikan Optimasi Baterai** — mencegah sistem membekukan alarm.

Jujur soal batas: alarm native ini jauh lebih andal daripada versi web dan
dirancang berbunyi walau aplikasi ditutup dan layar terkunci. Tapi tidak ada
aplikasi Android yang bisa menjamin 100%: mode hemat baterai ekstrem,
Jangan Ganggu (DND), atau **force-stop manual** dari Pengaturan bisa tetap
menahan alarm. Karena itu jangan force-stop aplikasi bila masih ada jadwal.

## Checklist uji cepat setelah terpasang

- [ ] Buat tugas mulai 2 menit dari sekarang, tutup aplikasi, kunci layar:
      tepat pada jamnya alarm harus **berdering terus** (nada alarm + getar)
      dengan layar full-screen "Waktunya Mulai Tugas!", dan baru berhenti
      setelah **Matikan Alarm** / **Mulai Tugas Sekarang** ditekan —
      bukan hanya muncul notifikasi sekali bunyi.
- [ ] Tekan Mulai, tunggu / majukan jadwal uji: pengingat 30 menit berbunyi
      dan timer fokus tetap berjalan.
- [ ] **Baru:** pada uji pertama di atas tekan **Matikan Alarm** saja
      (jangan mulai tugasnya): ±30 menit setelah jam mulai, alarm harus
      **berbunyi lagi** ("Tugas Belum Dikerjakan!"), dan berulang tiap
      30 menit sampai tugas dimulai.
- [ ] **Baru:** mulai sebuah tugas SETELAH jam mulainya, lalu lihat
      Beranda / Halaman Fokus: tulisan "Mulai terlambat dari jadwal" tampil
      utuh satu baris di bawah badge — TIDAK ada kolom huruf vertikal di
      tepi kanan kartu dan timer tidak terdorong ke bawah.
- [ ] **Baru:** selagi tugas di atas berjalan, biarkan jam mulai tugas
      lain tiba: hanya notifikasi biasa yang muncul (layar fokus tidak
      tertutup alarm layar-penuh); selesaikan tugas yang berjalan, dan
      pada pengingat 30 menit berikutnya tugas tertunda itu berdering penuh.
- [ ] **Baru:** buka tab **Tugas**: yang tampil kalender bulanan. Tanggal
      bertugas ada titiknya; tap tanggal itu -> daftar tugas tanggal
      tersebut muncul di bawah kalender; tombol < / > memindah bulan.
- [ ] **Baru:** di Beranda, bar **Jadwal Hari Ini** tampil ringkas: judul
      utuh 1 baris, status berupa lingkaran simbol berwarna (bukan pill
      teks), dan tidak ada tulisan "Tap untuk buka Fokus" yang membungkus
      ke bawah. Tap bar-nya tetap masuk ke Halaman Fokus.
- [ ] **Baru:** kalender tampil rapi dan lebih tinggi: angka tanggal di
      lingkaran kecil, titik + jumlah tugas tepat di bawah angka dan tidak
      terpotong, legenda titik ada di bawah grid kalender.
- [ ] **Baru:** mulai tugas -> tarik panel notifikasi: ada notifikasi
      menetap berisi judul tugas + timer fokus berjalan. Tekan Istirahat ->
      timer berubah jadi hitung mundur sisa istirahat. Selesaikan /
      batalkan tugas -> notifikasi timer hilang.
- [ ] Tekan Istirahat, tunggu 15 menit: alarm istirahat berbunyi; timer lanjut
      hanya setelah **Kembali Bekerja**.
- [ ] Buat tugas berulang harian tanpa tanggal berakhir: harus terbentuk
      28 tugas terpisah.
- [ ] Restart HP: alarm tugas yang belum dimulai harus tetap terjadwal.

## Struktur proyek (ringkas)

```
app/src/main/java/com/gusti/personallifetodo/
├── MainActivity.kt          # navigasi Beranda/Tugas/Tambah/Statistik/Profil
├── PersonalLifeApp.kt       # inisialisasi Room + AlarmScheduler
├── data/Task.kt             # Entity, DAO, Database Room, Repository
├── alarm/
│   ├── AlarmScheduler.kt    # setExactAndAllowWhileIdle, 4 jenis alarm (termasuk pengingat terlambat 30 mnt)
│   ├── AlarmReceiver.kt     # memanggil layanan dering, siklus 30 mnt + rantai pengingat terlambat
│   ├── AlarmRingService.kt  # dering alarm looping + getar sampai dimatikan
│   ├── FocusTimerService.kt # timer fokus/istirahat menetap di notifikasi (chronometer)
│   ├── AlarmActivity.kt     # layar alarm di atas lock screen (Matikan/Mulai/Kembali Bekerja)
│   ├── BootReceiver.kt      # jadwal ulang setelah restart
│   └── NotificationHelper.kt# channel alarm + channel dering + channel timer
├── ui/
│   ├── AppViewModel.kt      # aturan bisnis: mulai/selesai/batal/istirahat
│   ├── Components.kt        # chip status, tombol & kartu gaya web
│   ├── Screens.kt           # Beranda, Tugas (kalender bulanan + daftar per tanggal), Halaman Fokus
│   ├── Screens2.kt          # Tambah/Ubah + Perulangan, Statistik, Profil
│   └── theme/Theme.kt       # palet persis theme.css web + warna status
└── util/Utils.kt            # format tanggal & generator perulangan
```

Teknologi: Kotlin 2.0.20, Jetpack Compose (Material 3), Room 2.6.1,
minSdk 26 (Android 8.0), targetSdk 34.
