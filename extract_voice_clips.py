#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Manav Sayım - Voice Clips Extractor
HTML'den base64 ses kliplerini çıkarıp Android res/raw klasörüne MP3 olarak kaydeder
"""

import re
import base64
import os
import sys

def extract_voice_clips(html_path, output_dir):
    """HTML'den VOICE_AUDIO_VAULT'u parse edip MP3'leri çıkarır"""
    
    print(f"📄 HTML dosyası okunuyor: {html_path}")
    with open(html_path, 'r', encoding='utf-8') as f:
        html_content = f.read()
    
    # VOICE_AUDIO_VAULT objesini bul
    pattern = r'(?:var\s+)?VOICE_AUDIO_VAULT\s*=\s*\{([^}]+)\}'
    match = re.search(pattern, html_content, re.DOTALL)
    
    if not match:
        print("❌ VOICE_AUDIO_VAULT bulunamadı!")
        return False
    
    vault_content = match.group(1)
    print(f"✓ VOICE_AUDIO_VAULT bulundu ({len(vault_content)} karakter)")
    
    # Her satırı parse et
    clips = {}
    for line in vault_content.split('\n'):
        line = line.strip()
        if not line or line.startswith('//'):
            continue
            
        # Key: "VALUE" formatı
        if ':' in line and 'data:audio' in line:
            try:
                key_part = line.split(':')[0].strip().strip('"').strip("'")
                
                # Base64 data'yı çıkar
                if 'base64,' in line:
                    b64_start = line.index('base64,') + 7
                    b64_end = line.find('"', b64_start)
                    if b64_end == -1:
                        b64_end = line.find("'", b64_start)
                    
                    b64_data = line[b64_start:b64_end].strip()
                    clips[key_part] = b64_data
                    
            except Exception as e:
                print(f"⚠️ Satır parse edilemedi: {line[:50]}... ({e})")
    
    if not clips:
        print("❌ Hiç ses klibi bulunamadı!")
        return False
    
    print(f"✓ {len(clips)} ses klibi bulundu")
    
    # Output klasörünü oluştur
    os.makedirs(output_dir, exist_ok=True)
    print(f"📁 Çıktı klasörü: {output_dir}")
    
    # MP3 dosyalarını kaydet
    success_count = 0
    for key, b64_data in clips.items():
        try:
            # Base64 decode
            mp3_bytes = base64.b64decode(b64_data)
            
            # Dosya adı: küçük harf + .mp3
            filename = key.lower() + '.mp3'
            filepath = os.path.join(output_dir, filename)
            
            # Kaydet
            with open(filepath, 'wb') as f:
                f.write(mp3_bytes)
            
            print(f"  ✓ {filename:30s} ({len(mp3_bytes):6d} bytes)")
            success_count += 1
            
        except Exception as e:
            print(f"  ❌ {key}: {e}")
    
    print(f"\n🎉 {success_count}/{len(clips)} klip başarıyla kaydedildi!")
    print(f"📊 Toplam boyut: {sum(len(base64.b64decode(b)) for b in clips.values()) / 1024:.1f} KB")
    
    return success_count > 0


if __name__ == '__main__':
    if len(sys.argv) < 2:
        html_path = 'ST/index.html'
    else:
        html_path = sys.argv[1]
    
    output_dir = 'android/app/src/main/res/raw'
    
    if not os.path.exists(html_path):
        print(f"❌ HTML dosyası bulunamadı: {html_path}")
        sys.exit(1)
    
    success = extract_voice_clips(html_path, output_dir)
    sys.exit(0 if success else 1)
