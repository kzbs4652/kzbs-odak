# 🛡️ KZBS ODAK - Kullanım ve Kurulum Kılavuzu

**KZBS ODAK**, dikkat dağınıklığını önlemek ve tavizsiz derin odaklanma sağlamak üzere tasarlanmış profesyonel bir Android uygulamasıdır.

---

## 📦 APK Dosyası Nerede?
İlk kararlı sürüm başarıyla derlendi ve doğrudan masaüstünüzdeki klasöre bırakıldı:
👉 **Dosya Yolu:** `c:\Users\Acer\OneDrive\Masaüstü\apk\KZBS_ODAK_v1.0.0.apk`

---

## 📲 Telefona Nasıl Yüklenir?
1. `KZBS_ODAK_v1.0.0.apk` dosyasını telefonunuza aktarın (USB kablo, WhatsApp Kendime Notlar, Telegram veya Google Drive ile).
2. Dosyaya telefonunuzda dokunun ve **"Yükle"** deyin.
   *(Eğer ilk kez dışarıdan APK kuruyorsanız "Bu kaynaktan izin ver" seçeneğini açmanız yeterlidir).*

---

## 🚀 İlk Açılış ve Kurulum Sihirbazı (Onboarding)
Uygulamayı ilk açtığınızda **KZBS ODAK Kurulum Sihirbazı** sizi karşılar. Tavizsiz koruma için şu 4 temel izni vermeniz istenir:

1. **Kullanım Erişimi:** Yasaklı bir uygulamanın açıldığını anlık tespit eder.
2. **Ekran Üzerinde Gösterme (Overlay):** Yasaklı uygulama açıldığında özel kilit perdesini sermek için kullanılır.
3. **Bildirim Erişimi:** Odak süresince seçilen uygulamaların bildirimlerini süresi bitene kadar anında susturur.
4. **Arka Plan Pil Koruması:** Telefon ekranı kapalıyken Android'in odak sayacını uyutmasını engeller.
5. **Kişisel Motivasyon Mesajınız:** Kilit ekranında görmek istediğiniz mesajı yazın (Hafızada saklanır ve sonraki seanslarda otomatik hazır gelir).

> **Not:** İzinleri verdikçe butonlar otomatik olarak yeşil **"TAMAM ✓"** haline döner.

---

## 🎯 Temel Özellikler

### 1. Odak Ekranı (Dashboard)
* **Süre Seçimi:** 25 dk, 45 dk, 60 dk, 90 dk veya özel süre.
* **Kalıcı Mesaj:** Belirlediğiniz motivasyon mesajı otomatik gelir, isterseniz değiştirebilirsiniz.
* **Tavizsiz Mod:** Başlat butonuna basıldığı an süre işlemeye başlar. Pes etme butonu yoktur.

### 2. Uygulama Yönetimi (Çift Korumalı Seçici)
* Telefonda yüklü uygulamalar aranabilir liste halinde gelir.
* **Kilitle (🔒):** Açılırsa o uygulamaya girildiğinde tam ekran kilit perdesi çıkar.
* **Sustur (🔕):** Açılırsa o uygulamadan odak süresi boyunca bildirim gelmez.

### 3. Kilit Perdesi (Engelleme Ekranı)
* Yasaklı bir uygulamaya basıldığında arka plan kararır.
* Tepede parlayan **KZBS ODAK Logosu** yer alır.
* Kalan süreyi gösteren canlı büyük sayaç (`00:34:12`) ve ilerleme barı akar.
* Sizin ayarladığınız motivasyon sözü büyük puntolarla gösterilir.
* Tek çıkış yolu: **"MASANIN BAŞINA DÖN"** butonu (Kullanıcıyı doğrudan telefonun ana sayfasına atar).

### 4. Telefon Yeniden Başlatılsa Bile Koruma
* Telefon kapatılıp açılsa dahi (`BOOT_COMPLETED`), sistem odak süresini kontrol eder.
* Kalan süre varsa kilit ekranı ve bildirim susturma otomatik devam eder.

### 5. Uygulama İçi Otomatik Güncelleme (OTA Updater)
* İleride benden bir değişiklik veya yeni sürüm istediğinizde:
  1. Ben yeni sürümü hazırlarım.
  2. Uygulamayı açtığınızda ekranda **"Yeni Sürüm Mevcut! (v1.x.x)"** penceresi çıkar.
  3. Değişiklik listesini okuyup **[Şimdi İndir]** butonuna basarak doğrudan uygulama içinden güncelleyebilirsiniz.
  4. İstemezseniz **[X]** butonuyla pencereyi kapatıp kullanmaya devam edebilirsiniz.
