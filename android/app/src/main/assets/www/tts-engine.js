/**
 * TTS Motor - Text-to-Speech Engine
 * Türkçe sesli asistan için gelişmiş TTS yönetimi
 * Birden fazla ses modeli desteği
 */

class TTSEngine {
    constructor() {
        this.synth = window.speechSynthesis;
        this.voices = [];
        this.turkishVoices = [];
        this.allVoices = [];
        this.selectedVoice = null;
        this.settings = {
            rate: 1.0,      // Konuşma hızı (0.1 - 10)
            pitch: 1.0,     // Ses tonu (0 - 2)
            volume: 1.0,    // Ses seviyesi (0 - 1)
            voiceURI: null, // Seçili ses
            language: 'tr-TR' // Dil
        };
        
        // Gelişmiş özellikler
        this.voiceCategories = {
            turkish: [],
            female: [],
            male: [],
            local: [],
            cloud: [],
            premium: []
        };
        
        this.voiceMetadata = new Map(); // Her ses için detaylı bilgi
        this.voiceRatings = new Map(); // Kullanıcı değerlendirmeleri
        this.usageStats = new Map(); // Kullanım istatistikleri
        
        // Callback fonksiyonları
        this.onVoicesLoaded = null;
        this.onSpeakStart = null;
        this.onSpeakEnd = null;
        this.onSpeakProgress = null;
        this.onSpeakPause = null;
        this.onSpeakResume = null;
        this.onError = null;
        this.onVoiceChange = null;
        
        // Durum yönetimi
        this.isInitialized = false;
        this.isSpeaking = false;
        this.isPaused = false;
        this.queue = [];
        this.currentUtterance = null;
        this.speechHistory = [];
        
        // Performans metrikleri
        this.metrics = {
            totalSpeeches: 0,
            totalDuration: 0,
            averageRate: 0,
            errorCount: 0,
            lastSpeechTime: null
        };
        
        this.init();
    }
    
    /**
     * TTS motorunu başlat
     */
    init() {
        if (!this.synth) {
            console.error('❌ TTS desteklenmiyor!');
            if (this.onError) this.onError('TTS_NOT_SUPPORTED');
            return false;
        }
        
        // Sesleri yükle
        this.loadVoices();
        
        // Sesler değiştiğinde yeniden yükle (Android için önemli)
        if (this.synth.onvoiceschanged !== undefined) {
            this.synth.onvoiceschanged = () => {
                this.loadVoices();
            };
        }
        
        // Ayarları localStorage'dan yükle
        this.loadSettings();
        
        return true;
    }
    
    /**
     * Mevcut sesleri yükle, analiz et ve kategorize et
     */
    loadVoices() {
        this.voices = this.synth.getVoices();
        this.allVoices = [...this.voices];
        
        console.log(`🔍 Toplam ${this.voices.length} ses taranıyor...`);
        
        // Her sesi analiz et ve kategorize et
        this.voices.forEach(voice => {
            this.analyzeVoice(voice);
        });
        
        // Türkçe sesleri filtrele (geniş kriter)
        this.turkishVoices = this.voices.filter(voice => 
            voice.lang.startsWith('tr') || 
            voice.lang.startsWith('tr-TR') ||
            voice.lang.toLowerCase().includes('turkish') ||
            voice.lang.toLowerCase().includes('türkçe') ||
            voice.name.toLowerCase().includes('turkish') ||
            voice.name.toLowerCase().includes('türkçe') ||
            voice.name.toLowerCase().includes('turk')
        );
        
        // Türkçe ses yoksa TÜM sesleri göster
        if (this.turkishVoices.length === 0) {
            console.warn('⚠️ Türkçe ses bulunamadı, TÜM sesler kullanılabilir hale getiriliyor');
            this.turkishVoices = this.voices;
        }
        
        // Gelişmiş sıralama algoritması
        this.turkishVoices.sort((a, b) => {
            const aScore = this.calculateVoiceQualityScore(a);
            const bScore = this.calculateVoiceQualityScore(b);
            return bScore - aScore; // Yüksek skordan düşüğe
        });
        
        // İstatistikleri hesapla
        const stats = this.calculateVoiceStatistics();
        
        console.log(`✅ ${this.turkishVoices.length} ses hazır`);
        console.log(`📊 İstatistikler:`, stats);
        console.log(`📋 Tüm Sesler:`, this.turkishVoices.map(v => 
            `${v.name} (${v.lang}) ${v.localService ? '📱' : '☁️'}`
        ));
        
        // Varsayılan ses seçimi (akıllı algoritma)
        if (!this.selectedVoice && this.turkishVoices.length > 0) {
            this.selectedVoice = this.selectBestVoice();
            this.settings.voiceURI = this.selectedVoice.voiceURI;
        }
        
        this.isInitialized = true;
        
        // Callback tetikle
        if (this.onVoicesLoaded) {
            this.onVoicesLoaded(this.turkishVoices);
        }
        
        return this.turkishVoices;
    }
    
