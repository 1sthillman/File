/**
 * Native Voice Integration for Manav Sayım
 * 
 * Bu dosya, mevcut playVoiceResponse() fonksiyonunu
 * native ses çalma ile entegre eder.
 * 
 * KULLANIM:
 * HTML'de VoiceCommanderEngine.playVoiceResponse() çağrılarına
 * bu wrapper'ı ekleyin:
 */

// Orijinal playVoiceResponse'u sakla
if (typeof VoiceCommanderEngine !== 'undefined') {
    const originalPlayVoice = VoiceCommanderEngine.playVoiceResponse;
    
    VoiceCommanderEngine.playVoiceResponse = function(key) {
        // Native playVoice varsa onu kullan
        if (typeof AndroidBridge !== 'undefined' && 
            typeof AndroidBridge.playVoice === 'function') {
            
            console.log(`[VOICE] Native playVoice: ${key}`);
            
            try {
                AndroidBridge.playVoice(key);
                
                // Mikrofon UI'ını güncelle (native zaten kapatacak)
                if (this._isSpeaking) return; // Zaten konuşuyorsa atlama
                this._isSpeaking = true;
                this._updateMicStatus();
                
            } catch (e) {
                console.error('[VOICE] Native playVoice failed, falling back to web:', e);
                // Hata durumunda eski yönteme dön
                originalPlayVoice.call(this, key);
            }
        } else {
            // Native yok, eski yöntemi kullan
            console.log(`[VOICE] Web fallback playVoice: ${key}`);
            originalPlayVoice.call(this, key);
        }
    };
    
    console.log('[VOICE] Native voice integration ready');
}

/**
 * Test fonksiyonu: Native ses sistemini test et
 */
function testNativeVoice() {
    if (typeof AndroidBridge === 'undefined' || 
        typeof AndroidBridge.playVoice !== 'function') {
        console.error('[VOICE TEST] Native playVoice not available');
        return;
    }
    
    const testClips = [
        'WAKEUP',
        'PROD_FOUND',
        'SAYIM_SAVED'
    ];
    
    console.log('[VOICE TEST] Testing native voice...');
    
    let index = 0;
    function playNext() {
        if (index >= testClips.length) {
            console.log('[VOICE TEST] All tests completed');
            return;
        }
        
        const key = testClips[index++];
        console.log(`[VOICE TEST] Playing: ${key}`);
        AndroidBridge.playVoice(key);
        
        // 5 saniye sonra sonrakini çal (ses + tail için yeter)
        setTimeout(playNext, 5000);
    }
    
    playNext();
}

/**
 * Logcat için timing analizi
 * Konsola timing bilgilerini yazdır
 */
function enableVoiceTimingLogs() {
    if (typeof AndroidBridge === 'undefined') return;
    
    // Native taraf zaten detaylı log üretiyor
    // JS tarafında ek timing ekleyelim
    
    const originalPlayVoice = VoiceCommanderEngine.playVoiceResponse;
    const jsStartTimes = {};
    
    VoiceCommanderEngine.playVoiceResponse = function(key) {
        const jsStart = performance.now();
        jsStartTimes[key] = jsStart;
        
        console.log(`[VOICE_TIMING] JS call: ${key} at ${jsStart.toFixed(2)}ms`);
        
        // Orijinal çağrı
        originalPlayVoice.call(this, key);
        
        const jsEnd = performance.now();
        console.log(`[VOICE_TIMING] JS done: ${key} took ${(jsEnd - jsStart).toFixed(2)}ms`);
    };
}
