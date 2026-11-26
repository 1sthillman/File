/**
 * Native Android TTS Engine - Profesyonel Entegrasyon
 * Tüm Android TTS seslerine erişim, gelişmiş özellikler, akıllı yönetim
 */

class NativeTTSEngine {
    constructor() {
        this.isNativeAvailable = false;
        this.isReady = false;
        this.voices = [];
        this.allVoices = [];
        this.turkishVoices = [];
        this.selectedVoice = null;
        
        this.settings = {
            rate: 1.0,      // 0.1 - 3.0
            pitch: 1.0,     // 0.1 - 2.0
            volume: 1.0,    // 0 - 1
            voiceName: null,
            language: 'tr-TR'
        };
        
        // Gelişmiş özellikler
        this.voiceCategories = {
            turkish: [],
            female: [],
            male: [],
            local: [],
            cloud: [],
            premium: [],
            google: [],
            samsung: []
        };
        
        this.voiceMetadata = new Map();
        this.voiceRatings = new Map();
        this.usageStats = new Map();
        this.favorites = new Set();
        
        // Callback'ler
        this.onReady = null;
        this.onVoicesLoaded = null;
        this.onSpeechStart = null;
        this.onSpeechEnd = null;
        this.onSpeechError = null;
        this.onVoiceChange = null;
        
        // Durum
        this.isSpeaking = false;
        this.currentUtteranceId = null;
        this.speechQueue = [];
        this.autoQueue = false;
        
        // Metrikler
        this.metrics = {
            totalSpeeches: 0,
            totalDuration: 0,
            successCount: 0,
            errorCount: 0,
            lastSpeechTime: null,
            averageRate: 0
        };
        
        this.init();
    }
    
    /**
     * Sistemi başlat
     */
    init() {
        console.log('🎤 Native TTS Engine başlatılıyor...');
        
        // Android kontrolü
        if (typeof NativeTTS === 'undefined') {
            console.warn('⚠️ NativeTTS bulunamadı');
            this.isNativeAvailable = false;
            return false;
        }
        
        this.isNativeAvailable = true;
        
        // Native event listener
        window.onNativeTTSEvent = (event, data) => {
            this.handleNativeEvent(event, data);
        };
        
        // Sesleri yükle
        this.loadVoices();
        
        // Ayarları yükle
        this.loadSettings();
        
        return true;
    }
    
    /**
     * Native event'leri işle
     */
    handleNativeEvent(event, data) {
        console.log(`📡 Native TTS Event: ${event}`, data);
        
        switch(event) {
            case 'ready':
                this.isReady = true;
                console.log('✅ Native TTS hazır');
                if (this.onReady) this.onReady();
                break;
                
            case 'start':
                this.isSpeaking = true;
                this.currentUtteranceId = data;
                this.metrics.totalSpeeches++;
                this.metrics.lastSpeechTime = Date.now();
                if (this.onSpeechStart) this.onSpeechStart(data);
                break;
                
            case 'end':
                this.isSpeaking = false;
                this.currentUtteranceId = null;
                this.metrics.successCount++;
                
                // Süre hesapla
                if (this.metrics.lastSpeechTime) {
                    const duration = Date.now() - this.metrics.lastSpeechTime;
                    this.metrics.totalDuration += duration;
                }
                
                if (this.onSpeechEnd) this.onSpeechEnd(data);
                
                // Kuyruk varsa devam et
                this.processQueue();
                break;
                
            case 'error':
                this.isSpeaking = false;
                this.currentUtteranceId = null;
                this.metrics.errorCount++;
                console.error('❌ TTS Hatası:', data);
                if (this.onSpeechError) this.onSpeechError(data);
                
                // Kuyruk varsa devam et
                this.processQueue();
                break;
                
            case 'stopped':
                this.isSpeaking = false;
                this.currentUtteranceId = null;
                this.speechQueue = []; // Kuyruğu temizle
                break;
                
            case 'voice_changed':
                console.log('🎵 Ses değiştirildi:', data);
                if (this.onVoiceChange) this.onVoiceChange(data);
                break;
        }
    }
    
