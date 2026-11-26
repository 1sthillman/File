package com.manav.sayim;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/**
 * Native TTS Bridge - Tüm Türkçe sesler için
 */
public class NativeTTSBridge {
    
    private static final String TAG = "NativeTTSBridge";
    private final Activity activity;
    private final WebView webView;
    private final Handler mainHandler;
    
    private TextToSpeech tts;
    private boolean isReady = false;
    private String currentUtteranceId = "";
    
    public NativeTTSBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        initTTS();
    }
    
    private void initTTS() {
        tts = new TextToSpeech(activity, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isReady = true;
                Log.i(TAG, "✅ TTS hazır");
                
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        sendEvent("start", utteranceId);
                    }
                    
                    @Override
                    public void onDone(String utteranceId) {
                        sendEvent("end", utteranceId);
                    }
                    
                    @Override
                    public void onError(String utteranceId) {
                        sendEvent("error", utteranceId);
                    }
                });
                
                sendEvent("ready", "");
            } else {
                Log.e(TAG, "❌ TTS başarısız");
                sendEvent("error", "INIT_FAILED");
            }
        });
    }
    
    @JavascriptInterface
    public String getAllVoices() {
        if (!isReady) return "[]";
        
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) return "[]";
            
            JSONArray arr = new JSONArray();
            for (Voice v : voices) {
                JSONObject obj = new JSONObject();
                obj.put("name", v.getName());
                obj.put("locale", v.getLocale().toString());
                obj.put("quality", v.getQuality());
                obj.put("latency", v.getLatency());
                obj.put("network", v.isNetworkConnectionRequired());
                arr.put(obj);
            }
            
            Log.i(TAG, "📋 " + arr.length() + " ses döndürüldü");
            return arr.toString();
        } catch (Exception e) {
            Log.e(TAG, "Sesler alınamadı", e);
            return "[]";
        }
    }
    
    @JavascriptInterface
    public boolean setVoice(String voiceName) {
        if (!isReady) return false;
        
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) return false;
            
            for (Voice v : voices) {
                if (v.getName().equals(voiceName)) {
                    int result = tts.setVoice(v);
                    if (result == TextToSpeech.SUCCESS) {
                        Log.i(TAG, "✅ Ses: " + voiceName);
                        sendEvent("voice_changed", voiceName);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ses değiştirilemedi", e);
        }
        return false;
    }
    
    @JavascriptInterface
    public String speak(String text, float rate, float pitch) {
        if (!isReady || text == null || text.isEmpty()) return "";
        
        try {
            String id = "tts_" + System.currentTimeMillis();
            currentUtteranceId = id;
            
            tts.setSpeechRate(rate);
            tts.setPitch(pitch);
            
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
            
            int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id);
            
            if (result == TextToSpeech.SUCCESS) {
                Log.i(TAG, "🔊 Konuşuyor: " + text.substring(0, Math.min(30, text.length())));
                return id;
            }
        } catch (Exception e) {
            Log.e(TAG, "Konuşma hatası", e);
        }
        return "";
    }
    
    @JavascriptInterface
    public void stop() {
        if (isReady && tts != null) {
            tts.stop();
            Log.i(TAG, "⏹️ Durduruldu");
            sendEvent("stopped", "");
        }
    }
    
    @JavascriptInterface
    public boolean isSpeaking() {
        return isReady && tts != null && tts.isSpeaking();
    }
    
    public void release() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            Log.i(TAG, "🔴 Kapatıldı");
        }
    }
    
    private void sendEvent(String event, String data) {
        mainHandler.post(() -> {
            if (webView != null && !activity.isFinishing()) {
                String safeData = data.replace("\\", "\\\\").replace("\"", "\\\"");
                String js = String.format(
                    "if(window.onNativeTTSEvent) window.onNativeTTSEvent('%s','%s');",
                    event, safeData
                );
                webView.evaluateJavascript(js, null);
            }
        });
    }
}
