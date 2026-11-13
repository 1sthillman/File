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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Base64;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
     * Dosyayı cihaz hafızasına kaydeder ve Android Share Sheet açarak paylaşmayı sağlar.
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

                // 1. Fiziksel Kayıt (İndirilenler / Downloads)
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

                // Fallback direct storage save (Android 9 ve altı veya MediaStore başarısız olursa)
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

                // 2. Paylaşım için Güvenli Cache Dosyası
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

                // 3. Android Native Share Sheet (Dosyayı Paylaş / Kaydet / Gönder)
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType(mimeType != null && !mimeType.isEmpty() ? mimeType : "*/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                shareIntent.putExtra(Intent.EXTRA_TEXT, fileName + " - Manav Sayım Raporu");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(shareIntent, "Dosyayı Paylaş / Farklı Kaydet");
                activity.startActivity(chooser);

            } catch (Exception e) {
                Log.e(TAG, "Save and Share error", e);
                Toast.makeText(activity, "Dosya aktarım hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Düz metin paylaşımı
     */
    @JavascriptInterface
    public void shareText(String text, String title) {
        mainHandler.post(() -> {
            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, title != null ? title : "Paylaş");
                activity.startActivity(shareIntent);
            } catch (Exception e) {
                Log.e(TAG, "Share text error", e);
            }
        });
    }

    // =====================================================================
    //  SÜREKLİ SES TANIMA (bip sesi yok, otomatik yeniden başlatma native tarafta)
    // =====================================================================

    // true: dinleme sırasında müzik kanalı da susturulur (bip bazı cihazlarda bu kanaldan çalar).
    // Vosk AudioRecord kullandığı için bip sesi zaten yok — müzik kanalını kapatma!
    // Ses dosyaları (STREAM_MUSIC) üzerinden çalıyor, kapatırsak ses duyulmaz.
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
    //  VOSK: ÇEVRİMDIŞI, KESİNTİSİZ TANIMA (bip yok, oturum boşluğu yok)
    // =====================================================================
    // Model: app/src/main/assets/model-tr/  (içinde ayrıca "uuid" adlı bir dosya olmalı)
    private static final String VOSK_ASSET_DIR = "model-tr";
    private static final int V_IDLE = 0, V_LOADING = 1, V_READY = 2, V_FAILED = 3;

    private volatile int voskState = V_IDLE;
    private Model voskModel;
    private volatile boolean voskActive = false;
    private boolean pendingVoskStart = false;
    private Thread voskThread;
    private volatile AudioRecord currentAudioRecord = null; // stopVosk anında durdursun diye
    
    // Kesintisiz konuşma için ek parametreler
    private static final long WORD_MERGE_TIMEOUT_MS = 1200; // kelimeler arası sessizlik toleransı
    private static final float SILENCE_THRESHOLD = 0.08f;   // sessizlik eşiği (0-1 arası)
    private static final int MIN_SILENCE_FRAMES = 12;       // minimum sessiz frame sayısı (yaklaşık 1.2 saniye)
    
    private long lastSpeechTimeMs = 0;
    private StringBuilder speechBuffer = new StringBuilder();
    private final Object speechBufferLock = new Object();

    private void initVosk() {
        voskState = V_LOADING;
        try {
            StorageService.unpack(activity, VOSK_ASSET_DIR, "vosk-model",
                    (Model model) -> {
                        voskModel = model;
                        voskState = V_READY;
                        Log.i(TAG, "Vosk modeli hazır");
                        if (pendingVoskStart) {
                            pendingVoskStart = false;
                            startVosk();
                        }
                    },
                    (IOException e) -> {
                        Log.e(TAG, "Vosk modeli yüklenemedi, sistem tanıyıcıya dönülüyor", e);
                        voskState = V_FAILED;
                        if (pendingVoskStart) {
                            pendingVoskStart = false;
                            startSystemSpeech();
                        }
                    });
        } catch (Throwable t) {
            Log.e(TAG, "Vosk başlatma hatası", t);
            voskState = V_FAILED;
        }
    }

    private void startVosk() {
        if (voskActive) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            sendSpeechEvent("error", "Mikrofon izni verilmedi");
            sendSpeechEvent("end", "");
            return;
        }
        voskActive = true;
        voskThread = new Thread(this::voskLoop, "vosk-audio");
        voskThread.start();
    }

    private void stopVosk() {
        voskActive = false;
        // AudioRecord'u anında durdur — ses çalmaya engel olmasın
        AudioRecord recToStop = currentAudioRecord;
        if (recToStop != null) {
            try { recToStop.stop(); } catch (Exception ignored) {}
        }
        // Buffer'daki son veriyi gönder
        synchronized (speechBufferLock) {
            if (speechBuffer.length() > 0) {
                sendSpeechResult(speechBuffer.toString());
                speechBuffer.setLength(0);
            }
        }
        Thread t = voskThread;
        voskThread = null;
        if (t != null && t != Thread.currentThread()) {
            t.interrupt();
            try { t.join(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        // Ses kanallarını kesinlikle aç — ses dosyaları çalabilsin
        muteBeeps(false);
        // JS'e Vosk'un tamamen durduğunu bildir — ses çalma bu eventi bekliyor
        sendSpeechEvent("stopped", "");
    }

    private void voskLoop() {
        final int rate = 16000;
        AudioRecord rec = null;
        Recognizer recognizer = null;
        try {
            int minBuf = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufBytes = Math.max(minBuf, rate * 2); // en az ~1 sn tampon
            rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufBytes);
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("Mikrofon başlatılamadı");
            }
            currentAudioRecord = rec; // stopVosk() anında durdurabilsin
            recognizer = new Recognizer(voskModel, rate);
            rec.startRecording();
            sendSpeechEvent("ready", "");

            short[] buf = new short[rate / 10]; // 100 ms
            boolean speaking = false;
            String lastPartial = "";
            int silenceFrameCount = 0;
            long sessionStartTime = System.currentTimeMillis();

            while (voskActive) {
                int n = rec.read(buf, 0, buf.length);
                if (n < 0) throw new IllegalStateException("Ses kaydı hatası (" + n + ")");
                if (n == 0) continue;

                float volume = volumeFromPcm(buf, n);
                sendVolume01(volume);

                // VAD: Sessizlik algılama
                boolean isSilent = volume < SILENCE_THRESHOLD;
                if (isSilent) {
                    silenceFrameCount++;
                } else {
                    silenceFrameCount = 0;
                }

                if (recognizer.acceptWaveForm(buf, n)) {
                    String text = new JSONObject(recognizer.getResult()).optString("text", "").trim();
                    
                    if (!text.isEmpty()) {
                        long now = System.currentTimeMillis();
                        
                        synchronized (speechBufferLock) {
                            // Kelime birleştirme: son kelimeden bu yana geçen süre kısa mı?
                            if (speechBuffer.length() > 0 && (now - lastSpeechTimeMs) < WORD_MERGE_TIMEOUT_MS) {
                                // Aynı cümlenin devamı, boşluk ekle
                                speechBuffer.append(" ").append(text);
                            } else {
                                // Yeni cümle başladı
                                if (speechBuffer.length() > 0) {
                                    // Önceki cümleyi gönder
                                    sendSpeechResult(speechBuffer.toString());
                                }
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
                        long now = System.currentTimeMillis();
                        lastSpeechTimeMs = now;
                        
                        if (!speaking) { 
                            speaking = true; 
                            sendSpeechEvent("start", ""); 
                        }
                        
                        if (!p.equals(lastPartial)) { 
                            lastPartial = p; 
                            
                            // Partial sonuçları buffer ile birleştirerek göster
                            synchronized (speechBufferLock) {
                                String fullPartial = speechBuffer.length() > 0 
                                    ? speechBuffer.toString() + " " + p 
                                    : p;
                                sendSpeechPartial(fullPartial);
                            }
                        }
                    }
                }
                
                // Uzun sessizlik varsa buffer'ı gönder
                if (silenceFrameCount >= MIN_SILENCE_FRAMES) {
                    synchronized (speechBufferLock) {
                        if (speechBuffer.length() > 0) {
                            long now = System.currentTimeMillis();
                            if ((now - lastSpeechTimeMs) >= WORD_MERGE_TIMEOUT_MS) {
                                sendSpeechResult(speechBuffer.toString());
                                speechBuffer.setLength(0);
                            }
                        }
                    }
                    silenceFrameCount = 0;
                }
            }
            
            // Durdurulurken son kelimeler kaybolmasın
            String fin = new JSONObject(recognizer.getFinalResult()).optString("text", "").trim();
            synchronized (speechBufferLock) {
                if (!fin.isEmpty() && speechBuffer.length() > 0) {
                    speechBuffer.append(" ").append(fin);
                } else if (!fin.isEmpty()) {
                    speechBuffer.append(fin);
                }
                if (speechBuffer.length() > 0) {
                    sendSpeechResult(speechBuffer.toString());
                    speechBuffer.setLength(0);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Vosk döngü hatası", e);
            if (voskActive) {
                voskActive = false;
                sendSpeechEvent("error", String.valueOf(e.getMessage()));
                sendSpeechEvent("end", "");
            }
        } finally {
            currentAudioRecord = null;
            if (rec != null) {
                try { rec.stop(); } catch (Exception ignored) {}
                rec.release();
            }
            if (recognizer != null) {
                try { recognizer.close(); } catch (Exception ignored) {}
            }
        }
    }

    /** PCM bloğundan 0..1 arası ses seviyesi (arayüzdeki ses animasyonu için). */
    private static float volumeFromPcm(short[] buf, int n) {
        double sum = 0;
        for (int i = 0; i < n; i++) sum += (double) buf[i] * buf[i];
        double rms = Math.sqrt(sum / n) / 32768.0;
        double db = 20.0 * Math.log10(Math.max(rms, 1e-6));
        return (float) Math.max(0.0, Math.min(1.0, (db + 60.0) / 40.0));
    }

    private void sendVolume01(float vol) {
        mainHandler.post(() -> webView.evaluateJavascript(
                "if(window.onNativeSpeechVolume) window.onNativeSpeechVolume(" + vol + ");", null));
    }

    // ---------------- JS'in çağırdığı giriş noktaları ----------------

    /** JS: dinlemeyi başlatır. Vosk hazırsa onu, değilse sistem tanıyıcıyı kullanır. */
    @JavascriptInterface
    public void startNativeSpeech() {
        mainHandler.post(() -> {
            if (voskActive || continuousMode) return; // zaten dinliyor
            if (voskState == V_LOADING) {
                pendingVoskStart = true; // model açılınca otomatik başlar
                sendSpeechEvent("loading", "");
                return;
            }
            if (voskState == V_READY) {
                startVosk();
                return;
            }
            startSystemSpeech(); // Vosk yok/başarısız: eski yöntem
        });
    }

    /** JS: dinlemeyi tamamen durdurur. */
    @JavascriptInterface
    public void stopNativeSpeech() {
        mainHandler.post(() -> {
            pendingVoskStart = false;
            stopVosk();
            shutdownSpeech(true);
        });
    }

    /** Activity.onDestroy içinden (UI thread) çağır. */
    public void release() {
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
     * WebView'ın Web Speech API için mikrofon iznini otomatik vermesi için
     * MainActivity'de bu metodu kullanın:
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
     * Web Speech API'nin düzgün çalışması için gerekli WebView ayarlarını kontrol eder
     */
    @JavascriptInterface
    public boolean isWebSpeechSupported() {
        // Web Speech API Chrome 71+ desteklenir
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Mikrofon izninin verilip verilmediğini kontrol eder
     */
    @JavascriptInterface
    public boolean hasMicrophonePermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Web Speech API için optimize edilmiş ses kaydı modu
     * (Daha az CPU, daha iyi pil ömrü)
     */
    @JavascriptInterface
    public void enableWebSpeechOptimizations() {
        mainHandler.post(() -> {
            try {
                AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    // Ses odağını al (diğer uygulamalar susar)
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
     * Ses odağını bırak
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
            sendSpeechEvent("error", "Bu cihazda ses tanıma servisi bulunamadı");
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
                isRecognizing = false; // "end" JS'e gönderilmez, dinleme sürüyor
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
                    default: // ses, ağ, sunucu hataları
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
        // Bu ipuçlarını bazı cihazlar yok sayar; sorun çıkarmaz.
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

    /** Başlangıç/bitiş bip sesini engellemek için ilgili ses kanallarını kapatır/açar.
     *  Yalnızca bizim kapattığımız kanalları geri açar (kullanıcı zaten sessize aldıysa dokunmaz). */
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
            } catch (Exception e) { // ör. SecurityException (Rahatsız Etmeyin politikası)
                mutedByUs[i] = false;
                Log.w(TAG, "Stream mute/unmute failed: " + s, e);
            }
        }
    }

    // ---------------- JS'e güvenli veri gönderimi ----------------
    private static String q(String s) {
        return JSONObject.quote(s == null ? "" : s); // tırnak, ters bölü, satır sonu vb. güvenle kaçırılır
    }

    private void sendSpeechResult(String text) {
        mainHandler.post(() -> webView.evaluateJavascript(
                "if(window.onNativeSpeechResult) window.onNativeSpeechResult(" + q(text) + ");", null));
    }

    private void sendSpeechPartial(String text) {
        mainHandler.post(() -> webView.evaluateJavascript(
                "if(window.onNativeSpeechPartial) window.onNativeSpeechPartial(" + q(text) + ");", null));
    }

    private void sendSpeechEvent(String eventType, String msg) {
        mainHandler.post(() -> webView.evaluateJavascript(
                "if(window.onNativeSpeechEvent) window.onNativeSpeechEvent(" + q(eventType) + ", " + q(msg) + ");", null));
    }

    private void sendSpeechVolume(float rmsdB) {
        float vol = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
        mainHandler.post(() -> webView.evaluateJavascript(
                "if(window.onNativeSpeechVolume) window.onNativeSpeechVolume(" + vol + ");", null));
    }

    private String getSpeechErrorMsg(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Ses kaydı hatası";
            case SpeechRecognizer.ERROR_CLIENT: return "İstemci hatası";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Mikrofon izni verilmedi";
            case SpeechRecognizer.ERROR_NETWORK: return "Ağ hatası";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Ağ zaman aşımı";
            case SpeechRecognizer.ERROR_NO_MATCH: return "Ses eşleşmedi";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Servis meşgul";
            case SpeechRecognizer.ERROR_SERVER: return "Sunucu hatası";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "Ses algılanamadı";
            default: return "Hata kodu: " + error;
        }
    }
}
