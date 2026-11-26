/**
 * TTS Entegrasyon Modülü
 * Mevcut uygulamadaki tüm ses dosyalarını TTS ile değiştirmek için
 */

class TTSIntegration {
    constructor(ttsEngine) {
        this.tts = ttsEngine;
        this.audioMap = new Map();
        this.isEnabled = true;
        
        // Ses dosyası → Metin eşlemeleri
        this.initAudioMapping();
        
        // Ayarları yükle
        this.loadPreferences();
    }
    
    /**
     * Ses dosyası → TTS metin eşlemelerini tanımla
     */
    initAudioMapping() {
        // Sayfa bildirimleri
        this.audioMap.set('sayımsayfasıacıldi.mp3', 'Sayım sayfası açıldı');
        this.audioMap.set('ürünlersayfasıacıldi.mp3', 'Ürünler sayfası açıldı');
        this.audioMap.set('sevkiyatsayfasıacıldi.mp3', 'Sevkiyat sayfası açıldı');
        this.audioMap.set('panosayfası.mp3', 'Pano sayfası açıldı');
        this.audioMap.set('firesayfasıacıldi.mp3', 'Fire sayfası açıldı');
        this.audioMap.set('aktarsayfasıacıldi.mp3', 'Aktar sayfası açıldı');
        
        // Genel bildirimler
        this.audioMap.set('secim_yapin.mp3', 'Seçim yapın');
        this.audioMap.set('secildi.mp3', 'Seçildi');
        this.audioMap.set('ve.mp3', 've');
        
        // Ürün sesleri (000-101)
        for (let i = 0; i <= 101; i++) {
            const fileName = `urun_ses/${String(i).padStart(3, '0')}.mp3`;
            this.audioMap.set(fileName, this.getProductName(i));
        }
    }
    
    /**
     * Ürün numarasına göre isim döndür
     * @param {number} productId - Ürün ID'si
     * @returns {string}
     */
    getProductName(productId) {
        // Burada gerçek ürün isimleri database'den gelecek
        // Şimdilik örnek veriyoruz
        const productNames = {
            0: 'Domates',
            1: 'Salatalık',
            2: 'Biber',
            3: 'Patlıcan',
            4: 'Kabak',
            5: 'Havuç',
            6: 'Soğan',
            7: 'Sarımsak',
            8: 'Patates',
            9: 'Marul',
            10: 'Ispanak',
            // ... daha fazla ürün eklenebilir
        };
        
        return productNames[productId] || `Ürün ${productId}`;
    }
    
    /**
     * Ses dosyası yerine TTS kullan
     * @param {string} audioFileName - Ses dosyası adı (örn: 'sayımsayfasıacıldi.mp3')
     * @param {Object} options - TTS opsiyonları
     * @returns {Promise}
     */
    playAudio(audioFileName, options = {}) {
        // TTS devre dışıysa orijinal ses dosyasını çal
        if (!this.isEnabled) {
            return this.playOriginalAudio(audioFileName);
        }
        
        // Dosya adından metin al
        const text = this.audioMap.get(audioFileName);
        
        if (!text) {
            console.warn(`⚠️ TTS eşlemesi bulunamadı: ${audioFileName}, orijinal ses çalınıyor`);
            return this.playOriginalAudio(audioFileName);
        }
        
        // TTS ile seslendir
        console.log(`🔊 TTS: "${text}" (${audioFileName} yerine)`);
        return this.tts.speak(text, options);
    }
    
    /**
     * Orijinal ses dosyasını çal (fallback)
     * @param {string} audioFileName
     * @returns {Promise}
     */
    playOriginalAudio(audioFileName) {
        return new Promise((resolve, reject) => {
            try {
                const audio = new Audio(audioFileName);
                audio.onended = resolve;
                audio.onerror = reject;
                audio.play().catch(reject);
            } catch (error) {
                console.error('Ses çalma hatası:', error);
                reject(error);
            }
        });
    }
    
    /**
     * Birden fazla ses/metni sırayla çal
     * @param {Array} items - Ses dosyaları veya metinler
     * @param {number} delay - Aralarındaki gecikme (ms)
     */
    async playSequence(items, delay = 300) {
        for (const item of items) {
            if (typeof item === 'string' && item.endsWith('.mp3')) {
                await this.playAudio(item);
            } else {
                await this.tts.speak(item);
            }
            
            if (delay > 0) {
                await this.sleep(delay);
            }
        }
    }
    