    /**
     * Sesleri yükle ve analiz et
     */
    loadVoices() {
        if (!this.isNativeAvailable) {
            console.warn('Native TTS kullanılamıyor');
            return [];
        }
        
        try {
            console.log('🔍 Native sesler yükleniyor...');
            
            // TÜM sesleri al
            const voicesJson = NativeTTS.getAllVoices();
            this.allVoices = JSON.parse(voicesJson);
            
            console.log(`📋 Toplam ${this.allVoices.length} native ses bulundu`);
            
            // Her sesi analiz et
            this.allVoices.forEach(voice => {
                this.analyzeVoice(voice);
            });
            
            // Türkçe sesleri filtrele
            this.turkishVoices = this.allVoices.filter(voice => 
                this.isTurkishVoice(voice)
            );
            
            // Kategorize et
            this.categorizeVoices();
            
            // Sırala (kalite, yerel öncelik)
            this.turkishVoices.sort((a, b) => {
                return this.calculateVoiceScore(b) - this.calculateVoiceScore(a);
            });
            
            this.voices = this.turkishVoices;
            
            console.log(`🇹🇷 ${this.turkishVoices.length} Türkçe ses bulundu`);
            console.log('📊 Kategoriler:', {
                turkish: this.voiceCategories.turkish.length,
                local: this.voiceCategories.local.length,
                cloud: this.voiceCategories.cloud.length,
                premium: this.voiceCategories.premium.length,
                google: this.voiceCategories.google.length,
                samsung: this.voiceCategories.samsung.length,
                female: this.voiceCategories.female.length,
                male: this.voiceCategories.male.length
            });
            
            // Varsayılan ses seç
            if (this.voices.length > 0 && !this.selectedVoice) {
                this.selectedVoice = this.selectBestVoice();
                if (this.selectedVoice) {
                    this.setVoice(this.selectedVoice.name);
                }
            }
            
            // Callback tetikle
            if (this.onVoicesLoaded) {
                this.onVoicesLoaded(this.voices);
            }
            
            return this.voices;
            
        } catch (error) {
            console.error('Sesler yüklenirken hata:', error);
            return [];
        }
    }
    
    /**
     * Ses Türkçe mi kontrol et
     */
    isTurkishVoice(voice) {
        const locale = voice.locale.toLowerCase();
        const name = voice.name.toLowerCase();
        
        return locale.includes('tr') ||
               locale.includes('turkish') ||
               locale.includes('türk') ||
               name.includes('turkish') ||
               name.includes('türk') ||
               name.includes('turk');
    }
    
    /**
     * Sesi analiz et ve metadata oluştur
     */
    analyzeVoice(voice) {
        const metadata = {
            name: voice.name,
            locale: voice.locale,
            quality: voice.quality || 300, // Android TTS quality codes
            latency: voice.latency || 300,
            isLocal: !voice.requiresNetworkConnection,
            features: voice.features || [],
            
            // Cinsiyet tahmini
            gender: this.detectGender(voice.name),
            
            // Platform
            platform: this.detectPlatform(voice.name),
            
            // Türkçe desteği
            turkishSupport: this.isTurkishVoice(voice),
            
            // Kalite skoru (1-10)
            qualityScore: this.calculateQualityScore(voice),
            
            // Kullanım
            usageCount: this.usageStats.get(voice.name) || 0,
            rating: this.voiceRatings.get(voice.name) || 0,
            isFavorite: this.favorites.has(voice.name)
        };
        
        this.voiceMetadata.set(voice.name, metadata);
    }
    
    /**
     * Cinsiyet algılama
     */
    detectGender(name) {
        const nameLower = name.toLowerCase();
        
        // Kadın işaretleri
        const femaleKeywords = ['female', 'woman', 'kadın', 'bayan', 'melina', 'elif', 
                                'zeynep', 'ece', 'ayşe', 'fatma', 'emine'];
        
        // Erkek işaretleri
        const maleKeywords = ['male', 'man', 'erkek', 'bay', 'ahmet', 'mehmet', 
                              'ali', 'murat', 'cem'];
        
        for (const kw of femaleKeywords) {
            if (nameLower.includes(kw)) return 'female';
        }
        
        for (const kw of maleKeywords) {
            if (nameLower.includes(kw)) return 'male';
        }
        
        return 'unknown';
    }
    
    /**
     * Platform algılama
     */
    detectPlatform(name) {
        const nameLower = name.toLowerCase();
        
        if (nameLower.includes('google')) return 'Google';
        if (nameLower.includes('samsung')) return 'Samsung';
        if (nameLower.includes('pico')) return 'Pico';
        if (nameLower.includes('espeak')) return 'eSpeak';
        
        return 'System';
    }
    
    /**
     * Kalite skoru hesapla (1-10)
     */
    calculateQualityScore(voice) {
        let score = 5;
        
        // Android TTS quality: 500 = çok yüksek, 400 = yüksek, 300 = normal, 200 = düşük
        if (voice.quality >= 500) score = 10;
        else if (voice.quality >= 400) score = 8;
        else if (voice.quality >= 300) score = 6;
        else score = 4;
        
        // Yerel ses bonusu
        if (!voice.requiresNetworkConnection) score += 1;
        
        // Düşük latency bonusu
        if (voice.latency < 300) score += 0.5;
        
        // Platform bonusu
        const name = voice.name.toLowerCase();
        if (name.includes('google')) score += 1;
        if (name.includes('premium')) score += 0.5;
        if (name.includes('enhanced')) score += 0.5;
        
        return Math.min(10, Math.max(1, score));
    }
    
