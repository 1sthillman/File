from pydub import AudioSegment
import json
import os

mp3_path = r'C:\File\android\app\src\main\assets\www\urun_sesleri.mp3'
audio = AudioSegment.from_mp3(mp3_path)

# 85 tespit edilen segment
segments = [
    {'idx': 0, 'start': 0.0, 'dur': 0.91},
    {'idx': 1, 'start': 1.16, 'dur': 1.05},
    {'idx': 2, 'start': 2.47, 'dur': 0.81},
    {'idx': 3, 'start': 3.53, 'dur': 0.69},
    {'idx': 4, 'start': 4.48, 'dur': 0.95},
    {'idx': 5, 'start': 5.67, 'dur': 1.13},
    {'idx': 6, 'start': 7.05, 'dur': 1.28},
    {'idx': 7, 'start': 8.58, 'dur': 1.15},
    {'idx': 8, 'start': 9.98, 'dur': 0.92},
    {'idx': 9, 'start': 11.15, 'dur': 0.65},
    {'idx': 10, 'start': 12.03, 'dur': 0.87},
    {'idx': 11, 'start': 13.15, 'dur': 0.5},
    {'idx': 12, 'start': 13.87, 'dur': 0.79},
    {'idx': 13, 'start': 14.91, 'dur': 0.97},
    {'idx': 14, 'start': 16.13, 'dur': 0.7},
    {'idx': 15, 'start': 17.06, 'dur': 1.0},
    {'idx': 16, 'start': 18.24, 'dur': 0.47},
    {'idx': 17, 'start': 18.9, 'dur': 0.89},
    {'idx': 18, 'start': 20.04, 'dur': 0.92},
    {'idx': 19, 'start': 21.21, 'dur': 0.59},
    {'idx': 20, 'start': 22.01, 'dur': 0.66},
    {'idx': 21, 'start': 22.89, 'dur': 0.54},
    {'idx': 22, 'start': 23.65, 'dur': 0.78},
    {'idx': 23, 'start': 24.66, 'dur': 0.77},
    {'idx': 24, 'start': 25.66, 'dur': 0.53},
    {'idx': 25, 'start': 26.4, 'dur': 0.49},
    {'idx': 26, 'start': 27.1, 'dur': 0.81},
    {'idx': 27, 'start': 28.14, 'dur': 0.49},
    {'idx': 28, 'start': 28.84, 'dur': 0.69},
    {'idx': 29, 'start': 29.73, 'dur': 0.51},
    {'idx': 30, 'start': 30.46, 'dur': 0.9},
    {'idx': 31, 'start': 31.58, 'dur': 0.55},
    {'idx': 32, 'start': 32.36, 'dur': 0.72},
    {'idx': 33, 'start': 33.33, 'dur': 0.48},
    {'idx': 34, 'start': 34.05, 'dur': 0.68},
    {'idx': 35, 'start': 34.95, 'dur': 0.96},
    {'idx': 36, 'start': 36.14, 'dur': 0.83},
    {'idx': 37, 'start': 37.22, 'dur': 0.25},
    {'idx': 38, 'start': 37.66, 'dur': 0.66},
    {'idx': 39, 'start': 38.55, 'dur': 0.95},
    {'idx': 40, 'start': 39.74, 'dur': 0.86},
    {'idx': 41, 'start': 40.83, 'dur': 0.5},
    {'idx': 42, 'start': 41.56, 'dur': 1.04},
    {'idx': 43, 'start': 42.84, 'dur': 0.65},
    {'idx': 44, 'start': 43.7, 'dur': 0.42},
    {'idx': 45, 'start': 44.3, 'dur': 0.39},
    {'idx': 46, 'start': 44.87, 'dur': 0.87},
    {'idx': 47, 'start': 45.95, 'dur': 0.73},
    {'idx': 48, 'start': 46.91, 'dur': 0.78},
    {'idx': 49, 'start': 47.88, 'dur': 0.89},
    {'idx': 50, 'start': 49.02, 'dur': 1.16},
    {'idx': 51, 'start': 50.38, 'dur': 0.53},
    {'idx': 52, 'start': 51.12, 'dur': 0.67},
    {'idx': 53, 'start': 52.03, 'dur': 1.07},
    {'idx': 54, 'start': 53.31, 'dur': 0.8},
    {'idx': 55, 'start': 54.34, 'dur': 0.71},
    {'idx': 56, 'start': 55.27, 'dur': 0.69},
    {'idx': 57, 'start': 56.16, 'dur': 0.34},
    {'idx': 58, 'start': 56.7, 'dur': 0.52},
    {'idx': 59, 'start': 57.4, 'dur': 0.73},
    {'idx': 60, 'start': 58.34, 'dur': 0.64},
    {'idx': 61, 'start': 59.19, 'dur': 0.45},
    {'idx': 62, 'start': 59.83, 'dur': 0.64},
    {'idx': 63, 'start': 60.66, 'dur': 0.56},
    {'idx': 64, 'start': 61.43, 'dur': 0.45},
    {'idx': 65, 'start': 62.06, 'dur': 0.48},
    {'idx': 66, 'start': 62.74, 'dur': 1.16},
    {'idx': 67, 'start': 64.1, 'dur': 0.74},
    {'idx': 68, 'start': 65.05, 'dur': 0.9},
    {'idx': 69, 'start': 66.13, 'dur': 1.02},
    {'idx': 70, 'start': 67.35, 'dur': 0.53},
    {'idx': 71, 'start': 68.05, 'dur': 0.48},
    {'idx': 72, 'start': 68.74, 'dur': 0.59},
    {'idx': 73, 'start': 69.5, 'dur': 0.5},
    {'idx': 74, 'start': 70.18, 'dur': 0.69},
    {'idx': 75, 'start': 71.04, 'dur': 0.81},
    {'idx': 76, 'start': 72.01, 'dur': 0.72},
    {'idx': 77, 'start': 72.92, 'dur': 0.45},
    {'idx': 78, 'start': 73.56, 'dur': 0.85},
    {'idx': 79, 'start': 74.6, 'dur': 0.64},
    {'idx': 80, 'start': 75.43, 'dur': 0.68},
    {'idx': 81, 'start': 76.3, 'dur': 0.91},
    {'idx': 82, 'start': 77.4, 'dur': 0.37},
    {'idx': 83, 'start': 77.97, 'dur': 1.1},
    {'idx': 84, 'start': 79.2, 'dur': 0.7},
]

