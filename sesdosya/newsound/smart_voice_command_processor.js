/**
 * AKILLI SES KOMUT İŞLEME SİSTEMİ
 * ================================
 * 
 * ÖZELLİKLER:
 * - Çoklu komut desteği: "domates 5 kilo sayım patates 4 kilo fişe yaz"
 * - Varyasyon seçimi: "muz" → "İthal Muz / Baby Muz seçin"
 * - Akıllı ürün eşleştirme: "domates" → Salkım/Pembe/Kokteyl arasında seç
 * - Ses feedback: Her işlem sonrası uygun ses
 * - Kombinasyon zinciri: Birden fazla işlemi sırayla yap
 */

window.SmartVoiceProcessor = (function() {
    'use strict';
    
    // =============================================
    // 1. TEMEL PARSING FONKSİYONLARI
    // =============================================
    
    /**
     * Metni normalize et (Türkçe karakterler, küçük harf)
     */
    function normalize(text) {
        return text
            .toLowerCase()
            .replace(/ı/g, 'i')
            .replace(/ş/g, 's')
            .replace(/ğ/g, 'g')
            .replace(/ü/g, 'u')
            .replace(/ö/g, 'o')
            .replace(/ç/g, 'c')
            .trim();
    }
    
    /**
     * Sayıları metinden çıkar
     */
    function extractNumbers(text) {
        const matches = text.match(/\d+(?:[.,]\d+)?/g);
        return matches ? matches.map(n => parseFloat(n.replace(',', '.'))) : [];
    }
    
    /**
     * Birim tespit et
     */
    function detectUnit(text) {
        const unitMap = window.VOICE_COMMAND_MAP.quantity_units;
        const lower = normalize(text);
        
        for (const [unit, keywords] of Object.entries(unitMap)) {
            if (keywords.some(kw => lower.includes(kw))) {
                return unit;
            }
        }
        
        return 'kg';  // Default
    }
    
    /**
     * Aksiyon tespit et (sayım, fire, sevkiyat)
     */
    function detectAction(text) {
        return window.VoiceHelpers.detectAction(text);
    }
    
    // =============================================
    // 2. ÜRÜN EŞLEŞTIRME VE VARYASYON YÖNETİMİ
    // =============================================
    
    /**
     * Metinden ürün ismini bul
     * Örnek: "domates 5 kilo" → "domates"
     */
    function extractProductName(text) {
        // Sayıları ve aksiyonları temizle
        let cleaned = text
            .replace(/\d+(?:[.,]\d+)?/g, '')  // Sayıları kaldır
            .replace(/kilo|kilogram|kg|adet|tane|kutu|kasa/gi, '')  // Birimleri kaldır
            .replace(/sayim|sayım|fire|atik|atık|çöp|sevkiyat|sevk|fişe|fise|yaz/gi, '')  // Aksiyonları kaldır
            .trim();
        
        return cleaned;
    }
    
    /**
     * Ürün varyasyonlarını bul
     */
    function findProductVariants(productName) {
        const normalized = normalize(productName).replace(/\s+/g, '_');
        
        // Önce direkt eşleşme ara
        if (window.VOICE_VAULT.products[normalized]) {
            // Varyasyon var mı?
            const variants = window.VoiceHelpers.getProductVariants(normalized);
            if (variants.length > 1) {
                return {
                    hasVariants: true,
                    variants: variants,
                    baseProduct: normalized
                };
            }
            return {
                hasVariants: false,
                product: normalized
            };
        }
        
        // Fuzzy match dene
        const allProducts = Object.keys(window.VOICE_VAULT.products);
        const matches = allProducts.filter(p => p.includes(normalized) || normalized.includes(p));
        
        if (matches.length === 0) {
            return { notFound: true, searchTerm: productName };
        }
        
        if (matches.length === 1) {
            return {
                hasVariants: false,
                product: matches[0]
            };
        }
        
        // Birden fazla eşleşme = varyasyon seçimi gerekli
        return {
            hasVariants: true,
            variants: matches,
            baseProduct: productName
        };
    }
    
    // =============================================
    // 3. KOMUT PARSING (TEKLİ VE ÇOKLU)
    // =============================================
    
    /**
     * Tek komut parse et
     * Örnek: "domates 5 kilo sayım" → { product, quantity, unit, action }
     */
    function parseSingleCommand(text) {
        const product = extractProductName(text);
        const numbers = extractNumbers(text);
        const unit = detectUnit(text);
        const action = detectAction(text);
        
        return {
            rawText: text,
            product: product,
            quantity: numbers.length > 0 ? numbers[0] : null,
            quantity2: numbers.length > 1 ? numbers[1] : null,  // İkinci sayı (ör: "3 kasa 15 kilo")
            unit: unit,
            action: action,
            variants: findProductVariants(product)
        };
    }
    
    /**
     * Çoklu komutları ayır
     * Örnek: "domates 5 kilo sayım patates 4 kilo fişe yaz" → 2 komut
     */
    function splitMultipleCommands(text) {
        // "ve" kelimesini ayırıcı olarak kullan VEYA yeni ürün ismi başladığında ayır
        
        // Önce "ve" ile ayır
        const byVe = text.split(/\s+ve\s+/i);
        
        let commands = [];
        for (const segment of byVe) {
            // Her segmentte birden fazla komut olabilir mi?
            // Heuristic: Eğer segment içinde 2'den fazla sayı varsa muhtemelen 2 komut var
            const numbers = extractNumbers(segment);
            
            if (numbers.length > 2) {
                // Muhtemelen birden fazla komut
                // Basit split: aksiyonlara göre ayır
                const actionKeywords = ['sayım', 'sayim', 'fire', 'atık', 'sevkiyat', 'fişe', 'fise'];
                
                let currentCmd = '';
                const words = segment.split(/\s+/);
                
                for (let i = 0; i < words.length; i++) {
                    const word = words[i];
                    currentCmd += word + ' ';
                    
                    // Eğer bu kelime bir aksiyon ve sonraki kelime bir ürün ismi ise
                    // mevcut komutu kaydet ve yenisini başlat
                    if (actionKeywords.some(kw => normalize(word).includes(kw))) {
                        if (i < words.length - 1) {
                            const nextWord = words[i + 1];
                            // Eğer sonraki kelime sayı değilse (muhtemelen ürün ismi)
                            if (!/^\d+$/.test(nextWord)) {
                                commands.push(currentCmd.trim());
                                currentCmd = '';
                            }
                        }
                    }
                }
                
                if (currentCmd.trim()) {
                    commands.push(currentCmd.trim());
                }
            } else {
                commands.push(segment.trim());
            }
        }
        
        return commands.filter(c => c.length > 0);
    }
    
    /**
     * Ana parse fonksiyonu
     */
    function parseVoiceCommand(text) {
        const commands = splitMultipleCommands(text);
        
        return {
            originalText: text,
            commandCount: commands.length,
            commands: commands.map(parseSingleCommand)
        };
    }
    
    // =============================================
    // 4. VARYASYON SEÇİM UI
    // =============================================
    
    /**
     * Varyasyon seçim modal göster
     */
    function showVariantSelection(variants, baseProduct, callback) {
        // Modal HTML oluştur
        const modalHTML = `
            <div id="variant-selection-modal" style="
                position: fixed;
                inset: 0;
                background: rgba(5, 8, 16, 0.92);
                backdrop-filter: blur(10px);
                z-index: 100010;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 16px;
            ">
                <div style="
                    background: linear-gradient(145deg, #1a1d2e 0%, #16182a 100%);
                    border-radius: 20px;
                    padding: 24px;
                    max-width: 400px;
                    width: 100%;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.4);
                ">
                    <h3 style="
                        font-size: 18px;
                        font-weight: 700;
                        color: #fff;
                        margin: 0 0 12px 0;
                    ">Seçim Yapın</h3>
                    <p style="
                        font-size: 13px;
                        color: rgba(255,255,255,0.6);
                        margin: 0 0 20px 0;
                    ">"${baseProduct}" için ${variants.length} çeşit bulundu:</p>
                    <div id="variant-list" style="
                        display: flex;
                        flex-direction: column;
                        gap: 10px;
                    "></div>
                    <button id="variant-cancel" style="
                        margin-top: 16px;
                        width: 100%;
                        padding: 12px;
                        background: rgba(255,255,255,0.1);
                        border: 1px solid rgba(255,255,255,0.2);
                        border-radius: 12px;
                        color: #fff;
                        font-size: 14px;
                        font-weight: 600;
                        cursor: pointer;
                    ">İptal</button>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHTML);
        
        const modal = document.getElementById('variant-selection-modal');
        const listContainer = document.getElementById('variant-list');
        const cancelBtn = document.getElementById('variant-cancel');
        
        // Varyantları listele
        variants.forEach(variant => {
            const displayName = variant.replace(/_/g, ' ').toUpperCase();
            const btn = document.createElement('button');
            btn.textContent = displayName;
            btn.style.cssText = `
                padding: 14px;
                background: rgba(11, 148, 104, 0.15);
                border: 1.5px solid rgba(11, 148, 104, 0.4);
                border-radius: 12px;
                color: #0b9468;
                font-size: 14px;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s;
            `;
            
            btn.onmouseover = () => {
                btn.style.background = 'rgba(11, 148, 104, 0.25)';
                btn.style.borderColor = '#0b9468';
            };
            btn.onmouseout = () => {
                btn.style.background = 'rgba(11, 148, 104, 0.15)';
                btn.style.borderColor = 'rgba(11, 148, 104, 0.4)';
            };
            
            btn.onclick = () => {
                // Seçildi sesini çal
                window.VoiceHelpers.play('seçildi', 'helpers');
                
                // Ürün ismini söyle
                setTimeout(() => {
                    window.VoiceHelpers.play(variant, 'products');
                }, 400);
                
                modal.remove();
                callback(variant);
            };
            
            listContainer.appendChild(btn);
        });
        
        // İptal butonu
        cancelBtn.onclick = () => {
            modal.remove();
            callback(null);
        };
    }
    
    // =============================================
    // 5. KOMUT İŞLEME VE ÇALIŞTIRMA
    // =============================================
    
    /**
     * Tek komutu işle ve çalıştır
     */
    async function executeSingleCommand(command, callback) {
        // Varyasyon kontrolü
        if (command.variants.hasVariants) {
            return new Promise((resolve) => {
                showVariantSelection(
                    command.variants.variants,
                    command.variants.baseProduct,
                    (selectedVariant) => {
                        if (selectedVariant) {
                            // Komutu seçilen varyantla güncelle
                            command.product = selectedVariant;
                            command.variants = { hasVariants: false, product: selectedVariant };
                            
                            // İşlemi devam ettir
                            executeConfirmedCommand(command, callback);
                            resolve(true);
                        } else {
                            resolve(false);
                        }
                    }
                );
            });
        } else if (command.variants.notFound) {
            // Ürün bulunamadı
            window.VoiceHelpers.play('prod_not_found', 'system');
            if (callback) callback({ success: false, reason: 'product_not_found' });
            return false;
        } else {
            // Direkt çalıştır
            return executeConfirmedCommand(command, callback);
        }
    }
    
    /**
     * Onaylanmış komutu çalıştır (varyasyon seçimi yapılmış)
     */
    function executeConfirmedCommand(command, callback) {
        console.log('[SmartVoice] Executing:', command);
        
        // Ürün ismini söyle
        window.VoiceHelpers.play(command.product, 'products');
        
        // Miktar varsa "ve" söyle + miktarı söyle
        if (command.quantity) {
            setTimeout(() => {
                window.VoiceHelpers.play('ve', 'helpers');
                // TODO: Sayı sesi (0-100.mp3'ler gerekirse eklenebilir)
            }, 500);
        }
        
        // Ana işlemi yap
        setTimeout(() => {
            const result = performAction(command);
            
            // Feedback sesi
            if (result.success) {
                const feedbackKey = getFeedbackSound(command.action);
                window.VoiceHelpers.play(feedbackKey, 'system');
            }
            
            if (callback) callback(result);
        }, 1200);
        
        return true;
    }
    
    /**
     * Aksiyona göre feedback sesi seç
     */
    function getFeedbackSound(action) {
        const map = {
            'sayim': 'sayim_saved',
            'fire': 'fire_saved',
            'sevkiyat': 'ship_line_added'
        };
        return map[action] || 'sayim_saved';
    }
    
    /**
     * Gerçek aksiyonu gerçekleştir (mevcut app koduna entegre)
     */
    function performAction(command) {
        // Bu fonksiyon mevcut index.html'deki sayım/fire/sevkiyat fonksiyonlarını çağırmalı
        // Şimdilik mock return
        
        console.log('[SmartVoice] Action:', {
            action: command.action,
            product: command.product,
            quantity: command.quantity,
            unit: command.unit
        });
        
        // TODO: Gerçek entegrasyon
        // Örnek: window.AppHandlers.addToSayim(command.product, command.quantity, command.unit)
        
        return {
            success: true,
            action: command.action,
            product: command.product,
            quantity: command.quantity
        };
    }
    
    /**
     * Çoklu komutları sırayla işle
     */
    async function processMultipleCommands(parsedCommands, statusCallback) {
        const results = [];
        
        for (let i = 0; i < parsedCommands.commands.length; i++) {
            const cmd = parsedCommands.commands[i];
            
            if (statusCallback) {
                statusCallback({
                    current: i + 1,
                    total: parsedCommands.commands.length,
                    command: cmd
                });
            }
            
            const result = await executeSingleCommand(cmd);
            results.push(result);
            
            // Komutlar arası kısa bekleme
            if (i < parsedCommands.commands.length - 1) {
                await new Promise(resolve => setTimeout(resolve, 800));
            }
        }
        
        return results;
    }
    
    // =============================================
    // 6. PUBLIC API
    // =============================================
    
    return {
        /**
         * Ana giriş noktası: Ses komutunu işle
         */
        process: async function(voiceText, options = {}) {
            const parsed = parseVoiceCommand(voiceText);
            
            console.log('[SmartVoice] Parsed:', parsed);
            
            if (parsed.commandCount === 1) {
                return await executeSingleCommand(parsed.commands[0], options.callback);
            } else {
                return await processMultipleCommands(parsed, options.statusCallback);
            }
        },
        
        /**
         * Test ve debug
         */
        test: function(text) {
            return parseVoiceCommand(text);
        },
        
        /**
         * Ürün varyasyonlarını sorgula
         */
        queryVariants: findProductVariants
    };
})();

console.log('[SmartVoiceProcessor] Yüklendi ✓');
