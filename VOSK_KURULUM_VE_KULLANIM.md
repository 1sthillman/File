# 🎤 Vosk ile Kesintisiz Ses Tanıma (BİP YOK!)

## ✅ Neden Vosk?

- **%100 Çevrimdışı** - İnternet gerektirmez
- **HİÇBİR BİP SESİ YOK** - Tamamen sessiz
- **Sürekli Dinleme** - Açılıp kapanma yok
- **Anlık Transkript** - Konuşurken kelimeler geliyor
- **Akıllı Kelime Birleştirme** - Cümleleri otomatik birleştirir

---

## 📦 1. Vosk Modelini İndirin

### Türkçe Model (Küçük - 45MB)
```
https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip
```

**veya**

### Türkçe Model (Büyük - 190MB - Daha İyi)
```
https://alphacephei.com/vosk/models/vosk-model-tr-0.3.zip
```

---

## 📁 2. Modeli Projeye Ekleyin

### Adım 1: Zip'i açın
İndirdiğiniz dosyayı açın, içinden `vosk-model-small-tr-0.3` klasörünü bulun.

### Adım 2: Yeniden adlandırın
Klasör adını `model-tr` yapın.

### Adım 3: Projeye kopyalayın
```
c:\File\android\app\src\main\assets\model-tr\
```

### Klasör yapısı şöyle olmalı:
```
android/app/src/main/assets/
└── model-tr/
    ├── uuid                    (dosya)
    ├── am/                     (klasör)
    ├── conf/                   (klasör)
    ├── graph/                  (klasör)
    └── ivector/                (klasör - opsiyonel)
```

**ÖNEMLІ**: `uuid` dosyası MUTLAKA olmalı!

---

## 🔧 3. Gradle Bağımlılığını Ekleyin

`android/app/build.gradle` dosyasını açın ve ekleyin:

```gradle
dependencies {
    // ... mevcut bağımlılıklar ...
    
    implementation 'com.alphacep:vosk-android:0.3.47'
}
```

### Eğer hata alırsanız, repositories ekleyin:

`android/build.gradle` veya `settings.gradle` içine:

```gradle
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }  // Burası önemli
}
```

---

## 🚀 4. Gradle Sync + Build

```bash
cd android
./gradlew clean
./gradlew assembleDebug
```

veya Android Studio'da:
- **File → Sync Project with Gradle Files**
- **Build → Rebuild Project**

---

## 🎯 5. Kullanım (JavaScript Tarafı)

AndroidBridge zaten hazır! Sadece şunu yapın:

```javascript
// Dinlemeyi başlat
AndroidBridge.startNativeSpeech();

// Anlık transkript (konuşurken)
window.onNativeSpeechPartial = function(text) {
    console.log("Anlık:", text);
    document.getElementById('live-text').textContent = text;
};

// Kesin sonuç (cümle tamamlandı)
window.onNativeSpeechResult = function(text) {
    console.log("Sonuç:", text);
    // Burada cümleyi kullanın
    sendToAPI(text);
};

// Olay bildirimleri
window.onNativeSpeechEvent = function(eventType, message) {
    if (eventType === 'ready') {
        console.log('Mikrofon hazır');
    } else if (eventType === 'error') {
        console.error('Hata:', message);
    }
};

// Ses seviyesi (animasyon için)
window.onNativeSpeechVolume = function(level) {
    // level: 0.0 - 1.0 arası
    document.getElementById('mic-icon').style.opacity = 0.5 + (level * 0.5);
};

// Durdurmak için
AndroidBridge.stopNativeSpeech();
```

---

## ⚙️ 6. Ayarlar (AndroidBridge.java)

Kodunuzda bu parametreleri değiştirebilirsiniz:

### Kelime Birleştirme Süresi
```java
private static final long WORD_MERGE_TIMEOUT_MS = 1200;
```
- **Küçük (800ms)**: Hızlı konuşmalar için
- **Büyük (2000ms)**: Yavaş konuşmalar için

### Sessizlik Eşiği
```java
private static final float SILENCE_THRESHOLD = 0.08f;
```
- **Küçük (0.05f)**: Daha hassas (gürültüyü de yakalar)
- **Büyük (0.12f)**: Daha toleranslı

### Minimum Sessizlik Süresi
```java
private static final int MIN_SILENCE_FRAMES = 12;
```
- **Her frame ~100ms** → 12 frame = 1.2 saniye
- Sessizlik bu süreden uzun olursa cümle gönderilir

---

## 🧪 7. Test

APK'yı yükledikten sonra:

1. **Mikrofon simgesine basın**
2. **"Test bir iki üç"** deyin
3. **Konsolu kontrol edin:**
   - Anlık transkript geliyor mu?
   - Bip sesi var mı?
   - Cümle doğru birleşiyor mu?

---

## 🐛 8. Sorun Giderme

### "Vosk modeli yüklenemedi"
- `uuid` dosyası var mı kontrol edin
- Klasör adı tam olarak `model-tr` mi?
- `assets` klasörüne doğru kopyalandı mı?

### "Mikrofon başlatılamadı"
- AndroidManifest.xml'de izin var mı?
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Kelimeler doğru gelmiyor
- Büyük modeli deneyin (vosk-model-tr-0.3)
- Mikrofona daha yakın konuşun
- Gürültüsüz ortamda test edin

### "on beş" gibi kelime sayılar geliyor
✅ **Bu beklenen davranış!** Vosk sayıları kelime olarak tanır.
Bunları sayıya çevirmek için dönüştürücü lazım.

---

## 🔢 9. Sayı Dönüştürücü (Bonus)

"on beş kilo" → "15 kilo" dönüşümü için:

```javascript
function convertTurkishNumberWords(text) {
    const numbers = {
        'sıfır': 0, 'bir': 1, 'iki': 2, 'üç': 3, 'dört': 4,
        'beş': 5, 'altı': 6, 'yedi': 7, 'sekiz': 8, 'dokuz': 9,
        'on': 10, 'yirmi': 20, 'otuz': 30, 'kırk': 40, 'elli': 50,
        'altmış': 60, 'yetmiş': 70, 'seksen': 80, 'doksan': 90,
        'yüz': 100, 'bin': 1000
    };
    
    // "on beş" → "15"
    let result = text;
    
    // İki kelimelik sayılar (on beş, yirmi üç, vs.)
    Object.keys(numbers).forEach(tens => {
        if (numbers[tens] >= 10 && numbers[tens] < 100) {
            Object.keys(numbers).forEach(ones => {
                if (numbers[ones] < 10) {
                    const phrase = tens + ' ' + ones;
                    const value = numbers[tens] + numbers[ones];
                    result = result.replace(new RegExp(phrase, 'gi'), value);
                }
            });
        }
    });
    
    // Tek kelimelik sayılar
    Object.keys(numbers).forEach(word => {
        result = result.replace(new RegExp('\\b' + word + '\\b', 'gi'), numbers[word]);
    });
    
    return result;
}

// Kullanım
window.onNativeSpeechResult = function(text) {
    const converted = convertTurkishNumberWords(text);
    console.log("Orijinal:", text);      // "on beş kilo domates"
    console.log("Dönüştürülmüş:", converted); // "15 kilo domates"
};
```

---

## 📊 10. Beklenen Sonuçlar

### ✅ Çalışırsa:
- Mikrofon açılır, **BİP YOK**
- Konuşurken anlık kelimeler görünür
- Kısa sessizlikte kelimeler birleşir
- Uzun sessizlikte cümle gönderilir
- Mikrofon sürekli açık kalır

### ❌ Çalışmazsa:
1. Logcat'i kontrol edin: `adb logcat | grep AndroidBridge`
2. Model dosyası hatasını arayın
3. Mikrofon izni var mı kontrol edin

---

## 💡 11. İpuçları

### Batarya Optimizasyonu
Sürekli mikrofon açıksa:
```java
// AndroidBridge.java'da
private static final int SAMPLE_RATE = 16000; // Düşük = az pil
```

### Daha İyi Tanıma
- Gürültü filtreleme ekleyin
- Ses normalizasyonu yapın
- Büyük model kullanın

### Üretim Ortamı
- Model'i APK boyutunu artırır (45-190MB)
- Split APK kullanabilirsiniz
- Veya ilk açılışta indirebilirsiniz

---

## 🎉 Sonuç

Vosk kurulumu tamamlandığında:
- ✅ %100 Çevrimdışı
- ✅ Bip sesi YOK
- ✅ Gemini/ChatGPT gibi sürekli dinleme
- ✅ Anlık transkript
- ✅ Akıllı cümle birleştirme

**Model indirip `assets/model-tr/` klasörüne koymanız yeterli!**

---

## 📞 Sonraki Adımlar

1. **Model indirin**: https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip
2. **Kopyalayın**: `android/app/src/main/assets/model-tr/`
3. **Derleyin**: `./gradlew assembleDebug`
4. **Test edin**: APK'yı yükleyin ve konuşun

Sonuçları bana bildirin! 🚀
