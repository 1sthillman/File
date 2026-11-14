import os
import base64
from datetime import datetime

src_dir = r'C:\File\sesdosya\newsound\ürünsesleri'

# Ürün isimleri (txt'den okunan sıra)
urun_txt = """Alabaş Turp. Arpacık Soğan. Balkabağı. Brokoli. Çalı Fasulye. Çarliston Biber. Çengelköy Salatalık. Dikenli Salatalık. Dolma Biber. Domates. Gümüş Soğan. Havuç. Ispanak. Karnabahar. Kalifornia Biber. Kapya Biber. Kemer Patlıcan. Kereviz. Kırmızı Lahana. Kokteyl Domates. Köy Biber. Kuru Soğan. Lahana. Mini Patates. Mor Soğan. Pancar. Patates. Pembe Domates. Pırasa. Sakız Kabak. Salatalık. Salkım Domates. Sarımsak. Sivri Biber. Soğan. Tarla Kabak. Tarla Patlıcan. Tatlı Patates. Turp. Zencefil. Amasya Elma. Ankara Armut. Ayva. Baby Avokado. Baby Muz. Ceviz. Çilek. Deveci Armut. Fuji Elma. Gala Elma. Golden Elma. Granny Smith Elma. Greyfurt. İthal Muz. İzmir Mandalina. Karma Elma. Kestane. Kırmızı Erik. Kivi. Limon. Mandalina. Muşmula. Nar. Portakal. Karpuz. Kavun. Üzüm. Santa Maria Armut. Siyah Üzüm. Starking Elma. Trabzon Hurması. Semizotu. Roka. Nane. Dereotu. Maydanoz. Taze Soğan. Göbek Marul. Marul. Kıvırcık Marul. Biberiye. Fesleğen. Kültür Mantarı. Tere. İstiridye Mantar."""

products = [p.strip().upper() for p in urun_txt.split('.') if p.strip()]

# MP3 dosyalarını zamana göre sırala
mp3_files = sorted([f for f in os.listdir(src_dir) if f.endswith('.mp3')])

print(f'Toplam {len(products)} ürün, {len(mp3_files)} MP3 dosyası')

# Her MP3'ü base64'e çevir
js_lines = []
total_size = 0
for i, fname in enumerate(mp3_files):
    if i >= len(products):
        print(f'UYARI: {i+1}. MP3 için ürün adı yok')
        break
    
    fpath = os.path.join(src_dir, fname)
    with open(fpath, 'rb') as f:
        data = f.read()
        b64 = base64.b64encode(data).decode('ascii')
    
    total_size += len(data)
    key = products[i]
    js_lines.append(f'    "{key}": "data:audio/mp3;base64,{b64}"')
    print(f'{i+1:3d}. {key:30s} -> {len(data)/1024:6.1f} KB')

js_code = "const URUN_SES_VAULT = {\n" + ",\n".join(js_lines) + "\n  };"

out_path = r'C:\File\sesdosya\newsound\urun_vault_final.js'
with open(out_path, 'w', encoding='utf-8') as f:
    f.write(js_code)

print(f'\n✅ {len(js_lines)} ürün vault oluşturuldu')
print(f'   Toplam boyut: {total_size/1024:.1f} KB')
print(f'   Base64 boyut: {len(js_code)/1024:.1f} KB')
print(f'   Dosya: {out_path}')
