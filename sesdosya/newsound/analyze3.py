from pydub import AudioSegment
from pydub.silence import split_on_silence
import json

mp3_path = r'C:\File\android\app\src\main\assets\www\urun_sesleri.mp3'
audio = AudioSegment.from_mp3(mp3_path)

# Çok hassas parametreler
chunks = split_on_silence(audio, min_silence_len=100, silence_thresh=-30, keep_silence=50)
print(f'Segment sayisi: {len(chunks)}')

segments = []
pos = 0
for i, chunk in enumerate(chunks):
    start = pos / 1000.0
    dur = len(chunk) / 1000.0
    segments.append({'idx': i, 'start': round(start, 2), 'dur': round(dur, 2)})
    pos += len(chunk) + 100

# Sonuçları kaydet
with open(r'C:\File\sesdosya\newsound\segments.json', 'w', encoding='utf-8') as f:
    json.dump(segments, f, ensure_ascii=False, indent=2)

# Tüm segmentleri yazdır
for s in segments:
    print(f"  {s['idx']}: start={s['start']}s dur={s['dur']}s")