    /**
     * Sesi analiz et ve metadata oluştur
     */
    analyzeVoice(voice) {
        const metadata = {
            id: voice.voiceURI,
            name: voice.name,
            lang: voice.lang,
            isLocal: voice.localService,
            isDefault: voice.default,
            
            // Cinsiyet tahmini (isimden)
            gender: this.detectGender(voice.name),
            
            // Kalite tahmini
            quality: this.estimateQuality(voice),
            
            // Platform bilgisi
            platform: this.detectPlatform(voice.name),
            
            // Türkçe desteği
            turkishSupport: this.checkTurkishSupport(voice),
            
            // Kullanım sayısı
            usageCount: 0,
            
            // Kullanıcı değerlendirmesi
            rating: 0,
            ratingCount: 0
        };
        
        this.voiceMetadata.set(voice.voiceURI, metadata);
        
        // Kategorize et
        if (metadata.turkishSupport) this.voiceCategories.turkish.push(voice);
        if (metadata.gender === 'female') this.voiceCategories.female.push(voice);
        if (metadata.gender === 'male') this.voiceCategories.male.push(voice);
        if (metadata.isLocal) this.voiceCategories.local.push(voice);
        if (!metadata.isLocal) this.voiceCategories.cloud.push(voice);
        if (metadata.quality >= 8) this.voiceCategories.premium.push(voice);
    }
    
    /**
     * Cinsiyet algılama
     */
    detectGender(name) {
        const nameLower = name.toLowerCase();
        
        // Kadın sesi işaretleri
        const femaleKeywords = ['female', 'woman', 'kadın', 'kız', 'bayan', 'melina', 'aylin', 'elif', 
                                'zeynep', 'selin', 'ece', 'nisa', 'filiz', 'emel', 'samantha', 'victoria', 
                                'karen', 'susan', 'moira', 'karen', 'fiona', 'alice', 'amelie'];
        
        // Erkek sesi işaretleri
        const maleKeywords = ['male', 'man', 'erkek', 'bay', 'ahmet', 'mehmet', 'ali', 'murat', 
                              'emre', 'cem', 'daniel', 'alex', 'thomas', 'oliver', 'fred'];
        
        for (const keyword of femaleKeywords) {
            if (nameLower.includes(keyword)) return 'female';
        }
        
        for (const keyword of maleKeywords) {
            if (nameLower.includes(keyword)) return 'male';
        }
        
        return 'unknown';
    }
    
    /**
     * Kalite tahmini (1-10 skala)
     */
    estimateQuality(voice) {
        let score = 5; // Başlangıç skoru
        
        const nameLower = voice.name.toLowerCase();
        
        // Premium sesler
        if (nameLower.includes('premium')) score += 3;
        if (nameLower.includes('neural')) score += 2;
        if (nameLower.includes('wavenet')) score += 2;
        if (nameLower.includes('google')) score += 1;
        if (nameLower.includes('microsoft')) score += 1;
        if (nameLower.includes('amazon')) score += 1;
        if (nameLower.includes('enhanced')) score += 1;
        if (nameLower.includes('natural')) score += 1;
        
        // Yerel sesler genelde daha iyi
        if (voice.localService) score += 1;
        
        // Türkçe ses ise bonus
        if (voice.lang.startsWith('tr')) score += 1;
        
        // Varsayılan ses ise bonus
        if (voice.default) score += 0.5;
        
        return Math.min(10, Math.max(1, score));
    }
    
    /**
     * Platform algılama
     */
    detectPlatform(name) {
        const nameLower = name.toLowerCase();
        
        if (nameLower.includes('google')) return 'Google';
        if (nameLower.includes('microsoft')) return 'Microsoft';
        if (nameLower.includes('amazon')) return 'Amazon';
        if (nameLower.includes('apple')) return 'Apple';
        if (nameLower.includes('samsung')) return 'Samsung';
        
        return 'System';
    }
    
