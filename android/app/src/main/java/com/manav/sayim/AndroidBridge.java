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
    private volatile AudioRecord currentAudioRecord = null;

    // Her mikrofon oturumuna benzersiz kimlik: eski (zombi) iş parçacığı yeni oturumu asla bozamaz.
    private final AtomicInteger voskSession = new AtomicInteger(0);
    private volatile int voskRetryCount = 0;     // mikrofon beklenmedik düşerse otomatik yeniden deneme sayacı
    private volatile boolean micWanted = false;  // kullanıcı/JS mikrofonu istiyor mu?
    private boolean hostPaused = false;          // uygulama arka planda mı?
    private boolean micWasOnBeforePause = false;

    // Mikrofon kaynağı. VOICE_RECOGNITION: ham/temiz ses. VOICE_COMMUNICATION: bazı cihazlarda
    // donanım yankı engelleme (AEC) daha iyi çalışır ama medya sesini kısabilir. Cihazda test ederek seçin.
    private static final int MIC_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;

    // ---------------- SIRA ALMA (HALF-DUPLEX) ----------------
    // TEK SAHİBİ native taraf. Asistan konuşurken mikrofon fiziksel olarak serbest bırakılır,
    // konuşma penceresi bitince native KENDİ KENDİNE yeniden açar. JS yalnızca "şu kadar ms konuşacağım" der;
    // mikrofonu açıp kapatmaz. Böylece iki taraf birbirini çekiştiremez.
    private static final String TURN = "VoiceTurn";
    private static final long SPEECH_TAIL_MS = 500;   // 🔴 FIX: 350 → 500 (ses bittikten sonra hoparlör buffer temizliği)
    private static final long SPEECH_PAD_MS  = 300;   // 🔴 FIX: 400 → 300 (play() zaten hızlı başlıyor)
    private static final long SPEECH_MAX_MS  = 15000; // emniyet üst sınırı: pencere asla bundan uzun kalamaz
    private volatile long speechWindowEndMs = 0;
    private final Runnable reopenRunnable = this::reopenAfterSpeech;

    private boolean inSpeechWindow() {
        return SystemClock.elapsedRealtime() < speechWindowEndMs;
    }

    /** JS: asistan sesi çalmadan HEMEN ÖNCE (ve çalmaya başlayınca kalan süreyle) çağırır. */
    @JavascriptInterface
    public void beginAssistantSpeech(final int durationMs) {
        final long ms = Math.max(300, Math.min(durationMs > 0 ? durationMs : 4000, SPEECH_MAX_MS));
        mainHandler.post(() -> {
            speechWindowEndMs = SystemClock.elapsedRealtime() + ms + SPEECH_PAD_MS + SPEECH_TAIL_MS;
            // 🔴 YENİ YAKLAŞIM: Mikrofonu KAPATMIYORUZ - sürekli açık!
            Log.i(TURN, "🔴 SPEAK BEGIN " + ms + "ms | pad=" + SPEECH_PAD_MS + "ms | tail=" + SPEECH_TAIL_MS + "ms | mic=AÇIK KALACAK (yankı filtresi) | wanted=" + micWanted + " | inWindow=" + inSpeechWindow());
            
            // Vosk çalışmaya devam eder, JS tarafı isSpeaking=true iken sonuçları filtreleyecek
            Log.i(TURN, "  → Mikrofon açık, Vosk tanımaya devam ediyor (JS filtre aktif)");
        });
    }

    /** JS: ses erken bittiyse (ended) pencereyi kısaltır; mikrofon tail sonrası açılır. */
    @JavascriptInterface
    public void endAssistantSpeech() {
        mainHandler.post(() -> {
            if (!inSpeechWindow()) {
                Log.i(TURN, "⚠️ END çağrıldı ama pencere zaten bitmiş");
                return;
            }
            long oldEnd = speechWindowEndMs;
            speechWindowEndMs = SystemClock.elapsedRealtime() + SPEECH_TAIL_MS;
            Log.i(TURN, "🟢 SPEAK END | pencere kısaltıldı (" + (oldEnd - speechWindowEndMs) + "ms) | MİKROFON ZATEN AÇIK (filtre kalkacak)");
            // Artık scheduleReopen() çağırmıyoruz - mikrofon zaten açık!
            // JS tarafında isSpeaking=false olunca filtre kalkacak
        });
    }

    private void scheduleReopen() {
        mainHandler.removeCallbacks(reopenRunnable);
        long delay = Math.max(0, speechWindowEndMs - SystemClock.elapsedRealtime());
        mainHandler.postDelayed(reopenRunnable, delay + 20);
    }

    private void reopenAfterSpeech() {
        if (inSpeechWindow()) { 
            Log.i(TURN, "  reopen: pencere henüz aktif, yeniden zamanlanıyor");
            scheduleReopen(); 
            return; 
        }   // pencere uzatıldıysa yeniden bekle
        if (!micWanted || hostPaused) {
            Log.i(TURN, "  reopen: micWanted=" + micWanted + " hostPaused=" + hostPaused + " → mikrofon açılmayacak");
            return;
        }
        if (voskActive) {
            Log.i(TURN, "  reopen: mikrofon zaten açık");
            return;
        }
        if (continuousMode) { 
            if (!isRecognizing) {
                Log.i(TURN, "🎤 MIC AÇILIYOR (continuousMode)");
                scheduleRestart(0); 
            }
            return; 
        }
        if (voskState == V_READY) { 
            Log.i(TURN, "🎤 MIC AÇILIYOR (Vosk)"); 
            startVosk();
            // 🔴 KRİTİK: JS'e mikrofon açıldığını bildir
            sendSpeechEvent("mic_reopened", "");
        }
        else if (voskState == V_LOADING) {
            Log.i(TURN, "  reopen: Vosk yükleniyor, pendingVoskStart=true");
            pendingVoskStart = true;
        } else {
            Log.w(TURN, "  reopen: voskState=" + voskState + " → mikrofon açılamıyor");
        }
    }

    /** JS kalp atışı: native mikrofon dinliyor ya da birazdan kendisi açacak mı? */
    @JavascriptInterface
    public boolean isNativeListening() {
        return voskActive || pendingVoskStart || continuousMode || (micWanted && inSpeechWindow());
    }

    // Kesintisiz konuşma için ek parametreler
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
        if (inSpeechWindow()) { Log.i(TURN, "start ertelendi (asistan konuşuyor)"); scheduleReopen(); return; }
        synchronized (this) {
            if (voskActive || voskThread != null) {
                Log.w(TAG, "Vosk zaten aktif, çift başlatma engellendi");
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
        Log.i(TAG, "🔴 stopVosk(" + notify + ") çağrıldı | voskActive=" + voskActive);
        
        // 🔴 KRİTİK: ÖNCE flag'i false yap
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

        // 🔴 KRİTİK: AudioRecord'u HEMEN durdur
        if (recToStop != null) {
            Log.i(TAG, "  → AudioRecord HEMEN durduruluyor...");
            try { recToStop.stop(); Log.i(TAG, "  → stop() OK"); } 
            catch (Exception e) { Log.w(TAG, "stop() hata", e); }
            
            try { recToStop.release(); Log.i(TAG, "  ✅ release() OK - MİK SERBEST"); } 
            catch (Exception e) { Log.e(TAG, "  ❌ release() hata", e); }
        }

        // 🔴 KRİTİK: Thread TAMAMEN durana kadar bekle (500ms)
        if (t != null && t != Thread.currentThread()) {
            Log.i(TAG, "  → Thread join bekleniyor...");
            t.interrupt();
            try { 
                t.join(500); // 200ms → 500ms
                Log.i(TAG, "  ✅ Thread join tamamlandı");
            } catch (InterruptedException ie) { 
                Thread.currentThread().interrupt();
                Log.w(TAG, "  ⚠️ join kesildi");
            }
        }

        // Buffer yönetimi
        synchronized (speechBufferLock) {
            if (speechBuffer.length() > 0) {
                Log.i(TAG, "  → Buffer: " + speechBuffer.length() + " | notify=" + notify);
                if (notify) {
                    Log.i(TAG, "  → Gönder: \"" + speechBuffer.toString() + "\"");
                    sendSpeechResult(speechBuffer.toString());
                } else {
                    Log.i(TAG, "  → Atıldı (asistan konuş)");
                }
                speechBuffer.setLength(0);
            }
        }

        muteBeeps(false);
        if (notify) sendSpeechEvent("stopped", "");
        Log.i(TAG, "✅ stopVosk TAMAMEN tamamlandı");
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
            // 🔴 LATENCY FIX: Buffer'ı küçült (rate yerine minBuf*2 = ~100-200ms)
            int bufBytes = Math.max(minBuf * 2, 8000); // Daha düşük latency
            rec = new AudioRecord(MIC_SOURCE, rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufBytes);
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("Mikrofon başlatılamadı");
            }

            // Donanım yankı engelleme + gürültü bastırma (varsa). Kapı yine de ana savunmadır.
            try {
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(rec.getAudioSessionId());
                    if (aec != null) aec.setEnabled(true);
                }
            } catch (Throwable t) { Log.w(TAG, "AEC açılamadı", t); }
            try {
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(rec.getAudioSessionId());
                    if (ns != null) ns.setEnabled(true);
                }
            } catch (Throwable t) { Log.w(TAG, "NS açılamadı", t); }
            Log.i(TAG, "Mikrofon: AEC=" + (aec != null) + " NS=" + (ns != null));

            recognizer = new Recognizer(voskModel, rate);
            rec.startRecording();
            currentAudioRecord = rec;
            voskRetryCount = 0;
            sendSpeechEvent("ready", "");
            Log.i(TAG, "🎙️ voskLoop: Kayıt başladı, döngüye giriliyor...");

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
                    Log.i(TAG, "voskLoop: çalışıyor, iterasyon=" + loopCount + ", avg_read_ms=" + ((nowMs - lastReadTime) / 100));
                    lastReadTime = nowMs;
                }
                // 🔴 KRİTİK: Her iterasyonda kontrol - stopVosk() çağrıldıysa HEMEN çık
                if (!voskActive || voskSession.get() != session) {
                    Log.i(TAG, "voskLoop: flag false oldu, çıkılıyor");
                    break;
                }
                
                int n = rec.read(buf, 0, buf.length);
                if (n < 0) throw new IllegalStateException("Ses kaydı hatası (" + n + ")");
                if (n == 0) continue;

                float volume = volumeFromPcm(buf, n);
                sendVolume01(volume);
                
                if (loopCount % 20 == 0) { // Her saniye (50ms * 20)
                    Log.i(TAG, "voskLoop: n=" + n + " volume=" + String.format("%.3f", volume));
                }

                // 🔴 VOLUME-BASED SUPPRESSION: Asistan konuşurken yüksek volume → Vosk'a gönderme
                if (inSpeechWindow() && volume > 0.15f) {
                    // Asistan konuşuyor ve yüksek ses var → bu asistanın sesi, Vosk'a gönderme!
                    if (loopCount % 20 == 0) {
                        Log.i(TAG, "🔇 Volume suppression: " + String.format("%.3f", volume) + " (asistan konuşuyor)");
                    }
                    // Buffer'ı oku ama Vosk'a gönderme - mikrofon açık kalsın, sadece tanıma yapma
                    silenceFrameCount = 0; // Sessizlik sayacını sıfırla
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

            // Normal çıkış (stopVosk çağrıldı): kalan son kelimeler kaybolmasın (asistan konuşmuyorsa)
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
            boolean intentional = !voskActive || voskSession.get() != session; // stopVosk AudioRecord'u serbest bıraktı
            if (intentional) {
                Log.d(TAG, "Vosk döngüsü bilinçli durduruldu");
            } else {
                Log.e(TAG, "Vosk döngü hatası", e);
                failed = true;
                failMsg = String.valueOf(e.getMessage());
            }
        } finally {
            synchronized (this) {
                if (currentAudioRecord == rec) currentAudioRecord = null;
                // Yalnızca KENDİ oturumumuzsa durumu sıfırla; yeni oturumu asla bozma.
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

    /** Mikrofon beklenmedik şekilde düştüyse (başka uygulama, arama vb.) kendini toparlar. */
    private void onVoskFailure(final String msg) {
        mainHandler.post(() -> {
            if (!micWanted || hostPaused) return;
            if (voskRetryCount < 3) {
                final int attempt = ++voskRetryCount;
                Log.w(TAG, "Mikrofon düştü, yeniden deneniyor (" + attempt + "/3): " + msg);
                mainHandler.postDelayed(() -> {
                    if (micWanted && !hostPaused && !voskActive && voskState == V_READY) startVosk();
                }, 700L * attempt);
            } else {
                sendSpeechEvent("error", msg);
                sendSpeechEvent("end", "");
            }
        });
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
        mainHandler.post(() -> {
            // 🔴 FIX #8: WebView null check before JS callback
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechVolume) window.onNativeSpeechVolume(" + vol + ");", null);
        });
    }

    // ---------------- JS'in çağırdığı giriş noktaları ----------------

    /** JS: dinlemeyi başlatır. Vosk hazırsa onu, değilse sistem tanıyıcıyı kullanır. */
    @JavascriptInterface
    public void startNativeSpeech() {
        micWanted = true;
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

    /** (Geriye uyumluluk) Dinlemeyi durdurur. Artık ses kanalı odağı İSTENMEZ: asistan sesi için
     *  mikrofonu kapatmaya gerek yok, bunun yerine beginAssistantSpeech() kullanılır. */
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

    /** (Geriye uyumluluk) Duraklatılan dinlemeyi sürdürür. */
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

    /** Activity.onResume: JS'e "geri döndük" der; JS sürekli dinleme açıksa mikrofonu yeniden başlatır. UI thread. */
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

    /** Activity.onDestroy içinden (UI thread) çağır. */
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

    // 🔴 ASISTAN CEVAPLARINI FİLTRELE - SADECE asistanın konuştuğu tam cümleler
    // NOT: Kullanıcı komutları ("geri al", "sayım", "seçildi") burada OLMAMALI!
    private static final String[] ASISTAN_CUMLELERI = {
        "yazılımcısı sedat dağlıdır",
        "sayım kaydedildi",
        "ürün sayımdan çıkarıldı",
        "sayım miktarı güncellendi",
        "yeni sevkiyat başlatıldı",
        "sevkiyat kapatıldı",
        "ürün sevkiyata eklendi",
        "ürün sevkiyattan çıkarıldı",
        "açık sevkiyat yok önce sevkiyat başlatın",
        "fire kaydı başarıyla oluşturuldu",
        "ürün düzenleme ekranı açıldı",
        "kasa ağırlığı ayarlandı",
        "ürün birimi güncellendi",
        "ürün başarıyla kaydedildi",
        "düzenleme iptal edildi",
        "ilgili sayfa açıldı",
        "filtre uygulandı",
        "kamera açıldı",
        "kamera kapatıldı",
        "whatsapp raporu hazırlanıyor",
        "son işlem geri alındı",
        "geri alınacak işlem bulunmuyor",
        "anlaşılamadı lütfen tekrar edin",
        "sesli asistan kapatıldı",
        "sesli asistan hazır sizi dinliyorum",
        "yeni ürün ekleme formu açıldı",
        "p l u kodu güncellendi",
        "plu kodu güncellendi",
        "barkod güncellendi",
        "ürün adı güncellendi",
        "sayım durumu özetlendi",
        "günlük fire raporu hazırlandı",
        "sevkiyat durumu özetlendi",
        "belirtilen ürün veya kod bulunamadı",
        "ürün bulundu ve seçildi",
        "mikrofon duraklatıldı"
    };

    private boolean isAssistantEcho(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String normalized = normalizeTurkish(text.toLowerCase().trim());
        
        // 🔴 KRİTİK: SADECE uzun cümleler için substring match
        // Kısa komutlar için EXACT match gerekli (yoksa "geri al" komutunu da filtreliyoruz!)
        for (String phrase : ASISTAN_CUMLELERI) {
            String normalizedPhrase = normalizeTurkish(phrase.toLowerCase());
            
            // Asistan cümlesi 5 kelimeden uzunsa substring match OK
            boolean isLongPhrase = normalizedPhrase.split("\\s+").length >= 5;
            
            if (isLongPhrase) {
                // Uzun cümleler: substring match
                if (normalized.contains(normalizedPhrase) || normalizedPhrase.contains(normalized)) {
                    Log.i(TAG, "🚫 Native Echo Filter (substring): '" + text + "' ~ '" + phrase + "'");
                    return true;
                }
            } else {
                // Kısa cümleler: exact match (kelime sınırları kontrol et)
                if (normalized.equals(normalizedPhrase)) {
                    Log.i(TAG, "🚫 Native Echo Filter (exact): '" + text + "' == '" + phrase + "'");
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeTurkish(String text) {
        return text.replaceAll("[üû]", "u")
                   .replaceAll("[îı]", "i")
                   .replaceAll("[öô]", "o")
                   .replaceAll("[çć]", "c")
                   .replaceAll("[şś]", "s")
                   .replaceAll("[ğ]", "g")
                   .replaceAll("[^a-z0-9]", "");
    }

    private void sendSpeechResult(String text) {
        // 🔴 ECHO FİLTRESİ: Asistan seslerini JS'e göndermeden filtrele
        if (isAssistantEcho(text)) {
            return; // Loop'u önle!
        }
        
        Log.i(TAG, "✅ Kullanıcı komutu JS'e gönderiliyor: " + text);
        mainHandler.post(() -> {
            // 🔴 FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechResult) window.onNativeSpeechResult(" + q(text) + ");", null);
        });
    }

    private void sendSpeechPartial(String text) {
        mainHandler.post(() -> {
            // 🔴 FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechPartial) window.onNativeSpeechPartial(" + q(text) + ");", null);
        });
    }

    private void sendSpeechEvent(String eventType, String msg) {
        mainHandler.post(() -> {
            // 🔴 FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechEvent) window.onNativeSpeechEvent(" + q(eventType) + ", " + q(msg) + ");", null);
        });
    }

    private void sendSpeechVolume(float rmsdB) {
        float vol = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
        mainHandler.post(() -> {
            // 🔴 FIX #8: WebView null check
            if (webView == null || activity.isFinishing() || activity.isDestroyed()) return;
            webView.evaluateJavascript(
                    "if(window.onNativeSpeechVolume) window.onNativeSpeechVolume(" + vol + ");", null);
        });
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
