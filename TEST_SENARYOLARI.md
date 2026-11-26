# Ses Kesintisi Fix - Test Senaryoları

## Hızlı Test (5 dakika)

### 1. APK Yükleme
```powershell
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

### 2. Logcat Başlat (Ayrı Terminal)
```powershell
adb logcat AndroidBridge:I VoiceTurn:I *:S | Select-String -Pattern "SPEAK|MIC|stopVosk|Buffer"
```

### 3. Temel Ses Testi

#### Test 1: Normal Ses Çalma
**Adımlar**:
1. Uygulamayı aç
2. Mikrofonu başlat (sağ üst köşe mikrofon butonu)
3. Herhangi bir komut söyle (örn: "domates")
4. Asistan yanıtını bekle

**Beklenen Log**:
```
🔴 SPEAK BEGIN ... | mic=KAPATILIYOR
✅ AudioRecord.release() başarılı
🟢 SPEAK END | mic 500ms sonra açılacak
🎤 MIC AÇILIYOR
```

**Beklenen Davranış**:
- ✅ Asistan konuşurken mikrofon simgesi sönük
- ✅ Ses bitince mikrofon simgesi tekrar parlak
- ✅ Asistan sesinin kesilme/takılma YOK
- ✅ Mikrofon tekrar sorunsuz dinliyor

#### Test 2: Arka Arkaya Sesler
**Adımlar**:
1. "domates" de
2. Hemen ardından "biber" de
3. Hemen ardından "patlıcan" de

**Beklenen Davranış**:
- ✅ Her komut için ayrı yanıt
- ✅ Sesler birbirini kesmiyor
- ✅ Mikrofon her ses sonrası açılıyor

#### Test 3: Konuşma Sırasında Komut
**Adımlar**:
1. "domates" de
2. Asistan konuşurken hemen "biber" de
3. Asistan sesinin kesilip kesilmediğini gözle

**Beklenen Davranış**:
- ✅ İkinci komut ("biber") DUYULMASIN (mikrofon kapalı)
- ✅ İlk asistan sesi tamamlanana kadar devam etsin
- ✅ Ses bitince mikrofon otomatik açılsın

**Beklenen Log**:
```
🔴 stopVosk(false) çağrıldı
  → Buffer atılıyor (asistan konuşuyor)
```

## Detaylı Test (15 dakika)

### Test 4: Ses Hatası Simülasyonu

**Hazırlık**: Chrome DevTools Console açık tutun:
```javascript
// Console'da çalıştır (ses çalma hatasını simüle et)
Audio.prototype._originalPlay = Audio.prototype.play;
Audio.prototype.play = function() {
    if (Math.random() < 0.3) { // %30 hata
        return Promise.reject(new Error('Simulated error'));
    }
    return this._originalPlay();
};
```

**Beklenen Davranış**:
- ✅ Ses çalmazsa bile mikrofon AÇILMALI
- ✅ "play() hatası" logu görülmeli
- ✅ Emniyet zamanlayıcısı devreye girmeli

### Test 5: Uygulama Pause/Resume

**Adımlar**:
1. Mikrofonu başlat
2. Home tuşuna bas (uygulamayı arka plana at)
3. 5 saniye bekle
4. Uygulamaya geri dön

**Beklenen Log**:
```
I/AndroidBridge: 🔴 stopVosk(true) çağrıldı
D/ManavWebConsole: [VC] suspended
D/ManavWebConsole: [VC] resumed
I/VoiceTurn: 🎤 MIC AÇILIYOR
```

**Beklenen Davranış**:
- ✅ Arka planda mikrofon kapanır
- ✅ Geri dönünce otomatik açılır

### Test 6: Yankı Testi (Hoparlör Maksimum)

**Adımlar**:
1. Hoparlör sesini maksimuma çıkar
2. "domates" de
3. Asistan yanıtı sırasında mikrofon yakınına gel
4. Asistan sesinin mikrofondan yakalanıp yakalanmadığını kontrol et

**Beklenen Log**:
```
🔴 SPEAK BEGIN ... | mic=KAPATILIYOR
✅ AudioRecord.release() başarılı
  → Buffer atılıyor (asistan konuşuyor)
```

**Beklenen Davranış**:
- ✅ Asistan konuşurken hiçbir yankı YAKALANMAMALI
- ✅ Ses bittikten 500ms sonra mikrofon açılmalı
- ✅ Yankının yeniden komut olarak algılanmaması

### Test 7: Buffer Yönetimi

**Adımlar**:
1. Mikrofonu başlat
2. "domates bir kilo" diye başla ama tamamlama
3. Cümle ortasında asistan yanıtı gelirse ne olacağını gözle

**Beklenen Log**:
```
🔴 stopVosk(false) çağrıldı
  → speechBuffer: 15 karakter var | notify=false
  → Buffer atılıyor (asistan konuşuyor)
