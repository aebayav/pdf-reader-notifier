# Changelog

Bu dosyadaki tum degisiklikler tarihe gore, en yeni ustte olacak sekilde listelenmistir.

## 27 Agustos 2026

### Bildirim CRUD (listele / guncelle / sil)
- `GET /api/v1/notifications` - tum bildirimler (son tarihe gore artan)
- `PUT /api/v1/notifications/{id}` - kismi guncelleme; sadece gonderilen alanlar degisir
- `DELETE /api/v1/notifications/{id}` - 204; bilinmeyen id icin 404 `NOT_FOUND`
- Frontend: sayfa acilisinda bildirimler DB'den yuklenir; kartlardaki **Guncelle/Sil** butonlari aktif (satir ici duzenleme + onayli silme) (`640563e`)

### Yapay Zeka (Gemini) entegrasyonu
- `POST /api/v1/notifications/ai-upload` - OCR metni Gemini'ye gonderilir, tarihli yukumlulukler JSON olarak alinip bildirime donusturulur (`ad35d4d`)
- Model: `gemini-3.6-flash`; yanit `responseMimeType=application/json` ile zorlanir
- **Tek istek garantisi**: belgenin tamami tek `generateContent` cagrisinda gider (limit 1M karakter); yanit parcalara ayrilir (`bca1cbf`)
- Frontend: "AI ile analiz et (Gemini)" secenegi (varsayilan acik) (`1cd6024`)

### Guvenlik / Yapilandirma
- Gemini API anahtari kaynak koddan cikarildi; `GEMINI_API_KEY` ortam degiskeni veya repo kokundeki `.env` dosyasindan okunur (`.gitignore`'da) (`75767cd`)
- Backend `.env`'i baslatma yonteminden bagimsiz okur (IntelliJ / mvnw / run-backend.cmd) (`1bb7e92`)
- `run-backend.cmd`: tek tikla derle + baslat; `.env`'i otomatik yukler (`f5b4e5e`)

## 26 Agustos 2026

### Bildirim kalitesi
- Parca parca notlar butunlesti: satirlar cumle sonu noktalamasina kadar birlestirilir; ayni aralik + tarih tek not uretir (`1141abf`)
- Anlamli basliklar: anahtar kelimeye gore etiket (Sozlesme Tarihi, Yer Teslimi, Gecici/Kesin Kabul, Ceza / Mueyyide vb.), etiket yoksa ilk cumle
- Mukerrer kontrolu **belge bazli** yapildi: `source_hash` (SHA-256) kolonu; ayni belge yeniden yuklenince tum notlar atlanir, farkli belgeler atlanmaz

## 21 Agustos 2026

### OCR iyilestirmeleri
- Gurultu reddi: bolum numaralari (`3.2.1.10.5`) ve teknik spec'ler (`20.3/35 kV`) tarih sanilmiyor; 2 haneli yil guard'i (`d30f2ae`)
- Tesseract'a gercek DPI bildirildi (`user_defined_dpi`) - "Estimating resolution" log spam'i bitti (`60530de`)

## 20 Agustos 2026

### Turkce OCR dogrulugu
- OCR dili `tur` olarak sabitlendi (tur+eng Turkce aksanlari bozuyordu); 300 DPI gri tonlama; `repairTurkishText` onarim sozlugu (`b34d68d`)

### Backend temeli
- Bildirimler PostgreSQL'e kaydediliyor; CORS; `/extract-text` ve `/analyze-contract` endpoint'leri (`bc52d4a`)
- Frontend backend API'ye baglandi: upload + bildirim kartlari (`b2b7316`)

## 19 Mayis 2026

### Ilk surum
- Spring Boot 4 (Java 26) backend + React 19/Vite frontend iskeleti (`1570cc4`)
