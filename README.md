# Лабораторная работа №3

---

**Выполнил:** Барашко Арсений Александрович  

**Группа:** Р3331  

**Преподаватель:** Пенской Александр Владимирович  

**Язык:** Clojure

---

## Цель

Получить навыки работы с вводом/выводом, потоковой обработкой данных, командной строкой.

---

## Задание

Реализовать потоковую интерполяцию с поддержкой нескольких алгоритмов:

- Линейная интерполяция (отрезками)
- Интерполяция Лагранжа
- Интерполяция Ньютона

### Требования

- Настройки через аргументы командной строки
- Входные данные в CSV-подобном формате на stdin
- Выходные данные на stdout
- Потоковый режим обработки (как `cat | grep`)
- Функциональный стиль программирования
- Разделение I/O от алгоритмов

---

## Архитектура

```
+---------------------------+
| обработка входного потока |  <- parse.clj
+---------------------------+
            |
            | поток точек (lazy seq)
            v
+-----------------------+      +------------------------------+
| алгоритм интерполяции |<-----| генератор точек (grid.clj)   |
| (linear/lagrange/     |      +------------------------------+
|  newton)              |
+-----------------------+
            |
            | поток рассчитанных точек
            v
+------------------------+
| печать выходных данных |  <- format.clj
+------------------------+
```

### Потоковый режим (оконная обработка)

```
o o o o o o . . x x x      <- первое окно (вывод слева)
  x x x . . o . . x x x    <- середина (вывод в центре)
    x x x . . o . . x x x
      x x x . . o o o o o o EOF  <- последнее окно (вывод справа)
```

- `o` — рассчитанные точки
- `.` — точки окна, используемые для расчёта
- `x` — точки вне текущего окна

---

## Структура проекта

```
interpolation/
├── deps.edn
├── README.md
├── src/interpolation/
│   ├── core.clj              # Main, CLI
│   ├── parse.clj             # Парсинг входных данных
│   ├── grid.clj              # Генерация сетки x-значений
│   ├── format.clj            # Форматирование вывода
│   ├── stream.clj            # Потоковая обработка
│   └── algorithms/
│       ├── linear.clj        # Линейная интерполяция
│       ├── lagrange.clj      # Интерполяция Лагранжа
│       ├── newton.clj        # Интерполяция Ньютона
│       └── window.clj        # Оконная обработка
├── test/interpolation/
│   ├── test_helpers.clj
│   ├── parse_test.clj
│   ├── grid_test.clj
│   ├── format_test.clj
│   ├── integration_test.clj
│   └── algorithms/
│       ├── linear_test.clj
│       ├── lagrange_test.clj
│       ├── newton_test.clj
│       └── window_test.clj
└── .github/workflows/
    └── ci.yml                
```

---

## Реализации алгоритмов

### Линейная интерполяция (`algorithms/linear.clj`)

Интерполяция отрезками между соседними точками.

Для каждой пары точек $(x_1, y_1)$ и $(x_2, y_2)$:

$$y = y_1 + \frac{y_2 - y_1}{x_2 - x_1} \cdot (x - x_1)$$

```clojure
(defn linear-fn
  "Returns linear interpolation function for segment [p1, p2]."
  [[x1 y1] [x2 y2]]
  (let [dx (- x2 x1)
        m  (if (zero? dx) 0.0 (/ (- y2 y1) dx))]
    (fn [x]
      (+ y1 (* m (- x x1))))))
```

**Особенности:**
- Работает в потоковом режиме — выдаёт результаты сразу при получении второй точки
- Не требует накопления точек в буфере
- Простая и быстрая реализация

---

### Интерполяция Лагранжа (`algorithms/lagrange.clj`)

Полином Лагранжа степени $n-1$ по $n$ точкам:

$$L(x) = \sum_{i=0}^{n-1} y_i \prod_{j \neq i} \frac{x - x_j}{x_i - x_j}$$

```clojure
(defn lagrange-basis
  "Compute Lagrange basis polynomial L_i(x) for given points and index i."
  [points i x]
  (let [n (count points)
        xi (first (nth points i))]
    (reduce (fn [acc j]
              (if (= i j)
                acc
                (let [xj (first (nth points j))]
                  (* acc (/ (- x xj) (- xi xj))))))
            1.0
            (range n))))

(defn lagrange-poly
  "Build Lagrange polynomial function from points."
  [points]
  (let [n (count points)]
    (fn [x]
      (reduce (fn [acc i]
                (let [yi (second (nth points i))
                      Li (lagrange-basis points i x)]
                  (+ acc (* yi Li))))
              0.0
              (range n)))))
```

**Особенности:**
- Использует базисные полиномы $L_i(x)$
- Каждый базис равен 1 в точке $x_i$ и 0 в остальных точках
- Работает через оконный механизм (`window.clj`)

---

### Интерполяция Ньютона (`algorithms/newton.clj`)

Полином Ньютона с разделёнными разностями:

$$N(x) = f[x_0] + f[x_0, x_1](x - x_0) + f[x_0, x_1, x_2](x - x_0)(x - x_1) + \ldots$$

Разделённые разности вычисляются рекурсивно:

$$f[x_i, \ldots, x_{i+k}] = \frac{f[x_{i+1}, \ldots, x_{i+k}] - f[x_i, \ldots, x_{i+k-1}]}{x_{i+k} - x_i}$$

```clojure
(defn divided-diffs
  "Compute divided differences for Newton interpolation."
  [points]
  (let [n  (count points)
        xs (mapv first points)
        ys (mapv second points)]
    (loop [table [ys]
           k 1]
      (if (= k n)
        (mapv first table)
        (let [prev (peek table)
              next-row (mapv (fn [i]
                               (/ (- (nth prev (inc i)) (nth prev i))
                                  (- (nth xs (+ i k)) (nth xs i))))
                             (range (- n k)))]
          (recur (conj table next-row) (inc k)))))))

(defn newton-poly
  "Build Newton polynomial function from points."
  [points]
  (let [xs    (mapv first points)
        coefs (divided-diffs points)
        n     (count xs)]
    (fn [x]
      (loop [i 0, acc 0.0, term 1.0]
        (if (= i n)
          acc
          (recur (inc i)
                 (+ acc (* (nth coefs i) term))
                 (* term (- x (nth xs i)))))))))
```

**Особенности:**
- Эффективное вычисление коэффициентов через таблицу разделённых разностей
- Использует `loop/recur` для хвостовой рекурсии
- Работает через оконный механизм (`window.clj`)

---

### Потоковая обработка (`stream.clj`, `algorithms/window.clj`)

```clojure
(defn process-stream!
  "Process input stream with given algorithms."
  [algorithms lines verbose?]
  (loop [lines lines
         states (mapv :state algorithms)
         last-point nil]
    (if-let [line (first lines)]
      (if-let [point (parse-point line)]
        (do
          (validate-order! last-point point)
          (let [results (mapv (fn [algo state]
                                ((:process-fn algo) state point))
                              algorithms states)
                new-states (mapv first results)
                outputs (mapcat second results)]
            (print-points! (sort-by second outputs))
            (recur (rest lines) new-states point)))
        (recur (rest lines) states last-point))
      ;; EOF - finalize
      (let [final-outputs (mapcat (fn [algo state]
                                    ((:finalize-fn algo) state))
                                  algorithms states)]
        (print-points! (sort-by second final-outputs))))))
```

**Особенности:**
- Ленивая обработка через `line-seq`
- Каждый алгоритм имеет своё состояние
- Результаты выводятся сразу при поступлении достаточного количества данных
- Валидация сортировки входных данных

---


### Опции командной строки

| Опция | Описание |
|-------|----------|
| `-l`, `--linear` | Линейная интерполяция |
| `-L`, `--lagrange` | Интерполяция Лагранжа |
| `-N`, `--newton` | Интерполяция Ньютона |
| `-n`, `--points N` | Размер окна (по умолчанию: 4) |
| `-s`, `--step STEP` | Шаг дискретизации |
| `-v`, `--verbose` | Отладочный вывод |
| `-h`, `--help` | Справка |

### Формат данных

**Вход:** `x y`, `x;y` или `x\ty` (отсортировано по x)

**Выход:** `<algorithm>: <x> <y>`

---

## Пример работы

```
$ echo -e "0 0\n1 1\n2 4\n3 9\n4 16" | clojure -M -m interpolation.core --linear --newton -n 3 --step 1

linear: 0 0
newton: 0 0
linear: 1 1
newton: 1 1
linear: 2 4
newton: 2 4
linear: 3 9
newton: 3 9
linear: 4 16
newton: 4 16
```

---

## Выводы

В данной лабораторной работе я:

- Реализовал потоковую интерполяцию с тремя алгоритмами (линейная, Лагранж, Ньютон)
- Применил функциональный стиль: чистые функции для алгоритмов, ленивые последовательности для потоковой обработки
- Разделил ответственность между модулями: парсинг, форматирование, алгоритмы, потоковая логика
- Использовал идиоматичные приёмы Clojure:
  - `loop/recur` для хвостовой рекурсии
  - `reduce` для построения таблицы разделённых разностей
  - `line-seq` для ленивого чтения stdin
  - Функции высшего порядка (`map`, `filter`, `mapcat`)
- Настроил CI/CD с тестами, линтером и форматтером
- Написал 21 тест с 160 assertions, покрывающих все модули

**Ключевые приёмы программирования:**

| Приём | Где использован |
|-------|-----------------|
| Хвостовая рекурсия | `newton-poly`, `divided-diffs`, `process-stream!` |
| Ленивые последовательности | `grid`, `line-seq` |
| Функции высшего порядка | `lagrange-poly`, `window/process` |
| Замыкания | `linear-fn`, `lagrange-poly`, `newton-poly` |
| Разделение эффектов | I/O в `stream.clj`, чистые функции в `algorithms/` |

---