    /**
     * Ses skoru hesapla (sıralama için)
     */
    calculateVoiceScore(voice) {
        const metadata = this.voiceMetadata.get(voice.name);
        if (!metadata) return 0;
        
        let score = 0;
        
        // Kalite (40%)
        score += metadata.qualityScore * 4;
        
        // Türkçe desteği (30%)
        if (metadata.turkishSupport) score += 30;
        
        // Yerel (15%)
        if (metadata.isLocal) score += 15;
        
        // Kullanım sıklığı (10%)
        score += Math.min(10, metadata.usageCount / 10);
        
        // Rating (5%)
        score += metadata.rating;
        
        // Favori bonusu
        if (metadata.isFavorite) score += 20;
        
        return score;
    }
    
    /**
     * Sesleri kategorize et
     */
    categorizeVoices() {
        this.allVoices.forEach(voice => {
            const metadata = this.voiceMetadata.get(voice.name);
            if (!metadata) return;
            
            if (metadata.turkishSupport) this.voiceCategories.turkish.push(voice);
            if (metadata.gender === 'female') this.voiceCategories.female.push(voice);
            if (metadata.gender === 'male') this.voiceCategories.male.push(voice);
            if (metadata.isLocal) this.voiceCategories.local.push(voice);
            if (!metadata.isLocal) this.voiceCategories.cloud.push(voice);
            if (metadata.qualityScore >= 8) this.voiceCategories.premium.push(voice);
            if (metadata.platform === 'Google') this.voiceCategories.google.push(voice);
            if (metadata.platform === 'Samsung') this.voiceCategories.samsung.push(voice);
        });
    }
    
    /**
     * En iyi sesi seç
     */
    selectBestVoice() {
        // Kayıtlı tercih
        if (this.settings.voiceName) {
            const saved = this.voices.find(v => v.name === this.settings.voiceName);
            if (saved) return saved;
        }
        
        // Favori Türkçe ses
        const favoriteTurkish = this.voices.find(v => 
            this.favorites.has(v.name) && this.isTurkishVoice(v)
        );
        if (favoriteTurkish) return favoriteTurkish;
        
        // Yerel Türkçe premium
        const localTurkishPremium = this.voices.find(v => {
            const meta = this.voiceMetadata.get(v.name);
            return meta && meta.isLocal && meta.turkishSupport && meta.qualityScore >= 8;
        });
        if (localTurkishPremium) return localTurkishPremium;
        
        // Yerel Türkçe
        const localTurkish = this.voices.find(v => {
            const meta = this.voiceMetadata.get(v.name);
            return meta && meta.isLocal && meta.turkishSupport;
        });
        if (localTurkish) return localTurkish;
        
        // Herhangi bir Türkçe
        const anyTurkish = this.voices.find(v => this.isTurkishVoice(v));
        if (anyTurkish) return anyTurkish;
        
        // İlk ses
        return this.voices[0];
    }
    
    /**
     * Ses değiştir
     */
    setVoice(voiceName) {
        if (!this.isNativeAvailable) return false;
        
        try {
            const result = NativeTTS.setVoice(voiceName);
            
            if (result) {
                const voice = this.voices.find(v => v.name === voiceName);
                this.selectedVoice = voice;
                this.settings.voiceName = voiceName;
                this.saveSettings();
                
                console.log('✅ Ses değiştirildi:', voiceName);
                return true;
            }
            
            return false;
            
        } catch (error) {
            console.error('Ses değiştirme hatası:', error);
            return false;
        }
    }
    
    /**
     * Konuş
     */
    speak(text, options = {}) {
        return new Promise((resolve, reject) => {
            if (!this.isNativeAvailable) {
                reject('Native TTS kullanılamıyor');
                return;
            }
            
            if (!text || text.trim().length === 0) {
                console.warn('Boş metin');
                resolve();
                return;
            }
            
            // Kuyruk sistemi
            if (this.autoQueue && this.isSpeaking) {
                this.speechQueue.push({ text, options, resolve, reject });
                console.log('📝 Kuyruğa eklendi:', text.substring(0, 30));
                return;
            }
            
            // Parametreler
            const rate = options.rate ?? this.settings.rate;
            const pitch = options.pitch ?? this.settings.pitch;
            
            try {
                const utteranceId = NativeTTS.speak(text, rate, pitch);
                
                if (utteranceId) {
                    this.currentUtteranceId = utteranceId;
                    
                    // Kullanım sayısını artır
                    if (this.selectedVoice) {
                        const count = this.usageStats.get(this.selectedVoice.name) || 0;
                        this.usageStats.set(this.selectedVoice.name, count + 1);
                    }
                    
                    // Metrik güncelle
                    this.metrics.averageRate = (this.metrics.averageRate * (this.metrics.totalSpeeches - 1) + rate) / this.metrics.totalSpeeches;
                    
                    resolve(utteranceId);
                } else {
                    reject('TTS başarısız');
                }
                
            } catch (error) {
                console.error('Speak hatası:', error);
                reject(error);
            }
        });
    }
    
