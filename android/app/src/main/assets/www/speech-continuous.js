/**
 * Sürekli Ses Tanıma Modülü (Web Speech API)
 * Gemini ve ChatGPT gibi kesintisiz, bip sesi olmayan mikrofon sistemi
 */

class ContinuousSpeech {
    constructor() {
        this.recognition = null;
        this.isListening = false;
        this.currentTranscript = '';
        this.finalTranscript = '';
        this.silenceTimer = null;
        this.SILENCE_TIMEOUT = 1500; // 1.5 saniye sessizlikten sonra sonucu gönder
        
        // Callback fonksiyonları
        this.onResult = null;      // Kesin sonuç geldiğinde
        this.onPartial = null;     // Anlık transkript güncellemesi
        this.onStart = null;       // Dinleme başladığında
        this.onEnd = null;         // Dinleme durduğunda
        this.onError = null;       // Hata oluştuğunda
        this.onVolume = null;      // Ses seviyesi (opsiyonel)
        
        this.initRecognition();
    }
    
    initRecognition() {
        // Web Speech API kontrolü
        if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
            console.error('Web Speech API desteklenmiyor');
            return false;
        }
        
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        this.recognition = new SpeechRecognition();
        
        // Ayarlar
        this.recognition.continuous = true;         // Sürekli dinle
        this.recognition.interimResults = true;     // Anlık sonuçları al
        this.recognition.lang = 'tr-TR';            // Türkçe
        this.recognition.maxAlternatives = 1;
        
        // Olay dinleyicileri
        this.recognition.onstart = () => {
            console.log('🎤 Mikrofon başladı (sürekli mod)');
            this.isListening = true;
            if (this.onStart) this.onStart();
        };
        
        this.recognition.onresult = (event) => {
            this.handleResult(event);
        };
        
        this.recognition.onerror = (event) => {
            console.error('Ses tanıma hatası:', event.error);
            
            // Bazı hataları yok say ve devam et
            if (event.error === 'no-speech' || event.error === 'audio-capture') {
                // Sessizlik veya geçici ses yakalama hatası - otomatik yeniden başlat
                if (this.isListening) {
                    setTimeout(() => {
                        if (this.isListening) this.recognition.start();
                    }, 100);
                }
                return;
            }
            
            if (this.onError) this.onError(event.error);
        };
        
        this.recognition.onend = () => {
            console.log('🎤 Mikrofon durduruldu');
            
            // Eğer hala dinleme modundaysak, otomatik yeniden başlat
            if (this.isListening) {
                console.log('🔄 Otomatik yeniden başlatılıyor...');
                setTimeout(() => {
                    if (this.isListening) {
                        try {
                            this.recognition.start();
                        } catch(e) {
                            console.warn('Yeniden başlatma hatası:', e);
                        }
                    }
                }, 100);
            } else {
                if (this.onEnd) this.onEnd();
            }
        };
        
        return true;
    }
    
    handleResult(event) {
        let interimTranscript = '';
        let finalText = '';
        
        // Tüm sonuçları işle
        for (let i = event.resultIndex; i < event.results.length; i++) {
            const transcript = event.results[i][0].transcript;
            
            if (event.results[i].isFinal) {
                finalText += transcript;
            } else {
                interimTranscript += transcript;
            }
        }
        
        // Anlık transkript güncelleme
        if (interimTranscript && this.onPartial) {
            this.currentTranscript = interimTranscript;
            this.onPartial(interimTranscript);
        }
        
        // Kesin sonuç
        if (finalText) {
            this.finalTranscript = finalText;
            
            // Sessizlik zamanlayıcısını sıfırla
            this.resetSilenceTimer();
            
            // Sonucu gönder
            if (this.onResult) {
                this.onResult(finalText.trim());
            }
        }
    }
    
    resetSilenceTimer() {
        if (this.silenceTimer) {
            clearTimeout(this.silenceTimer);
        }
        
        // Uzun sessizlik sonrası buffer temizle
        this.silenceTimer = setTimeout(() => {
            this.currentTranscript = '';
            this.finalTranscript = '';
        }, this.SILENCE_TIMEOUT);
    }
    
    /**
     * Sürekli dinlemeyi başlat
     */
    start() {
        if (!this.recognition) {
            console.error('Speech Recognition başlatılamadı');
            return false;
        }
        
        if (this.isListening) {
            console.warn('Zaten dinleme modunda');
            return false;
        }
        
        try {
            this.isListening = true;
            this.recognition.start();
            console.log('✅ Sürekli dinleme başlatıldı');
            return true;
        } catch (e) {
            console.error('Başlatma hatası:', e);
            this.isListening = false;
            return false;
        }
    }
    
    /**
     * Dinlemeyi durdur
     */
    stop() {
        if (!this.isListening) {
            console.warn('Zaten durdurulmuş');
            return false;
        }
        
        this.isListening = false;
        
        if (this.silenceTimer) {
            clearTimeout(this.silenceTimer);
        }
        
        try {
            this.recognition.stop();
            console.log('⏹️ Dinleme durduruldu');
            return true;
        } catch (e) {
            console.error('Durdurma hatası:', e);
            return false;
        }
    }
    
    /**
     * Dinleme durumunu kontrol et
     */
    getIsListening() {
        return this.isListening;
    }
}

// Global kullanım için
window.ContinuousSpeech = ContinuousSpeech;
