#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
KAPSAMLI SES ENTEGRASYON SCRIPT'İ
==================================
Tüm ses dosyalarını (ürün, sayfa geçiş, yardımcı sesler) base64'e çevirip
index.html'e gömme ve komut sistemi için metadata oluşturma
"""

import os
import base64
import json
from pathlib import Path

# Klasör yolları
BASE_DIR = Path(r'C:\File\sesdosya\newsound')
SPLIT_DIR = BASE_DIR / 'split'  # Ürün isimleri
SAYFA_DIR = BASE_DIR / 'sayfageçişsesleri'  # Sayfa geçiş sesleri

# Çıktı dosyaları
OUTPUT_JS = BASE_DIR / 'comprehensive_voice_vault.js'
OUTPUT_MAP = BASE_DIR / 'voice_command_map.json'

def mp3_to_base64(file_path):
    """MP3 dosyasını base64'e çevir"""
    with open(file_path, 'rb') as f:
        return base64.b64encode(f.read()).decode('utf-8')

def normalize_name(filename):
    """Dosya adını normalize et (uzantısız, küçük harf)"""
    return filename.replace('.mp3', '').lower()

def create_product_aliases():
    """
    Ürün varyasyonları ve aliasları
    Key: asıl ürün ismi (küçük harf, boşluksuz)
    Value: liste of varyasyonlar
    """
    return {
        'domates': ['domates', 'salkim_domates', 'pembe_domates', 'kokteyl_domates'],
        'muz': ['ithal_muz', 'baby_muz'],
        'avokado': ['baby_avokado'],  # Ana avokado yok ama variant var
        'elma': ['fuji_elma', 'gala_elma', 'golden_elma', 'granny_smith_elma', 'karma_elma', 'starking_elma', 'amasya_elma'],
        'armut': ['ankara_armut', 'deveci_armut', 'santa_maria_armut', 'karma_armut'],
        'patates': ['patates_dokme', 'patates_kizartma', 'patates_yemeklik', 'mini_patates', 'tatli_patates'],
        'biber': ['carliston_biber', 'dolma_biber', 'kalifornia_biber', 'kapya_biber', 'koy_biber', 'sivri_biber'],
        'sogan': ['kuru_sogan', 'gumus_sogan', 'mor_sogan', 'sogan_yemeklik', 'arpacik_sogan'],
        'salatalik': ['salatalik', 'cengelkoy_salatalik', 'dikenli_salatalik'],
        'patlican': ['kemer_patlican', 'tarla_patlican'],
        'uzum': ['cekirdeksiz_kirmizi_uzum', 'cekirdeksiz_sultani_uzum', 'red_globe_uzum', 'siyah_uzum'],
        'limon': ['limon_enter', 'limon_enter_file', 'limon_mayer_file'],
        'portakal': ['portakal', 'portakal_file'],
        'kavun': ['kirkagac_kavun', 'kislik_kavun_kg'],
        'mandalina': ['mandalina', 'izmir_mandalina'],
        'lahana': ['lahana', 'kirmizi_lahana'],
        'kabak': ['sakiz_kabak', 'tarla_kabak'],
        'havuc': ['havuc', 'havuc_paket_kg'],
        'pirasa': ['pirasa', 'pirasa_paket_kg'],
        'turp': ['turp', 'alabas_turp'],
    }

def build_voice_vault():
    """Tüm sesleri topla ve base64 vault oluştur"""
    vault = {
        'products': {},       # Ürün isimleri
        'pages': {},          # Sayfa geçişleri
        'helpers': {},        # Yardımcı sesler (seçildi, ve)
        'system': {}          # Sistem sesleri (asistan cevapları - android/app/src/main/res/raw/)
    }
    
    # 1. Ürün sesleri
    print("[1/4] Ürün seslerini işliyorum...")
    if SPLIT_DIR.exists():
        for mp3_file in SPLIT_DIR.glob('*.mp3'):
            key = normalize_name(mp3_file.name)
            print(f"  + {key}")
            vault['products'][key] = mp3_to_base64(mp3_file)
    
    # 2. Sayfa geçiş sesleri
    print("[2/4] Sayfa geçiş seslerini işliyorum...")
    if SAYFA_DIR.exists():
        for mp3_file in SAYFA_DIR.glob('*.mp3'):
            key = normalize_name(mp3_file.name)
            print(f"  + {key}")
            vault['pages'][key] = mp3_to_base64(mp3_file)
    
    # 3. Yardımcı sesler (newsound klasöründeki doğrudan mp3'ler)
    print("[3/4] Yardımcı sesleri işliyorum...")
    helper_files = ['seçildi.mp3', 've.mp3']
    for filename in helper_files:
        file_path = BASE_DIR / filename
        if file_path.exists():
            key = normalize_name(filename)
            print(f"  + {key}")
            vault['helpers'][key] = mp3_to_base64(file_path)
    
    # 4. Sistem sesleri (android res/raw/*.mp3 - sadece key listesi, base64 yok çünkü zaten native)
    print("[4/4] Sistem ses anahtarlarını ekliyorum...")
    system_keys = [
        'wakeup', 'prod_found', 'prod_not_found', 'not_understood',
        'sayim_saved', 'fire_saved', 'ship_line_added', 'ship_line_removed',
        'prod_new_opened', 'prod_edit_opened', 'prod_cancelled', 'prod_saved',
        'filter_applied', 'ship_started', 'ship_closed', 'ship_no_active',
        'ship_summary', 'fire_summary', 'sayim_cleared', 'sayim_delta',
        'cam_opened', 'cam_closed', 'mic_paused', 'dev_info', 'status_summary',
        'undo_success', 'undo_empty', 'shutdown', 'whatsapp_opened',
        'modal_name_set', 'modal_plu_set', 'modal_barcode_set',
        'modal_crate_set', 'modal_unit_set', 'urunler_sekmesi_acildi'
    ]
    for key in system_keys:
        vault['system'][key] = f"NATIVE://{key}"  # Native playback marker
    
    return vault

