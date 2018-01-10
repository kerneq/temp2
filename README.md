# 2017-highload-kv
Курсовой проект 2017 года [курса](https://polis.mail.ru/curriculum/program/discipline/50/) "Проектирование высоконагруженных систем" в [Технополис](https://polis.mail.ru).

## Этап 1. HTTP API + хранилище (deadline 2017-10-10)
### Fork
[Форкните проект](https://help.github.com/articles/fork-a-repo/), склонируйте и добавьте `upstream`:
```
$ git clone git@github.com:<username>/2017-highload-kv.git
Cloning into '2017-highload-kv'...
remote: Counting objects: 34, done.
remote: Compressing objects: 100% (24/24), done.
remote: Total 34 (delta 2), reused 34 (delta 2), pack-reused 0
Receiving objects: 100% (34/34), 11.43 KiB | 3.81 MiB/s, done.
Resolving deltas: 100% (2/2), done.
$ git remote add upstream git@github.com:polis-mail-ru/2017-highload-kv.git
$ git fetch upstream
From github.com:polis-mail-ru/2017-highload-kv
 * [new branch]      master     -> upstream/master
```

### Make
Так можно запустить тесты:
```
$ gradle test
```

А вот так -- сервер:
```
$ gradle run
```

### Develop
Откройте в IDE -- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/) нам будет достаточно.

**ВНИМАНИЕ!** При запуске тестов или сервера в IDE необходимо передавать Java опцию `-Xmx1g`. 

В своём Java package `ru.mail.polis.<username>` реализуйте интерфейс [`KVService`](src/main/java/ru/mail/polis/KVService.java) и поддержите следующий HTTP REST API протокол:
* HTTP `GET /v0/entity?id=<ID>` -- получить данные по ключу `<ID>`. Возвращает `200 OK` и данные или `404 Not Found`.
* HTTP `PUT /v0/entity?id=<ID>` -- создать/перезаписать (upsert) данные по ключу `<ID>`. Возвращает `201 Created`.
* HTTP `DELETE /v0/entity?id=<ID>` -- удалить данные по ключу `<ID>`. Возвращает `202 Accepted`.

Возвращайте реализацию интерфейса в [`KVServiceFactory`](src/main/java/ru/mail/polis/KVServiceFactory.java#L48).

Продолжайте запускать тесты и исправлять ошибки, не забывая [подтягивать новые тесты и фиксы из `upstream`](https://help.github.com/articles/syncing-a-fork/). Если заметите ошибку в `upstream`, заводите баг и присылайте pull request ;)

### Report
Когда всё будет готово, присылайте pull request со своей реализацией на review. Не забывайте **отвечать на комментарии в PR** и **исправлять замечания**!

## Этап 2. Кластер (deadline 2017-11-07)
Реализуем поддержку кластерных конфигураций, состоящих из нескольких узлов, взаимодействующих друг с другом через реализованный HTTP API.
Для этого в `KVServiceFactory` передаётся "топология", представленная в виде множества координат **всех** узлов кластера в формате `http://<host>:<port>`.

Кроме того, HTTP API расширяется query-параметром `replicas`, содержащим количество узлов, которые должны подтвердить операцию, чтобы она считалась выполненной успешно.
Значение параметра `replicas` указывается в формате `ack/from`, где:
* `ack` -- сколько ответов нужно получить
* `from` -- от какого количества узлов

Таким образом, теперь узлы должны поддерживать расширенный протокол (совместимый с предыдущей версией):
* HTTP `GET /v0/entity?id=<ID>[&replicas=ack/from]` -- получить данные по ключу `<ID>`. Возвращает:
  * `200 OK` и данные, если ответили хотя бы `ack` из `from` реплик
  * `404 Not Found`, если ни одна из `ack` реплик, вернувших ответ, не содержит данные (либо данные **удалены хотя бы** на одной из `ack` ответивших реплик)
  * `504 Not Enough Replicas`, если не получили `200`/`404` от `ack` реплик из всего множества `from` реплик

* HTTP `PUT /v0/entity?id=<ID>[&replicas=ack/from]` -- создать/перезаписать (upsert) данные по ключу `<ID>`. Возвращает:
  * `201 Created`, если хотя бы `ack` из `from` реплик подтвердили операцию
  * `504 Not Enough Replicas`, если не набралось `ack` подтверждений из всего множества `from` реплик

* HTTP `DELETE /v0/entity?id=<ID>[&replicas=ack/from]` -- удалить данные по ключу `<ID>`. Возвращает:
  * `202 Accepted`, если хотя бы `ack` из `from` реплик подтвердили операцию
  * `504 Not Enough Replicas`, если не набралось `ack` подтверждений из всего множества `from` реплик

Если параметр `replicas` не указан, то в качестве `ack` используется значение по умолчанию, равное **кворуму** от количества узлов в кластере,
а `from` равен общему количеству узлов в кластере, например:
* `1/1` для кластера из одного узла
* `2/2` для кластера из двух узлов
* `2/3` для кластера из трёх узлов
* `3/4` для кластера из четырёх узлов
* `3/5` для кластера из пяти узлов

Выбор узлов-реплик (множества `from`) для каждого `<ID>` является **детерминированным**:
* Множество узлов-реплик для фиксированного ID и меньшего значения `from` является строгим подмножеством для большего значения `from` 
* При `PUT` не сохраняется больше копий данных, чем указано в `from`

Фактически, с помощью параметра `replicas` клиент выбирает, сколько копий данных он хочет хранить, а также
уровень консистентности при выполнении последовательности операций для одного ID.

Таким образом, например, обеспечиваются следующие инварианты (список не исчерпывающий):
* `GET` с `1/2` всегда вернёт данные, сохранённые с помощью `PUT` с `2/2` (даже при недоступности одной реплики при `GET`)
* `GET` с `2/3` всегда вернёт данные, сохранённые с помощью `PUT` с `2/3` (даже при недоступности одной реплики при `GET`)
* `GET` с `1/2` "увидит" результат `DELETE` с `2/2` (даже при недоступности одной реплики при `GET`)
* `GET` с `2/3` "увидит" результат `DELETE` с `2/3` (даже при недоступности одной реплики при `GET`)
* `GET` с `1/2` может не "увидеть" результат `PUT` с `1/2`
* `GET` с `1/3` может не "увидеть" результат `PUT` с `2/3`
* `GET` с `1/2` может вернуть данные несмотря на предшествующий `DELETE` с `1/2`
* `GET` с `1/3` может вернуть данные несмотря на предшествующий `DELETE` с `2/3`
* `GET` с `ack` равным `quorum(from)` "увидит" результат `PUT`/`DELETE` с `ack` равным `quorum(from)` даже при недоступности **<** `quorum(from)` реплик

Так же как и на Этапе 1 присылайте pull request со своей реализацией поддержки кластерной конфигурации на review.
Набор тестов будет расширяться, поэтому не забывайте **подмёрдживать upstream** и **реагировать на замечания**.

## Этап 3. Нагрузочное тестирование и оптимизация (deadline 2017-12-05)
На этом этапе нам предстоит:
* Подать на кластер нагрузку с помощью инструментов нагрузочного тестирования
* Воспользоваться профайлером, чтобы определить места для улучшений
* Пооптимизировать, чтобы улучшить характеристики хранилища
* Повторить процедуру

### Окружение
План-минимум -- поднять 3 локальных узла:
```
$ ./gradlew run
```

План-максимум -- поднять 3 узла в отдельных контейнерах/приложениях.

### Что измеряем
* Пропускную способность (**успешные запросы/сек**)
* Задержку (обязательно **мс/запрос** в **среднем**, а также желательно **90%** и **99%**-перцентили)
* Не менее 1 мин

### Нагрузка
* Обязательно только `PUT` (c/без перезаписи) с `replicas=2/3` и `replicas=3/3`
* Обязательно только `GET` (на большом наборе ключей с/без повторов) с `replicas=2/3` и `replicas=3/3`
* Желательно смесь `PUT`/`GET` 50/50 (с/без перезаписи) с `replicas=2/3` и `replicas=3/3`

Каждый вид нагрузки тестируем в режимах 1/2/4 потока/соединения.

Если готовы по-взрослому, то адаптируйте [Yahoo! Cloud Serving Benchmark](https://github.com/brianfrankcooper/YCSB) к своему хранилищу и получите бонусные баллы.

### Нагрузочное тестирование
#### `curl`
Smoke test, только в один поток и статистику нужно считать самим, но низкий порог входа, чтобы начать:
```
$ for i in $(seq 0 1000000); do time curl -X PUT -d value$i http://localhost:8080/v0/entity?id=key$i; done
...
```

#### `wrk`
Более изощрённые виды нагрузки, в т.ч. с Keep-Alive и многопоточно, но необходимо пописать на Lua.
См. [сайт проекта](https://github.com/wg/wrk) и [примеры скриптов](https://github.com/wg/wrk/tree/master/scripts).

Выглядеть может так:
```
$ wrk --latency -c4 -d5m -s scripts/put.lua http://localhost:8080
Running 5m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.80ms    8.37ms 345.00ms   99.61%
    Req/Sec     1.56k   238.51     2.20k    74.12%
  Latency Distribution
     50%    1.09ms
     75%    1.33ms
     90%    2.59ms
     99%    7.41ms
  928082 requests in 5.00m, 83.20MB read
Requests/sec:   3093.04
Transfer/sec:    283.93KB
$ wrk --latency -c4 -d1m -s scripts/get.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.48ms    1.84ms  47.85ms   97.07%
    Req/Sec     1.55k   297.86     2.04k    58.75%
  Latency Distribution
     50%    1.18ms
     75%    1.40ms
     90%    1.66ms
     99%    9.95ms
  185247 requests in 1.00m, 21.16MB read
Requests/sec:   3085.96
Transfer/sec:    360.95KB
```

#### Yandex.Tank
Возможно всё, но необходимо написать генератор [патронов](http://yandextank.readthedocs.io/en/latest/tutorial.html#preparing-requests) и всё настроить.
См. [сайт проекта](https://overload.yandex.net) и [tutorial](https://overload.yandex.net/login/?next=/mainpage/guide#install).
Если получится, то будут бонусные баллы.

### Профилирование
Чтобы сузить область поиска, можно попробовать протестировать чисто сетевую часть, используя простую in-memory реализацию хранилища.

#### `jvisualvm`
Входит в состав JDK и [поддерживает профилирование](https://docs.oracle.com/javase/8/docs/technotes/guides/visualvm/profiler.html).
Если возникает ошибка при запуске профилирования, укажите опцию JVM `-Xverify:none`.

#### Java Mission Control
Также [входит в состав JDK](https://docs.oracle.com/javacomponents/jmc-5-5/jmc-user-guide/jmc.htm#JMCCI113) и бесплатен для разработки, но не забудьте включить [Java Flight Recorder](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm#JFRUH174).

#### `async-profiler`
Бесплатный и с открытым исходным кодом.
См. [сайт проекта](https://github.com/jvm-profiling-tools/async-profiler).

### Report
Присылайте PR, в который входят commit'ы с оптимизациями по результатам профилирования, а также файл `LOADTEST.md`, содержащий результаты
нагрузочного тестирования и профилирования до и после оптимизаций (в виде дампов консоли, скриншотов и/или графиков).

## Bonus (deadline 2017-12-19)
Фичи, которые позволяют получить дополнительные баллы:
* 10М ключей: нетривиальная реализация хранения данных
* [Consistent Hashing](https://en.wikipedia.org/wiki/Consistent_hashing): распределение данных между узлами устойчивое к сбоям
* Streaming: работоспособность при значениях больше 1 ГБ (и `-Xmx1g`)
* Conflict resolution: [отметки времени Лампорта](https://en.wikipedia.org/wiki/Lamport_timestamps) или [векторные часы](https://en.wikipedia.org/wiki/Vector_clock)
* Expire: возможность указания [времени жизни записей](https://en.wikipedia.org/wiki/Time_to_live)
* Server-side processing: трансформация данных с помощью скрипта, запускаемого на узлах кластера через API
* Нагрузочное тестирование при помощи [Y!CSB](https://github.com/brianfrankcooper/YCSB) или [Yandex.Tank](https://overload.yandex.net)
* Предложите своё

Если решите реализовать что-то бонусное, обязательно сначала обсудите это с преподавателем.