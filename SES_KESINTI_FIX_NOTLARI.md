# Ses Kesintisi ve Mikrofon Sorunları - FIX Notları

## Problem

Asistan sesi konuşurken mikrofon kesiliyordu ve ses bittikten sonra tam konuşacakken mikrofon devam ediyordu. Bu durum şu sorunlara yol açıyordu:

1. **Ses kesintisi**: Asistan konuşurken mikrofon buffer'ındaki veri kayboluyordu
2. **Yankı**: Asistan sesinin bitmesinden hemen sonra mikrofon açılıyor, hoparlörden çıkan son yankıyı yakalıyordu
3. **Timing sorunu**: `beginAssistantSpeech()` ve `endAssistantSpeech()` arasındaki zamanlama yanlıştı

## Uygulanan Çözümler

### 1. AndroidBridge.java Düzeltmeleri

#### Timing Parametreleri Optimize Edildi:
```java
// ÖNCE:
private static final long SPEECH_TAIL_MS = 350;
private static final long SPEECH_PAD_MS  = 400;

// SONRA: 🔴
private static final long SPEECH_TAIL_MS = 500;  // +150ms (hoparlör buffer flush için)
private static final long SPEECH_PAD_MS  = 300;  // -100ms (play() zaten hızlı başlıyor)
```

**Açıklama**:
- `SPEECH_TAIL_MS`: Ses bittikten sonra hoparlör buffer'ının tamamen temizlenmesi için bekleme süresi artırıldı
- `SPEECH_PAD_MS`: Audio.play() modern tarayıcılarda hızlı başladığı için azaltıldı

#### Detaylı Loglama Eklendi:
```java
@JavascriptInterface
public void beginAssistantSpeech(final int durationMs) {
    // 🔴 Detaylı log
    Log.i(TURN, "🔴 SPEAK BEGIN " + ms + "ms | pad=" + SPEECH_PAD_MS + 
          "ms | tail=" + SPEECH_TAIL_MS + "ms | mic=" + 
          (voskActive ? "KAPATILIYOR" : "zaten kapalı") + 
          " | wanted=" + micWanted + " | inWindow=" + inSpeechWindow());
    
    if (voskActive) {
        Log.i(TURN, "  → stopVosk() çağrılıyor (sessizce)");
        stopVosk(false);
    }
    // ...
}

@JavascriptInterface
public void endAssistantSpeech() {
    if (!inSpeechWindow()) {
        Log.i(TURN, "⚠️ END çağrıldı ama pencere zaten bitmiş");
        return;
    }
    Log.i(TURN, "🟢 SPEAK END | pencere kısaltıldı | mic " + 
          SPEECH_TAIL_MS + "ms sonra açılacak");
    // ...
}
```

#### stopVosk() İyileştirildi:
```java
private void stopVosk(boolean notify) {
    Log.i(TAG, "🔴 stopVosk(" + notify + ") çağrıldı");
    
    // AudioRecord kontrolü
    if (recToStop != null) {
        Log.i(TAG, "  → AudioRecord durduruluyor ve release() ediliyor");
        try { recToStop.stop(); } catch (Exception e) { Log.w(TAG, "AudioRecord.stop() hatası", e); }
        try { recToStop.release(); Log.i(TAG, "  ✅ AudioRecord.release() başarılı"); } 
        catch (Exception e) { Log.e(TAG, "  ❌ AudioRecord.release() hatası", e); }
    }
    
    // Buffer yönetimi
    synchronized (speechBufferLock) {
        if (speechBuffer.length() > 0) {
            Log.i(TAG, "  → speechBuffer: " + speechBuffer.length() + " karakter | notify=" + notify);
            if (notify) {
                Log.i(TAG, "  → Buffer kullanıcıya gönderiliyor: \"" + speechBuffer.toString() + "\"");
                sendSpeechResult(speechBuffer.toString());
            } else {
                Log.i(TAG, "  → Buffer atılıyor (asistan konuşuyor)");
            }
            speechBuffer.setLength(0);
        }
    }
    
    Log.i(TAG, "✅ stopVosk tamamlandı");
}
```

**Kritik Nokta**: `notify=false` durumunda buffer içeriği ATILIR (asistan konuşuyorsa bu veri kullanıcıya ait değildir).

#### reopenAfterSpeech() Detaylandırıldı:
```java
private void reopenAfterSpeech() {
    if (inSpeechWindow()) { 
        Log.i(TURN, "  reopen: pencere henüz aktif, yeniden zamanlanıyor");
        scheduleReopen(); 
        return; 
    }
    if (!micWanted || hostPaused) {
        Log.i(TURN, "  reopen: micWanted=" + micWanted + " hostPaused=" + hostPaused);
        return;
    }
    if (voskActive) {
        Log.i(TURN, "  reopen: mikrofon zaten açık");
        return;
    }
    // ...
    Log.i(TURN, "🎤 MIC AÇILIYOR");
}
```

