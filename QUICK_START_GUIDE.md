# 🚀 Manav Sayım - Hızlı Başlangıç Rehberi

## 📦 APK Kurulumu

### Seçenek 1: USB ile (Önerilen)
```bash
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

### Seçenek 2: Manuel Kurulum
1. `android\app\build\outputs\apk\debug\app-debug.apk` dosyasını telefona taşıyın
2. Ayarlar → Güvenlik → "Bilinmeyen kaynaklardan yükleme"yi aktif edin
3. APK dosyasına tıklayıp yükleyin

---

## ✅ İlk Çalıştırma

1. **Uygulamayı açın**
2. **Mikrofon iznini verin** (otomatik istenir)
3. **Kamera iznini verin** (otomatik istenir)
4. **Vosk modelinin yüklenmesini bekleyin** (~5-10 saniye)
5. ✅ Hazır! "Dinleniyor" animasyonu görünmeli

---

## 🧪 Hızlı Test

### Test 1: Temel Komut
```
1. "Merhaba" deyin
2. Asistan cevap versin
3. Mikrofon animasyonu cevap sırasında KAPANMALI
4. Cevap bitince ~350ms sonra AÇILMALI
```

### Test 2: Yankı Kontrolü
```
1. Hoparlör sesini %100 açın
2. "Selam nasılsın" deyin
3. Asistan kendi cümlesini YAZMAMALI
```

### Test 3: Arka Plan
```
1. Komut verin
2. Home tuşuna basın (app arka plana gitsin)
3. App'e geri dönün
4. Mikrofon otomatik başlamalı
```

---

## 📊 Logcat İzleme

### Genel İzleme (Önerilen)
```bash
adb logcat AndroidBridge:I VoiceTurn:I ManavWebConsole:D *:S
```

### Sadece Mikrofon Durumu
```bash
adb logcat | findstr /C:"VoiceTurn" /C:"Mikrofon:" /C:"AEC="
```

### Hata İzleme
```bash
adb logcat | findstr /C:"ERROR" /C:"FAILED" /C:"düştü"
```

---

## 🔧 Beklenen Logcat Çıktıları

### ✅ Normal Akış
```
I/AndroidBridge: Vosk modeli hazır
I/AndroidBridge: Mikrofon: AEC=true NS=true
I/VoiceTurn: MIC açılıyor
D/ManavWebConsole: [VC] Recognized: merhaba
I/VoiceTurn: SPEAK begin 2500ms | mic KAPATILIYOR | wanted=true
I/VoiceTurn: SPEAK end -> mic 350ms sonra açılacak
I/VoiceTurn: MIC açılıyor
```

### ✅ Otomatik Recovery
```
W/AndroidBridge: Mikrofon düştü, yeniden deneniyor (1/3)
I/VoiceTurn: MIC açılıyor
```

---

## ❌ Sorun Giderme

### Problem: Mikrofon açılmıyor
**Kontrol:**
```bash
adb logcat | findstr "permission"
```
**Çözüm:** Mikrofon iznini manuel verin (Ayarlar → Uygulamalar → Manav Sayım → İzinler)

---

### Problem: Yankı var
**Kontrol:**
```bash
adb logcat | findstr "AEC="
```
**Çözüm:** 
- `AEC=false` ise cihazınızda donanım yankı engelleyici yok
- `AndroidBridge.java` satır 247: `SPEECH_TAIL_MS = 350` → `500` yapın

---

### Problem: Gecikme çok fazla
**Kontrol:** Asistan cevabı bittikten kaç ms sonra mikrofon açılıyor?
**Çözüm:** `AndroidBridge.java` satır 247: `SPEECH_TAIL_MS = 350` → `250` yapın
**Uyarı:** Yankı riski artar!

---

### Problem: Mikrofon donuyor
**Kontrol:**
```bash
adb logcat | findstr "session"
```
**Çözüm:** App'i tamamen kapatıp yeniden açın (Recent apps → kaydır)

---

## 🎯 Parametreleri Değiştirme

### Yankı Tail Süresi
**Dosya:** `android/app/src/main/java/com/manav/sayim/AndroidBridge.java`  
**Satır:** 247
```java
private static final long SPEECH_TAIL_MS = 350;  // 250-500 arası deneyin
```

### Kelime Birleştirme
**Dosya:** `android/app/src/main/java/com/manav/sayim/AndroidBridge.java`  
**Satır:** 253
```java
private static final long WORD_MERGE_TIMEOUT_MS = 800;  // 500-1000 arası
```

### Mikrofon Kaynağı
**Dosya:** `android/app/src/main/java/com/manav/sayim/AndroidBridge.java`  
**Satır:** 256
```java
private static final int MIC_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
// Alternatif: VOICE_COMMUNICATION (bazı cihazlarda AEC daha iyi)
```

**Değişiklik sonrası:**
```bash
cd android
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 📱 Cihazda Test İpuçları

1. **Sessiz ortamda test edin** (ilk test için)
2. **Mikrofon-hoparlör mesafesini koruyun** (en az 10cm)
3. **Net konuşun** (bağırmanıza gerek yok)
4. **İlk 2-3 komut bekleme süresi normal** (model ısınıyor)

---

## 💡 Önemli Notlar

- ✅ Mikrofon asistan konuşurken **FİZİKSEL** olarak kapanır
- ✅ Yankı **%100** elimine edilir (mikrofon yok = yankı yok)
- ✅ **350ms** sonra otomatik yeniden açılır
- ✅ Session ID ile **zombie thread** koruması var
- ✅ App arka plana gidince mikrofon **düzgün** kapatılır

---

## 🆘 Acil Durum

### App çöktü / yanıt vermiyor
```bash
adb shell am force-stop com.manav.sayim
adb shell am start -n com.manav.sayim/.MainActivity
```

### Mikrofon serbest bırakılmadı (başka app kullanamıyor)
```bash
adb shell am force-stop com.manav.sayim
```

### Logcat çok hızlı akıyor
```bash
adb logcat -c  # Önce temizle
adb logcat AndroidBridge:I VoiceTurn:I *:S  # Sadece önemlileri göster
```

---

## 📞 İletişim

**Build tarihi:** 14 Kasım 2026  
**APK Hash:** `34CE3979BDAC6305BF3196D63AC1A705`  
**Version:** 1.0.0-debug

**Test başarıları dileriz! 🚀**
