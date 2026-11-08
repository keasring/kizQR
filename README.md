# kizQR

Быстрый сканер и генератор QR-кодов без рекламы, интернета и трекеров.

Статус: в разработке!

## Возможности

**Создание**
- Типы контента: текст, ссылка, Wi-Fi (скан подключается к сети), контакт (vCard 3.0), SMS, телефон, гео-точка
- Живое превью, экспорт PNG 2048px в галерею (`Pictures/kizQR`), системный шторинг
- Полностью офлайн — без аналитики и разрешений на интернет

**Стилизация**
- 6 фирменных пресетов с оригинальными векторными логотипами: Telegram, WhatsApp, VK, Instagram, YouTube, Spotify
- Автодетект по ссылке: вставили `t.me/...` — предложился Telegram-стиль
- 5 форм точек и 4 формы «глаз», однотонный цвет / линейный / радиальный градиент, прозрачный фон
- Логотип в центре: брендовый или любое фото из галереи (коррекция ошибок автоматически поднимается до H)
- Регулируемый уровень коррекции ошибок (L/M/Q/H) и отступ (quiet zone)

**Сканер**
- Распознавание камерой (CameraX + ML Kit, модель вшита — работает офлайн) и из галереи (Photo Picker)
- Умные действия: открыть ссылку, подключиться к Wi-Fi, позвонить, отправить SMS, открыть карту, создать контакт, «создать QR с этим содержимым»

**Прочее**
- История созданных и отсканированных кодов (переживает перезапуск)
- Тёмная тема, динамические цвета Material You (Android 12+)
- Интерфейс на русском

## Технологии

- Kotlin, **Jetpack Compose + Material 3** (AGP 9 built-in Kotlin), Navigation Compose, MVVM
- Генерация QR: матрица через **ZXing core**, рендер свой (`QrRenderer`) — точки, глаза, градиенты, логотипы
- Сканирование: **ML Kit Barcode Scanning** (bundled) + **CameraX**
- История: JSON-файл + StateFlow; настройки — DataStore
- Логотипы брендов: path data из [Simple Icons](https://github.com/simple-icons/simple-icons) (CC0) с нормализацией под `androidx.core.graphics.PathParser`

## Сборка

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew test                 # юнит-тесты форматов QR и парсеров
```

Требования: JDK 17+, Android SDK (compileSdk 37, minSdk 24).

## Структура

```
app/src/main/java/com/kizqr/app/
├── qr/          # QrContent (payload-билдеры), QrStyle, QrRenderer, пресеты, декодер
├── data/        # история (JSON+StateFlow), настройки (DataStore), экспорт PNG
├── ui/
│   ├── create/  # экран создания + редактор стилей (bottom sheet)
│   ├── scan/    # сканер (CameraX + ML Kit) и результат-шит
│   ├── history/ # история
│   └── settings/# тема и о приложении
├── ui/theme/    # оранжевая тема Material 3
art/icon.jpg    # исходник иконки приложения
```

## Лицензии и товарные знаки

Логотипы Telegram, WhatsApp, VK, Instagram, YouTube и Spotify являются товарными знаками соответствующих компаний и используются исключительно для указания назначения пресетов. Path data глифов взяты из [Simple Icons](https://github.com/simple-icons/simple-icons) (CC0).