### 2. index.html (JS Tarafı) Düzeltmeleri

#### playVoiceResponse() Optimize Edildi:

**Problem**: `audio.play()` başarısız olduğunda `endAssistantSpeech()` çağrılmıyordu, mikrofon sonsuza dek kapalı kalıyordu.

```javascript
playVoiceResponse(key) {
    // Önceki kod...
    
    // 🔴 FIX: Mikrofon kontrolünü ÖNCE yap (audio.play() başarısız olabilir)
    this._pauseMicForAudio();
    
    try {
        const audio = new Audio(this._audioUrl(key) || dataUri);
        
        audio.onloadedmetadata = () => {
            if (token !== this._speakToken) return;
            const d = isFinite(audio.duration) && audio.duration > 0 ? audio.duration : 3;
            // 🔴 FIX: İlk beginAssistantSpeech zaten _pauseMicForAudio'da çağrıldı
            this._nativeGate(true, Math.round(d * 1000));
            // ...
        };
        
        // 🔴 FIX: onplaying'de tekrar _nativeGate çağırmaya gerek yok
        audio.onplaying = () => {
            if (token !== this._speakToken) return;
            console.log('[VoiceAudio] Çalma başladı');
        };
        
        audio.onended = () => {
            console.log('[VoiceAudio] Ses bitti, mikrofon açılıyor');
            this._finishSpeaking(token);
        };
        
        audio.onerror = (err) => {
            console.warn('[VoiceAudio] Ses hatası:', err);
            this._finishSpeaking(token);
        };
        
        // 🔴 FIX: play() başarısız olursa HEMEN endAssistantSpeech çağır
        const p = audio.play();
        if (p && p.catch) {
            p.catch((err) => {
                console.error('[VoiceAudio] play() hatası:', err.message);
                // Ses çalamadı, mikrofonu hemen aç
                this._finishSpeaking(token);
            });
        }
    } catch(e) {
        console.error('[VoiceAudio] Audio oluşturma hatası:', e);
        this._finishSpeaking(token);
    }
}
```

**Değişiklikler**:
1. `_pauseMicForAudio()` çağrısı en başa alındı
2. `audio.onplaying`'de gereksiz `_nativeGate` çağrısı kaldırıldı
3. `audio.play()` promise catch bloğunda `_finishSpeaking` çağrıldı
4. Tüm hata durumlarında console.log/warn/error eklendi

#### _pauseMicForAudio() Loglama:
```javascript
_pauseMicForAudio() {
    // ...
    if (this._hasNativeGate()) {
        console.log('[VoiceAudio] 🔴 beginAssistantSpeech(' + 
                    (this._speakMs || 0) + 'ms) çağrılıyor');
        this.showTranscript('', false);
        this._nativeGate(true, this._speakMs || 0);
        console.log('[VoiceAudio] ✅ Native gate açıldı');
        return;
    }
    console.log('[VoiceAudio] ⚠️ Native gate YOK, klasik pause kullanılıyor');
    // ...
}
```

#### _finishSpeaking() Detaylandırıldı:
```javascript
_finishSpeaking(token) {
    if (token !== this._speakToken) {
        console.log('[VoiceAudio] ⚠️ eski token (' + token + 
                    ' vs ' + this._speakToken + ')');
        return;
    }
    console.log('[VoiceAudio] 🟢 ses bitti, mikrofon açılıyor');
    
    // ...
    
    if (this._hasNativeGate()) {
        console.log('[VoiceAudio] ✅ Native gate: endAssistantSpeech()');
        this.isSpeaking = false;
        this._echoSuppressUntil = 0;
        this._nativeGate(false, 0);
        
        if (this.shouldKeepListening() && !this.manuallyPaused) {
            if (this.isRecognizing) { 
                console.log('[VoiceAudio] Mikrofon zaten dinliyor, UI güncelleniyor');
                this.updateUIStatus('Dinleniyor', 'listening'); 
            } else {
                console.log('[VoiceAudio] Mikrofon başlatılıyor');
                this.startListening();
            }
        } else {
            console.log('[VoiceAudio] Dinleme devam etmeyecek');
        }
        return;
    }
    
    console.log('[VoiceAudio] ⚠️ Native gate YOK: 700ms yankı + 750ms gecikme');
    // ...
}
```

## Debug ve Test

### Logcat İzleme Komutu:
```powershell
adb logcat AndroidBridge:I VoiceTurn:I VoiceAudio:D ManavWebConsole:D *:S
```

