import os
import base64

split_dir = r'C:\File\sesdosya\newsound\split'
files = sorted([f for f in os.listdir(split_dir) if f.endswith('.mp3')])

# Ürün isimleri (orijinal)
originals = [
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
    "MAYDANOZ","TAZE SOĞAN","GÖBEK MARUL","MARUL","KIVIRCIK MARUL"
]

js_lines = []
for i, fname in enumerate(files):
    if i >= len(originals):
        break
    fpath = os.path.join(split_dir, fname)
    with open(fpath, 'rb') as f:
        b64 = base64.b64encode(f.read()).decode('ascii')
    js_lines.append(f'    "{originals[i]}": "data:audio/mp3;base64,{b64}"')
    print(f'{i+1}. {originals[i]} -> {len(b64)} chars')

js_map = "const URUN_SES_VAULT = {\n" + ",\n".join(js_lines) + "\n  };"

with open(r'C:\File\sesdosya\newsound\urun_vault_embed.js', 'w', encoding='utf-8') as f:
    f.write(js_map)

print(f'\n✅ {len(js_lines)} ürün vault olarak hazır — toplam {len(js_map)} karakter')
