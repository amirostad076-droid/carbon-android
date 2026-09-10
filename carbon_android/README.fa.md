# CARBON Android

کلاینت اندروید CARBON با شناسه `ir.arashyn.carbon` و دامنه ثابت
`https://fluxer.arashyn.ir` ساخته شده است. برنامه از Trusted Web Activity استفاده
می‌کند تا قابلیت‌های فعلی Fluxer شامل WebRTC/LiveKit، انتخاب و آپلود فایل، Service
Worker و Web Push در موتور Chrome اجرا شوند.

## پیش‌نیاز سمت سرور

فایل `server/.well-known/assetlinks.json` باید پس از جایگزینی اثر انگشت گواهی امضا در
مسیر زیر سرو شود و پاسخ آن `200` با `Content-Type: application/json` باشد:

`https://fluxer.arashyn.ir/.well-known/assetlinks.json`

اگر این فایل نصب نشود، برنامه همچنان باز می‌شود ولی نوار مرورگر نمایش داده خواهد شد
و اعلان‌های تفویض‌شده قابل اتکا نیستند.

## ساخت آزمایشی

پروژه را در Android Studio باز کنید یا از Gradle 8.11.1 و JDK 17 استفاده کنید:

```sh
gradle assembleDebug bundleDebug
```

خروجی‌ها در `app/build/outputs/apk/debug` و `app/build/outputs/bundle/debug` قرار
می‌گیرند. Workflow همراه پروژه نیز همین دو خروجی را به‌صورت artifact می‌سازد.

## امضای انتشار

کلید انتشار را عمومی یا داخل Git قرار ندهید. متغیرهای زیر را در محیط CI تنظیم کنید:

- `CARBON_KEYSTORE_FILE`
- `CARBON_KEYSTORE_PASSWORD`
- `CARBON_KEY_ALIAS`
- `CARBON_KEY_PASSWORD`

سپس اثر انگشت SHA-256 همان certificate را جایگزین
`REPLACE_WITH_RELEASE_CERT_SHA256` کنید. برای انتشار در Google Play بهتر است Play App
Signing فعال شود و fingerprint گواهی Play نیز به `assetlinks.json` اضافه شود.

## محدودیت تماس پس‌زمینه

تماس صوتی/تصویری داخل برنامه و هنگام باز بودن آن از WebRTC استفاده می‌کند. دریافت تماس
کاملاً native در زمانی که برنامه force-stop شده است به FCM و یک Android foreground
service در بک‌اند Fluxer نیاز دارد و صرفاً با wrapper قابل پیاده‌سازی نیست.
