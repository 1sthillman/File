#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
HTML'E SMART VOICE ENTEGRASYONU
================================
comprehensive_voice_vault.js ve smart_voice_command_processor.js'i
index.html'in <head> bölümüne enjekte eder.
"""

import re
from pathlib import Path

# Dosya yolları
HTML_SOURCE = Path(r'C:\File\ST\index.html')
HTML_TARGET = Path(r'C:\File\ST\index_with_smart_voice.html')
VAULT_JS = Path(r'C:\File\sesdosya\newsound\comprehensive_voice_vault.js')
PROCESSOR_JS = Path(r'C:\File\sesdosya\newsound\smart_voice_command_processor.js')
BRIDGE_JS = Path(r'C:\File\sesdosya\newsound\voice_integration_bridge.js')

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def inject_scripts(html_content, vault_js, processor_js, bridge_js):
    """
    </head> tag'inden önce script'leri ekle
    """
    
    # Script bloğu oluştur
    injection = f'''
<!-- ================================================== -->
<!-- SMART VOICE SYSTEM - INJECTED BY inject_to_html.py -->
<!-- ================================================== -->
<script>
{vault_js}
</script>

<script>
{processor_js}
</script>

<script>
{bridge_js}
</script>
<!-- ================================================== -->
'''
    
    # </head> bulup önüne ekle
    if '</head>' in html_content:
        html_content = html_content.replace('</head>', injection + '</head>')
    else:
        # </head> yoksa <body> öncesine ekle
        html_content = html_content.replace('<body', injection + '<body')
    
    return html_content

def main():
    print("=" * 60)
    print("HTML'E SMART VOICE ENTEGRASYONU")
    print("=" * 60)
    print()
    
    # Dosyaları oku
    print("[1/4] HTML okunuyor...")
    html = read_file(HTML_SOURCE)
    
    print("[2/5] Voice Vault JS okunuyor...")
    vault = read_file(VAULT_JS)
    
    print("[3/5] Smart Processor JS okunuyor...")
    processor = read_file(PROCESSOR_JS)
    
    print("[4/5] Integration Bridge JS okunuyor...")
    bridge = read_file(BRIDGE_JS)
    
    print("[5/5] Script'ler HTML'e enjekte ediliyor...")
    final_html = inject_scripts(html, vault, processor, bridge)
    
    # Yaz
    with open(HTML_TARGET, 'w', encoding='utf-8') as f:
        f.write(final_html)
    
    print()
    print("=" * 60)
    print("✅ BAŞARILI!")
    print("=" * 60)
    print(f"Çıktı: {HTML_TARGET}")
    print(f"Boyut: {len(final_html) / 1024 / 1024:.2f} MB")
    print()
    print("Sonraki adım:")
    print("1. index_with_smart_voice.html'i test et")
    print("2. Çalışıyorsa index.html olarak kopyala")

if __name__ == '__main__':
    main()
