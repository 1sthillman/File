from pydub import AudioSegment
from pydub.silence import detect_nonsilent
import json

mp3_path = r'C:\File\android\app\src\main\assets\www\urun_sesleri.mp3'
audio = AudioSegment.from_mp3(mp3_path)

# Ultra hassas: 100ms sessizlik, -32 dB eşik
nonsilent_ranges = detect_nonsilent(audio, min_silence_len=100, silence_thresh=-32)

print(f'Tespit edilen segment: {len(nonsilent_ranges)}')

segments = []
for i, (start_ms, end_ms) in enumerate(nonsilent_ranges):
    start_s = start_ms / 1000.0
    dur_s = (end_ms - start_ms) / 1000.0
    segments.append({'idx': i, 'start': round(start_s, 2), 'dur': round(dur_s, 2)})
    print(f"{i:3d}: {start_s:6.2f}s dur={dur_s:5.2f}s")

with open(r'C:\File\sesdosya\newsound\segments_ultra.json', 'w', encoding='utf-8') as f:
    json.dump(segments, f, ensure_ascii=False, indent=2)

print(f'\nToplam: {len(segments)}')