    /**
     * Kuyruğu işle
     */
    processQueue() {
        if (this.speechQueue.length === 0) return;
        
        const next = this.speechQueue.shift();
        this.speak(next.text, next.options)
            .then(next.resolve)
            .catch(next.reject);
    }
    
    /**
     * Durdur
     */
    stop() {
        if (!this.isNativeAvailable) return;
        
        try {
            NativeTTS.stop();
            this.speechQueue = [];
            console.log('⏹️ TTS durduruldu');
        } catch (error) {
            console.error('Stop hatası:', error);
        }
    }
    
    /**
     * Parametreleri ayarla
     */
    setRate(rate) {
        this.settings.rate = Math.max(0.1, Math.min(3.0, rate));
        this.saveSettings();
    }
    
    setPitch(pitch) {
        this.settings.pitch = Math.max(0.1, Math.min(2.0, pitch));
        this.saveSettings();
    }
    
    setVolume(volume) {
        this.settings.volume = Math.max(0, Math.min(1, volume));
        this.saveSettings();
    }
    
    /**
     * Favori ekle/çıkar
     */
    toggleFavorite(voiceName) {
        if (this.favorites.has(voiceName)) {
            this.favorites.delete(voiceName);
        } else {
            this.favorites.add(voiceName);
        }
        this.saveFavorites();
    }
    
    /**
     * Durum bilgisi
     */
    getStatus() {
        return {
            isNativeAvailable: this.isNativeAvailable,
            isReady: this.isReady,
            isSpeaking: this.isSpeaking,
            selectedVoice: this.selectedVoice?.name,
            voiceCount: this.voices.length,
            turkishVoiceCount: this.turkishVoices.length,
            settings: this.settings,
            metrics: this.metrics,
            queueLength: this.speechQueue.length
        };
    }
    
    /**
     * İstatistikler
     */
    getStatistics() {
        return {
            voices: {
                total: this.allVoices.length,
                turkish: this.turkishVoices.length,
                local: this.voiceCategories.local.length,
                cloud: this.voiceCategories.cloud.length,
                premium: this.voiceCategories.premium.length,
                google: this.voiceCategories.google.length,
                samsung: this.voiceCategories.samsung.length,
                female: this.voiceCategories.female.length,
                male: this.voiceCategories.male.length
            },
            usage: {
                totalSpeeches: this.metrics.totalSpeeches,
                successCount: this.metrics.successCount,
                errorCount: this.metrics.errorCount,
                successRate: this.metrics.totalSpeeches > 0 
                    ? (this.metrics.successCount / this.metrics.totalSpeeches * 100).toFixed(1) + '%'
                    : '0%',
                averageRate: this.metrics.averageRate.toFixed(2),
                totalDuration: (this.metrics.totalDuration / 1000).toFixed(1) + 's'
            },
            favorites: Array.from(this.favorites)
        };
    }
    
    /**
     * Ayarları kaydet
     */
    saveSettings() {
        try {
            localStorage.setItem('native_tts_settings', JSON.stringify(this.settings));
        } catch (error) {
            console.error('Ayarlar kaydedilemedi:', error);
        }
    }
    
    /**
     * Ayarları yükle
     */
    loadSettings() {
        try {
            const saved = localStorage.getItem('native_tts_settings');
            if (saved) {
                this.settings = { ...this.settings, ...JSON.parse(saved) };
            }
        } catch (error) {
            console.error('Ayarlar yüklenemedi:', error);
        }
    }
    
    /**
     * Favorileri kaydet
     */
    saveFavorites() {
        try {
            localStorage.setItem('native_tts_favorites', JSON.stringify(Array.from(this.favorites)));
        } catch (error) {
            console.error('Favoriler kaydedilemedi:', error);
        }
    }
    
    /**
     * Favorileri yükle
     */
    loadFavorites() {
        try {
            const saved = localStorage.getItem('native_tts_favorites');
            if (saved) {
                this.favorites = new Set(JSON.parse(saved));
            }
        } catch (error) {
            console.error('Favoriler yüklenemedi:', error);
        }
    }
    
    /**
     * Test konuşması
     */
    test(text) {
        const testText = text || 'Merhaba, ben native Android sesli asistanıyım. Sesim nasıl?';
        return this.speak(testText);
    }
}

// Global kullanım için
window.NativeTTSEngine = NativeTTSEngine;
