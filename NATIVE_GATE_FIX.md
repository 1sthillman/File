# v2.2 Native Gate Fix - JS/Native Çekişme Sorunu Çözüldü

## Problem (v2.1'de Devam Ediyordu)

Asistan konuşurken ve ses bittikten sonra **mikrofon ile ses sistemi çekişiyordu**:
- ❌ Asistan konuşurken mikrofon kapanıyor
- ❌ Ses yarıda kesiliyor
- ❌ Mikrofon öne geçiyor
- ❌ İki sistem birbirini sürekli engellemeye çalışıyor

## Kök Neden

**JavaScript tarafı native gate olmasına rağmen hala mikrofon kontrolü yapmaya çalışıyordu!**

```javascript
// ❌ YANLIŞ (v2.1):
if (this._hasNativeGate()) {
    this._nativeGate(false, 0);
    // SORUN: JS hala startListening() çağırıyor!
    this.startListening(); // ← Bu çağrı çekişmeye yol açıyor
}
```

**Sonuç**: Native mikrofonu açmaya çalışırken JS de açmaya çalışıyordu → çekişme!

---

## Çözüm (v2.2)

### 1. JS Tarafı - Tam Kontrol Native'e Devredildi

**Önceki Kod (v2.1 - Yanlış)**:
```javascript
_finishSpeaking(token) {
    if (this._hasNativeGate()) {
        this._nativeGate(false, 0);
        // ❌ JS mikrofon kontrolü yapıyor
        if (this.shouldKeepListening()) {
            if (!this.isRecognizing) {
                this.startListening(); // ← SORUN!
            }
        }
    }
}
```

**Yeni Kod (v2.2 - Doğru)**:
```javascript
_finishSpeaking(token) {
    if (this._hasNativeGate()) {
        // 🔴 KRİTİK: Native gate varken mikrofon TAMAMEN native tarafından yönetilir
        // JS ASLA startListening() çağırmamalı, native kendisi açacak
        this.isSpeaking = false;
        this._echoSuppressUntil = 0;
        this._nativeGate(false, 0);
        
        // ✅ Sadece UI güncelle, startListening() ÇAĞIRMA
        if (this.shouldKeepListening() && !this.manuallyPaused) {
            console.log('[VoiceAudio] Native mikrofonu kendi açacak, sadece UI güncelleniyor');
            this.updateUIStatus('Hazır (native açacak)', 'idle');
            // isRecognizing = false kalıyor, native açana kadar
        }
        return;
    }
    // Native gate yoksa klasik yöntem...
}
```

**Ana İlke**: 
- ✅ Native gate varsa → **JS ASLA mikrofon kontrolü yapmamalı**
- ✅ Mikrofon kontrolü %100 native tarafında
- ✅ JS sadece UI günceller

---

### 2. Native Tarafı - JS'e Bildirim Eklendi

**AndroidBridge.java - reopenAfterSpeech()**:
```java
private void reopenAfterSpeech() {
    // ... kontroller ...
    
    if (voskState == V_READY) { 
        Log.i(TURN, "🎤 MIC AÇILIYOR (Vosk)"); 
        startVosk();
        // 🔴 KRİTİK: JS'e mikrofon açıldığını bildir
        sendSpeechEvent("mic_reopened", "");
    }
}
```

**index.html - Event Handler**:
```javascript
window.onNativeSpeechEvent = function(ev, msg) {
    const eng = window.__voiceEngine;
    if (!eng) return;
    
    if (ev === 'mic_reopened') {
        // 🔴 KRİTİK: Native mikrofonu açtı, JS UI'ı güncelle
        console.log('[NativeSpeech] 🎤 Native mikrofon açıldı, UI güncelleniyor');
        eng.isRecognizing = true;
        eng.updateUIStatus('Dinleniyor', 'listening');
        eng.setListeningVisuals(true);
        // ✅ startListening() ÇAĞIRILMIYOR!
    }
    // ... diğer eventler ...
}
```

---

### 3. Heartbeat Devre Dışı (Native Gate Varken)

**Önceki Kod (v2.1)**:
```javascript
// Heartbeat her 4 saniyede native kontrolü yapıyordu
setInterval(() => {
    if (engine.isRecognizing && !B.isNativeListening()) {
        // ❌ JS yeniden başlatmaya çalışıyor → çekişme!
        engine.startListening();
    }
}, 4000);
```

**Yeni Kod (v2.2)**:
```javascript
setInterval(() => {
    // 🔴 FIX: Native gate varken heartbeat gerekmiyor
    if (engine._hasNativeGate && engine._hasNativeGate()) {
        return; // ✅ Atla, native kendi yönetiyor
    }
    // Native gate yoksa eski kontrol...
}, 4000);
```

---

## Akış Diyagramı

