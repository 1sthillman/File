import os
import base64

src_dir = r'C:\File\sesdosya\newsound'

# Ek sesler
extra_sounds = {
    've.mp3': 've',  # "ve" bağlacı (çoklu komutlar için)
    'seçildi.mp3': 'secildi'  # "seçildi" onay sesi
}

js_lines = []
total_size = 0

for filename, key in extra_sounds.items():
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

js_code = "const EXTRA_SOUNDS = {\n" + ",\n".join(js_lines) + "\n};"

out_path = r'C:\File\sesdosya\newsound\extra_sounds_vault.js'
with open(out_path, 'w', encoding='utf-8') as f:
    f.write(js_code)

print(f'\n✅ {len(js_lines)} ek ses vault oluşturuldu')
print(f'   Toplam boyut: {total_size/1024:.1f} KB')
print(f'   Base64 boyut: {len(js_code)/1024:.1f} KB')
print(f'   Dosya: {out_path}')
