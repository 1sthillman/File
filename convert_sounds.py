import base64

files = {
    'aktarsayfasıacıldi.mp3': 'PANO_PAGE',
    'firesayfasıacıldi.mp3': 'FIRE_PAGE', 
    'panosayfası.mp3': 'PANO_PAGE2',
    'sayımsayfasıacıldi.mp3': 'COUNT_PAGE',
    'sevkiyatsayfasıacıldi.mp3': 'SHIP_PAGE',
    'ürünlersayfasıacıldi (2).mp3': 'PRODUCT_PAGE'
}

vault_entries = []

for fname, key in files.items():
    path = f'android/app/src/main/assets/www/{fname}'
    try:
        with open(path, 'rb') as f:
            data = base64.b64encode(f.read()).decode()
        vault_entries.append(f'    "{key}": "data:audio/mp3;base64,{data}",')
        print(f'✓ {key}: {len(data)} chars')
    except Exception as e:
        print(f'✗ {fname}: {e}')

# Write all entries to a file
with open('vault_additions.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(vault_entries))

print(f'\n✅ {len(vault_entries)} entries written to vault_additions.txt')