### Eski Akış (v2.1 - Çekişme):
```
Asistan Konuşuyor
    ↓
JS: beginAssistantSpeech() → Native: Mikrofon kapanıyor
    ↓
Ses Bitiyor
    ↓
JS: endAssistantSpeech() → Native: Mikrofon açılacak (500ms)
    ↓
JS: startListening() ← ❌ ÇEKİŞME BAŞLIYOR!
    ↓
Native: startVosk() ← ❌ Aynı anda iki taraf açmaya çalışıyor
    ↓
Ses Kesiliyor / Mikrofon Kapanıp Açılıyor
```

### Yeni Akış (v2.2 - Senkronize):
```
Asistan Konuşuyor
    ↓
JS: beginAssistantSpeech() → Native: Mikrofon kapanıyor
    ↓
Ses Bitiyor
    ↓
JS: endAssistantSpeech() → Native: Mikrofon açılacak (500ms)
    ↓
JS: Sadece UI güncelle (startListening() YOK!) ✅
    ↓
Native: startVosk() + sendEvent("mic_reopened") ✅
    ↓
JS: Event alıyor, UI güncelle (mikrofon açmaya çalışma!) ✅
    ↓
✅ Sorunsuz Çalışıyor - Çekişme YOK
```

---

## Test Senaryoları

### Test 1: Normal Ses Çalma
**Adımlar**:
1. "domates" de
2. Asistan yanıtını bekle
3. Ses bitsin

**Beklenen Log**:
```
[VoiceAudio] 🔴 beginAssistantSpeech(2500ms)
I/VoiceTurn:  🔴 SPEAK BEGIN 2500ms | mic=KAPATILIYOR
I/AndroidBridge: 🔴 stopVosk(false) | Buffer atılıyor
[VoiceAudio] Çalma başladı
[VoiceAudio] Ses bitti, mikrofon açılıyor
[VoiceAudio] ✅ Native gate: endAssistantSpeech()
[VoiceAudio] Native mikrofonu kendi açacak, sadece UI güncelleniyor
I/VoiceTurn:  🟢 SPEAK END | mic 500ms sonra açılacak
I/VoiceTurn:  🎤 MIC AÇILIYOR (Vosk)
[NativeSpeech] 🎤 Native mikrofon açıldı, UI güncelleniyor
```

**❌ Olmaması Gereken**: `[VoiceAudio] Mikrofon başlatılıyor` (JS mikrofon açmaya çalışıyor)

### Test 2: Arka Arkaya Sesler
**Adımlar**:
1. "domates" de
2. Hemen "biber" de
3. Hemen "patlıcan" de

**Beklenen Davranış**:
- ✅ Her ses tamamlanıyor (kesinti YOK)
- ✅ Mikrofon her ses sonrası düzgün açılıyor
- ✅ Çekişme YOK

### Test 3: Konuşma Sırasında Komut
**Adımlar**:
1. "domates" de
2. Asistan konuşurken "biber" de

**Beklenen Davranış**:
- ✅ İkinci komut duyulmamalı (mikrofon kapalı)
- ✅ İlk ses tamamlanmalı
- ✅ Ses bitince mikrofon açılmalı

**Beklenen Log**:
```
I/AndroidBridge: Buffer atılıyor (asistan konuşuyor)
```

---

## Kritik Değişiklikler Özeti

| Özellik | v2.1 (Yanlış) | v2.2 (Doğru) |
|---------|---------------|--------------|
| JS `_finishSpeaking` | `startListening()` çağırıyor | Sadece UI güncelle |
| Mikrofon Kontrolü | JS + Native (çekişme) | %100 Native |
| Event Sistemi | Yok | `mic_reopened` eventi |
| Heartbeat | Her zaman aktif | Native gate varken kapalı |
| Senkronizasyon | Yok | Tam senkronize |

---

## Logcat İzleme

```powershell
# Tüm eventleri izle
adb logcat AndroidBridge:I VoiceTurn:I *:S | Select-String -Pattern "🔴|🟢|🎤|mic_reopened|startListening"

# Sadece kritik noktalar
adb logcat | Select-String -Pattern "SPEAK|MIC AÇILIYOR|mic_reopened|startListening"
```

---

## Başarı Kriterleri

### ✅ v2.2 Hedefleri:
- ✅ Asistan konuşurken ses KESİNTİSİZ
- ✅ Mikrofon çekişme YOK
- ✅ JS ASLA `startListening()` çağırmıyor (native gate varken)
- ✅ Native tam kontrol sahibi
- ✅ Event sistemi senkronize

### ❌ v2.1 Sorunları (Çözüldü):
- ❌ JS `startListening()` çağırıyordu → ✅ Artık çağırmıyor
- ❌ Çekişme vardı → ✅ Artık yok
- ❌ Event sistemi yoktu → ✅ Eklendi

---

## Özet

**v2.2 ile yapılan tek ama kritik değişiklik**:

> **Native gate varken JS ASLA mikrofon kontrolü yapmıyor. Mikrofon %100 native tarafında yönetiliyor.**

Bu basit ama güçlü değişiklik tüm çekişme sorunlarını çözdü.

---

**Tarih**: 15 Kasım 2026  
**Versiyon**: v2.2 - Native Gate Fix  
**Durum**: ✅ Çözüldü - JS/Native tam senkronize