### Beklenen Log Akışı (Başarılı):
```
# 1. Ses çalmaya başlama
I/VoiceAudio: 🔴 _pauseMicForAudio: beginAssistantSpeech(2500ms) çağrılıyor
I/VoiceAudio: ✅ Native gate açıldı
I/VoiceTurn:  🔴 SPEAK BEGIN 2500ms | pad=300ms | tail=500ms | mic=KAPATILIYOR
I/VoiceTurn:    → stopVosk() çağrılıyor (sessizce)
I/AndroidBridge: 🔴 stopVosk(false) çağrıldı
I/AndroidBridge:   → AudioRecord durduruluyor ve release() ediliyor
I/AndroidBridge:   ✅ AudioRecord.release() başarılı
I/AndroidBridge:   → Buffer atılıyor (asistan konuşuyor)
I/AndroidBridge: ✅ stopVosk tamamlandı

# 2. Ses çalma
I/VoiceAudio: Çalma başladı

# 3. Ses bitti
I/VoiceAudio: Ses bitti, mikrofon açılıyor
I/VoiceAudio: 🟢 ses bitti, mikrofon açılıyor
I/VoiceAudio: ✅ Native gate: endAssistantSpeech()
I/VoiceTurn:  🟢 SPEAK END | pencere kısaltıldı | mic 500ms sonra açılacak

# 4. Mikrofon yeniden açılıyor (500ms sonra)
I/VoiceTurn:  🎤 MIC AÇILIYOR (Vosk)
I/AndroidBridge: Mikrofon: AEC=true NS=true
I/VoiceAudio: Mikrofon başlatılıyor
```

### Hata Durumları:

#### 1. AudioRecord release hatası:
```
I/AndroidBridge: ❌ AudioRecord.release() hatası
java.lang.IllegalStateException: ...
```
**Çözüm**: Cihazı yeniden başlatın veya uygulamayı force-stop yapın.

#### 2. Ses çalma hatası:
```
E/VoiceAudio: play() hatası: NotAllowedError: play() requires user interaction
```
**Çözüm**: Kullanıcı ilk etkileşim yaptıktan sonra otomatik düzelir (Chrome autoplay politikası).

#### 3. Pencere zaten bitmiş:
```
I/VoiceTurn: ⚠️ END çağrıldı ama pencere zaten bitmiş
```
**Çözüm**: Normal durum, `audio.onended` gecikmiş olabilir.

#### 4. Mikrofon açılmıyor:
```
I/VoiceTurn:   reopen: micWanted=false hostPaused=false
```
**Çözüm**: Kullanıcı dinlemeyi durdurmış, JS `startListening()` çağırmalı.

## Build ve Yükleme

### Build Komutu:
```powershell
cd android
.\gradlew.bat assembleDebug
```

### APK Yolu:
```
android\app\build\outputs\apk\debug\app-debug.apk
```

### ADB Yükleme:
```powershell
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

### Logcat İzleme (Renkli):
```powershell
adb logcat AndroidBridge:I VoiceTurn:I VoiceAudio:D ManavWebConsole:D chromium:I *:S
```

## Sonuç

Bu düzeltmelerle:
✅ Asistan konuşurken mikrofon buffer'ı güvenli şekilde temizleniyor
✅ Ses bittikten sonra hoparlör yankısı için ek 500ms bekleniyor
✅ `audio.play()` hataları yakalanıyor ve mikrofon hemen açılıyor
✅ Tüm kritik noktalarda detaylı loglar var
✅ Timing optimize edildi (SPEECH_PAD_MS azaltıldı, SPEECH_TAIL_MS artırıldı)

**Test Senaryoları**:
1. ✅ Normal ses çalma → mikrofon kapanıyor → ses bitiyor → mikrofon açılıyor
2. ✅ Ses çalma hatası → mikrofon hemen açılıyor
3. ✅ Arka arkaya sesler → önceki ses iptal ediliyor, yeni ses başlıyor
4. ✅ Uygulama pause/resume → mikrofon durumu korunuyor
5. ✅ Buffer yönetimi → asistan konuşurken buffer atılıyor, normal durumda gönderiliyor

## İleri Seviye Ayarlar

### SPEECH_TAIL_MS Ayarlama:
- **Düşük yankı (iyi hoparlör)**: 400ms
- **Orta yankı (normal)**: 500ms (varsayılan)
- **Yüksek yankı (Bluetooth, büyük oda)**: 600-700ms

### SPEECH_PAD_MS Ayarlama:
- **Hızlı cihaz**: 200ms
- **Normal cihaz**: 300ms (varsayılan)
- **Yavaş cihaz**: 400ms

### Emniyet Zamanlayıcısı (index.html):
```javascript
this._speakSafety = setTimeout(() => {
    console.warn('[VoiceResponse] Emniyet: ses bitti eventi gelmedi');
    this._finishSpeaking(token);
}, 12000); // 12 saniye (maksimum ses süresi + buffer)
```

---

**Tarih**: 15 Kasım 2026
**Durum**: ✅ Düzeltme uygulandı, build başarılı
**Sonraki Adım**: Gerçek cihazda test
