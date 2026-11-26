# 🎤 TTS (Text-to-Speech) Kullanım Kılavuzu

## 📋 İçindekiler
1. [Genel Bakış](#genel-bakış)
2. [Özellikler](#özellikler)
3. [Kurulum](#kurulum)
4. [Kullanım](#kullanım)
5. [API Referansı](#api-referansı)
6. [Örnekler](#örnekler)
7. [Sorun Giderme](#sorun-giderme)

---

## 🎯 Genel Bakış

Bu TTS (Text-to-Speech) sistemi, mevcut ses dosyalarınızı dinamik, yapay zeka tabanlı sesli konuşma ile değiştirmenizi sağlar. Artık her bildirim için ayrı MP3 dosyası oluşturmanıza gerek yok!

### Avantajlar
- ✅ **Dinamik İçerik**: İstediğiniz metni anında seslendirin
- ✅ **Birden Fazla Ses**: Türkçe için birçok farklı ses modeli
- ✅ **Özelleştirilebilir**: Hız, ton ve ses seviyesi ayarlanabilir
- ✅ **Offline Çalışma**: Cihaz içi sesler internet gerektirmez
- ✅ **Hafif**: Hiç ses dosyası indirmenize gerek yok
- ✅ **Kolay Entegrasyon**: Mevcut kodunuza kolayca entegre edilir

---

## 🚀 Özellikler

### 1. TTS Engine (tts-engine.js)
- Web Speech API tabanlı
- Türkçe ses modeli otomatik algılama
- Konuşma hızı kontrolü (0.1x - 10x)
- Ses tonu kontrolü (0 - 2)
- Ses seviyesi kontrolü (0% - 100%)
- Ayarları otomatik kaydetme
- Hata yönetimi ve geri dönüş

### 2. TTS Ayarlar UI (tts-settings-ui.js)
- Modern ve kullanıcı dostu arayüz
- Ses modeli seçimi
- Gerçek zamanlı ayar değişikliği
- Ses testi özelliği
- Responsive tasarım (mobil uyumlu)

### 3. TTS Entegrasyon (tts-integration.js)
- Ses dosyalarını otomatik TTS'e çevirme
- Ürün isimleri seslendirme
- Sayıları Türkçe metne çevirme
- Sıralı bildirimler (sekans)
- Fallback mekanizması (TTS yoksa orijinal ses)

---

## 📦 Kurulum

### 1. Dosyaları Projenize Ekleyin

Tüm TTS dosyaları `www` klasörüne eklenmiştir:
```
www/
├── tts-engine.js          # TTS motoru
├── tts-settings-ui.js     # Ayarlar arayüzü
├── tts-integration.js     # Entegrasyon yardımcıları
└── tts-demo.html          # Test sayfası
```

### 2. HTML'inize Ekleyin

Ana `index.html` dosyanıza şu satırları ekleyin:

```html
<!-- TTS Modülleri -->
<script src="tts-engine.js"></script>
<script src="tts-settings-ui.js"></script>
<script src="tts-integration.js"></script>
```

### 3. JavaScript'te Başlatın

```javascript
// TTS motorunu başlat
const ttsEngine = new TTSEngine();

// Ayarlar UI'ını oluştur
const ttsSettingsUI = new TTSSettingsUI(ttsEngine);

// Entegrasyon modülünü başlat
const ttsIntegration = new TTSIntegration(ttsEngine);
```

---

## 💻 Kullanım

### Temel Kullanım

```javascript
// Basit metin seslendirme
ttsEngine.speak('Merhaba, nasılsınız?');

// Özel ayarlarla seslendirme
ttsEngine.speak('Bu hızlı konuşma', { 
    rate: 1.5,    // Hızlı
    pitch: 1.2,   // Yüksek ton
    volume: 0.8   // Orta ses
});

// Konuşmayı durdur
ttsEngine.stop();
```

### Ses Dosyası Yerine TTS Kullanma

```javascript
// Eski yöntem (ses dosyası)
// const audio = new Audio('sayımsayfasıacıldi.mp3');
// audio.play();

// Yeni yöntem (TTS)
ttsIntegration.playAudio('sayımsayfasıacıldi.mp3');
// Otomatik olarak "Sayım sayfası açıldı" der
```

### Ürün Listesi Seslendirme

```javascript
// Ürün ID'lerini seslendir
ttsIntegration.announceProducts([0, 1, 2]);
// "Domates ve Salatalık ve Biber" der
```

### Sayı Seslendirme

```javascript
// Sayıyı Türkçe seslendir
ttsIntegration.announceNumber(123);
// "yüz yirmi üç" der

ttsIntegration.announceNumber(1500);
// "bin beş yüz" der
```

### Sıralı Bildirimler

```javascript
// Birden fazla bildirimi sırayla çal
await ttsIntegration.playSequence([
    'sayımsayfasıacıldi.mp3',
    'Toplam 15 ürün bulundu',
    'secim_yapin.mp3'
], 500); // 500ms gecikme ile
```

### Ayarlar Panelini Aç

```javascript
// Kullanıcı ses ayarlarını değiştirmek isterse
ttsSettingsUI.open();

// Ayarları programatik olarak değiştir
ttsEngine.setRate(1.5);     // Hız
ttsEngine.setPitch(1.2);    // Ton
ttsEngine.setVolume(0.9);   // Ses seviyesi
ttsEngine.setVoice('tr-TR-Wavenet-A'); // Ses modeli
```

---

## 📚 API Referansı

### TTSEngine

#### Methods

**`speak(text, options)`**
- Metni seslendirir
- `text`: String - Seslendirilecek metin
- `options`: Object (opsiyonel)
  - `rate`: Number (0.1-10) - Konuşma hızı
  - `pitch`: Number (0-2) - Ses tonu
  - `volume`: Number (0-1) - Ses seviyesi
  - `interrupt`: Boolean - Mevcut konuşmayı durdur (varsayılan: true)
- Returns: Promise

**`stop()`**
- Konuşmayı durdur

**`pause()`**
- Konuşmayı duraklat

**`resume()`**
- Konuşmaya devam et

**`setVoice(voiceURI)`**
- Ses modelini değiştir
- `voiceURI`: String - Sesin URI'si

**`setRate(rate)`**
- Konuşma hızını ayarla
- `rate`: Number (0.1-10)

**`setPitch(pitch)`**
- Ses tonunu ayarla
- `pitch`: Number (0-2)

**`setVolume(volume)`**
- Ses seviyesini ayarla
- `volume`: Number (0-1)

**`getCurrentVoice()`**
- Mevcut sesi getir
- Returns: Voice object

**`getTurkishVoices()`**
- Türkçe sesleri listele
- Returns: Array<Voice>

**`getStatus()`**
- TTS durumunu getir
- Returns: Object

**`test(text)`**
- Test konuşması yap
- Returns: Promise

#### Events

```javascript
ttsEngine.onVoicesLoaded = (voices) => {
    console.log('Sesler yüklendi:', voices);
};

ttsEngine.onSpeakStart = (text) => {
    console.log('Konuşma başladı:', text);
};

ttsEngine.onSpeakEnd = (text) => {
    console.log('Konuşma bitti:', text);
};

ttsEngine.onError = (error) => {
    console.error('TTS hatası:', error);
};
```

### TTSIntegration

#### Methods

**`playAudio(audioFileName, options)`**
- Ses dosyası yerine TTS kullan
- `audioFileName`: String - Ses dosyası adı
- `options`: Object - TTS opsiyonları
- Returns: Promise

**`playSequence(items, delay)`**
- Sıralı bildirimler çal
- `items`: Array<String> - Ses dosyaları veya metinler
- `delay`: Number - Aralarındaki gecikme (ms)
- Returns: Promise

**`announceProducts(productIds)`**
- Ürün listesi seslendir
- `productIds`: Array<Number> - Ürün ID'leri
- Returns: Promise

**`announceNumber(number)`**
- Sayıyı Türkçe seslendir
- `number`: Number
- Returns: Promise

**`setEnabled(enabled)`**
- TTS'yi aç/kapat
- `enabled`: Boolean

**`isEnabled()`**
- TTS durumunu kontrol et
- Returns: Boolean

### TTSSettingsUI

#### Methods

**`open()`**
- Ayarlar panelini aç

**`close()`**
- Ayarlar panelini kapat

**`reset()`**
- Ayarları varsayılana döndür

---

## 🎨 Örnekler

### Örnek 1: Sayfa Geçişlerinde Bildirim

```javascript
function navigateToPage(pageName) {
    // Sayfa değiştir
    showPage(pageName);
    
    // TTS bildirimi
    const pageAnnouncements = {
        'sayim': 'Sayım sayfası açıldı',
        'urunler': 'Ürünler sayfası açıldı',
        'sevkiyat': 'Sevkiyat sayfası açıldı',
        'pano': 'Pano sayfası açıldı'
    };
    
    const text = pageAnnouncements[pageName];
    if (text) {
        ttsEngine.speak(text);
    }
}
```

### Örnek 2: Ürün Seçimi Bildirimi

```javascript
function selectProduct(productId, productName) {
    // Ürünü seç
    selectedProducts.push(productId);
    
    // Sesli bildirim
    ttsIntegration.playSequence([
        productName,
        'secildi.mp3'
    ]);
}
```

### Örnek 3: Sayım Sonucu Bildirimi

```javascript
async function announceCountResult(count) {
    await ttsIntegration.playSequence([
        'Toplam',
        ttsIntegration.numberToText(count),
        'adet ürün sayıldı'
    ]);
}
```

### Örnek 4: Hata Bildirimi

```javascript
function showError(errorMessage) {
    ttsEngine.speak('Hata: ' + errorMessage, {
        rate: 0.9,  // Biraz yavaş
        pitch: 0.8  // Biraz kalın
    });
}
```

### Örnek 5: Ses Ayarları Butonu

```html
<!-- HTML -->
<button onclick="openTTSSettings()">
    🎤 Ses Ayarları
</button>

<script>
function openTTSSettings() {
    ttsSettingsUI.open();
}
</script>
```

---

## 🧪 Test Etme

### Demo Sayfası

Tarayıcınızda şu adresi açın:
```
file:///android_asset/www/tts-demo.html
```

Bu sayfada:
- ✅ Farklı ses modellerini test edebilirsiniz
- ✅ Hız, ton ve ses seviyesini ayarlayabilirsiniz
- ✅ Hızlı test cümlelerine tıklayabilirsiniz
- ✅ Mevcut sesleri görebilirsiniz

### Console'dan Test

```javascript
// Console'u açın (F12) ve şunları deneyin:

// Basit test
ttsEngine.speak('Merhaba dünya');

// Türkçe sesleri listele
console.log(ttsEngine.getTurkishVoices());

// Durum kontrolü
console.log(ttsEngine.getStatus());

// Ses değiştir ve test et
ttsEngine.setVoice('Google Türkçe');
ttsEngine.test();
```

---

## 🔧 Sorun Giderme

### Ses Çıkmıyor

**Olası Nedenler:**
1. TTS desteklenmiyor (eski tarayıcı)
2. Ses seviyesi 0'da
3. Cihaz sessiz modda

**Çözüm:**
```javascript
// TTS desteğini kontrol et
if (!window.speechSynthesis) {
    console.error('TTS desteklenmiyor!');
}

// Ses seviyesini kontrol et
console.log(ttsEngine.getStatus().settings.volume);

// Test et
ttsEngine.test();
```

### Türkçe Ses Yok

**Olası Nedenler:**
1. Cihazda Türkçe dil paketi yüklü değil
2. Sesler henüz yüklenmedi

**Çözüm:**
```javascript
// Sesleri manuel yükle
ttsEngine.loadVoices();

// Türkçe sesleri kontrol et
const voices = ttsEngine.getTurkishVoices();
console.log(voices);

// Yoksa tüm sesleri göster
if (voices.length === 0) {
    console.log(ttsEngine.voices);
}
```

### Kesik Kesik Konuşuyor

**Olası Nedenler:**
1. Hız çok yüksek veya çok düşük
2. İşlemci yoğun

**Çözüm:**
```javascript
// Hızı normalleştir
ttsEngine.setRate(1.0);

// Ayarları sıfırla
ttsSettingsUI.reset();
```

### Android'de Çalışmıyor

**Çözüm:**
1. Chrome kullanın (varsayılan tarayıcı yerine)
2. Android TTS motor ayarlarını kontrol edin
3. Google Text-to-Speech uygulamasını güncelleyin

```javascript
// Android için özel ayarlar
if (/Android/i.test(navigator.userAgent)) {
    ttsEngine.setRate(0.9);  // Biraz yavaşlat
}
```

---

## 🎯 En İyi Pratikler

### 1. Ayarları Kullanıcıya Bırakın
```javascript
// Kullanıcı ses tercihlerini ayarlasın
// Ayarlar otomatik kaydediliyor
```

### 2. Fallback Mekanizması Kullanın
```javascript
// TTS yoksa orijinal sese dön
if (!ttsIntegration.isEnabled()) {
    playOriginalAudio('file.mp3');
}
```

### 3. Kısa ve Net Cümleler
```javascript
// İyi ✅
ttsEngine.speak('Sayım başladı');

// Kötü ❌
ttsEngine.speak('Sayım işlemi başarılı bir şekilde başlatıldı ve şu anda devam etmektedir');
```

### 4. Hata Yönetimi
```javascript
ttsEngine.speak('Mesaj')
    .then(() => console.log('Başarılı'))
    .catch(err => console.error('Hata:', err));
```

### 5. Performans
```javascript
// Birden fazla bildirimi sequence ile çal
await ttsIntegration.playSequence(messages);

// Her biri için ayrı ayrı çağırma ❌
```

---

## 📱 Mobil Optimizasyon

### Android İçin

```javascript
// Android'de ses yükleme gecikmesi olabilir
setTimeout(() => {
    if (ttsEngine.isInitialized) {
        ttsEngine.test();
    }
}, 1000);
```

### iOS İçin

```javascript
// iOS'ta ilk konuşma kullanıcı etkileşimi gerektirir
document.addEventListener('click', () => {
    if (!ttsEngine.hasSpoken) {
        ttsEngine.speak('Hazır');
        ttsEngine.hasSpoken = true;
    }
}, { once: true });
```

---

## 🌟 Sonuç

TTS sistemi artık tamamen hazır! 

**Hızlı Başlangıç:**
1. `tts-demo.html`'i açın ve test edin
2. Beğendiğiniz sesi seçin
3. Ana uygulamanızda kullanmaya başlayın

**Sorularınız için:**
- Demo sayfasındaki örneklere bakın
- Console'da test edin
- Bu kılavuzu referans olarak kullanın

Başarılar! 🚀