    /**
     * Ürün listesini seslendir (örn: "Domates, Salatalık, Biber")
     * @param {Array} productIds - Ürün ID'leri
     */
    async announceProducts(productIds) {
        if (productIds.length === 0) return;
        
        const productNames = productIds.map(id => this.getProductName(id));
        
        // İlk ürünü söyle
        await this.tts.speak(productNames[0]);
        
        // Diğer ürünler varsa
        for (let i = 1; i < productNames.length; i++) {
            await this.sleep(300);
            await this.tts.speak('ve');
            await this.sleep(200);
            await this.tts.speak(productNames[i]);
        }
    }
    
    /**
     * Sayıyı seslendir
     * @param {number} number
     */
    announceNumber(number) {
        return this.tts.speak(this.numberToText(number));
    }
    
    /**
     * Sayıyı metne çevir (Türkçe)
     * @param {number} num
     * @returns {string}
     */
    numberToText(num) {
        const ones = ['', 'bir', 'iki', 'üç', 'dört', 'beş', 'altı', 'yedi', 'sekiz', 'dokuz'];
        const tens = ['', 'on', 'yirmi', 'otuz', 'kırk', 'elli', 'altmış', 'yetmiş', 'seksen', 'doksan'];
        const hundreds = ['', 'yüz', 'iki yüz', 'üç yüz', 'dört yüz', 'beş yüz', 'altı yüz', 'yedi yüz', 'sekiz yüz', 'dokuz yüz'];
        
        if (num === 0) return 'sıfır';
        if (num === 1000) return 'bin';
        
        let result = '';
        
        // Binler
        if (num >= 1000) {
            const thousands = Math.floor(num / 1000);
            if (thousands === 1) {
                result += 'bin ';
            } else {
                result += this.numberToText(thousands) + ' bin ';
            }
            num = num % 1000;
        }
        
        // Yüzler
        if (num >= 100) {
            const hundred = Math.floor(num / 100);
            result += hundreds[hundred] + ' ';
            num = num % 100;
        }
        
        // Onlar
        if (num >= 10) {
            const ten = Math.floor(num / 10);
            result += tens[ten] + ' ';
            num = num % 10;
        }
        
        // Birler
        if (num > 0) {
            result += ones[num];
        }
        
        return result.trim();
    }
    
    /**
     * TTS'yi aç/kapat
     * @param {boolean} enabled
     */
    setEnabled(enabled) {
        this.isEnabled = enabled;
        this.savePreferences();
        console.log(enabled ? '✅ TTS etkinleştirildi' : '❌ TTS devre dışı bırakıldı');
    }
    
    /**
     * TTS durumunu getir
     */
    isEnabled() {
        return this.isEnabled;
    }
    
    /**
     * Sleep fonksiyonu
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    
    /**
     * Ayarları kaydet
     */
    savePreferences() {
        try {
            localStorage.setItem('tts_integration_enabled', JSON.stringify(this.isEnabled));
        } catch (error) {
            console.error('Ayarlar kaydedilemedi:', error);
        }
    }
    
    /**
     * Ayarları yükle
     */
    loadPreferences() {
        try {
            const saved = localStorage.getItem('tts_integration_enabled');
            if (saved !== null) {
                this.isEnabled = JSON.parse(saved);
            }
        } catch (error) {
            console.error('Ayarlar yüklenemedi:', error);
        }
    }
}

// Global kullanım için
window.TTSIntegration = TTSIntegration;

/**
 * Kullanım Örnekleri:
 * 
 * // TTS motorunu başlat
 * const tts = new TTSEngine();
 * const ttsIntegration = new TTSIntegration(tts);
 * 
 * // Ses dosyası yerine TTS kullan
 * ttsIntegration.playAudio('sayımsayfasıacıldi.mp3');
 * 
 * // Ürünleri seslendir
 * ttsIntegration.announceProducts([0, 1, 2]); // "Domates ve Salatalık ve Biber"
 * 
 * // Sayı seslendir
 * ttsIntegration.announceNumber(123); // "yüz yirmi üç"
 * 
 * // Sıralı bildirimler
 * ttsIntegration.playSequence([
 *     'sayımsayfasıacıldi.mp3',
 *     'Toplam 15 ürün var',
 *     'secim_yapin.mp3'
 * ]);
 * 
 * // TTS'yi kapat (orijinal sesleri kullan)
 * ttsIntegration.setEnabled(false);
 */
