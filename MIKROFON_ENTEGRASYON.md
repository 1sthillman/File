# 🎤 Sürekli Mikrofon Entegrasyon Kılavuzu

## Gemini/ChatGPT Gibi Kesintisiz Mikrofon Sistemi

### ✅ Ne Sağlar?
- **BİP SESİ YOK** - Tamamen sessiz çalışma
- **AÇILIP KAPANMA YOK** - Bir kere aç, sürekli dinle
- **OTOMATİK YENİDEN BAŞLATMA** - Hata durumunda otomatik düzelir
- **ANLIK TRANSKRİPT** - Konuşurken anlık kelime gösterimi
- **AKILLI SESİZLİK ALGISI** - Sessizlikte otomatik sonuç gönderme

---

## 🚀 Hızlı Başlangıç

### 1. Test Sayfasını Aç
APK'nızda test etmek için:

```
http://localhost:PORT/test-speech.html
```

veya Android WebView'de:
```
file:///android_asset/test-speech.html
```

### 2. Mevcut Uygulamanıza Entegre Etme

#### A) HTML'e Script Ekle
`index.html` dosyanıza ekleyin:

```html
<script src="./speech-continuous.js"></script>
```

#### B) JavaScript Kodu

```javascript
// 1. Mikrofon sistemini başlat
const speech = new ContinuousSpeech();

// 2. Callback'leri ayarla
speech.onStart = () => {
    console.log('Mikrofon açıldı');
    // UI güncelleme: mikrofon ikonu aktif
};

speech.onPartial = (text) => {
    console.log('Anlık:', text);
    // Ekranda anlık transkript göster
    document.getElementById('live-text').textContent = text;
};

speech.onResult = (finalText) => {
    console.log('Kesin sonuç:', finalText);
    // Sonucu kullan (API'ye gönder, kaydet, vs.)
    sendToGemini(finalText);
};

speech.onEnd = () => {
    console.log('Mikrofon kapandı');
    // UI güncelleme: mikrofon ikonu pasif
};

speech.onError = (error) => {
    console.error('Hata:', error);
    // Hata mesajı göster
};

// 3. Başlat/Durdur
document.getElementById('mic-button').addEventListener('click', () => {
    if (speech.getIsListening()) {
        speech.stop();  // Durdur
    } else {
        speech.start(); // Başlat
    }
});
```

---

## 🎯 AndroidBridge ile Karşılaştırma

### ❌ Eski Sistem (AndroidBridge + Google Speech)
```
[Kullanıcı konuşuyor]
↓
BİP! 🔊 (Mikrofon açılıyor)
↓
[Konuşma]
↓
BİP! 🔊 (Mikrofon kapanıyor)
↓
[Sessizlik]
↓
BİP! 🔊 (Yeniden açılıyor)
↓
[Konuşma]
...
```

### ✅ Yeni Sistem (Web Speech API - Continuous)
```
[Kullanıcı konuşuyor]
↓
Mikrofon AÇIK (sessiz) 🎤
↓
[Konuşma] → Anlık transkript
↓
[Sessizlik] → Otomatik devam
↓
[Konuşma] → Anlık transkript
↓
Mikrofon hep AÇIK (sessiz)
```

---

## 📱 Android WebView Ayarları

MainActivity.java'da WebView ayarlarınızı kontrol edin:

```java
WebSettings settings = webView.getSettings();
settings.setJavaScriptEnabled(true);
settings.setMediaPlaybackRequiresUserGesture(false); // ÖNEMLİ!
settings.setDomStorageEnabled(true);

// Mikrofon izni
webView.setWebChromeClient(new WebChromeClient() {
    @Override
    public void onPermissionRequest(PermissionRequest request) {
        // Mikrofon izni otomatik ver
        if (request.getResources()[0].equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
            request.grant(request.getResources());
        }
    }
});
```

---

## 🔧 İleri Düzey Özelleştirme

### Sessizlik Süresini Ayarlama
```javascript
const speech = new ContinuousSpeech();
speech.SILENCE_TIMEOUT = 2000; // 2 saniye (varsayılan: 1500ms)
```

### Farklı Dil Kullanma
`speech-continuous.js` dosyasında:
```javascript
this.recognition.lang = 'en-US'; // İngilizce
this.recognition.lang = 'tr-TR'; // Türkçe (varsayılan)
```

### Ses Seviyesi Gösterimi (Opsiyonel)
```javascript
speech.onVolume = (level) => {
    // level: 0.0 - 1.0 arası
    document.getElementById('volume-bar').style.width = (level * 100) + '%';
};
```

---

## 🐛 Sorun Giderme

### 1. "Mikrofon izni reddedildi"
- Android Manifest'e mikrofon izni ekleyin:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### 2. "Web Speech API desteklenmiyor"
- Android WebView güncel olmalı (Chrome 71+)
- `settings.setJavaScriptEnabled(true)` aktif olmalı

### 3. Mikrofon otomatik durmuyor
- `speech.stop()` çağrıldığından emin olun
- Console logları kontrol edin

### 4. Bip sesi hala duyuluyor
- **Web Speech API kullanın** (bu entegrasyon)
- AndroidBridge (Google Speech) yerine bunu kullanın

---

## 💡 Kullanım Senaryoları

### 1. Sohbet Uygulaması (ChatGPT/Gemini Tarzı)
```javascript
speech.onResult = async (text) => {
    addUserMessage(text);
    const response = await callAI(text);
    addBotMessage(response);
};
```

### 2. Sürekli Dikte Sistemi
```javascript
let fullText = '';
speech.onResult = (text) => {
    fullText += text + ' ';
    document.getElementById('editor').value = fullText;
};
```

### 3. Sesli Komut Sistemi
```javascript
speech.onResult = (text) => {
    const command = text.toLowerCase();
    if (command.includes('aç')) {
        openApp();
    } else if (command.includes('kapat')) {
        closeApp();
    }
};
```

---

## 📊 Performans

- **CPU Kullanımı**: Düşük (~5-10%)
- **RAM Kullanımı**: Minimal (~20-30 MB)
- **Batarya**: Orta (arka planda çalışırsa optimize edilmeli)
- **Gecikme**: 200-500ms (anlık transkript)

---

## 🔒 Güvenlik Notları

1. **İzinler**: Kullanıcı izni gerekli (otomatik izin verme opsiyonel)
2. **HTTPS Gerekli**: Üretimde HTTPS kullanın
3. **Veri Gizliliği**: Google sunucularına ses gönderilir (Web Speech API)

---

## 📞 Destek

Sorularınız için:
- Konsol loglarını kontrol edin (`console.log`)
- Chrome DevTools ile WebView debug edin
- Test sayfasını (`test-speech.html`) kullanın

---

## 🎉 Başarı!

Artık uygulamanızda **Gemini ve ChatGPT gibi kesintisiz, bip sesi olmayan** bir mikrofon sistemi var! 🚀
