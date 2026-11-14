from pydub import AudioSegment
from pydub.silence import detect_nonsilent
import json

mp3_path = r'C:\File\android\app\src\main\assets\www\urun_sesleri.mp3'
audio = AudioSegment.from_mp3(mp3_path)

print(f'Toplam sure: {len(audio)/1000:.2f}s')

# Sessiz olmayan bölgeleri tespit et (her kelimenin başı ve sonu)
# min_silence_len: minimum sessizlik süresi (ms)
# silence_thresh: sessizlik eşiği (dB, negatif değer)
nonsilent_ranges = detect_nonsilent(
    audio, 
    min_silence_len=150,  # 150ms sessizlik kelimeler arası
    silence_thresh=-35    # -35 dB altı sessizlik
)

print(f'Tespit edilen kelime sayisi: {len(nonsilent_ranges)}')

# Her kelimeyi segment olarak kaydet
segments = []
for i, (start_ms, end_ms) in enumerate(nonsilent_ranges):
    start_s = start_ms / 1000.0
    dur_s = (end_ms - start_ms) / 1000.0
    segments.append({
        'idx': i,
        'start': round(start_s, 2),
        'dur': round(dur_s, 2)
    })

# JSON'a kaydet
with open(r'C:\File\sesdosya\newsound\segments_precise.json', 'w', encoding='utf-8') as f:
    json.dump(segments, f, ensure_ascii=False, indent=2)

# Tümünü yazdır
for s in segments:
    print(f"  {s['idx']:3d}: start={s['start']:6.2f}s dur={s['dur']:5.2f}s")

print(f'\nToplam {len(segments)} segment')
