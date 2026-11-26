/**
 * VOICE INTEGRATION BRIDGE
 * =========================
 * Mevcut VoiceCommanderEngine ile SmartVoiceProcessor arasında köprü
 * 
 * Bu script, mevcut onNativeSpeechResult callback'ini intercept edip
 * SmartVoiceProcessor'a yönlendirir.
 */

(function() {
    'use strict';
    
    // Orijinal engine'i sakla
    let originalEngine = null;
    let originalOnResult = null;
    
    // Init fonksiyonu
    function initBridge() {
        console.log('[VoiceBridge] Initializing...');
        
        // Mevcut engine'i bul
        if (window.__voiceEngine) {
            originalEngine = window.__voiceEngine;
            console.log('[VoiceBridge] Found existing engine');
        }
        
        // onNativeSpeechResult'ı intercept et
        if (window.onNativeSpeechResult) {
            originalOnResult = window.onNativeSpeechResult;
        }
        
        // Yeni handler
        window.onNativeSpeechResult = function(text) {
            console.log('[VoiceBridge] Speech result:', text);
            
            // Önce SmartVoiceProcessor'a gönder
            handleSmartVoiceCommand(text);
            
            // Sonra orijinal handler'ı da çağır (UI güncellemeleri için)
            if (originalOnResult) {
                originalOnResult(text);
            }
        };
        
        console.log('[VoiceBridge] Initialized ✓');
    }
    
    /**
     * Smart Voice Processor entegrasyonu
     */
    async function handleSmartVoiceCommand(text) {
        // Live text'i güncelle
        updateLiveTranscript(text);
        
        try {
            // SmartVoiceProcessor ile işle
            const result = await window.SmartVoiceProcessor.process(text, {
                callback: (cmdResult) => {
                    console.log('[VoiceBridge] Command result:', cmdResult);
                    
                    // Feedback göster
                    showVoiceFeedback(cmdResult);
                },
                statusCallback: (status) => {
                    console.log('[VoiceBridge] Multi-command status:', status);
                    
                    // İlerleme göster
                    updateProgressIndicator(status);
                }
            });
            
            console.log('[VoiceBridge] Processing complete:', result);
            
        } catch (error) {
            console.error('[VoiceBridge] Error:', error);
            
            // Hata sesi
            if (window.VoiceHelpers) {
                window.VoiceHelpers.play('not_understood', 'system');
            }
        }
    }
    
    /**
     * Live transcript güncelle
     */
    function updateLiveTranscript(text) {
        const liveTextEl = document.getElementById('voice-live-text');
        if (liveTextEl) {
            liveTextEl.textContent = text;
            liveTextEl.classList.remove('empty');
        }
    }
    
    /**
     * Feedback göster
     */
    function showVoiceFeedback(result) {
        const feedbackEl = document.getElementById('voice-feedback');
        const feedbackTextEl = document.getElementById('voice-feedback-text');
        
        if (feedbackEl && feedbackTextEl) {
            if (result.success) {
                feedbackTextEl.textContent = `${result.product} ${result.action} işlendi (${result.quantity || '?'})`;
            } else {
                feedbackTextEl.textContent = 'İşlem başarısız: ' + (result.reason || 'Bilinmeyen hata');
            }
            
            feedbackEl.style.display = 'flex';
            
            // 3 saniye sonra gizle
            setTimeout(() => {
                feedbackEl.style.display = 'none';
            }, 3000);
        }
    }
    
    /**
     * Çoklu komut ilerleme göstergesi
     */
    function updateProgressIndicator(status) {
        // Intent badge'de göster
        const intentBadge = document.getElementById('voice-intent-badge');
        const intentName = document.getElementById('voice-intent-name');
        
        if (intentBadge && intentName) {
            intentName.textContent = `${status.current}/${status.total} ${status.command.action.toUpperCase()}`;
            intentBadge.style.display = 'flex';
        }
    }
    
    /**
     * Geri al fonksiyonu (mevcut son işlemi geri al)
     */
    window.voiceUndoLastAction = function() {
        console.log('[VoiceBridge] Undo requested');
        
        // TODO: Mevcut app'in undo mekanizmasına entegre et
        
        // Feedback
        if (window.VoiceHelpers) {
            window.VoiceHelpers.play('undo_success', 'system');
        }
        
        const feedbackEl = document.getElementById('voice-feedback');
        if (feedbackEl) {
            feedbackEl.style.display = 'none';
        }
    };
    
    // Geri al butonunu bağla
    function bindUndoButton() {
        const undoBtn = document.getElementById('voice-undo-btn');
        if (undoBtn) {
            undoBtn.onclick = window.voiceUndoLastAction;
        }
    }
    
    // Hint chip'leri bağla (hızlı komutlar)
    function bindHintChips() {
        const chips = document.querySelectorAll('.voice-hint-chip');
        chips.forEach(chip => {
            chip.onclick = function() {
                const cmd = this.getAttribute('data-cmd');
                if (cmd) {
                    console.log('[VoiceBridge] Quick command:', cmd);
                    handleSmartVoiceCommand(cmd);
                }
            };
        });
    }
    
    // DOM ready'de init
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initBridge();
            bindUndoButton();
            bindHintChips();
        });
    } else {
        initBridge();
        bindUndoButton();
        bindHintChips();
    }
    
    // Export
    window.VoiceBridge = {
        init: initBridge,
        handleCommand: handleSmartVoiceCommand,
        undo: window.voiceUndoLastAction
    };
    
    console.log('[VoiceBridge] Module loaded ✓');
})();