    /**
     * Türkçe desteği kontrolü
     */
    checkTurkishSupport(voice) {
        const lang = voice.lang.toLowerCase();
        const name = voice.name.toLowerCase();
        
        return lang.includes('tr') || 
               lang.includes('turkish') ||
               name.includes('turkish') ||
               name.includes('türkçe') ||
               name.includes('turk');
    }
    
    /**
     * Ses kalite skoru hesaplama
     */
    calculateVoiceQualityScore(voice) {
        let score = 0;
        
        const metadata = this.voiceMetadata.get(voice.voiceURI);
        if (!metadata) return 0;
        
        // Kalite puanı (ağırlık: 40%)
        score += metadata.quality * 4;
        
        // Türkçe desteği (ağırlık: 30%)
        if (metadata.turkishSupport) score += 30;
        
        // Yerel ses (ağırlık: 15%)
        if (metadata.isLocal) score += 15;
        
        // Kullanıcı değerlendirmesi (ağırlık: 10%)
        score += metadata.rating;
        
        // Kullanım sıklığı (ağırlık: 5%)
        score += Math.min(5, metadata.usageCount / 10);
        
        return score;
    }
    
    /**
     * İstatistik hesaplama
     */
    calculateVoiceStatistics() {
        return {
            total: this.turkishVoices.length,
            turkish: this.voiceCategories.turkish.length,
            local: this.voiceCategories.local.length,
            cloud: this.voiceCategories.cloud.length,
            female: this.voiceCategories.female.length,
            male: this.voiceCategories.male.length,
            premium: this.voiceCategories.premium.length,
            platforms: this.getPlatformDistribution()
        };
    }
    
    /**
     * Platform dağılımı
     */
    getPlatformDistribution() {
        const platforms = {};
        this.turkishVoices.forEach(voice => {
            const metadata = this.voiceMetadata.get(voice.voiceURI);
            if (metadata) {
                platforms[metadata.platform] = (platforms[metadata.platform] || 0) + 1;
            }
        });
        return platforms;
    }
    
    /**
     * En iyi sesi akıllı seç
     */
    selectBestVoice() {
        // Kayıtlı tercih varsa
        if (this.settings.voiceURI) {
            const saved = this.turkishVoices.find(v => v.voiceURI === this.settings.voiceURI);
            if (saved) return saved;
        }
        
        // Öncelik sırası:
        // 1. Yerel Türkçe ses
        const localTurkish = this.turkishVoices.find(v => 
            v.localService && v.lang.startsWith('tr')
        );
        if (localTurkish) return localTurkish;
        
        // 2. Bulut Türkçe ses
        const cloudTurkish = this.turkishVoices.find(v => 
            !v.localService && v.lang.startsWith('tr')
        );
        if (cloudTurkish) return cloudTurkish;
        
        // 3. En yüksek skorlu ses
        return this.turkishVoices[0];
    }
    
    /**
     * Metni sesli söyle
     * @param {string} text - Söylenecek metin
     * @param {Object} options - Opsiyonel ayarlar (rate, pitch, volume)
     * @returns {Promise}
     */
    speak(text, options = {}) {
        return new Promise((resolve, reject) => {
            if (!text || text.trim().length === 0) {
                console.warn('⚠️ Boş metin, TTS atlanıyor');
                resolve();
                return;
            }
            
            if (!this.isInitialized) {
                console.warn('⚠️ TTS henüz hazır değil, bekleniyor...');
                setTimeout(() => this.speak(text, options).then(resolve).catch(reject), 100);
                return;
            }
            
            // Önce mevcut konuşmayı durdur (opsiyonel)
            if (options.interrupt !== false) {
                this.stop();
            }
            
            const utterance = new SpeechSynthesisUtterance(text);
            
            // Ses ayarları
            utterance.voice = this.selectedVoice;
            utterance.rate = options.rate ?? this.settings.rate;
            utterance.pitch = options.pitch ?? this.settings.pitch;
            utterance.volume = options.volume ?? this.settings.volume;
            utterance.lang = 'tr-TR';
            
            // Olaylar
            utterance.onstart = () => {
                this.isSpeaking = true;
                console.log('🔊 Konuşuyor:', text.substring(0, 50));
                if (this.onSpeakStart) this.onSpeakStart(text);
            };
            
            utterance.onend = () => {
                this.isSpeaking = false;
                console.log('✅ Konuşma bitti');
                if (this.onSpeakEnd) this.onSpeakEnd(text);
                resolve();
            };
            
            utterance.onerror = (event) => {
                this.isSpeaking = false;
                console.error('❌ TTS hatası:', event.error);
                if (this.onError) this.onError(event.error);
                reject(event.error);
            };
            
            // Konuşmayı başlat
            try {
                this.synth.speak(utterance);
            } catch (error) {
                console.error('❌ TTS speak hatası:', error);
                this.isSpeaking = false;
                reject(error);
            }
        });
    }
    
