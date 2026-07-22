# Что изменено и как интегрировать

## Новые файлы

### `app/src/main/java/com/letify/app/ui/water/WaterRepository.kt`
Хранилище данных о воде на SharedPreferences.
- Баланс воды начинается с 0 каждый день (автосброс при смене даты)
- Записи хранятся постоянно (история)
- Синглтон: `WaterRepository.get(context)`

### `app/src/main/java/com/letify/app/ui/water/WaterFillCircle.kt`
Новый компонент — анимированный круг воды:
- Без кольца-обводки — чистый заполняющийся круг
- Волновая анимация на поверхности воды
- Плавное заполнение снизу вверх

### `app/src/main/java/com/letify/app/ui/screens/WaterScreen.kt`
Переработанный экран питания/воды:
- Компактный переключатель вкладок (Питание / Вода) — 36 dp вместо 44 dp
- Плавная анимация при переключении вкладок (slide + fade)
- Слайдер вместо кнопок быстрого добавления (50–1000 мл, шаг 25 мл)
- Кнопка «Добавить» с количеством
- Строка «История» → переход на экран истории

### `app/src/main/java/com/letify/app/ui/screens/WaterHistoryScreen.kt`
Отдельный экран истории воды:
- Сводная карточка за сегодня (выпито / цель / прогресс)
- Бар-чарт за 14 дней (зелёный = достигнута цель, синий = текущий, прозрачный синий = прошлые)
- Список записей за сегодня с временем
- Общая статистика (дней / среднее / лучший день)

## Изменённые файлы

### `components/Segmented.kt`
- Радиус контейнера: 999 dp → 14 dp (как на скрине)
- Пилюля внутри: 999 dp → 10 dp
- Высота вкладок: 44 dp → 36 dp (компактнее)

### `components/Card.kt`
- Радиус по умолчанию: `Letify.shapes.Card` → 20 dp (явный, стабильный)

### `components/PrimaryActionButton.kt`
- Радиус кнопки: 18 dp → 16 dp (чуть мягче, как в скрине)

## Как подключить новые экраны

В вашем навигационном графе (предположительно в NavGraph или AppNavHost) добавьте:

```kotlin
// Импорты
import com.letify.app.ui.screens.NutritionScreen
import com.letify.app.ui.screens.WaterHistoryScreen

// Вместо старого экрана питания:
NutritionScreen(
    onOpenWaterHistory = { /* навигация на WaterHistoryScreen */ }
)

// Новый экран истории:
WaterHistoryScreen(
    onBack = { /* popBackStack() */ }
)
```

## Иконки (Solar)
Используемые иконки (все из набора Solar Duotone Bold на iconify):
- `waterdrop-bold-duotone` — вода
- `cup-paper-bold-duotone` — кружка
- `target-bold-duotone` — цель
- `chart-2-bold-duotone` — прогресс
- `history-bold-duotone` — история
- `apple-bold-duotone` — питание
- `add-circle-bold-duotone` — добавить
- `alt-arrow-right-outline` / `alt-arrow-left-outline` — стрелки
- `calendar-bold-duotone`, `graph-up-bold-duotone`, `star-bold-duotone`

Убедитесь, что SVG-файлы этих иконок есть в `assets/icons/solar/`.

## Зависимости
Никаких новых зависимостей не требуется — используются только
Material3 `Slider` (уже в BOM), Compose Canvas и SharedPreferences.

Однако убедитесь что в `build.gradle.kts` есть:
```kotlin
"-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
```
(уже присутствует в вашем gradle).
