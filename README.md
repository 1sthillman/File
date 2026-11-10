# Manav Sayım Zebra (PWA)

Bu proje Zebra el terminalleri, iPhone (Safari) ve Android (Chrome/Edge) cihazlar için optimize edilmiş, **çevrimdışı (offline)** çalışabilen **Progressive Web App (PWA)** sayım uygulamasıdır.

## 🌐 Canlı Uygulama Adresi
👉 **[https://1sthillman.github.io/File/](https://1sthillman.github.io/File/)**

---

## 📱 Telefona Uygulama Olarak İndirme / Yükleme Rehberi (APK Gerektirmez)

Bu uygulama modern **PWA (Progressive Web App)** teknolojisini kullanır. Herhangi bir APK dosyası indirmeye veya mağazaya gerek kalmadan telefonunuza yerel bir uygulama gibi yüklenir:

### 🍎 iPhone / iPad (Safari)
1. Safari tarayıcısında **[https://1sthillman.github.io/File/](https://1sthillman.github.io/File/)** adresini açın.
2. Alttaki **Paylaş (⎙ / ⎋)** simgesine dokunun.
3. Menüden aşağı kaydırıp **"Ana Ekrana Ekle" (Add to Home Screen)** seçeneğini seçin.
4. Sağ üstteki **"Ekle"** butonuna dokunun.
5. Uygulama artık ana ekranınızda özel logosuyla bir mobil uygulama gibi yer alır; tarayıcı çubuğu olmadan tam ekran çalışır.

### 🤖 Android & Zebra El Terminalleri (Chrome / Edge)
1. Chrome veya tarayıcınızda bağlantıyı açın.
2. Ekranda çıkan **"Telefona Yükle"** butonuna dokunun veya sağ üstteki **üç nokta (⋮)** menüsünden **"Uygulamayı Yükle"** ya da **"Ana Ekrana Ekle"** seçeneğini seçin.
3. Onayladıktan sonra uygulama cihazınıza yüklenir.

---

## ⚡ Çevrimdışı (Offline) Çalışma Özelliği
- Uygulamada **Service Worker (`sw.js`)** entegrasyonu mevcuttur.
- İlk kez açıldığında tüm dosyalar, arayüz ve kütüphaneler telefonun hafızasına önbelleğe alınır.
- İnternet bağlantınız kopsa veya depoda/manavda hiç internet çekmese bile uygulama kesintisiz çalışmaya, barkod okumaya ve sayım yapmaya devam eder.
- Yapılan sayımlar cihazınızın yerel hafızasında (LocalStorage / IndexedDB) güvenle saklanır.

---

## 🛠️ GitHub Actions Dağıtımı
Deponun `main` dalına her kod gönderildiğinde `.github/workflows/deploy.yml` otomatik çalışır ve sayfayı günceller.