```

**Beklenen Davranış**:
- ✅ Yarım komut ATILMALI (asistan konuşurken buffer temizlenir)
- ✅ Ses bitince mikrofon açılmalı
- ✅ Yeni komut temiz slate ile başlamalı

## Performans Testi (10 dakika)

### Test 8: Uzun Süreli Kullanım

**Adımlar**:
1. 20 komut üst üste söyle
2. Her komut sonrası asistan yanıtını bekle
3. Memory leak veya yavaşlama olup olmadığını kontrol et

**Kontrol Noktaları**:
- ✅ 20 komut sonrası mikrofon hızı düşmemeli
- ✅ Ses çalma gecikmeleri artmamalı
- ✅ UI donmaları olmamalı

**Logcat Kontrol**:
```powershell
adb logcat | Select-String -Pattern "OutOfMemoryError|GC_|ANR"
```

### Test 9: Bluetooth Hoparlör/Kulaklık

**Adımlar**:
1. Bluetooth kulaklık/hoparlör bağla
2. Test 1-3'ü tekrarla

**Beklenen Davranış**:
- ✅ Bluetooth cihazlarla da aynı performans
- ✅ Ses gecikmesi varsa SPEECH_TAIL_MS ayarla (600-700ms)

## Hata Senaryoları

### Hata 1: Mikrofon Açılmıyor

**Belirtiler**:
- Ses bittikten sonra mikrofon simgesi yanmıyor
- Yeni komut dinlenmiyor

**Debug**:
```powershell
adb logcat VoiceTurn:I *:S
```

**Kontrol**:
```
I/VoiceTurn:   reopen: micWanted=? hostPaused=?
```

**Çözüm**:
- `micWanted=false` → JS `startListening()` çağırmalı
- `hostPaused=true` → Uygulamayı foreground'a al
- `voskState!=V_READY` → Vosk modelini yeniden yükle

### Hata 2: Ses Kesilme/Takılma

**Belirtiler**:
- Asistan sesi bölük bölük
- Ses kasıyor

**Debug**:
```powershell
adb logcat AudioTrack:W MediaPlayer:W *:S
```

**Kontrol**:
- `AudioTrack: underrun` → Buffer boyutu artır
- `MediaPlayer: error` → Ses dosyası bozuk

**Çözüm**:
1. İnternet bağlantısını kontrol et (web versiyonuysa)
2. Cihazı yeniden başlat
3. Uygulamayı force-stop yap

### Hata 3: Yankı Algılanıyor

**Belirtiler**:
- Asistan cümleleri yeniden komut olarak algılanıyor
- Sonsuz döngü

**Debug**:
```powershell
adb logcat VoiceTurn:I AndroidBridge:I *:S | Select-String -Pattern "Buffer"
```

**Kontrol**:
```
I/AndroidBridge:   → Buffer kullanıcıya gönderiliyor: "urun bulundu"
```

**Çözüm**:
1. `SPEECH_TAIL_MS` artır (600-700ms)
2. `_isRecentAssistantEcho()` fonksiyonunu kontrol et (index.html)
3. Hoparlör sesini azalt

### Hata 4: AudioRecord Release Hatası

**Belirtiler**:
```
I/AndroidBridge: ❌ AudioRecord.release() hatası
java.lang.IllegalStateException: Unable to retrieve AudioRecord pointer
```

**Çözüm**:
1. Uygulamayı force-stop yap
2. Cihazı yeniden başlat
3. Başka uygulama mikrofonu kullanıyorsa kapat

## Başarı Kriterleri

### Minimum Kabul (MVP):
- ✅ Ses çalma sırasında mikrofon kesintisiz kapanıyor
- ✅ Ses bitince mikrofon 500ms içinde açılıyor
- ✅ Buffer içeriği güvenli şekilde yönetiliyor
- ✅ AudioRecord düzgün release() ediliyor

### İdeal Performans:
- ✅ 20 komut üst üste sorunsuz çalışıyor
- ✅ Bluetooth cihazlarla uyumlu
- ✅ Yankı %100 elimine
- ✅ Memory leak yok
- ✅ UI donma yok

### Kritik Hatalar (RED FLAG):
- ❌ Ses bitince mikrofon hiç açılmıyor
- ❌ AudioRecord release() sırasında crash
- ❌ Yankı sonsuz döngüye yol açıyor
- ❌ 10 komut sonrası yavaşlama

## Logcat Filtreleme İpuçları

### Sadece Kritik Loglar:
```powershell
adb logcat | Select-String -Pattern "🔴|🟢|🎤|❌|✅"
```

### Hata Takibi:
```powershell
adb logcat | Select-String -Pattern "ERROR|Exception|hatası|failed"
```

### Timing Analizi:
```powershell
adb logcat VoiceTurn:I | Select-String -Pattern "SPEAK|MIC"
```

### Buffer Takibi:
```powershell
adb logcat AndroidBridge:I | Select-String -Pattern "Buffer|speechBuffer"
```

## Rapor Formatı

Test sonuçlarını şu formatta raporlayın:

```markdown
## Test Raporu - [Tarih]

### Cihaz Bilgileri:
- Model: [örn: Samsung Galaxy A52]
- Android Sürümü: [örn: Android 12]
- Bluetooth Hoparlör: [Evet/Hayır]

### Test Sonuçları:
- Test 1 (Normal Ses): ✅/❌
- Test 2 (Arka Arkaya): ✅/❌
- Test 3 (Konuşma Sırasında): ✅/❌
- Test 4 (Ses Hatası): ✅/❌
- Test 5 (Pause/Resume): ✅/❌
- Test 6 (Yankı): ✅/❌
- Test 7 (Buffer): ✅/❌
- Test 8 (Uzun Süreli): ✅/❌
- Test 9 (Bluetooth): ✅/❌

### Karşılaşılan Sorunlar:
[Varsa detaylarıyla]

### Loglar:
[Kritik logların snippet'i]

### Önerilen İyileştirmeler:
[Varsa]
```

---

**Not**: Tüm testleri GERÇEK CİHAZDA yapın. Emülatörde ses sistemi farklı çalışır ve yanıltıcı sonuçlar verebilir.