products = [
    "ALABAS_TURP","ARPACIK_SOGAN","BALKABAGI","BROKOLI","CALI_FASULYE",
    "CARLISTON_BIBER","CENGELKOY_SALATALIK","DIKENLI_SALATALIK","DOLMA_BIBER","DOMATES",
    "GUMUS_SOGAN","HAVUC","HAVUC_PAKET_KG","ISPANAK","KARNABAHAR",
    "KALIFORNIA_BIBER","KAPYA_BIBER","KEMER_PATLICAN","KEREVIZ","KIRMIZI_LAHANA",
    "KOKTEYL_DOMATES","KOY_BIBER","KURU_SOGAN","LAHANA","MINI_PATATES",
    "MOR_SOGAN","PANCAR","PATATES_DOKME","PATATES_KIZARTMA","PATATES_YEMEKLIK",
    "PEMBE_DOMATES","PIRASA","PIRASA_PAKET_KG","SAKIZ_KABAK","SALATALIK",
    "SALKIM_DOMATES","SARIMSAK","SIVRI_BIBER","SOGAN_YEMEKLIK","TARLA_KABAK",
    "TARLA_PATLICAN","TATLI_PATATES","TURP","ZENCEFIL","AMASYA_ELMA",
    "ANKARA_ARMUT","AYVA","BABY_AVOKADO","BABY_MUZ","CEVIZ_KABUKLU",
    "CILEK_TARLA","CEKIRDEKSIZ_KIRMIZI_UZUM","CEKIRDEKSIZ_SULTANI_UZUM","DEVECI_ARMUT","FUJI_ELMA",
    "GALA_ELMA","GOLDEN_ELMA","GRANNY_SMITH_ELMA","GREYFURT","ITHAL_MUZ",
    "IZMIR_MANDALINA","KARMA_ARMUT","KARMA_ELMA","KESTANE","KIRKAGAC_KAVUN",
    "KIRMIZI_ERIK","KISLIK_KAVUN_KG","KIVI","LIMON_ENTER","LIMON_ENTER_FILE",
    "LIMON_MAYER_FILE","MANDALINA","MUSMULA","NAR","PORTAKAL",
    "PORTAKAL_FILE","RED_GLOBE_UZUM","SANTA_MARIA_ARMUT","SIYAH_UZUM","STARKING_ELMA",
    "TRABZON_HURMASI","SEMIZOTU","ROKA","NANE","DEREOTU",
    "MAYDANOZ","TAZE_SOGAN","GOBEK_MARUL","MARUL","KIVIRCIK_MARUL"
]

out_dir = r'C:\File\sesdosya\newsound\split'
os.makedirs(out_dir, exist_ok=True)

for i, seg in enumerate(segments):
    if i >= len(products):
        break
    start_ms = int(seg['start'] * 1000)
    end_ms = int((seg['start'] + seg['dur']) * 1000)
    chunk = audio[start_ms:end_ms]
    out_path = os.path.join(out_dir, f'{products[i]}.mp3')
    chunk.export(out_path, format='mp3')
    print(f'{i+1}. {products[i]}.mp3 ({seg["dur"]:.2f}s)')

print(f'\nToplam {len(segments)} MP3 olusturuldu: {out_dir}')
