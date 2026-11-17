import os
import base64

src_dir = r'C:\File\sesdosya\newsound\sayfageçişsesleri'

# Sayfa geçiş sesleri
page_sounds = {
    'aktarsayfasıacıldi.mp3': 'AKTAR_PAGE_OPEN',
    'firesayfasıacıldi.mp3': 'FIRE_PAGE_OPEN',
    'panosayfası.mp3': 'PANO_PAGE_OPEN',
    'sayımsayfasıacıldi.mp3': 'SAYIM_PAGE_OPEN',
    'sevkiyatsayfasıacıldi.mp3': 'SEVKIYAT_PAGE_OPEN',
    'ürünlersayfasıacıldi (2).mp3': 'URUNLER_PAGE_OPEN'
}

js_lines = []
total_size = 0

for filename, key in page_sounds.items():
    fpath = os.path.join(src_dir, filename)
    if not os.path.exists(fpath):
        print(f'UYARI: {filename} bulunamadı')
        continue
    
    with open(fpath, 'rb') as f:
        data = f.read()
        b64 = base64.b64encode(data).decode('ascii')
    
    total_size += len(data)
    js_lines.append(f'    "{key}": "data:audio/mp3;base64,{b64}"')
    print(f'{key:25s} -> {len(data)/1024:6.1f} KB')

js_code = "const PAGE_TRANSITION_SOUNDS = {\n" + ",\n".join(js_lines) + "\n};"

out_path = r'C:\File\sesdosya\newsound\page_sounds_vault.js'
with open(out_path, 'w', encoding='utf-8') as f:
    f.write(js_code)

print(f'\n✅ {len(js_lines)} sayfa sesi vault oluşturuldu')
print(f'   Toplam boyut: {total_size/1024:.1f} KB')
print(f'   Base64 boyut: {len(js_code)/1024:.1f} KB')
print(f'   Dosya: {out_path}')
