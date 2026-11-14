from pydub import AudioSegment
from pydub.silence import split_on_silence
import json

mp3_path = r'C:\File\android\app\src\main\assets\www\urun_sesleri.mp3'
audio = AudioSegment.from_mp3(mp3_path)
print(f'Toplam sure: {len(audio)/1000:.2f}s')

chunks = split_on_silence(audio, min_silence_len=250, silence_thresh=-40, keep_silence=100)
print(f'Segment sayisi: {len(chunks)}')

segments = []
pos = 0
for i, chunk in enumerate(chunks):
    start = pos / 1000.0
    dur = len(chunk) / 1000.0
    segments.append({'idx': i, 'start': round(start, 2), 'dur': round(dur, 2)})
    pos += len(chunk) + 250

with open(r'C:\File\sesdosya\newsound\segments.json', 'w', encoding='utf-8') as f:
    json.dump(segments, f, ensure_ascii=False, indent=2)

for s in segments[:10]:
    print(f"  {s['idx']}: start={s['start']}s dur={s['dur']}s")
print(f'  ... toplam {len(segments)} segment')
