# ViewModel ve İş Mantığı Refactoring Planı

Bu çalışma kapsamında, mevcut `HomeViewModel`'in sorumluluklarını (API çağrıları, veritabanı işlemleri, state yönetimi) daha temiz bir mimariye taşımayı hedefliyoruz.

## User Review Required
- ViewModel'daki iş mantığını "Use Case" (Domain layer) sınıflarına taşımayı öneriyorum.
- Bu işlem sonucunda `HomeViewModel` sadece UI state'ini tutan ve kullanıcı etkileşimlerini Use Case'lere yönlendiren bir yapıya kavuşacak.

## Open Questions
- Mevcut ViewModel içinde test edilmesi zor olan spesifik bir mantık var mı?
- Use Case yapısı için `domain` paketinin oluşturulmasını onaylıyor musun?

## Proposed Changes

### [Domain Layer]
- **[NEW]** `domain/usecase/GetHomeDataUseCase.kt` (Home ekranı verilerini getiren mantık buraya taşınacak)
- **[NEW]** `domain/usecase/UpdateWordStatusUseCase.kt` (Kelime durumunu güncelleyen mantık buraya taşınacak)

### [UI Layer]
#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/apollo/AndroidStudioProjects/Finebulary/app/src/main/java/com/language/finebulary/ui/viewmodel/HomeViewModel.kt)
- İş mantığını ilgili Use Case'lere devret.
- UI state yönetimini koru.

## Verification Plan

### Automated Tests
- `HomeViewModel` için mevcut testlerin (varsa) geçmeye devam ettiğini doğrula.
- Yeni oluşturulan Use Case sınıfları için birer unit test oluştur.

### Manual Verification
- Home ekranındaki tüm etkileşimlerin (kelime ekleme, durum güncelleme vb.) aynı şekilde çalışmaya devam ettiğini kontrol et.
