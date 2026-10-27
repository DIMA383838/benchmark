# Проект №5

В проекте требуется реализовать два крупных блока функциональности:

1. Бенчмарк-тесты, сравнивающие производительность различных способов вызова методов и решения типовых задач (в зависимости от языка).
2. Утилиту ClassInspector, которая анализирует структуру типа, строит иерархию наследования, выводит данные в текстовом и JSON-форматах, а также генерирует экземпляры классов по аналогии с `Instancio.create()`.

## 1. Бенчмарки производительности

Бенчмарк-тесты обеспечивают объективный, измеримый ориентир для сравнения и оценки производительности. Они устанавливают четкие ожидания и позволяют проводить сравнительный анализ результатов.

В этом задании вам потребуется написать тесты и провести сравнительное испытание для разных способов вызова метода.

### Общие требования

* Написать набор бенчмарк-тестов для каждого варианта.
* Использовать профильный фреймворк измерения производительности: JMH или аналогичный - для других языков.
* Подготовительная работа должна выполняться в setup-методах.   
  Для разных способов вызова метода потребуется вызвать `clazz.getMethod(...)`, `MethodHandles.lookup().findVirtual(...)`, `LambdaMetafactory.metafactory(...)`
* Использовать механизм "чёрной дыры" (Blackhole или аналог), чтобы исключить оптимизации компилятора/интерпретатора.
* Длительность прогонов должна позволять получить стабильные результаты.
* Опубликовать итоговую таблицу с результатами.

### Сценарий тестирования

Реализовать бенчмарки для четырёх вариантов вызова метода `Student#name()`:

* прямой вызов
* java.lang.reflect.Method
* MethodHandles
* LambdaMetafactory

### Инструкции по реализации

* Замеры производительности следует выполнять с использованием одинаковых вычислительных ресурсов, при низкой нагрузке на тестовую платформу.
* Весь код получения reflection-объектов, method handles, regexp, сериализаторов и т.д. вынести в setup.
* В каждом тестовом методе вызывать blackhole.consume(), чтобы исключить оптимизацию выполнения.
* Прогревочные итерации + измерения должны быть явно заданы.
* Обеспечить длительные и стабильные прогоны.
* Зафиксировать итоговую таблицу результатов в отдельном файле и приложить к основному MR.

#### Пример сформированной таблицы:

```text
Benchmark                                      Mode  Cnt     Score     Error  Units
StringConcatBenchmark.benchmarkStringAddition  avgt    5  1184,626 ± 149,152  ns/op
StringConcatBenchmark.benchmarkStringBuilder   avgt    5   235,312 ±  36,325  ns/op
```

### Полезные ссылки

1. https://www.baeldung.com/java-microbenchmark-harness
2. https://habr.com/ru/companies/sberbank/articles/814299/
3. https://davidvlijmincx.com/posts/benchmark-and-profile-java/
4. https://jenkov.com/tutorials/java-performance/jmh.html

## 2. Утилита ClassInspector

Утилита должна анализировать любой класс/тип, собирать информацию о его структуре, строить дерево наследования, а также создавать заполненные экземпляры классов.

### Функциональные требования

Программа должна представлять собой утилиту командной строки, которая принимает на вход следующие параметры:

* --class, -c - полное имя класса.  
  Программа должна завершать свою работу с ошибкой, если класс по переданному имени не найден
* --format, -f - необязательный параметр, указывающий на формат вывода результатов.
  * необходимо реализовать поддержку следующих форматов:
    * TEXT
    * JSON
  * утилита должна завершать свою работу с ошибкой, если на вход передан неподдерживаемый формат

При реализации не забывайте, что программа представляет собой консольную утилиту.
Поэтому, любое исключение в приложении должно транслироваться в соответствующий код возврата:

* 0 - программа успешно завершила свою работу
* 1 - непредвиденная ошибка
* 2 - некорректное использование программы (неверные параметры)

Пример запуска программы:
`java -jar hw5-class-inspector.jar --class java.util.ArrayList --format TEXT`

