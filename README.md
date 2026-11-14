# Manav Sayım Zebra (Android & PWA)

Zebra el terminalleri, Android cihazlar ve mobil tarayıcılar için optimize edilmiş, **%100 çevrimdışı (offline)** çalışan yerel Android uygulaması ve Progressive Web App (PWA).

---

## 1. Doğrudan Android APK İndir (Tamamen Çevrimdışı Çalışır)

İnternet bağlantısına hiç ihtiyaç duymadan, depoda veya manavda kesintisiz çalışabilen yerel Android APK paketi:

### 🆕 EN YENİ - Smart Voice Sistemi v2.0 (Önerilen)
- **Doğrudan İndirme:** [manav-sayim-v2.0-smart-voice.apk](https://github.com/1sthillman/File/raw/main/releases/manav-sayim-v2.0-smart-voice.apk) (86.48 MB)
- **Özellikler:**
  - ✅ **Çoklu Komut Desteği**: "domates 5 kilo sayım patates 4 kilo fire"
  - ✅ **Ürün Varyasyon Seçimi**: Muz → İthal/Baby, Domates → Salkım/Pembe/Kokteyl
  - ✅ **128 Ses Dosyası**: Ürün isimleri + Sayfa geçişleri + Sistem sesleri
  - ✅ **Akıllı Doğal Dil İşleme**: Farklı söyleyişleri anlayabilir

### Eski Versiyon - Native Voice v1.0
- **Doğrudan İndirme:** [manav-sayim-v1.0-native-voice.apk](https://github.com/1sthillman/File/raw/main/releases/manav-sayim-v1.0-native-voice.apk) (85.75 MB)

### Android APK Kurulum Adımları
1. Yukarıdaki **v2.0** bağlantısından APK'yı telefonunuza indirin
2. İndirilen dosyaya dokunup **Yükle** seçeneğini seçin (Bilinmeyen kaynak izni istenirse izin verin)
3. Uygulama açıldığında Kamera ve Mikrofon izinleri otomatik olarak istenir ve onaylanır
4. Artık internet bağlantısı olmasa dahi hiçbir uyarı vermeden doğrudan açılır ve çalışır

### 🎙️ Hızlı Test (v2.0)
İlk açılışta mikrofonu aç ve dene:
```
✓ "domates 5 kilo"          → Basit sayım
✓ "muz 3 kasa"              → Varyasyon seçimi (İthal/Baby)
✓ "domates 5 kilo sayım patates 4 kilo fire"  → Çoklu komut
```

---

## 2. Canlı Web / PWA Uygulama Adresi

- **Web Adresi:** [https://1sthillman.github.io/File/](https://1sthillman.github.io/File/)

### PWA Olarak Telefona Ekleme (Safari / Chrome)
- **iPhone / Safari:** Paylaş menüsünden "Ana Ekrana Ekle" seçeneğini kullanın.
- **Android / Chrome:** Menüden "Uygulamayı Yükle" veya "Ana Ekrana Ekle" seçeneğini kullanın.

---

## 3. Öne Çıkan Özellikler

### 🎙️ Akıllı Ses Komut Sistemi (v2.0)
- **Çoklu Komut**: Tek seferde birden fazla işlem ("domates 5 kilo sayım patates 4 kilo fire")
- **Ürün Varyasyonları**: Otomatik tespit ve seçim (Muz → İthal/Baby, Elma → Fuji/Gala/Golden vs.)
- **128 Ses Dosyası**: Her ürün, sayfa geçişi ve işlem için özel ses
- **Doğal Dil İşleme**: Farklı söyleyişleri anlayabilir

### 📱 Offline-First Mimari
- **Tamamen Çevrimdışı (Offline-First):** APK yerel varlıkları cihazın içinden yükler (`file:///android_asset/`). İnternet bağlantısı olmasa bile asla "internete bağlanın" hatası vermez.
- **Android Share & Dosya Kaydetme:** PDF, Excel (.xlsx), CSV ve Yedek dosyaları tek tıkla cihazın İndirilenler (Downloads) klasörüne kaydedilir ve Android Native Paylaşım ekranı açılarak WhatsApp, Dosyalar veya Drive üzerinden gönderilebilir.

### 🔍 Barkod & Kamera
- **Kamera & Barkod Desteği:** El terminali lazer okuyucusu ve telefon kamerası ile anında barkod ve QR kod okuma.
- **Zebra El Terminali Senkronizasyonu:** Toplu QR ve klavye tuşlama simülasyonu.
