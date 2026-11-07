# مواقيتي · Mawaqiti Plus (Beta)

تطبيق أوقات صلاة أوفلاين يعتمد على حساب فلكي حقيقي لموضع الشمس، مع بوصلة قبلة.

An offline prayer-time app that computes times from the sun's real position,
with a qibla compass. Clean-room Android implementation (Kotlin + Jetpack Compose).

---

## ⬇️ تحميل النسخة التجريبية · Download the beta

اذهب إلى صفحة **Releases** وحمّل `MawaqitiPlus-1.0.0-beta1.apk`:

**→ https://github.com/amh2002/mawaqiti-plus/releases/tag/v1.0.0-beta1**

### التثبيت · Install

1. حمّل ملف APK على جهاز أندرويد (8.0 أو أحدث).
2. عند فتح الملف، اسمح بالتثبيت من هذا المصدر (نظام أندرويد سيسألك مرة واحدة).
3. افتح التطبيق واختر مدينتك أو اسمح بالوصول للموقع.

1. Download the APK on an Android 8.0+ device.
2. Open it and allow installing from this source when Android asks.
3. Launch the app, pick a city or allow location access.

> هذا إصدار تجريبي (beta): التطبيق موقّع بمفتاح تطوير، وليس للنشر على المتجر.
> This is a beta build signed with a debug key — not a Play Store release.

## ما الجديد في هذه النسخة · What's in this beta

- **محرك فلكي حقيقي** بدل جداول ثابتة: التاريخ اليولياني ← ميل الشمس ومعادلة الزمن ←
  زوال الشمس (الظهر) ← زوايا الساعة للفجر والعشاء ← الشروق والغروب عند ‎−0.833°‎ ←
  العصر بمعامل الظل ‎1‎ أو ‎2‎.
- **طرق الحساب**: رابطة العالم الإسلامي (افتراضي 18°/17°)، ISNA (15°/15°)،
  الهيئة المصرية (19.5°/17.5°)، أم القرى (18.5° والعشاء = المغرب + 90 دقيقة)،
  كراتشي (18°/18°)، وزوايا مخصصة.
- **قواعد خطوط العرض العالية**: منتصف الليل (افتراضي) أو حسب الزاوية.
- **الأوقات**: الفجر، الشروق، الظهر، العصر، المغرب، العشاء، الإمساك (الفجر − 10)،
  منتصف الليل، وبداية الثلث الأخير.
- **بوصلة القبلة** باستخدام حساس الدوران واتجاه الكعبة
  (‎21.422487° شمالاً، 39.826206° شرقاً‎).
- **واجهة عربية أولاً** (RTL) مع ترجمة إنجليزية، Material 3، وضع ليلي.
- **يعمل بدون إنترنت تماماً**: لا إذن إنترنت، ولا إعلانات، ولا تحليلات، ولا خدمات جوجل.

## ما نرجو اختباره · What testers should check

- هل تتطابق الأوقات مع مسجدك/تقويمك المحلي؟ (اذكر المدينة وطريقة الحساب)
- تبديل المدينة والموقع اليدوي، والإذن بالوصول للموقع ثم رفضه.
- دقة بوصلة القبلة في أماكن مختلفة (مع/بدون معايرة الحساس).
- الوضع الليلي، اللغة الإنجليزية، الخطوط الكبيرة، اتجاه RTL.
- أي تعطل أو شاشة لا تستجيب — أرفق طراز الجهاز وإصدار أندرويد.

أرسل الملاحظات عبر **Issues** في هذا المستودع.

## الخصوصية · Privacy

- لا يوجد إذن `INTERNET` في التطبيق إطلاقاً.
- الموقع يُستخدم محلياً فقط لتحديد الإحداثيات، ولا يُرسل لأي جهة.
- الإعدادات تُحفظ على الجهاز عبر DataStore.

## التحقق · Verification

- **مقارنة مستقلة** مع مكتبة Adhan 1.2.1: **2879/2879 حدثاً ضمن ±دقيقة واحدة (100%)**
  عبر 20 مدينة × 24 تاريخاً. التفاصيل في [`docs/ENGINE_CROSSCHECK.md`](docs/ENGINE_CROSSCHECK.md).
- **معايرة** مع 124 جدولاً من التطبيق الأصلي دون أي تعديل صناعي للمحرك:
  [`docs/CALIBRATION.md`](docs/CALIBRATION.md).
- **ملاحظات الإصدار**: [`docs/BETA_NOTES.md`](docs/BETA_NOTES.md).

## البناء من المصدر · Build from source

```bash
# JDK 17 + Android SDK (platform 36, build-tools 36)
export ANDROID_HOME=/path/to/android-sdk
./gradlew clean test assembleDebug
```

النتيجة: `app/build/outputs/apk/debug/app-debug.apk`

## الرخصة · License

لم تُحدَّد بعد. · Not decided yet — all rights reserved until a license is added.
