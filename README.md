# Screen Recorder (Android Native)

Aplikasi perekam layar Android Native berbasis Jetpack Compose dengan kualitas Full HD 1080p, dukungan pilihan 60, 90, dan 120 FPS, perekaman audio internal murni, serta in-app auto updater langsung dari GitHub Releases.

## Fitur Utama

- Resolusi tetap 1080p dengan aspect ratio proporsional terhadap layar perangkat.
- Pilihan Frame Rate: 60 FPS, 90 FPS, dan 120 FPS.
- Deteksi otomatis kapabilitas refresh rate layar dan encoder hardware MediaCodec (opsi otomatis dinonaktifkan jika perangkat tidak mendukung).
- Perekaman audio internal eksklusif via AudioPlaybackCaptureConfiguration (Android 10+).
- Foreground Service bertipe mediaProjection sesuai regulasi Android 14/15.
- In-App Auto Update otomatis dari GitHub Releases.

## Menjalankan dan Membangun

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```
