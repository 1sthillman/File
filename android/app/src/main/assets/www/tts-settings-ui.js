/**
 * TTS Ayarları Kullanıcı Arayüzü - Profesyonel Versiyon
 * Gelişmiş ses yönetimi ve kontrol sistemi
 */

class TTSSettingsUI {
    constructor(ttsEngine) {
        this.tts = ttsEngine;
        this.isOpen = false;
        this.modal = null;
        this.currentCategory = 'all';
        this.searchQuery = '';
        this.sortBy = 'quality'; // quality, name, usage
        this.viewMode = 'grid'; // grid, list
        
        this.init();
    }
    
    /**
     * Ayarlar UI'ını oluştur
     */
    init() {
        // Modal HTML'i oluştur
        this.createAdvancedModal();
        
        // TTS sesleri yüklendiğinde UI'ı güncelle
        this.tts.onVoicesLoaded = (voices) => {
            this.updateCompleteVoiceDisplay(voices);
        };
        
        // Sesler hazırsa hemen yükle
        if (this.tts.isInitialized) {
            this.updateCompleteVoiceDisplay(this.tts.getTurkishVoices());
        }
    }
    
    /**
     * Gelişmiş Modal yapısı
     */
    createAdvancedModal() {
        const modalHTML = `
            <div id="tts-settings-modal" class="tts-modal-advanced" style="display: none;">
                <div class="tts-modal-overlay"></div>
                <div class="tts-modal-container">
                    <!-- Sidebar -->
                    <div class="tts-sidebar">
                        <div class="tts-sidebar-header">
                            <div class="tts-logo">🎤</div>
                            <div class="tts-logo-text">
                                <div class="tts-logo-title">TTS Manager</div>
                                <div class="tts-logo-subtitle">Pro</div>
                            </div>
                        </div>
                        
                        <nav class="tts-nav">
                            <div class="tts-nav-item active" data-tab="voices">
                                <span class="tts-nav-icon">🗣️</span>
                                <span class="tts-nav-label">Sesler</span>
                                <span class="tts-nav-badge" id="sidebar-voice-count">0</span>
                            </div>
                            <div class="tts-nav-item" data-tab="settings">
                                <span class="tts-nav-icon">⚙️</span>
                                <span class="tts-nav-label">Ayarlar</span>
                            </div>
                            <div class="tts-nav-item" data-tab="stats">
                                <span class="tts-nav-icon">📊</span>
                                <span class="tts-nav-label">İstatistikler</span>
                            </div>
                            <div class="tts-nav-item" data-tab="favorites">
                                <span class="tts-nav-icon">⭐</span>
                                <span class="tts-nav-label">Favoriler</span>
                                <span class="tts-nav-badge" id="sidebar-fav-count">0</span>
                            </div>
                        </nav>
                        
                        <div class="tts-sidebar-footer">
                            <button class="tts-sidebar-btn" onclick="ttsSettingsUI.exportSettings()">
                                💾 Ayarları Dışa Aktar
                            </button>
                            <button class="tts-sidebar-btn" onclick="ttsSettingsUI.importSettings()">
                                📥 Ayarları İçe Aktar
                            </button>
                        </div>
                    </div>
                    
                    <!-- Main Content -->
                    <div class="tts-main-content">
                        <div class="tts-content-header">
                            <div class="tts-content-title" id="content-title">Ses Yönetimi</div>
                            <button class="tts-close-button" onclick="ttsSettingsUI.close()">
                                <span>✕</span>
                            </button>
                        </div>
                        
                        <!-- Voices Tab -->
                        <div class="tts-tab-content active" data-tab-content="voices">
                            <!-- Toolbar -->
                            <div class="tts-toolbar">
                                <div class="tts-search-container">
                                    <span class="tts-search-icon">🔍</span>
                                    <input type="text" class="tts-search-input" id="tts-advanced-search" 
                                           placeholder="Ses ara (isim, dil, platform, özellik)...">
                                </div>
                                
                                <div class="tts-toolbar-actions">
                                    <div class="tts-view-switcher">
                                        <button class="tts-view-btn active" data-view="grid" title="Izgara Görünümü">
                                            <span>▦</span>
                                        </button>
                                        <button class="tts-view-btn" data-view="list" title="Liste Görünümü">
                                            <span>☰</span>
                                        </button>
                                    </div>
                                    
                                    <select class="tts-sort-select" id="tts-sort-select">
                                        <option value="quality">Kalite (Yüksek → Düşük)</option>
                                        <option value="name">İsim (A → Z)</option>
                                        <option value="usage">Kullanım (Çok → Az)</option>
                                        <option value="rating">Değerlendirme (Yüksek → Düşük)</option>
                                    </select>
                                </div>
                            </div>
                            
                            <!-- Filters -->
                            <div class="tts-filter-bar">
                                <button class="tts-filter-chip active" data-filter="all">
                                    <span>🌐 Tümü</span>
                                </button>
                                <button class="tts-filter-chip" data-filter="turkish">
                                    <span>🇹🇷 Türkçe</span>
                                </button>
                                <button class="tts-filter-chip" data-filter="local">
                                    <span>📱 Cihaz İçi</span>
                                </button>
                                <button class="tts-filter-chip" data-filter="cloud">
                                    <span>☁️ Bulut</span>
                                </button>
                                <button class="tts-filter-chip" data-filter="premium">
                                    <span>⭐ Premium</span>
                                </button>
                                <button class="tts-filter-chip" data-filter="female">
                                    <span>👩 Kadın</span>
                                </button>
                                <button class="tts-filter-chip" data-filter="male">
                                    <span>👨 Erkek</span>
                                </button>
                            </div>
                            
                            <!-- Voice Display -->
                            <div class="tts-voice-display" id="tts-voice-display">
                                <div class="tts-loading-state">
                                    <div class="tts-spinner"></div>
                                    <div class="tts-loading-text">Sesler analiz ediliyor...</div>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Settings Tab -->
                        <div class="tts-tab-content" data-tab-content="settings">
                            <div class="tts-settings-grid">
                                <div class="tts-setting-card">
                                    <div class="tts-setting-header">
                                        <span class="tts-setting-icon">⚡</span>
                                        <span class="tts-setting-title">Konuşma Hızı</span>
                                    </div>
                                    <div class="tts-setting-value" id="setting-rate-display">1.0x</div>
                                    <input type="range" class="tts-range-slider" id="setting-rate-slider" 
                                           min="0.1" max="3.0" step="0.1" value="1.0">
                                    <div class="tts-range-labels">
                                        <span>Çok Yavaş</span>
                                        <span>Normal</span>
                                        <span>Çok Hızlı</span>
                                    </div>
                                </div>
                                
                                <div class="tts-setting-card">
                                    <div class="tts-setting-header">
                                        <span class="tts-setting-icon">🎵</span>
                                        <span class="tts-setting-title">Ses Tonu</span>
                                    </div>
                                    <div class="tts-setting-value" id="setting-pitch-display">1.0</div>
                                    <input type="range" class="tts-range-slider" id="setting-pitch-slider" 
                                           min="0.1" max="2.0" step="0.1" value="1.0">
                                    <div class="tts-range-labels">
                                        <span>Kalın</span>
                                        <span>Normal</span>
                                        <span>İnce</span>
                                    </div>
                                </div>
                                
                                <div class="tts-setting-card">
                                    <div class="tts-setting-header">
                                        <span class="tts-setting-icon">🔊</span>
                                        <span class="tts-setting-title">Ses Seviyesi</span>
                                    </div>
                                    <div class="tts-setting-value" id="setting-volume-display">100%</div>
                                    <input type="range" class="tts-range-slider" id="setting-volume-slider" 
                                           min="0" max="1" step="0.05" value="1.0">
                                    <div class="tts-range-labels">
                                        <span>Sessiz</span>
                                        <span>Orta</span>
                                        <span>Maksimum</span>
                                    </div>
                                </div>
                                
                                <div class="tts-setting-card">
                                    <div class="tts-setting-header">
                                        <span class="tts-setting-icon">🧪</span>
                                        <span class="tts-setting-title">Test Metni</span>
                                    </div>
                                    <textarea class="tts-test-textarea" id="test-text-input">Merhaba, ben sesli asistanınızım. Bu bir test mesajıdır.</textarea>
                                    <button class="tts-test-button" onclick="ttsSettingsUI.testCurrentSettings()">
                                        🔊 Ayarları Test Et
                                    </button>
                                </div>
                            </div>
                            
                            <div class="tts-preset-section">
                                <div class="tts-section-title">📦 Ön Ayarlar</div>
                                <div class="tts-preset-grid">
                                    <button class="tts-preset-btn" onclick="ttsSettingsUI.applyPreset('normal')">
                                        <span class="tts-preset-icon">🎯</span>
                                        <span class="tts-preset-name">Normal</span>
                                        <span class="tts-preset-desc">Hız: 1.0, Ton: 1.0</span>
                                    </button>
                                    <button class="tts-preset-btn" onclick="ttsSettingsUI.applyPreset('fast')">
                                        <span class="tts-preset-icon">⚡</span>
                                        <span class="tts-preset-name">Hızlı</span>
                                        <span class="tts-preset-desc">Hız: 1.5, Ton: 1.0</span>
                                    </button>
                                    <button class="tts-preset-btn" onclick="ttsSettingsUI.applyPreset('slow')">
                                        <span class="tts-preset-icon">🐌</span>
                                        <span class="tts-preset-name">Yavaş</span>
                                        <span class="tts-preset-desc">Hız: 0.7, Ton: 1.0</span>
                                    </button>
                                    <button class="tts-preset-btn" onclick="ttsSettingsUI.applyPreset('deep')">
                                        <span class="tts-preset-icon">🎙️</span>
                                        <span class="tts-preset-name">Kalın Ses</span>
                                        <span class="tts-preset-desc">Hız: 0.9, Ton: 0.7</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Stats Tab -->
                        <div class="tts-tab-content" data-tab-content="stats">
                            <div class="tts-stats-container" id="tts-stats-container">
                                <div class="tts-loading-state">
                                    <div class="tts-loading-text">İstatistikler yükleniyor...</div>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Favorites Tab -->
                        <div class="tts-tab-content" data-tab-content="favorites">
                            <div class="tts-favorites-container" id="tts-favorites-container">
                                <div class="tts-empty-state">
                                    <span class="tts-empty-icon">⭐</span>
                                    <div class="tts-empty-title">Henüz favori ses yok</div>
                                    <div class="tts-empty-desc">Sesler sekmesinden favori sesleri ekleyebilirsiniz</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHTML);
        this.modal = document.getElementById('tts-settings-modal');
        
        // Stil ekle
        this.injectAdvancedStyles();
        
        // Event listener'ları ekle
        this.attachAdvancedEventListeners();
    }
    
    /**
     * Gelişmiş event listener'lar
     */
    attachAdvancedEventListeners() {
        // Tab navigation
        document.querySelectorAll('.tts-nav-item').forEach(item => {
            item.addEventListener('click', () => {
                const tab = item.dataset.tab;
                this.switchTab(tab);
            });
        });
        
        // Filter chips
        document.querySelectorAll('.tts-filter-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                document.querySelectorAll('.tts-filter-chip').forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                this.currentCategory = chip.dataset.filter;
                this.renderVoiceDisplay();
            });
        });
        
        // View switcher
        document.querySelectorAll('.tts-view-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.tts-view-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.viewMode = btn.dataset.view;
                this.renderVoiceDisplay();
            });
        });
        
        // Search
        const searchInput = document.getElementById('tts-advanced-search');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchQuery = e.target.value.toLowerCase();
                this.renderVoiceDisplay();
            });
        }
        
        // Sort
        const sortSelect = document.getElementById('tts-sort-select');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                this.renderVoiceDisplay();
            });
        }
        
        // Settings sliders
        this.attachSettingsSliders();
        
        // Overlay close
        this.modal.querySelector('.tts-modal-overlay').addEventListener('click', () => {
            this.close();
        });
    }
    
    /**
     * Settings slider'larını bağla
     */
    attachSettingsSliders() {
        const rateSlider = document.getElementById('setting-rate-slider');
        const pitchSlider = document.getElementById('setting-pitch-slider');
        const volumeSlider = document.getElementById('setting-volume-slider');
        
        if (rateSlider) {
            rateSlider.addEventListener('input', (e) => {
                const value = parseFloat(e.target.value);
                this.tts.setRate(value);
                document.getElementById('setting-rate-display').textContent = value.toFixed(1) + 'x';
            });
        }
        
        if (pitchSlider) {
            pitchSlider.addEventListener('input', (e) => {
                const value = parseFloat(e.target.value);
                this.tts.setPitch(value);
                document.getElementById('setting-pitch-display').textContent = value.toFixed(1);
            });
        }
        
        if (volumeSlider) {
            volumeSlider.addEventListener('input', (e) => {
                const value = parseFloat(e.target.value);
                this.tts.setVolume(value);
                document.getElementById('setting-volume-display').textContent = Math.round(value * 100) + '%';
            });
        }
    }
    
    /**
     * Tab değiştir
     */
    switchTab(tabName) {
        // Nav items
        document.querySelectorAll('.tts-nav-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        
        // Tab contents
        document.querySelectorAll('.tts-tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.querySelector(`[data-tab-content="${tabName}"]`).classList.add('active');
        
        // Update title
        const titles = {
            voices: 'Ses Yönetimi',
            settings: 'Ayarlar',
            stats: 'İstatistikler',
            favorites: 'Favoriler'
        };
        document.getElementById('content-title').textContent = titles[tabName];
        
        // Load content
        if (tabName === 'stats') {
            this.loadStatistics();
        } else if (tabName === 'favorites') {
            this.loadFavorites();
        }
    }
    
    /**
     * Sesleritamamen güncelle
     */
    updateCompleteVoiceDisplay(voices) {
        document.getElementById('sidebar-voice-count').textContent = voices.length;
        this.renderVoiceDisplay();
    }
    
    /**
     * Ses görüntüsünü render et
     */
    renderVoiceDisplay() {
        const container = document.getElementById('tts-voice-display');
        const voices = this.getFilteredAndSortedVoices();
        
        if (voices.length === 0) {
            container.innerHTML = `
                <div class="tts-empty-state">
                    <span class="tts-empty-icon">🔍</span>
                    <div class="tts-empty-title">Ses bulunamadı</div>
                    <div class="tts-empty-desc">Farklı bir filtre veya arama terimi deneyin</div>
                </div>
            `;
            return;
        }
        
        const html = this.viewMode === 'grid' 
            ? this.renderGridView(voices)
            : this.renderListView(voices);
            
        container.innerHTML = html;
    }
    
    /**
     * Filtrelenmiş ve sıralanmış sesleri al
     */
    getFilteredAndSortedVoices() {
        let voices = this.tts.getTurkishVoices();
        
        // Kategori filtresi
        if (this.currentCategory !== 'all') {
            voices = this.tts.voiceCategories[this.currentCategory] || [];
        }
        
        // Arama filtresi
        if (this.searchQuery) {
            voices = voices.filter(voice => {
                const metadata = this.tts.voiceMetadata.get(voice.voiceURI);
                return voice.name.toLowerCase().includes(this.searchQuery) ||
                       voice.lang.toLowerCase().includes(this.searchQuery) ||
                       metadata.platform.toLowerCase().includes(this.searchQuery) ||
                       metadata.gender.toLowerCase().includes(this.searchQuery);
            });
        }
        
        // Sıralama
        voices.sort((a, b) => {
            const metaA = this.tts.voiceMetadata.get(a.voiceURI);
            const metaB = this.tts.voiceMetadata.get(b.voiceURI);
            
            switch (this.sortBy) {
                case 'quality':
                    return metaB.quality - metaA.quality;
                case 'name':
                    return a.name.localeCompare(b.name);
                case 'usage':
                    return metaB.usageCount - metaA.usageCount;
                case 'rating':
                    return metaB.rating - metaA.rating;
                default:
                    return 0;
            }
        });
        
        return voices;
    }
    
    /**
     * Grid görünümü
     */
    renderGridView(voices) {
        return `
            <div class="tts-voice-grid-view">
                ${voices.map(voice => this.createVoiceGridCard(voice)).join('')}
            </div>
        `;
    }
    
    /**
     * Liste görünümü
     */
    renderListView(voices) {
        return `
            <div class="tts-voice-list-view">
                ${voices.map(voice => this.createVoiceListItem(voice)).join('')}
            </div>
        `;
    }
    
    /**
     * Grid kart oluştur
     */
    createVoiceGridCard(voice) {
        const metadata = this.tts.voiceMetadata.get(voice.voiceURI);
        const isSelected = this.tts.selectedVoice?.voiceURI === voice.voiceURI;
        const qualityStars = '★'.repeat(Math.round(metadata.quality / 2));
        
        return `
            <div class="tts-voice-grid-card ${isSelected ? 'selected' : ''}" data-voice-uri="${voice.voiceURI}">
                <div class="tts-voice-card-header">
                    <div class="tts-voice-card-title">${this.truncate(voice.name, 25)}</div>
                    <button class="tts-favorite-btn" onclick="ttsSettingsUI.toggleFavorite('${voice.voiceURI}')">
                        ⭐
                    </button>
                </div>
                <div class="tts-voice-card-badges">
                    ${metadata.isLocal ? '<span class="tts-badge badge-local">📱 Yerel</span>' : '<span class="tts-badge badge-cloud">☁️ Bulut</span>'}
                    ${metadata.turkishSupport ? '<span class="tts-badge badge-turkish">🇹🇷</span>' : ''}
                    ${metadata.quality >= 8 ? '<span class="tts-badge badge-premium">⭐</span>' : ''}
                </div>
                <div class="tts-voice-card-meta">
                    <span>🌐 ${voice.lang}</span>
                    <span>${metadata.platform}</span>
                </div>
                <div class="tts-voice-card-quality">
                    <span class="tts-quality-stars">${qualityStars}</span>
                    <span class="tts-quality-score">${metadata.quality.toFixed(1)}/10</span>
                </div>
                <div class="tts-voice-card-actions">
                    <button class="tts-action-btn btn-test" onclick="ttsSettingsUI.testVoice('${voice.voiceURI}')">
                        🔊 Test
                    </button>
                    <button class="tts-action-btn btn-select" onclick="ttsSettingsUI.selectVoice('${voice.voiceURI}')">
                        ${isSelected ? '✓ Seçili' : 'Seç'}
                    </button>
                </div>
            </div>
        `;
    }
    
    /**
     * Liste öğesi oluştur
     */
    createVoiceListItem(voice) {
        const metadata = this.tts.voiceMetadata.get(voice.voiceURI);
        const isSelected = this.tts.selectedVoice?.voiceURI === voice.voiceURI;
        
        return `
            <div class="tts-voice-list-item ${isSelected ? 'selected' : ''}" data-voice-uri="${voice.voiceURI}">
                <div class="tts-list-item-info">
                    <div class="tts-list-item-name">${voice.name}</div>
                    <div class="tts-list-item-meta">
                        ${voice.lang} • ${metadata.platform} • Kalite: ${metadata.quality.toFixed(1)}/10
                    </div>
                </div>
                <div class="tts-list-item-actions">
                    <button class="tts-icon-btn" onclick="ttsSettingsUI.testVoice('${voice.voiceURI}')" title="Test">
                        🔊
                    </button>
                    <button class="tts-icon-btn" onclick="ttsSettingsUI.toggleFavorite('${voice.voiceURI}')" title="Favori">
                        ⭐
                    </button>
                    <button class="tts-icon-btn ${isSelected ? 'selected' : ''}" onclick="ttsSettingsUI.selectVoice('${voice.voiceURI}')" title="Seç">
                        ${isSelected ? '✓' : '○'}
                    </button>
                </div>
            </div>
        `;
    }
    
    /**
     * Sesi test et
     */
    testVoice(voiceURI) {
        const voice = this.tts.voices.find(v => v.voiceURI === voiceURI);
        if (!voice) return;
        
        this.tts.setVoice(voiceURI);
        this.tts.speak(`Merhaba, ben ${voice.name}. Sesim nasıl?`);
    }
    
    /**
     * Sesi seç
     */
    selectVoice(voiceURI) {
        this.tts.setVoice(voiceURI);
        this.renderVoiceDisplay();
        this.showNotification('✓ Ses seçildi', 'success');
    }
    
    /**
     * Favori ekle/çıkar
     */
    toggleFavorite(voiceURI) {
        // TODO: Implement favorites system
        this.showNotification('⭐ Favorilere eklendi', 'info');
    }
    
    /**
     * Mevcut ayarları test et
     */
    testCurrentSettings() {
        const text = document.getElementById('test-text-input').value.trim();
        if (!text) {
            this.showNotification('⚠️ Lütfen test metni girin', 'warning');
            return;
        }
        this.tts.speak(text);
    }
    
    /**
     * Ön ayar uygula
     */
    applyPreset(presetName) {
        const presets = {
            normal: { rate: 1.0, pitch: 1.0, volume: 1.0 },
            fast: { rate: 1.5, pitch: 1.0, volume: 1.0 },
            slow: { rate: 0.7, pitch: 1.0, volume: 1.0 },
            deep: { rate: 0.9, pitch: 0.7, volume: 1.0 }
        };
        
        const preset = presets[presetName];
        if (!preset) return;
        
        this.tts.setRate(preset.rate);
        this.tts.setPitch(preset.pitch);
        this.tts.setVolume(preset.volume);
        
        // UI güncelle
        document.getElementById('setting-rate-slider').value = preset.rate;
        document.getElementById('setting-rate-display').textContent = preset.rate.toFixed(1) + 'x';
        document.getElementById('setting-pitch-slider').value = preset.pitch;
        document.getElementById('setting-pitch-display').textContent = preset.pitch.toFixed(1);
        document.getElementById('setting-volume-slider').value = preset.volume;
        document.getElementById('setting-volume-display').textContent = Math.round(preset.volume * 100) + '%';
        
        this.showNotification('✓ Ön ayar uygulandı', 'success');
    }
    
    /**
     * İstatistikleri yükle
     */
    loadStatistics() {
        const stats = this.tts.calculateVoiceStatistics();
        const container = document.getElementById('tts-stats-container');
        
        container.innerHTML = `
            <div class="tts-stats-grid">
                <div class="tts-stat-box">
                    <div class="tts-stat-icon">🌐</div>
                    <div class="tts-stat-value">${stats.total}</div>
                    <div class="tts-stat-label">Toplam Ses</div>
                </div>
                <div class="tts-stat-box">
                    <div class="tts-stat-icon">🇹🇷</div>
                    <div class="tts-stat-value">${stats.turkish}</div>
                    <div class="tts-stat-label">Türkçe Ses</div>
                </div>
                <div class="tts-stat-box">
                    <div class="tts-stat-icon">📱</div>
                    <div class="tts-stat-value">${stats.local}</div>
                    <div class="tts-stat-label">Yerel Ses</div>
                </div>
                <div class="tts-stat-box">
                    <div class="tts-stat-icon">☁️</div>
                    <div class="tts-stat-value">${stats.cloud}</div>
                    <div class="tts-stat-label">Bulut Ses</div>
                </div>
                <div class="tts-stat-box">
                    <div class="tts-stat-icon">⭐</div>
                    <div class="tts-stat-value">${stats.premium}</div>
                    <div class="tts-stat-label">Premium Ses</div>
                </div>
                <div class="tts-stat-box">
                    <div class="tts-stat-icon">🔊</div>
                    <div class="tts-stat-value">${this.tts.metrics.totalSpeeches}</div>
                    <div class="tts-stat-label">Toplam Konuşma</div>
                </div>
            </div>
            
            <div class="tts-platform-stats">
                <div class="tts-section-title">📊 Platform Dağılımı</div>
                ${this.renderPlatformChart(stats.platforms)}
            </div>
        `;
    }
    
    /**
     * Platform chart render et
     */
    renderPlatformChart(platforms) {
        const total = Object.values(platforms).reduce((a, b) => a + b, 0);
        
        return Object.entries(platforms).map(([platform, count]) => {
            const percentage = (count / total * 100).toFixed(1);
            return `
                <div class="tts-chart-item">
                    <div class="tts-chart-label">${platform}</div>
                    <div class="tts-chart-bar-container">
                        <div class="tts-chart-bar" style="width: ${percentage}%"></div>
                    </div>
                    <div class="tts-chart-value">${count} (${percentage}%)</div>
                </div>
            `;
        }).join('');
    }
    
    /**
     * Favorileri yükle
     */
    loadFavorites() {
        // TODO: Implement favorites loading
    }
    
    /**
     * Ayarları dışa aktar
     */
    exportSettings() {
        const settings = {
            selectedVoice: this.tts.selectedVoice?.voiceURI,
            rate: this.tts.settings.rate,
            pitch: this.tts.settings.pitch,
            volume: this.tts.settings.volume
        };
        
        const blob = new Blob([JSON.stringify(settings, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'tts-settings.json';
        a.click();
        
        this.showNotification('💾 Ayarlar dışa aktarıldı', 'success');
    }
    
    /**
     * Ayarları içe aktar
     */
    importSettings() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.json';
        input.onchange = (e) => {
            const file = e.target.files[0];
            if (!file) return;
            
            const reader = new FileReader();
            reader.onload = (event) => {
                try {
                    const settings = JSON.parse(event.target.result);
                    this.tts.setVoice(settings.selectedVoice);
                    this.tts.setRate(settings.rate);
                    this.tts.setPitch(settings.pitch);
                    this.tts.setVolume(settings.volume);
                    this.showNotification('📥 Ayarlar içe aktarıldı', 'success');
                } catch (error) {
                    this.showNotification('❌ Geçersiz dosya', 'error');
                }
            };
            reader.readAsText(file);
        };
        input.click();
    }
    
    /**
     * Modal'ı aç
     */
    open() {
        this.modal.style.display = 'flex';
        this.isOpen = true;
        document.body.style.overflow = 'hidden';
        
        // İlk yükleme
        if (this.tts.isInitialized) {
            this.updateCompleteVoiceDisplay(this.tts.getTurkishVoices());
        }
    }
    
    /**
     * Modal'ı kapat
     */
    close() {
        this.modal.style.display = 'none';
        this.isOpen = false;
        document.body.style.overflow = '';
        this.tts.stop();
    }
    
    /**
     * Bildirim göster
     */
    showNotification(message, type = 'info') {
        // TODO: Implement toast notification system
        console.log(`[${type}]`, message);
    }
    
    /**
     * Metni kısalt
     */
    truncate(str, length) {
        return str.length > length ? str.substring(0, length) + '...' : str;
    }
    
    /**
     * Gelişmiş CSS stillerini ekle
     */
    injectAdvancedStyles() {
        // CSS dosyası çok uzun olduğu için devamı bir sonraki mesajda...
        const styles = document.createElement('link');
        styles.rel = 'stylesheet';
        styles.href = 'tts-advanced-styles.css';
        document.head.appendChild(styles);
        
        // Inline critical styles
        const inlineStyles = document.createElement('style');
        inlineStyles.textContent = `
            .tts-modal-advanced { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 10000; align-items: center; justify-content: center; }
            .tts-modal-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.7); backdrop-filter: blur(8px); }
            .tts-modal-container { position: relative; display: flex; width: 95%; max-width: 1400px; height: 90vh; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3); }
            .tts-sidebar { width: 280px; background: linear-gradient(180deg, #667eea 0%, #764ba2 100%); color: white; display: flex; flex-direction: column; }
            .tts-main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
            .tts-loading-state, .tts-empty-state { text-align: center; padding: 60px 20px; color: #94a3b8; }
            .tts-spinner { width: 50px; height: 50px; border: 4px solid #e2e8f0; border-top-color: #667eea; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 20px; }
            @keyframes spin { to { transform: rotate(360deg); } }
        `;
        document.head.appendChild(inlineStyles);
    }
}

// Global kullanım için
window.TTSSettingsUI = TTSSettingsUI;
