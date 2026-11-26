package com.manav.sayim;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Base64;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import java.util.concurrent.atomic.AtomicInteger;
import androidx.core.content.ContextCompat;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;
import java.io.IOException;
import org.json.JSONObject;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class AndroidBridge {

    private static final String TAG = "AndroidBridge";
    private final Activity activity;
    private final WebView webView;
    private final Handler mainHandler;
    private SpeechRecognizer speechRecognizer;
    private boolean isRecognizing = false;
    
    // =====================================================================
    // NATIVE ANDROID TTS - TÃƒÅ“M TÃƒÅ“RKÃƒâ€¡E SESLERÃ„Â° KULLAN
    // =====================================================================
    private TextToSpeech nativeTTS;
    private boolean ttsReady = false;
    private String ttsCurrentUtteranceId = "";
    private org.json.JSONArray cachedVoicesJson = null;

    public AndroidBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        initVosk();
    }

    @JavascriptInterface
    public boolean isNativeAndroid() {
        return true;
    }

    @JavascriptInterface
    public void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
    }

    @JavascriptInterface
    public void vibrate(long ms) {
        try {
            Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(ms);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibrate error", e);
        }
    }

    /**
     * DosyayÃ„Â± cihaz hafÃ„Â±zasÃ„Â±na kaydeder ve Android Share Sheet aÃƒÂ§arak paylaÃ…Å¸mayÃ„Â± saÃ„Å¸lar.
     */
    @JavascriptInterface
    public void saveAndShareFile(String base64Data, String fileName, String mimeType) {
        mainHandler.post(() -> {
            try {
                String cleanBase64 = base64Data;
                if (cleanBase64.contains(",")) {
                    cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                }
                byte[] bytes = Base64.decode(cleanBase64, Base64.DEFAULT);

                // 1. Fiziksel KayÃ„Â±t (Ã„Â°ndirilenler / Downloads)
                boolean saved = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Downloads.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");
                        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ManavSayim");

                        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        Uri itemUri = activity.getContentResolver().insert(collection, values);
                        if (itemUri != null) {
                            try (OutputStream os = activity.getContentResolver().openOutputStream(itemUri)) {
                                if (os != null) {
                                    os.write(bytes);
                                    saved = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "MediaStore save failed, trying fallback", e);
                    }
                }

                // Fallback direct storage save (Android 9 ve altÃ„Â± veya MediaStore baÃ…Å¸arÃ„Â±sÃ„Â±z olursa)
                if (!saved) {
                    try {
                        File dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!dlDir.exists()) dlDir.mkdirs();
                        File directFile = new File(dlDir, fileName);
                        try (FileOutputStream fos = new FileOutputStream(directFile)) {
                            fos.write(bytes);
                            saved = true;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Direct storage save failed", e);
                    }
                }

                // 2. PaylaÃ…Å¸Ã„Â±m iÃƒÂ§in GÃƒÂ¼venli Cache DosyasÃ„Â±
                File cacheDir = new File(activity.getCacheDir(), "shared");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File sharedFile = new File(cacheDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(sharedFile)) {
                    fos.write(bytes);
                }

                Uri contentUri = FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName() + ".fileprovider",
                        sharedFile
                );

                Toast.makeText(activity, "Kaydedildi: " + fileName, Toast.LENGTH_SHORT).show();

                // 3. Android Native Share Sheet (DosyayÃ„Â± PaylaÃ…Å¸ / Kaydet / GÃƒÂ¶nder)
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType(mimeType != null && !mimeType.isEmpty() ? mimeType : "*/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                shareIntent.putExtra(Intent.EXTRA_TEXT, fileName + " - Manav SayÃ„Â±m Raporu");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(shareIntent, "DosyayÃ„Â± PaylaÃ…Å¸ / FarklÃ„Â± Kaydet");
                activity.startActivity(chooser);

            } catch (Exception e) {
                Log.e(TAG, "Save and Share error", e);
                Toast.makeText(activity, "Dosya aktarÃ„Â±m hatasÃ„Â±: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * DÃƒÂ¼z metin paylaÃ…Å¸Ã„Â±mÃ„Â±
     */
    @JavascriptInterface
    public void shareText(String text, String title) {
        mainHandler.post(() -> {
            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, title != null ? title : "PaylaÃ…Å¸");
                activity.startActivity(shareIntent);
            } catch (Exception e) {
                Log.e(TAG, "Share text error", e);
            }
        });
    }

    // =====================================================================
    //  SÃƒÅ“REKLÃ„Â° SES TANIMA (bip sesi yok, otomatik yeniden baÃ…Å¸latma native tarafta)
    // =====================================================================

    // true: dinleme sÃ„Â±rasÃ„Â±nda mÃƒÂ¼zik kanalÃ„Â± da susturulur (bip bazÃ„Â± cihazlarda bu kanaldan ÃƒÂ§alar).
    // Vosk AudioRecord kullandÃ„Â±Ã„Å¸Ã„Â± iÃƒÂ§in bip sesi zaten yok Ã¢â‚¬â€ mÃƒÂ¼zik kanalÃ„Â±nÃ„Â± kapatma!
    // Ses dosyalarÃ„Â± (STREAM_MUSIC) ÃƒÂ¼zerinden ÃƒÂ§alÃ„Â±yor, kapatÃ„Â±rsak ses duyulmaz.
    private static final boolean MUTE_MUSIC_STREAM = false;

    private static final int[] BEEP_STREAMS = MUTE_MUSIC_STREAM
            ? new int[]{AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_MUSIC}
            : new int[]{AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM};

    private final boolean[] mutedByUs = new boolean[BEEP_STREAMS.length];
    private boolean continuousMode = false;
    private boolean readySent = false;
    private int consecutiveErrors = 0;
    private Intent recognizerIntent;
    private final Runnable restartRunnable = this::listenOnce;

    // =====================================================================
    //  VOSK: Ãƒâ€¡EVRÃ„Â°MDIÃ…ÂI, KESÃ„Â°NTÃ„Â°SÃ„Â°Z TANIMA (bip yok, oturum boÃ…Å¸luÃ„Å¸u yok)
    // =====================================================================
    // Model: app/src/main/assets/model-tr/  (iÃƒÂ§inde ayrÃ„Â±ca "uuid" adlÃ„Â± bir dosya olmalÃ„Â±)
    private static final String VOSK_ASSET_DIR = "model-tr";
    private static final int V_IDLE = 0, V_LOADING = 1, V_READY = 2, V_FAILED = 3;

    private volatile int voskState = V_IDLE;
    private Model voskModel;
    private volatile boolean voskActive = false;
    private boolean pendingVoskStart = false;
    private Thread voskThread;
    private volatile AudioRecord currentAudioRecord = null;

    // Her mikrofon oturumuna benzersiz kimlik: eski (zombi) iÃ…Å¸ parÃƒÂ§acÃ„Â±Ã„Å¸Ã„Â± yeni oturumu asla bozamaz.
    private final AtomicInteger voskSession = new AtomicInteger(0);
    private volatile int voskRetryCount = 0;     // mikrofon beklenmedik dÃƒÂ¼Ã…Å¸erse otomatik yeniden deneme sayacÃ„Â±
    private volatile boolean micWanted = false;  // kullanÃ„Â±cÃ„Â±/JS mikrofonu istiyor mu?
    private boolean hostPaused = false;          // uygulama arka planda mÃ„Â±?
    private boolean micWasOnBeforePause = false;

    // Mikrofon kaynaÃ„Å¸Ã„Â±. VOICE_RECOGNITION: ham/temiz ses. VOICE_COMMUNICATION: bazÃ„Â± cihazlarda
    // donanÃ„Â±m yankÃ„Â± engelleme (AEC) daha iyi ÃƒÂ§alÃ„Â±Ã…Å¸Ã„Â±r ama medya sesini kÃ„Â±sabilir. Cihazda test ederek seÃƒÂ§in.
    private static final int MIC_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;

    // ---------------- SIRA ALMA (HALF-DUPLEX) ----------------
    // TEK SAHÃ„Â°BÃ„Â° native taraf. Asistan konuÃ…Å¸urken mikrofon fiziksel olarak serbest bÃ„Â±rakÃ„Â±lÃ„Â±r,
    // konuÃ…Å¸ma penceresi bitince native KENDÃ„Â° KENDÃ„Â°NE yeniden aÃƒÂ§ar. JS yalnÃ„Â±zca "Ã…Å¸u kadar ms konuÃ…Å¸acaÃ„Å¸Ã„Â±m" der;
    // mikrofonu aÃƒÂ§Ã„Â±p kapatmaz. BÃƒÂ¶ylece iki taraf birbirini ÃƒÂ§ekiÃ…Å¸tiremez.
    private static final String TURN = "VoiceTurn";
    private static final long SPEECH_TAIL_MS = 500;   // ÄŸÅ¸â€Â´ FIX: 350 Ã¢â€ â€™ 500 (ses bittikten sonra hoparlÃƒÂ¶r buffer temizliÃ„Å¸i)
    private static final long SPEECH_PAD_MS  = 300;   // ÄŸÅ¸â€Â´ FIX: 400 Ã¢â€ â€™ 300 (play() zaten hÃ„Â±zlÃ„Â± baÃ…Å¸lÃ„Â±yor)
    private static final long SPEECH_MAX_MS  = 15000; // emniyet ÃƒÂ¼st sÃ„Â±nÃ„Â±rÃ„Â±: pencere asla bundan uzun kalamaz
    private volatile long speechWindowEndMs = 0;
    private final Runnable reopenRunnable = this::reopenAfterSpeech;

    private boolean inSpeechWindow() {
        return SystemClock.elapsedRealtime() < speechWindowEndMs;
    }

    /** JS: asistan sesi ÃƒÂ§almadan HEMEN Ãƒâ€“NCE (ve ÃƒÂ§almaya baÃ…Å¸layÃ„Â±nca kalan sÃƒÂ¼reyle) ÃƒÂ§aÃ„Å¸Ã„Â±rÃ„Â±r. */
    @JavascriptInterface
    public void beginAssistantSpeech(final int durationMs) {
        final long ms = Math.max(300, Math.min(durationMs > 0 ? durationMs : 4000, SPEECH_MAX_MS));
        mainHandler.post(() -> {
            speechWindowEndMs = SystemClock.elapsedRealtime() + ms + SPEECH_PAD_MS + SPEECH_TAIL_MS;
            // ÄŸÅ¸â€Â´ YENÃ„Â° YAKLAÃ…ÂIM: Mikrofonu KAPATMIYORUZ - sÃƒÂ¼rekli aÃƒÂ§Ã„Â±k!
            Log.i(TURN, "ÄŸÅ¸â€Â´ SPEAK BEGIN " + ms + "ms | pad=" + SPEECH_PAD_MS + "ms | tail=" + SPEECH_TAIL_MS + "ms | mic=AÃƒâ€¡IK KALACAK (yankÃ„Â± filtresi) | wanted=" + micWanted + " | inWindow=" + inSpeechWindow());
            
            // Vosk ÃƒÂ§alÃ„Â±Ã…Å¸maya devam eder, JS tarafÃ„Â± isSpeaking=true iken sonuÃƒÂ§larÃ„Â± filtreleyecek
            Log.i(TURN, "  Ã¢â€ â€™ Mikrofon aÃƒÂ§Ã„Â±k, Vosk tanÃ„Â±maya devam ediyor (JS filtre aktif)");
        });
    }

    /** JS: ses erken bittiyse (ended) pencereyi kÃ„Â±saltÃ„Â±r; mikrofon tail sonrasÃ„Â± aÃƒÂ§Ã„Â±lÃ„Â±r. */
    @JavascriptInterface
    public void endAssistantSpeech() {
        mainHandler.post(() -> {
            if (!inSpeechWindow()) {
                Log.i(TURN, "Ã¢Å¡Â Ã¯Â¸Â END ÃƒÂ§aÃ„Å¸rÃ„Â±ldÃ„Â± ama pencere zaten bitmiÃ…Å¸");
                return;
            }
            long oldEnd = speechWindowEndMs;
            speechWindowEndMs = SystemClock.elapsedRealtime() + SPEECH_TAIL_MS;
            Log.i(TURN, "ÄŸÅ¸Å¸Â¢ SPEAK END | pencere kÃ„Â±saltÃ„Â±ldÃ„Â± (" + (oldEnd - speechWindowEndMs) + "ms) | MÃ„Â°KROFON ZATEN AÃƒâ€¡IK (filtre kalkacak)");
            // ArtÃ„Â±k scheduleReopen() ÃƒÂ§aÃ„Å¸Ã„Â±rmÃ„Â±yoruz - mikrofon zaten aÃƒÂ§Ã„Â±k!
            // JS tarafÃ„Â±nda isSpeaking=false olunca filtre kalkacak
        });
    }

    private void scheduleReopen() {
        mainHandler.removeCallbacks(reopenRunnable);
        long delay = Math.max(0, speechWindowEndMs - SystemClock.elapsedRealtime());
        mainHandler.postDelayed(reopenRunnable, delay + 20);
    }

    private void reopenAfterSpeech() {
        if (inSpeechWindow()) { 
            Log.i(TURN, "  reopen: pencere henÃƒÂ¼z aktif, yeniden zamanlanÃ„Â±yor");
            scheduleReopen(); 
            return; 
        }   // pencere uzatÃ„Â±ldÃ„Â±ysa yeniden bekle
        if (!micWanted || hostPaused) {
            Log.i(TURN, "  reopen: micWanted=" + micWanted + " hostPaused=" + hostPaused + " Ã¢â€ â€™ mikrofon aÃƒÂ§Ã„Â±lmayacak");
            return;
        }
        if (voskActive) {
            Log.i(TURN, "  reopen: mikrofon zaten aÃƒÂ§Ã„Â±k");
            return;
        }
        if (continuousMode) { 
            if (!isRecognizing) {
                Log.i(TURN, "ÄŸÅ¸ÂÂ¤ MIC AÃƒâ€¡ILIYOR (continuousMode)");
                scheduleRestart(0); 
            }
            return; 
        }
        if (voskState == V_READY) { 
            Log.i(TURN, "ÄŸÅ¸ÂÂ¤ MIC AÃƒâ€¡ILIYOR (Vosk)"); 
            startVosk();
            // ÄŸÅ¸â€Â´ KRÃ„Â°TÃ„Â°K: JS'e mikrofon aÃƒÂ§Ã„Â±ldÃ„Â±Ã„Å¸Ã„Â±nÃ„Â± bildir
            sendSpeechEvent("mic_reopened", "");
        }
        else if (voskState == V_LOADING) {
            Log.i(TURN, "  reopen: Vosk yÃƒÂ¼kleniyor, pendingVoskStart=true");
            pendingVoskStart = true;
        } else {
            Log.w(TURN, "  reopen: voskState=" + voskState + " Ã¢â€ â€™ mikrofon aÃƒÂ§Ã„Â±lamÃ„Â±yor");
        }
    }

    /** JS kalp atÃ„Â±Ã…Å¸Ã„Â±: native mikrofon dinliyor ya da birazdan kendisi aÃƒÂ§acak mÃ„Â±? */
    @JavascriptInterface
    public boolean isNativeListening() {
        return voskActive || pendingVoskStart || continuousMode || (micWanted && inSpeechWindow());
    }

    // Kesintisiz konuÃ…Å¸ma iÃƒÂ§in ek parametreler
    private static final long WORD_MERGE_TIMEOUT_MS = 800;
    private static final float SILENCE_THRESHOLD = 0.10f;
    private static final int MIN_SILENCE_FRAMES = 8;

    private volatile long lastSpeechTimeMs = 0;
    private StringBuilder speechBuffer = new StringBuilder();
    private final Object speechBufferLock = new Object();

    private void initVosk() {
        voskState = V_LOADING;
        try {
            StorageService.unpack(activity, VOSK_ASSET_DIR, "vosk-model",
                    (Model model) -> {
                        voskModel = model;
                        voskState = V_READY;
                        Log.i(TAG, "Vosk modeli hazÃ„Â±r");
                        if (pendingVoskStart) {
                            pendingVoskStart = false;
                            startVosk();
                        }
                    },
                    (IOException e) -> {
                        Log.e(TAG, "Vosk modeli yÃƒÂ¼klenemedi, sistem tanÃ„Â±yÃ„Â±cÃ„Â±ya dÃƒÂ¶nÃƒÂ¼lÃƒÂ¼yor", e);
                        voskState = V_FAILED;
                        if (pendingVoskStart) {
                            pendingVoskStart = false;
                            startSystemSpeech();
                        }
                    });
        } catch (Throwable t) {
            Log.e(TAG, "Vosk baÃ…Å¸latma hatasÃ„Â±", t);
            voskState = V_FAILED;
        }
    }

    private void startVosk() {
        if (inSpeechWindow()) { Log.i(TURN, "start ertelendi (asistan konuÃ…Å¸uyor)"); scheduleReopen(); return; }
        synchronized (this) {
            if (voskActive || voskThread != null) {
                Log.w(TAG, "Vosk zaten aktif, ÃƒÂ§ift baÃ…Å¸latma engellendi");
                return;
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                sendSpeechEvent("error", "Mikrofon izni verilmedi");
                sendSpeechEvent("end", "");
                return;
            }
            voskActive = true;
            final int session = voskSession.incrementAndGet();
            voskThread = new Thread(() -> voskLoop(session), "vosk-audio");
            voskThread.start();
        }
    }

    private void stopVosk() { stopVosk(true); }

    private void stopVosk(boolean notify) {
        Log.i(TAG, "ÄŸÅ¸â€Â´ stopVosk(" + notify + ") ÃƒÂ§aÃ„Å¸rÃ„Â±ldÃ„Â± | voskActive=" + voskActive);
        
        // ÄŸÅ¸â€Â´ KRÃ„Â°TÃ„Â°K: Ãƒâ€“NCE flag'i false yap
        voskActive = false;
        voskSession.incrementAndGet();
        
        AudioRecord recToStop;
        Thread t;
        synchronized (this) {
            recToStop = currentAudioRecord;
            currentAudioRecord = null;
            t = voskThread;
            voskThread = null;
        }

        // ÄŸÅ¸â€Â´ KRÃ„Â°TÃ„Â°K: AudioRecord'u HEMEN durdur
        if (recToStop != null) {
            Log.i(TAG, "  Ã¢â€ â€™ AudioRecord HEMEN durduruluyor...");
            try { recToStop.stop(); Log.i(TAG, "  Ã¢â€ â€™ stop() OK"); } 
            catch (Exception e) { Log.w(TAG, "stop() hata", e); }
            
            try { recToStop.release(); Log.i(TAG, "  Ã¢Å“â€¦ release() OK - MÃ„Â°K SERBEST"); } 
            catch (Exception e) { Log.e(TAG, "  Ã¢ÂÅ’ release() hata", e); }
        }

        // ÄŸÅ¸â€Â´ KRÃ„Â°TÃ„Â°K: Thread TAMAMEN durana kadar bekle (500ms)
        if (t != null && t != Thread.currentThread()) {
            Log.i(TAG, "  Ã¢â€ â€™ Thread join bekleniyor...");
            t.interrupt();
            try { 
                t.join(500); // 200ms Ã¢â€ â€™ 500ms
                Log.i(TAG, "  Ã¢Å“â€¦ Thread join tamamlandÃ„Â±");
            } catch (InterruptedException ie) { 
                Thread.currentThread().interrupt();
                Log.w(TAG, "  Ã¢Å¡Â Ã¯Â¸Â join kesildi");
            }
        }

        // Buffer yÃƒÂ¶netimi
        synchronized (speechBufferLock) {
            if (speechBuffer.length() > 0) {
                Log.i(TAG, "  Ã¢â€ â€™ Buffer: " + speechBuffer.length() + " | notify=" + notify);
                if (notify) {
                    Log.i(TAG, "  Ã¢â€ â€™ GÃƒÂ¶nder: \"" + speechBuffer.toString() + "\"");
                    sendSpeechResult(speechBuffer.toString());
                } else {
                    Log.i(TAG, "  Ã¢â€ â€™ AtÃ„Â±ldÃ„Â± (asistan konuÃ…Å¸)");
                }
                speechBuffer.setLength(0);
            }
        }

        muteBeeps(false);
        if (notify) sendSpeechEvent("stopped", "");
        Log.i(TAG, "Ã¢Å“â€¦ stopVosk TAMAMEN tamamlandÃ„Â±");
    }

    private void voskLoop(final int session) {
        final int rate = 16000;
        AudioRecord rec = null;
        Recognizer recognizer = null;
        AcousticEchoCanceler aec = null;
        NoiseSuppressor ns = null;
        boolean failed = false;
        String failMsg = "";
        try {
            int minBuf = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            // ÄŸÅ¸â€Â´ LATENCY FIX: Buffer'Ã„Â± kÃƒÂ¼ÃƒÂ§ÃƒÂ¼lt (rate yerine minBuf*2 = ~100-200ms)
            int bufBytes = Math.max(minBuf * 2, 8000); // Daha dÃƒÂ¼Ã…Å¸ÃƒÂ¼k latency
            rec = new AudioRecord(MIC_SOURCE, rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufBytes);
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("Mikrofon baÃ…Å¸latÃ„Â±lamadÃ„Â±");
            }

            // DonanÃ„Â±m yankÃ„Â± engelleme + gÃƒÂ¼rÃƒÂ¼ltÃƒÂ¼ bastÃ„Â±rma (varsa). KapÃ„Â± yine de ana savunmadÃ„Â±r.
            try {
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(rec.getAudioSessionId());
                    if (aec != null) aec.setEnabled(true);
                }
            } catch (Throwable t) { Log.w(TAG, "AEC aÃƒÂ§Ã„Â±lamadÃ„Â±", t); }
            try {
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(rec.getAudioSessionId());
                    if (ns != null) ns.setEnabled(true);
                }
            } catch (Throwable t) { Log.w(TAG, "NS aÃƒÂ§Ã„Â±lamadÃ„Â±", t); }
            Log.i(TAG, "Mikrofon: AEC=" + (aec != null) + " NS=" + (ns != null));

            recognizer = new Recognizer(voskModel, rate);
            rec.startRecording();
            currentAudioRecord = rec;
            voskRetryCount = 0;
            sendSpeechEvent("ready", "");
            Log.i(TAG, "ÄŸÅ¸Ââ„¢Ã¯Â¸Â voskLoop: KayÃ„Â±t baÃ…Å¸ladÃ„Â±, dÃƒÂ¶ngÃƒÂ¼ye giriliyor...");

            short[] buf = new short[rate / 20]; // 50 ms (800 sample)
            boolean speaking = false;
            String lastPartial = "";
            int silenceFrameCount = 0;
            int loopCount = 0;
            long lastReadTime = System.currentTimeMillis();

            while (voskActive && voskSession.get() == session) {
                loopCount++;
                long nowMs = System.currentTimeMillis();
                if (loopCount % 100 == 0) { // Her 5 saniyede bir
                    Log.i(TAG, "voskLoop: ÃƒÂ§alÃ„Â±Ã…Å¸Ã„Â±yor, iterasyon=" + loopCount + ", avg_read_ms=" + ((nowMs - lastReadTime) / 100));
                    lastReadTime = nowMs;
                }
                // ÄŸÅ¸â€Â´ KRÃ„Â°TÃ„Â°K: Her iterasyonda kontrol - stopVosk() ÃƒÂ§aÃ„Å¸rÃ„Â±ldÃ„Â±ysa HEMEN ÃƒÂ§Ã„Â±k
                if (!voskActive || voskSession.get() != session) {
                    Log.i(TAG, "voskLoop: flag false oldu, ÃƒÂ§Ã„Â±kÃ„Â±lÃ„Â±yor");
                    break;
                }
                
                int n = rec.read(buf, 0, buf.length);
                if (n < 0) throw new IllegalStateException("Ses kaydÃ„Â± hatasÃ„Â± (" + n + ")");
                if (n == 0) continue;

                float volume = volumeFromPcm(buf, n);
                sendVolume01(volume);
                
                if (loopCount % 20 == 0) { // Her saniye (50ms * 20)
                    Log.i(TAG, "voskLoop: n=" + n + " volume=" + String.format("%.3f", volume));
                }

                // ÄŸÅ¸â€Â´ VOLUME-BASED SUPPRESSION: Asistan konuÃ…Å¸urken yÃƒÂ¼ksek volume Ã¢â€ â€™ Vosk'a gÃƒÂ¶nderme
                if (inSpeechWindow() && volume > 0.15f) {
                    // Asistan konuÃ…Å¸uyor ve yÃƒÂ¼ksek ses var Ã¢â€ â€™ bu asistanÃ„Â±n sesi, Vosk'a gÃƒÂ¶nderme!
                    if (loopCount % 20 == 0) {
                        Log.i(TAG, "ÄŸÅ¸â€â€¡ Volume suppression: " + String.format("%.3f", volume) + " (asistan konuÃ…Å¸uyor)");
                    }
                    // Buffer'Ã„Â± oku ama Vosk'a gÃƒÂ¶nderme - mikrofon aÃƒÂ§Ã„Â±k kalsÃ„Â±n, sadece tanÃ„Â±ma yapma
                    silenceFrameCount = 0; // Sessizlik sayacÃ„Â±nÃ„Â± sÃ„Â±fÃ„Â±rla
                    continue; // Vosk'u atla!
                }

                if (volume < SILENCE_THRESHOLD) silenceFrameCount++; else silenceFrameCount = 0;

                if (recognizer.acceptWaveForm(buf, n)) {
                    String text = new JSONObject(recognizer.getResult()).optString("text", "").trim();
                    Log.i(TAG, "voskLoop: RESULT=" + text);
                    if (!text.isEmpty()) {
                        long now = System.currentTimeMillis();
                        synchronized (speechBufferLock) {
                            if (speechBuffer.length() > 0 && (now - lastSpeechTimeMs) < WORD_MERGE_TIMEOUT_MS) {
                                speechBuffer.append(" ").append(text);
                            } else {
                                if (speechBuffer.length() > 0) sendSpeechResult(speechBuffer.toString());
                                speechBuffer.setLength(0);
                                speechBuffer.append(text);
                            }
                            lastSpeechTimeMs = now;
                        }
                        speaking = false;
                        lastPartial = "";
                    }
                } else {
                    String p = new JSONObject(recognizer.getPartialResult()).optString("partial", "").trim();
                    if (!p.isEmpty()) {
                        if (loopCount % 10 == 0) Log.i(TAG, "voskLoop: PARTIAL=" + p);
                        lastSpeechTimeMs = System.currentTimeMillis();
                        if (!speaking) { speaking = true; sendSpeechEvent("start", ""); }
                        if (!p.equals(lastPartial)) {
                            lastPartial = p;
                            synchronized (speechBufferLock) {
                                sendSpeechPartial(speechBuffer.length() > 0 ? speechBuffer + " " + p : p);
                            }
                        }
                    }
                }

                if (silenceFrameCount >= MIN_SILENCE_FRAMES) {
                    synchronized (speechBufferLock) {
                        if (speechBuffer.length() > 0
                                && (System.currentTimeMillis() - lastSpeechTimeMs) >= WORD_MERGE_TIMEOUT_MS) {
                            sendSpeechResult(speechBuffer.toString());
                            speechBuffer.setLength(0);
                        }
                    }
                    silenceFrameCount = 0;
                }
            }

            // Normal ÃƒÂ§Ã„Â±kÃ„Â±Ã…Å¸ (stopVosk ÃƒÂ§aÃ„Å¸rÃ„Â±ldÃ„Â±): kalan son kelimeler kaybolmasÃ„Â±n (asistan konuÃ…Å¸muyorsa)
            if (!inSpeechWindow()) {
                String fin = new JSONObject(recognizer.getFinalResult()).optString("text", "").trim();
                synchronized (speechBufferLock) {
                    if (!fin.isEmpty()) {
                        if (speechBuffer.length() > 0) speechBuffer.append(" ");
                        speechBuffer.append(fin);
                    }
                    if (speechBuffer.length() > 0) {
                        sendSpeechResult(speechBuffer.toString());
                        speechBuffer.setLength(0);
                    }
                }
            }

        } catch (Exception e) {
            boolean intentional = !voskActive || voskSession.get() != session; // stopVosk AudioRecord'u serbest bÃ„Â±raktÃ„Â±
            if (intentional) {
                Log.d(TAG, "Vosk dÃƒÂ¶ngÃƒÂ¼sÃƒÂ¼ bilinÃƒÂ§li durduruldu");
            } else {
                Log.e(TAG, "Vosk dÃƒÂ¶ngÃƒÂ¼ hatasÃ„Â±", e);
                failed = true;
                failMsg = String.valueOf(e.getMessage());
            }
        } finally {
            synchronized (this) {
                if (currentAudioRecord == rec) currentAudioRecord = null;
                // YalnÃ„Â±zca KENDÃ„Â° oturumumuzsa durumu sÃ„Â±fÃ„Â±rla; yeni oturumu asla bozma.
                if (voskSession.get() == session) {
                    voskActive = false;
                    if (voskThread == Thread.currentThread()) voskThread = null;
                }
            }
            if (aec != null) { try { aec.release(); } catch (Exception ignored) {} }
            if (ns != null)  { try { ns.release(); }  catch (Exception ignored) {} }
            if (rec != null) {
                try { rec.stop(); } catch (Exception ignored) {}
                try { rec.release(); } catch (Exception ignored) {}
            }
            if (recognizer != null) { try { recognizer.close(); } catch (Exception ignored) {} }
        }

        if (failed) onVoskFailure(failMsg);
    }

    /** Mikrofon beklenmedik Ã…Å¸ekilde dÃƒÂ¼Ã…Å¸tÃƒÂ¼yse (baÃ…Å¸ka uygulama, arama vb.) kendini toparlar. */
    private void onVoskFailure(final String msg) {
        mainHandler.post(() -> {
            if (!micWanted || hostPaused) return;
            if (voskRetryCount < 3) {
                final int attempt = ++voskRetryCount;
                Log.w(TAG, "Mikrofon dÃƒÂ¼Ã…Å¸tÃƒÂ¼, yeniden deneniyor (" + attempt + "/3): " + msg);
                mainHandler.postDelayed(() -> {
                    if (micWanted && !hostPaused && !voskActive && voskState == V_READY) startVosk();
                }, 700L * attempt);
            } else {
                sendSpeechEvent("error", msg);
                sendSpeechEvent("end", "");
            }
        });
    }

    /** PCM bloÃ„Å¸undan 0..1 arasÃ„Â± ses seviyesi (arayÃƒÂ¼zdeki ses animasyonu iÃƒÂ§in). */
    private static float volumeFromPcm(short[] buf, int n) {
        double sum = 0;
        for (int i = 0; i < n; i++) sum += (double) buf[i] * buf[i];
        double rms = Math.sqrt(sum / n) / 32768.0;
        double db = 20.0 * Math.log10(Math.max(rms, 1e-6));
        return (float) Math.max(0.0, Math.min(1.0, (db + 60.0) / 40.0));
    }

    private void sendVolume01(float vol) {
        mainHandler.post(() -> {
            // ÄŸÅ¸â€Â´ FIX #8: WebView null check before JS callback
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechVolume) window.onNativeSpeechVolume(" + vol + ");", null);
        });
    }

    // ---------------- JS'in ÃƒÂ§aÃ„Å¸Ã„Â±rdÃ„Â±Ã„Å¸Ã„Â± giriÃ…Å¸ noktalarÃ„Â± ----------------

    /** JS: dinlemeyi baÃ…Å¸latÃ„Â±r. Vosk hazÃ„Â±rsa onu, deÃ„Å¸ilse sistem tanÃ„Â±yÃ„Â±cÃ„Â±yÃ„Â± kullanÃ„Â±r. */
    @JavascriptInterface
    public void startNativeSpeech() {
        micWanted = true;
        mainHandler.post(() -> {
            if (voskActive || continuousMode) return; // zaten dinliyor
            if (voskState == V_LOADING) {
                pendingVoskStart = true; // model aÃƒÂ§Ã„Â±lÃ„Â±nca otomatik baÃ…Å¸lar
                sendSpeechEvent("loading", "");
                return;
            }
            if (voskState == V_READY) {
                startVosk();
                return;
            }
            startSystemSpeech(); // Vosk yok/baÃ…Å¸arÃ„Â±sÃ„Â±z: eski yÃƒÂ¶ntem
        });
    }

    /** (Geriye uyumluluk) Dinlemeyi durdurur. ArtÃ„Â±k ses kanalÃ„Â± odaÃ„Å¸Ã„Â± Ã„Â°STENMEZ: asistan sesi iÃƒÂ§in
     *  mikrofonu kapatmaya gerek yok, bunun yerine beginAssistantSpeech() kullanÃ„Â±lÃ„Â±r. */
    @JavascriptInterface
    public void pauseNativeSpeech() {
        mainHandler.post(() -> {
            if (voskActive) {
                stopVosk();
            } else if (continuousMode && speechRecognizer != null) {
                try { speechRecognizer.cancel(); isRecognizing = false; } catch (Exception e) { Log.e(TAG, "Pause error", e); }
            }
        });
    }

    /** (Geriye uyumluluk) DuraklatÃ„Â±lan dinlemeyi sÃƒÂ¼rdÃƒÂ¼rÃƒÂ¼r. */
    @JavascriptInterface
    public void resumeNativeSpeech() {
        mainHandler.post(() -> {
            if (voskState == V_READY && !voskActive) {
                startVosk();
            } else if (continuousMode && !isRecognizing) {
                scheduleRestart(100);
            }
        });
    }

    /** Activity.onPause: arka planda mikrofon tutulmaz. UI thread. */
    public void onHostPause() {
        hostPaused = true;
        micWasOnBeforePause = micWanted;
        if (voskActive) {
            stopVosk();
        } else if (continuousMode && speechRecognizer != null) {
            try { speechRecognizer.cancel(); isRecognizing = false; } catch (Exception ignored) {}
        }
        if (micWasOnBeforePause) sendSpeechEvent("suspended", "");
    }

    /** Activity.onResume: JS'e "geri dÃƒÂ¶ndÃƒÂ¼k" der; JS sÃƒÂ¼rekli dinleme aÃƒÂ§Ã„Â±ksa mikrofonu yeniden baÃ…Å¸latÃ„Â±r. UI thread. */
    public void onHostResume() {
        hostPaused = false;
        if (micWasOnBeforePause) {
            micWasOnBeforePause = false;
            sendSpeechEvent("resumed", "");
        }
    }

    /** JS: dinlemeyi tamamen durdurur. */
    @JavascriptInterface
    public void stopNativeSpeech() {
        micWanted = false;
        voskRetryCount = 0;
        mainHandler.post(() -> {
            pendingVoskStart = false;
            stopVosk();
            shutdownSpeech(true);
        });
    }

    /** Activity.onDestroy iÃƒÂ§inden (UI thread) ÃƒÂ§aÃ„Å¸Ã„Â±r. */
    public void release() {
        micWanted = false;
        pendingVoskStart = false;
        stopVosk();
        shutdownSpeech(false);
        if (voskModel != null) {
            try { voskModel.close(); } catch (Exception ignored) {}
            voskModel = null;
        }
    }

    // =====================================================================
    //  WEB SPEECH API DESTEK METODLARI
    // =====================================================================

    /**
     * WebView'Ã„Â±n Web Speech API iÃƒÂ§in mikrofon iznini otomatik vermesi iÃƒÂ§in
     * MainActivity'de bu metodu kullanÃ„Â±n:
     * 
     * webView.setWebChromeClient(new WebChromeClient() {
     *     @Override
     *     public void onPermissionRequest(PermissionRequest request) {
     *         if (AndroidBridge.shouldGrantMicrophonePermission(request)) {
     *             request.grant(request.getResources());
     *         }
     *     }
     * });
     */
    public static boolean shouldGrantMicrophonePermission(android.webkit.PermissionRequest request) {
        if (request == null || request.getResources() == null) return false;
        for (String resource : request.getResources()) {
            if (android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Web Speech API'nin dÃƒÂ¼zgÃƒÂ¼n ÃƒÂ§alÃ„Â±Ã…Å¸masÃ„Â± iÃƒÂ§in gerekli WebView ayarlarÃ„Â±nÃ„Â± kontrol eder
     */
    @JavascriptInterface
    public boolean isWebSpeechSupported() {
        // Web Speech API Chrome 71+ desteklenir
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Mikrofon izninin verilip verilmediÃ„Å¸ini kontrol eder
     */
    @JavascriptInterface
    public boolean hasMicrophonePermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Web Speech API iÃƒÂ§in optimize edilmiÃ…Å¸ ses kaydÃ„Â± modu
     * (Daha az CPU, daha iyi pil ÃƒÂ¶mrÃƒÂ¼)
     */
    @JavascriptInterface
    public void enableWebSpeechOptimizations() {
        mainHandler.post(() -> {
            try {
                AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    // Ses odaÃ„Å¸Ã„Â±nÃ„Â± al (diÃ„Å¸er uygulamalar susar)
                    audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    );
                }
            } catch (Exception e) {
                Log.w(TAG, "Audio optimization failed", e);
            }
        });
    }

    /**
     * Ses odaÃ„Å¸Ã„Â±nÃ„Â± bÃ„Â±rak
     */
    @JavascriptInterface
    public void disableWebSpeechOptimizations() {
        mainHandler.post(() -> {
            try {
                AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.abandonAudioFocus(null);
                }
            } catch (Exception e) {
                Log.w(TAG, "Audio focus abandon failed", e);
            }
        });
    }

    // ---------------- Yedek: sistem SpeechRecognizer ----------------
    private void startSystemSpeech() {
        if (continuousMode) return;
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            sendSpeechEvent("error", "Bu cihazda ses tanÃ„Â±ma servisi bulunamadÃ„Â±");
            return;
        }
        continuousMode = true;
        consecutiveErrors = 0;
        readySent = false;
        muteBeeps(true);
        buildRecognizer();
        listenOnce();
    }

    private void buildRecognizer() {
        destroyRecognizer();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                isRecognizing = true;
                consecutiveErrors = 0;
                if (!readySent) {
                    readySent = true;
                    sendSpeechEvent("ready", "");
                }
            }
            @Override public void onBeginningOfSpeech() {
                sendSpeechEvent("start", "");
            }
            @Override public void onRmsChanged(float rmsdB) {
                sendSpeechVolume(rmsdB);
            }
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                isRecognizing = false; // "end" JS'e gÃƒÂ¶nderilmez, dinleme sÃƒÂ¼rÃƒÂ¼yor
            }
            @Override public void onError(int error) {
                isRecognizing = false;
                if (!continuousMode) return;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        scheduleRestart(80); // sessizlik: normal, sessizce yeniden dinle
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        try { speechRecognizer.cancel(); } catch (Exception ignored) {}
                        scheduleRestart(400);
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        scheduleRestart(300);
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        failSpeech(getSpeechErrorMsg(error));
                        break;
                    default: // ses, aÃ„Å¸, sunucu hatalarÃ„Â±
                        if (++consecutiveErrors >= 5) {
                            failSpeech(getSpeechErrorMsg(error));
                        } else {
                            Log.w(TAG, "Speech error (retry): " + getSpeechErrorMsg(error));
                            buildRecognizer();
                            scheduleRestart(1000);
                        }
                }
            }
            @Override public void onResults(Bundle results) {
                isRecognizing = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sendSpeechResult(matches.get(0));
                }
                scheduleRestart(80);
            }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sendSpeechPartial(matches.get(0));
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "tr-TR");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.getPackageName());
        // Bu ipuÃƒÂ§larÃ„Â±nÃ„Â± bazÃ„Â± cihazlar yok sayar; sorun ÃƒÂ§Ã„Â±karmaz.
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
    }

    private void listenOnce() {
        if (!continuousMode || speechRecognizer == null) return;
        try {
            speechRecognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            Log.e(TAG, "startListening error", e);
            if (++consecutiveErrors >= 5) {
                failSpeech(String.valueOf(e.getMessage()));
            } else {
                buildRecognizer();
                scheduleRestart(500);
            }
        }
    }

    private void scheduleRestart(long delayMs) {
        mainHandler.removeCallbacks(restartRunnable);
        if (continuousMode) mainHandler.postDelayed(restartRunnable, delayMs);
    }

    private void failSpeech(String msg) {
        shutdownSpeech(false);
        sendSpeechEvent("error", msg);
        sendSpeechEvent("end", "");
    }

    private void shutdownSpeech(boolean notifyJs) {
        continuousMode = false;
        isRecognizing = false;
        mainHandler.removeCallbacks(restartRunnable);
        destroyRecognizer();
        muteBeeps(false);
        if (notifyJs) sendSpeechEvent("end", "");
    }

    private void destroyRecognizer() {
        if (speechRecognizer != null) {
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
            try { speechRecognizer.destroy(); } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    /** BaÃ…Å¸langÃ„Â±ÃƒÂ§/bitiÃ…Å¸ bip sesini engellemek iÃƒÂ§in ilgili ses kanallarÃ„Â±nÃ„Â± kapatÃ„Â±r/aÃƒÂ§ar.
     *  YalnÃ„Â±zca bizim kapattÃ„Â±Ã„Å¸Ã„Â±mÃ„Â±z kanallarÃ„Â± geri aÃƒÂ§ar (kullanÃ„Â±cÃ„Â± zaten sessize aldÃ„Â±ysa dokunmaz). */
    @SuppressWarnings("deprecation")
    private void muteBeeps(boolean mute) {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        for (int i = 0; i < BEEP_STREAMS.length; i++) {
            int s = BEEP_STREAMS[i];
            try {
                if (mute) {
                    if (mutedByUs[i]) continue;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && am.isStreamMute(s)) continue;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, 0);
                    } else {
                        am.setStreamMute(s, true);
                    }
                    mutedByUs[i] = true;
                } else if (mutedByUs[i]) {
                    mutedByUs[i] = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, 0);
                    } else {
                        am.setStreamMute(s, false);
                    }
                }
            } catch (Exception e) { // ÃƒÂ¶r. SecurityException (RahatsÃ„Â±z Etmeyin politikasÃ„Â±)
                mutedByUs[i] = false;
                Log.w(TAG, "Stream mute/unmute failed: " + s, e);
            }
        }
    }

    // ---------------- JS'e gÃƒÂ¼venli veri gÃƒÂ¶nderimi ----------------
    private static String q(String s) {
        return JSONObject.quote(s == null ? "" : s); // tÃ„Â±rnak, ters bÃƒÂ¶lÃƒÂ¼, satÃ„Â±r sonu vb. gÃƒÂ¼venle kaÃƒÂ§Ã„Â±rÃ„Â±lÃ„Â±r
    }

    // ÄŸÅ¸â€Â´ ASISTAN CEVAPLARINI FÃ„Â°LTRELE - SADECE asistanÃ„Â±n konuÃ…Å¸tuÃ„Å¸u tam cÃƒÂ¼mleler
    // NOT: KullanÃ„Â±cÃ„Â± komutlarÃ„Â± ("geri al", "sayÃ„Â±m", "seÃƒÂ§ildi") burada OLMAMALI!
    private static final String[] ASISTAN_CUMLELERI = {
        "yazÃ„Â±lÃ„Â±mcÃ„Â±sÃ„Â± sedat daÃ„Å¸lÃ„Â±dÃ„Â±r",
        "sayÃ„Â±m kaydedildi",
        "ÃƒÂ¼rÃƒÂ¼n sayÃ„Â±mdan ÃƒÂ§Ã„Â±karÃ„Â±ldÃ„Â±",
        "sayÃ„Â±m miktarÃ„Â± gÃƒÂ¼ncellendi",
        "yeni sevkiyat baÃ…Å¸latÃ„Â±ldÃ„Â±",
        "sevkiyat kapatÃ„Â±ldÃ„Â±",
        "ÃƒÂ¼rÃƒÂ¼n sevkiyata eklendi",
        "ÃƒÂ¼rÃƒÂ¼n sevkiyattan ÃƒÂ§Ã„Â±karÃ„Â±ldÃ„Â±",
        "aÃƒÂ§Ã„Â±k sevkiyat yok ÃƒÂ¶nce sevkiyat baÃ…Å¸latÃ„Â±n",
        "fire kaydÃ„Â± baÃ…Å¸arÃ„Â±yla oluÃ…Å¸turuldu",
        "ÃƒÂ¼rÃƒÂ¼n dÃƒÂ¼zenleme ekranÃ„Â± aÃƒÂ§Ã„Â±ldÃ„Â±",
        "kasa aÃ„Å¸Ã„Â±rlÃ„Â±Ã„Å¸Ã„Â± ayarlandÃ„Â±",
        "ÃƒÂ¼rÃƒÂ¼n birimi gÃƒÂ¼ncellendi",
        "ÃƒÂ¼rÃƒÂ¼n baÃ…Å¸arÃ„Â±yla kaydedildi",
        "dÃƒÂ¼zenleme iptal edildi",
        "ilgili sayfa aÃƒÂ§Ã„Â±ldÃ„Â±",
        "filtre uygulandÃ„Â±",
        "kamera aÃƒÂ§Ã„Â±ldÃ„Â±",
        "kamera kapatÃ„Â±ldÃ„Â±",
        "whatsapp raporu hazÃ„Â±rlanÃ„Â±yor",
        "son iÃ…Å¸lem geri alÃ„Â±ndÃ„Â±",
        "geri alÃ„Â±nacak iÃ…Å¸lem bulunmuyor",
        "anlaÃ…Å¸Ã„Â±lamadÃ„Â± lÃƒÂ¼tfen tekrar edin",
        "sesli asistan kapatÃ„Â±ldÃ„Â±",
        "sesli asistan hazÃ„Â±r sizi dinliyorum",
        "yeni ÃƒÂ¼rÃƒÂ¼n ekleme formu aÃƒÂ§Ã„Â±ldÃ„Â±",
        "p l u kodu gÃƒÂ¼ncellendi",
        "plu kodu gÃƒÂ¼ncellendi",
        "barkod gÃƒÂ¼ncellendi",
        "ÃƒÂ¼rÃƒÂ¼n adÃ„Â± gÃƒÂ¼ncellendi",
        "sayÃ„Â±m durumu ÃƒÂ¶zetlendi",
        "gÃƒÂ¼nlÃƒÂ¼k fire raporu hazÃ„Â±rlandÃ„Â±",
        "sevkiyat durumu ÃƒÂ¶zetlendi",
        "belirtilen ÃƒÂ¼rÃƒÂ¼n veya kod bulunamadÃ„Â±",
        "ÃƒÂ¼rÃƒÂ¼n bulundu ve seÃƒÂ§ildi",
        "mikrofon duraklatÃ„Â±ldÃ„Â±"
    };

    private boolean isAssistantEcho(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String normalized = normalizeTurkish(text.toLowerCase().trim());
        
        // ÄŸÅ¸â€Â´ KRÃ„Â°TÃ„Â°K: SADECE uzun cÃƒÂ¼mleler iÃƒÂ§in substring match
        // KÃ„Â±sa komutlar iÃƒÂ§in EXACT match gerekli (yoksa "geri al" komutunu da filtreliyoruz!)
        for (String phrase : ASISTAN_CUMLELERI) {
            String normalizedPhrase = normalizeTurkish(phrase.toLowerCase());
            
            // Asistan cÃƒÂ¼mlesi 5 kelimeden uzunsa substring match OK
            boolean isLongPhrase = normalizedPhrase.split("\\s+").length >= 5;
            
            if (isLongPhrase) {
                // Uzun cÃƒÂ¼mleler: substring match
                if (normalized.contains(normalizedPhrase) || normalizedPhrase.contains(normalized)) {
                    Log.i(TAG, "ÄŸÅ¸Å¡Â« Native Echo Filter (substring): '" + text + "' ~ '" + phrase + "'");
                    return true;
                }
            } else {
                // KÃ„Â±sa cÃƒÂ¼mleler: exact match (kelime sÃ„Â±nÃ„Â±rlarÃ„Â± kontrol et)
                if (normalized.equals(normalizedPhrase)) {
                    Log.i(TAG, "ÄŸÅ¸Å¡Â« Native Echo Filter (exact): '" + text + "' == '" + phrase + "'");
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeTurkish(String text) {
        return text.replaceAll("[ÃƒÂ¼ÃƒÂ»]", "u")
                   .replaceAll("[ÃƒÂ®Ã„Â±]", "i")
                   .replaceAll("[ÃƒÂ¶ÃƒÂ´]", "o")
                   .replaceAll("[ÃƒÂ§Ã„â€¡]", "c")
                   .replaceAll("[Ã…Å¸Ã…â€º]", "s")
                   .replaceAll("[Ã„Å¸]", "g")
                   .replaceAll("[^a-z0-9]", "");
    }

    private void sendSpeechResult(String text) {
        // ÄŸÅ¸â€Â´ ECHO FÃ„Â°LTRESÃ„Â°: Asistan seslerini JS'e gÃƒÂ¶ndermeden filtrele
        if (isAssistantEcho(text)) {
            return; // Loop'u ÃƒÂ¶nle!
        }
        
        Log.i(TAG, "Ã¢Å“â€¦ KullanÃ„Â±cÃ„Â± komutu JS'e gÃƒÂ¶nderiliyor: " + text);
        mainHandler.post(() -> {
            // ÄŸÅ¸â€Â´ FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechResult) window.onNativeSpeechResult(" + q(text) + ");", null);
        });
    }

    private void sendSpeechPartial(String text) {
        mainHandler.post(() -> {
            // ÄŸÅ¸â€Â´ FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechPartial) window.onNativeSpeechPartial(" + q(text) + ");", null);
        });
    }

    private void sendSpeechEvent(String eventType, String msg) {
        mainHandler.post(() -> {
            // ÄŸÅ¸â€Â´ FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechEvent) window.onNativeSpeechEvent(" + q(eventType) + ", " + q(msg) + ");", null);
        });
    }

    private void sendSpeechVolume(float rmsdB) {
        float vol = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
        mainHandler.post(() -> {
            // ÄŸÅ¸â€Â´ FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechVolume) window.onNativeSpeechVolume(" + vol + ");", null);
        });
    }

    private String getSpeechErrorMsg(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Ses kaydÃ„Â± hatasÃ„Â±";
            case SpeechRecognizer.ERROR_CLIENT: return "Ã„Â°stemci hatasÃ„Â±";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Mikrofon izni verilmedi";
            case SpeechRecognizer.ERROR_NETWORK: return "AÃ„Å¸ hatasÃ„Â±";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "AÃ„Å¸ zaman aÃ…Å¸Ã„Â±mÃ„Â±";
            case SpeechRecognizer.ERROR_NO_MATCH: return "Ses eÃ…Å¸leÃ…Å¸medi";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Servis meÃ…Å¸gul";
            case SpeechRecognizer.ERROR_SERVER: return "Sunucu hatasÃ„Â±";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "Ses algÃ„Â±lanamadÃ„Â±";
            default: return "Hata kodu: " + error;
        }
    }
    

}
