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

    /**
     * Native Android Speech Recognition Başlat
     */
    @JavascriptInterface
    public void startNativeSpeech() {
        mainHandler.post(() -> {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
                    sendSpeechEvent("error", "Bu cihazda ses tanıma servisi bulunamadı");
                    return;
                }

                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override public void onReadyForSpeech(Bundle params) {
                        isRecognizing = true;
                        sendSpeechEvent("ready", "");
                    }
                    @Override public void onBeginningOfSpeech() {
                        sendSpeechEvent("start", "");
                    }
                    @Override public void onRmsChanged(float rmsdB) {
                        sendSpeechVolume(rmsdB);
                    }
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onEndOfSpeech() {
                        isRecognizing = false;
                        sendSpeechEvent("end", "");
                    }
                    @Override public void onError(int error) {
                        isRecognizing = false;
                        String errText = getSpeechErrorMsg(error);
                        sendSpeechEvent("error", errText);
                    }
                    @Override public void onResults(Bundle results) {
                        isRecognizing = false;
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            sendSpeechResult(matches.get(0));
                        } else {
                            sendSpeechEvent("no_match", "");
                        }
                    }
                    @Override public void onPartialResults(Bundle partialResults) {
                        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            sendSpeechPartial(matches.get(0));
                        }
                    }
                    @Override public void onEvent(int eventType, Bundle params) {}
                });

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR");
                intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "tr-TR");
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                speechRecognizer.startListening(intent);
            } catch (Exception e) {
                Log.e(TAG, "Native speech start error", e);
                sendSpeechEvent("error", e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public void stopNativeSpeech() {
        mainHandler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.stopListening();
                    isRecognizing = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Native speech stop error", e);
            }
        });
    }

    private void sendSpeechResult(String text) {
        mainHandler.post(() -> {
            String safeText = text.replace("'", "\\'").replace("\n", " ");
            webView.evaluateJavascript("if(window.onNativeSpeechResult) window.onNativeSpeechResult('" + safeText + "');", null);
        });
    }

    private void sendSpeechPartial(String text) {
        mainHandler.post(() -> {
            String safeText = text.replace("'", "\\'").replace("\n", " ");
            webView.evaluateJavascript("if(window.onNativeSpeechPartial) window.onNativeSpeechPartial('" + safeText + "');", null);
        });
    }

    private void sendSpeechEvent(String eventType, String msg) {
        mainHandler.post(() -> {
            String safeMsg = msg.replace("'", "\\'");
            webView.evaluateJavascript("if(window.onNativeSpeechEvent) window.onNativeSpeechEvent('" + eventType + "', '" + safeMsg + "');", null);
        });
    }

    private void sendSpeechVolume(float rmsdB) {
        float vol = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
        mainHandler.post(() -> {
            webView.evaluateJavascript("if(window.onNativeSpeechVolume) window.onNativeSpeechVolume(" + vol + ");", null);
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
