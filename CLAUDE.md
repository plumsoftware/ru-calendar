# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Правила работы

- Всегда отвечать на русском языке.
- Не проверять код (сборка, тесты, lint, ревью) по собственной инициативе — только когда пользователь явно об этом попросит.

## О проекте

«Календарь РФ» (`com.plumsoftware.rucalendar`) — коммерческое Android-приложение (с 2019 г.): производственный календарь РФ с праздниками и напоминаниями о событиях. Публикуется в RuStore, Huawei AppGallery и Google Play (последние два давно не обновлялись). Один модуль `:app`, язык — Java + XML-разметка. Архитектурных паттернов нет — проект писался в одиночку, логика сосредоточена в Activity.

## Команды

```bash
./gradlew assembleRustoreDebug        # debug-сборка для RuStore (также Huawei, Googleplay)
./gradlew assembleRustoreRelease      # release-сборка (подписывается debug-ключом, см. app/build.gradle)
./gradlew assembleRelease             # release для всех трёх магазинов
./gradlew installRustoreDebug         # установка на устройство
./gradlew testRustoreDebugUnitTest    # unit-тесты (только шаблонный ExampleUnitTest)
./gradlew testRustoreDebugUnitTest --tests "com.plumsoftware.rucalendar.ExampleUnitTest"   # один тест
./gradlew connectedAndroidTest   # инструментальные тесты
./gradlew lint                   # lint (abortOnError false, baseline app/lint-baseline.xml)
```

compileSdk/targetSdk 37, minSdk 21, Java 8, multidex. Gradle 9.4.1, AGP 9.2.0 (нужен JDK 17+).

## Сборка под разные магазины

Магазин задаётся product flavor-ом в измерении `store`: `rustore`, `huawei`, `googleplay`. Каждый задаёт `BuildConfig.PLATFORM` (1 — RuStore, 2 — Huawei AppGallery, 3 — Google Play) и рекламные ID Яндекса (РСЯ): `AD_FEED_BANNER_ID`, `AD_EVENT_BANNER_ID`, `AD_APP_OPEN_ID`, `AD_INTERSTITIAL_ID`. В debug-сборке эти ID подменяются демо-блоками Яндекса. Флаги в `defaultConfig`: `SHOW_APP_OPEN_AD`, `SHOW_FEED_BANNER_AD`, `SHOW_EVENT_BANNER_AD` — при false реклама этого вида даже не загружается. Код читает всё через `config/AdsConfig`. `applicationId` у всех flavor-ов общий. Перед релизом нужно проверить `versionCode`/`versionName`.

Реклама (пакет `ads/`): SDK инициализируется в `App`; `InterstitialController` — загрузка при входе на экран праздника и в форму нового события, показ при закрытии праздника и после создания события (если не догрузилась — индикатор, таймаут 5 с); `BannerAds` — адаптивный sticky-баннер внизу «Ленты» и экрана праздника (у контента отступ на высоту баннера + 12 dp); `AppOpenAdController` — реклама при открытии в `MainActivity`.

`config/MyBuildConfig.java`. В debug-сборке ID рекламы пустые/нулевые. Перед релизом нужно проверить `platform`, `versionCode`/`versionName`.

`config/MyBuildConfig.java` — это не сгенерированный BuildConfig, а рукописный класс с константами платформ, рекламными ID и ключом AppMetrica.

## Архитектура

Идёт редизайн по ТЗ `ТЗ редизайн приложения «Календарь — праздники России» (Android).md` (в корне). Макеты — холст Design на claude.ai: https://claude.ai/artifact/7SGDcBoTBQwnjBiD72RtgM (артборды `project/*.dc.html`, читать через Artifact `read`). Экраны делаются по очереди, по порядку из ТЗ.

- **Новый код** — пакеты `data/` и `ui/`. `MainActivity` — хост нижней панели («Месяц» / «Лента» / «Год») и фрагментов, реализует `MonthFragment.Host`. Ещё не сделанные переходы помечены `TODO(ТЗ п. …)` и показывают тост «в разработке».
- **База праздников** — `assets/holidays.json` (перенесена из бывшего `ArraysCelebrations`, тексты не менять). Читает `HolidayRepository`; у события есть тип (`EventType`), фиксированная или плавающая дата (`nth`/`weekday`) и флаг `nonWorking`.
- **Производственный календарь** — `ProductionCalendarRepository`: isdayoff.ru (`?year=&cc=ru&pre=1`, таймаут 5 с) с кэшем по годам в `filesDir/production_calendar`. Сначала показывается кэш, успешный ответ перезаписывает кэш и через `Listener` обновляет экран. Без кэша и сети выходными считаются сб/вс и праздники с `nonWorking`.
- **`CalendarRepository`** сводит праздники, свои события (`UserEventRepository`, SharedPreferences), фильтры (`FilterPreferences`) и производственный календарь; отдаёт события дня в порядке ТЗ п. 6.3 и `MonthModel` для сетки.
- **Сетка месяца** — `ui/month/MonthGridView`: собственный View, рисует на Canvas, сам обрабатывает свайп и выбор дня, доступность через `ExploreByTouchHelper`. Маркеры типов рисует `ui/MarkerDrawable`.
- **Дизайн-система** — `res/values*/colors_redesign.xml`, `styles_redesign.xml` (тема `Theme.Calendar`, текстовые стили), шрифты Golos Text / Unbounded в `res/font`, иконки `ic_*` из ТЗ п. 10. `java.time` доступен через core library desugaring.
- **Экраны**: вкладки `ui/month/MonthFragment`, `ui/feed/FeedFragment`, `ui/year/YearFragment` в `MainActivity` (show/hide, «Назад» возвращает на «Месяц»); `ui/sheet/DaySheetFragment` — карточка дня (BottomSheetDialogFragment); `ui/detail/HolidayActivity` — экран праздника; `ui/event/UserEventActivity` — новое/изменить своё событие (результат возвращается в `MainActivity`). Deep links: `rucalendar://day?date=&id=` — карточка дня, `rucalendar://month?date=` — «Месяц» на дате.
- **Напоминания** — праздники: `ReminderRepository` (упреждение + время); свои события: поля `UserEvent`. Планирует `reminders/ReminderScheduler` → `ReminderWorker` (WorkManager, ежегодные после срабатывания планируются на следующий год). `TimeChangeReceiver` перепланирует их при смене времени/пояса и после перезагрузки. Блок настройки — `ui/reminder/ReminderBlock`. Разрешение на уведомления запрашивается при первом включении напоминания.
- **Виджеты** — `widget/WidgetUpdater` собирает три RemoteViews-виджета (`widget_today`, `widget_weekend`, `widget_week`); обновляются в полночь (AlarmManager → `TimeChangeReceiver`), при системных событиях времени и при смене данных (фильтры, свои события, свежий производственный календарь через слушатель в `App`). В разметке виджетов нельзя использовать обычный `View` (не поддерживается RemoteViews до API 31). Маркеры для виджетов и уведомлений рисует `ui/MarkerBitmaps`.
- **Старый код**: `EventActivity`, `MyNotificationWorker`, `EventNotificationScheduler` остались только для напоминаний, поставленных до редизайна (deep link `rucalendar://event`). `EventService`, `MessagingService` ещё не переделаны. `SettingsActivity` больше не открывается (тема следует системной).
- **Аналитика** — AppMetrica (инициализация в `App`) и Firebase Analytics.
