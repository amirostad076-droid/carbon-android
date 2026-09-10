# CARBON Native Android

این پروژه کلاینت Native اندروید CARBON است و از WebView، TWA یا Chrome استفاده
نمی‌کند. UI با Jetpack Compose رندر می‌شود و مستقیماً به API و Gateway نمونه
`fluxer.arashyn.ir` متصل است.

## وضعیت milestone اول

- Splash و برند اصلی CARBON
- ورود ایمیل/رمز و TOTP MFA
- ذخیره رمزگذاری‌شده token
- فهرست server و text channel
- دریافت و ارسال پیام
- WebSocket Gateway و تازه‌سازی پیام‌های ورودی

آپلود presigned، تماس LiveKit Android و FCM در milestone بعدی اضافه می‌شوند.

## Build

```sh
gradle assembleDebug bundleDebug
```

Debug application id برابر `ir.arashyn.carbon.nativepreview` است تا کنار نسخه
wrapper نصب شود و امکان مقایسه امن فراهم باشد.