def build_command_map():
    """Ses komut işleme için metadata map oluştur"""
    aliases = create_product_aliases()
    
    command_map = {
        'product_aliases': aliases,
        'action_keywords': {
            'sayim': ['sayım', 'sayim', 'kasa', 'kutu'],
            'fire': ['fire', 'atık', 'çöp'],
            'sevkiyat': ['sevkiyat', 'sevk', 'fişe', 'fise', 'yaz'],
        },
        'quantity_units': {
            'kg': ['kilo', 'kilogram', 'kg'],
            'adet': ['adet', 'tane', 'kutu', 'kasa'],
        },
        'page_transitions': {
            'urunler': 'ürünlersayfasıacıldi (2)',
            'sayim': 'sayımsayfasıacıldi',
            'fire': 'firesayfasıacıldi',
            'sevkiyat': 'sevkiyatsayfasıacıldi',
            'aktar': 'aktarsayfasıacıldi',
            'pano': 'panosayfası',
        }
    }
    
    return command_map

def generate_js_file(vault, command_map):
    """JS dosyası oluştur"""
    js_content = '''/**
 * COMPREHENSIVE VOICE VAULT
 * =========================
 * Tüm ses dosyaları (ürün, sayfa, helper) ve komut metadata'sı
 * 
 * Generated by: embed_all_comprehensive.py
 */

// Ses kasası (base64 + native refs)
window.VOICE_VAULT = ''' + json.dumps(vault, indent=2, ensure_ascii=False) + ''';

// Komut işleme metadata
window.VOICE_COMMAND_MAP = ''' + json.dumps(command_map, indent=2, ensure_ascii=False) + ''';

// Yardımcı fonksiyonlar
window.VoiceHelpers = {
    /**
     * Ses çal (native veya base64)
     */
    play: function(key, category = 'products') {
        const vault = window.VOICE_VAULT[category];
        if (!vault || !vault[key]) {
            console.warn('[Voice] Ses bulunamadı:', key, category);
            return false;
        }
        
        const data = vault[key];
        
        // Native playback
        if (data.startsWith('NATIVE://')) {
            const nativeKey = data.replace('NATIVE://', '');
            if (window.AndroidBridge && window.AndroidBridge.playVoice) {
                window.AndroidBridge.playVoice(nativeKey);
                return true;
            }
        }
        
        // Base64 playback
        try {
            const audio = new Audio('data:audio/mp3;base64,' + data);
            audio.play();
            return true;
        } catch (e) {
            console.error('[Voice] Ses çalma hatası:', e);
            return false;
        }
    },
    
    /**
     * Ürün varyasyonlarını listele
     */
    getProductVariants: function(baseProduct) {
        const normalized = baseProduct.toLowerCase().replace(/\\s+/g, '_');
        const aliases = window.VOICE_COMMAND_MAP.product_aliases;
        
        for (const [base, variants] of Object.entries(aliases)) {
            if (base === normalized || variants.includes(normalized)) {
                return variants;
            }
        }
        
        return [normalized];  // Tek varyant
    },
    
    /**
     * Komut tipini tespit et (sayım, fire, sevkiyat)
     */
    detectAction: function(text) {
        const keywords = window.VOICE_COMMAND_MAP.action_keywords;
        const lower = text.toLowerCase();
        
        for (const [action, words] of Object.entries(keywords)) {
            if (words.some(w => lower.includes(w))) {
                return action;
            }
        }
        
        return 'sayim';  // Default
    }
};

console.log('[Voice Vault] Yüklendi:', {
    products: Object.keys(window.VOICE_VAULT.products).length,
    pages: Object.keys(window.VOICE_VAULT.pages).length,
    helpers: Object.keys(window.VOICE_VAULT.helpers).length,
    system: Object.keys(window.VOICE_VAULT.system).length
});
'''
    
    with open(OUTPUT_JS, 'w', encoding='utf-8') as f:
        f.write(js_content)
    
    print(f"\n✅ JS dosyası oluşturuldu: {OUTPUT_JS}")

def generate_json_map(command_map):
    """JSON map dosyası oluştur"""
    with open(OUTPUT_MAP, 'w', encoding='utf-8') as f:
        json.dump(command_map, f, indent=2, ensure_ascii=False)
    
    print(f"✅ JSON map oluşturuldu: {OUTPUT_MAP}")

def main():
    print("=" * 60)
    print("KAPSAMLI SES ENTEGRASYON SCRIPT'İ")
    print("=" * 60)
    print()
    
    # Vault oluştur
    vault = build_voice_vault()
    
    # Command map oluştur
    command_map = build_command_map()
    
    # Dosyaları yaz
    generate_js_file(vault, command_map)
    generate_json_map(command_map)
    
    print()
    print("=" * 60)
    print("✅ TÜM SES DOSYALARI HAZIR!")
    print("=" * 60)
    print(f"Ürün sesleri: {len(vault['products'])}")
    print(f"Sayfa sesleri: {len(vault['pages'])}")
    print(f"Yardımcı sesler: {len(vault['helpers'])}")
    print(f"Sistem sesleri: {len(vault['system'])}")
    print()
    print("Sonraki adım: comprehensive_voice_vault.js dosyasını index.html'e ekle")

if __name__ == '__main__':
    main()