    /**
     * Konuşmayı durdur
     */
    stop() {
        if (this.synth.speaking || this.synth.pending) {
            this.synth.cancel();
            this.isSpeaking = false;
            console.log('⏹️ TTS durduruldu');
        }
    }
    
    /**
     * Konuşmayı duraklat
     */
    pause() {
        if (this.synth.speaking && !this.synth.paused) {
            this.synth.pause();
            console.log('⏸️ TTS duraklatıldı');
        }
    }
    
    /**
     * Konuşmaya devam et
     */
    resume() {
        if (this.synth.paused) {
            this.synth.resume();
            console.log('▶️ TTS devam ediyor');
        }
    }
    
    /**
     * Ses değiştir
     * @param {string} voiceURI - Sesin URI'si
     */
    setVoice(voiceURI) {
        const voice = this.voices.find(v => v.voiceURI === voiceURI);
        if (voice) {
            this.selectedVoice = voice;
            this.settings.voiceURI = voiceURI;
            this.saveSettings();
            console.log('🎤 Ses değiştirildi:', voice.name);
            return true;
        }
        console.error('❌ Ses bulunamadı:', voiceURI);
        return false;
    }
    
    /**
     * Konuşma hızını ayarla (0.1 - 10)
     */
    setRate(rate) {
        this.settings.rate = Math.max(0.1, Math.min(10, rate));
        this.saveSettings();
        console.log('⚡ Hız:', this.settings.rate);
    }
    
    /**
     * Ses tonunu ayarla (0 - 2)
     */
    setPitch(pitch) {
        this.settings.pitch = Math.max(0, Math.min(2, pitch));
        this.saveSettings();
        console.log('🎵 Ton:', this.settings.pitch);
    }
    
    /**
     * Ses seviyesini ayarla (0 - 1)
     */
    setVolume(volume) {
        this.settings.volume = Math.max(0, Math.min(1, volume));
        this.saveSettings();
        console.log('🔊 Seviye:', this.settings.volume);
    }
    
    /**
     * Mevcut sesi getir
     */
    getCurrentVoice() {
        return this.selectedVoice;
    }
    
    /**
     * Türkçe sesleri getir
     */
    getTurkishVoices() {
        return this.turkishVoices;
    }
    
    /**
     * TTS durumunu getir
     */
    getStatus() {
        return {
            isInitialized: this.isInitialized,
            isSpeaking: this.isSpeaking,
            isPaused: this.synth.paused,
            isPending: this.synth.pending,
            selectedVoice: this.selectedVoice?.name,
            settings: this.settings
        };
    }
    
    /**
     * Ayarları localStorage'a kaydet
     */
    saveSettings() {
        try {
            localStorage.setItem('tts_settings', JSON.stringify(this.settings));
            console.log('💾 TTS ayarları kaydedildi');
        } catch (error) {
            console.error('❌ Ayarlar kaydedilemedi:', error);
        }
    }
    
    /**
     * Ayarları localStorage'dan yükle
     */
    loadSettings() {
        try {
            const saved = localStorage.getItem('tts_settings');
            if (saved) {
                this.settings = { ...this.settings, ...JSON.parse(saved) };
                console.log('📂 TTS ayarları yüklendi:', this.settings);
            }
        } catch (error) {
            console.error('❌ Ayarlar yüklenemedi:', error);
        }
    }
    
    /**
     * Test konuşması
     */
    test(text = 'Merhaba, ben sesli asistanınızım. Sesim nasıl?') {
        return this.speak(text);
    }
}

// Global kullanım için
window.TTSEngine = TTSEngine;