### Анализ класса

#### Сигнатура метода

`public static String inspect(Class<?> clazz, String format)`

#### Функционал анализа:

* Имя класса (полное квалифицированное имя)
* Родительский класс (если есть)
* Интерфейсы / трейты / протоколы
* Поля:
  - модификаторы доступа (public, private, protected)
  - имя
  - тип
* Методы:
  - модификаторы доступа
  - имя
  - параметры (тип и порядок)
  - возвращаемый тип
* Аннотации класса и его членов
* Иерархия наследования в виде дерева

#### Форматы вывода:

##### **TEXT**

```
Class: Person
Superclass: Human
Interfaces:
  - Serializable
Fields:
  - private name (String)
  - private age (int)
Methods:
  - public getName() : String
  - public setName(String) : void
  - public getAge() : int
  - public setAge(int) : void
Annotations:
  - @Entity
Hierarchy:
  Human
    └── Person
          └── Employee
                 └── Manager
```

##### **JSON**

```json
{
  "class": "Person",
  "superclass": "Human",
  "interfaces": ["Serializable"],
  "fields": [
    { "access": "private", "name": "name", "type": "String" },
    { "access": "private", "name": "age", "type": "int" }
  ],
  "methods": [
    { "access": "public", "name": "getName", "params": [], "returnType": "String" },
    { "access": "public", "name": "setName", "params": ["String"], "returnType": "void" },
    { "access": "public", "name": "getAge", "params": [], "returnType": "int" },
    { "access": "public", "name": "setAge", "params": ["int"], "returnType": "void" }
  ],
  "annotations": ["Entity"],
  "hierarchy": {
    "Human": {
      "Person": {
        "Employee": {
          "Manager": {}
        }
      }
    }
  }
}
```

#### Пример вызова функции

```java
@Entity
public class Person extends Human implements Named {
    private String name;
    private int age;

    //getter-ы and setter-ы опущены для краткости
}

    String result = ClassInspector.inspect(Person.class, "TEXT");
    System.out.println(result);
```

### Создание экземпляров классов

Метод должен создавать объект и автоматически заполнять его поля случайными значениями
(аналог `Instancio.create(Class)`).

#### Сигнатура метода

`public static <T> T create(Class<T> clazz)`

### Нефункциональные требования

Форматы вывода должны строго соответствовать описанному шаблону.
Генерация значений должна поддерживать:
- строки
- числа
- булевые
- даты
- вложенные объекты
- коллекции/списки/массивы

### Инструкции по реализации

#### Анализ класса

* Использовать Reflection API для получения (+ полезные ссылки):
  * Class.forName(String) для получения объекта-рефлексии класса
  * clazz.getName() - имя класса
  * clazz.getSuperclass() - суперкласс
  * clazz.getInterfaces() - интерфейсы
  * clazz.getDeclaredFields() - поля
  * clazz.getDeclaredMethods() - методы
  * clazz.getAnnotations() - аннотации
  * clazz.getPermittedSubclasses() - для поиска потомков
* Форматирование вывода
  * [TEXT](#TEXT): использовать StringBuilder для построчного формирования
  * [JSON](#JSON): создать структуру и сериализовать (использовать Jackson)

#### Создание экземпляров

Алгоритм создания нового объекта:

- находить конструктор
- заполнять примитивы и строки случайными значениями
- рекурсивно создавать вложенные объекты
- ограничивать глубину вложенности

### Полезные ссылки

1. https://www.baeldung.com/java-reflection
2. https://www.baeldung.com/java-find-all-classes-in-package
3. https://habr.com/ru/companies/otus/articles/764244/
4. https://habr.com/ru/articles/318418/
5. https://blog.skillfactory.ru/glossary/java-reflection-api/
6. https://dev.java/learn/reflection/
7. https://www.instancio.org/user-guide/#creating-objects

## Критерии оценки

Общее кол-во баллов за работу - 100 баллов
