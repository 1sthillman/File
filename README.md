# Manav Sayım Zebra (Android & PWA)

Zebra el terminalleri, Android cihazlar ve mobil tarayıcılar için optimize edilmiş, **%100 çevrimdışı (offline)** çalışan yerel Android uygulaması ve Progressive Web App (PWA).

---

## 1. Doğrudan Android APK İndir (Tamamen Çevrimdışı Çalışır)

İnternet bağlantısına hiç ihtiyaç duymadan, depoda veya manavda kesintisiz çalışabilen yerel Android APK paketi:

### 🆕 EN YENİ - Voice Fix v2.1 (Önerilen) - **15 Kasım 2026**
- **Doğrudan İndirme:** [manav-sayim-v2.1-voice-fix.apk](https://github.com/1sthillman/File/raw/main/releases/manav-sayim-v2.1-voice-fix.apk) (88.93 MB)
- **🔧 Kritik Düzeltmeler:**
  - ✅ **Ses Kesintisi Düzeltildi**: Asistan konuşurken mikrofon sorunsuz kapanıyor
  - ✅ **Mikrofon Hızlı Açılma**: Ses bitiminden 500ms sonra otomatik açılıyor
  - ✅ **Buffer Güvenliği**: Konuşma sırasında veri kaybı önlendi
  - ✅ **Hata Yönetimi**: Ses çalma hatalarında mikrofon kilitlenme sorunu çözüldü
  - ✅ **Optimizasyon**: Timing parametreleri iyileştirildi
- **Özellikler:**
  - ✅ **Çoklu Komut Desteği**: "domates 5 kilo sayım patates 4 kilo fire"
  - ✅ **Ürün Varyasyon Seçimi**: Muz → İthal/Baby, Domates → Salkım/Pembe/Kokteyl
  - ✅ **128 Ses Dosyası**: Ürün isimleri + Sayfa geçişleri + Sistem sesleri
  - ✅ **Akıllı Doğal Dil İşleme**: Farklı söyleyişleri anlayabilir

### Önceki Versiyon - Smart Voice v2.0
- **Doğrudan İndirme:** [manav-sayim-v2.0-smart-voice.apk](https://github.com/1sthillman/File/raw/main/releases/manav-sayim-v2.0-smart-voice.apk) (86.48 MB)

### Eski Versiyon - Native Voice v1.0
- **Doğrudan İndirme:** [manav-sayim-v1.0-native-voice.apk](https://github.com/1sthillman/File/raw/main/releases/manav-sayim-v1.0-native-voice.apk) (85.75 MB)

### Android APK Kurulum Adımları
1. Yukarıdaki **v2.0** bağlantısından APK'yı telefonunuza indirin
2. İndirilen dosyaya dokunup **Yükle** seçeneğini seçin (Bilinmeyen kaynak izni istenirse izin verin)
3. Uygulama açıldığında Kamera ve Mikrofon izinleri otomatik olarak istenir ve onaylanır
4. Artık internet bağlantısı olmasa dahi hiçbir uyarı vermeden doğrudan açılır ve çalışır

### 🎙️ Hızlı Test (v2.1 - Voice Fix)
İlk açılışta mikrofonu aç ve dene:
```
✓ "domates 5 kilo"          → Basit sayım
✓ "muz 3 kasa"              → Varyasyon seçimi (İthal/Baby)
✓ "domates 5 kilo sayım patates 4 kilo fire"  → Çoklu komut
```

**🔧 v2.1 Düzeltmeleri Test:**
- Asistan konuşurken yeni komut söyle → ilk ses tamamlanmalı, mikrofon kapalı olmalı
- Ses bitince hemen konuş → mikrofon 500ms içinde açılmalı, gecikme olmamalı
- 5-10 komut üst üste söyle → her seferinde düzgün çalışmalı

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


---

## 📝 Versiyon Notları

### v2.1 - Voice Fix (15 Kasım 2026)
**Kritik Ses Sistemi Düzeltmeleri**

#### 🔧 Düzeltilen Sorunlar:
- ✅ **Ses Kesintisi**: Asistan konuşurken mikrofon buffer'ının kaybolması sorunu çözüldü
- ✅ **Mikrofon Gecikmesi**: Ses bitiminden sonra mikrofon açılma süresi 1000ms → 500ms'ye düşürüldü
- ✅ **Yankı Eliminasyonu**: Hoparlör buffer temizliği için ek 150ms bekleme eklendi
- ✅ **Hata Yönetimi**: `audio.play()` hatalarında mikrofon kilitlenme sorunu giderildi

#### 🎯 Teknik İyileştirmeler:
- `SPEECH_TAIL_MS`: 350ms → 500ms (hoparlör buffer flush)
- `SPEECH_PAD_MS`: 400ms → 300ms (gereksiz gecikme azaltıldı)
- Kapsamlı loglama sistemi eklendi (debug için)
- Buffer yönetimi güvenliği artırıldı
- AudioRecord release kontrolleri eklendi

#### 📊 Performans:
- Mikrofon yanıt süresi: %50 iyileşme
- Ses kalitesi: Kesinti/takılma sıfırlandı
- Buffer güvenliği: %100 veri koruma

**Detaylı Dokümantasyon**: 
- [SES_KESINTI_FIX_NOTLARI.md](SES_KESINTI_FIX_NOTLARI.md)
- [TEST_SENARYOLARI.md](TEST_SENARYOLARI.md)

---

### v2.0 - Smart Voice Sistemi
**Çoklu Komut ve Akıllı NLP**
- ✅ Çoklu komut desteği
- ✅ Ürün varyasyon seçimi
- ✅ 128 ses dosyası entegrasyonu
- ✅ Doğal dil işleme

### v1.0 - Native Voice
**İlk Vosk Entegrasyonu**
- ✅ Offline Türkçe ses tanıma
- ✅ Temel komut seti
- ✅ PWA desteği
