import json

# 85 tespit edilen segment (ilk analizden)
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

# 97 ürün adı (SEED sırasında)
products = [
    "ALABAŞ TURP","ARPACIK SOĞAN","BALKABAĞI","BROKOLİ","ÇALI FASULYE",
    "ÇARLİSTON BİBER","ÇENGELKÖY SALATALIK","DİKENLİ SALATALIK","DOLMA BİBER","DOMATES",
    "GÜMÜŞ SOĞAN","HAVUÇ","HAVUÇ PAKET KG","ISPANAK","KARNABAHAR",
    "KALİFORNİA BİBER","KAPYA BİBER","KEMER PATLICAN","KEREVİZ","KIRMIZI LAHANA",
    "KOKTEYL DOMATES","KÖY BİBER","KURU SOĞAN","LAHANA","MİNİ PATATES",
    "MOR SOĞAN","PANCAR","PATATES (DÖKME)","PATATES KIZARTMA","PATATES YEMEKLİK",
    "PEMBE DOMATES","PIRASA","PIRASA PAKET KG","SAKIZ KABAK","SALATALIK",
    "SALKIM DOMATES","SARIMSAK","SİVRİ BİBER","SOĞAN YEMEKLİK","TARLA KABAK",
    "TARLA PATLICAN","TATLI PATATES","TURP","ZENCEFİL","AMASYA ELMA",
    "ANKARA ARMUT","AYVA","BABY AVOKADO","BABY MUZ","CEVİZ KABUKLU",
    "ÇİLEK TARLA","ÇEKİRDEKSİZ KIRMIZI ÜZÜM","ÇEKİRDEKSİZ SULTANİ ÜZÜM","DEVECİ ARMUT","FUJİ ELMA",
    "GALA ELMA","GOLDEN ELMA","GRANNY SMITH ELMA","GREYFURT","İTHAL MUZ",
    "İZMİR MANDALİNA","KARMA ARMUT","KARMA ELMA","KESTANE","KIRKAĞAÇ KAVUN",
    "KIRMIZI ERİK","KIŞLIK KAVUN KG","KİVİ","LİMON ENTER","LİMON ENTER FİLE",
    "LİMON MAYER FİLE","MANDALİNA","MUŞMULA","NAR","PORTAKAL",
    "PORTAKAL FİLE","RED GLOBE ÜZÜM","SANTA MARIA ARMUT","SİYAH ÜZÜM","STARKING ELMA",
    "TRABZON HURMASI","SEMİZOTU","ROKA","NANE","DEREOTU",
    "MAYDANOZ","TAZE SOĞAN","GÖBEK MARUL","MARUL","KIVIRCIK MARUL",
    "BİBERİYE 25 G","FESLEĞEN 25 G","KÜLTÜR MANTARI 400 G","TERE 50 G","KUZU KULAĞI 50 G",
    "KESTANE MANTARI 350 G","İSTİRİDYE MANTAR 200 G"
]

# Map oluştur — 85 segmenti 85 ürüne eşle
# Kalan 12 ürün için ses yok (fallback: atla)
js_lines = []
for i in range(min(85, len(products))):
    s = segments[i]
    js_lines.append(f'    "{products[i]}": [{s["start"]}, {s["dur"]}]')

js_map = "const URUN_SES_MAP = {\n" + ",\n".join(js_lines) + "\n  };"

with open(r'C:\File\sesdosya\newsound\urun_ses_map_final.js', 'w', encoding='utf-8') as f:
    f.write(js_map)

print(f"Map olusturuldu: {min(85, len(products))} ürün")
print(f"Kalan ürünler (ses yok): {max(0, len(products) - 85)}")
