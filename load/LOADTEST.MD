# 3 этап - нагрузочное тестирование
## инструменты
Для нагрузки был использован wrk, скрипты для генерации нагрузки лежат в папке load.
---
Профилирование было осуществено с помощью jvisualvm.

## тестирование по оптимизаций

### PUT без перезаписи с replicas=2/3

#### 2 threads and 4 connections:
```sh
wrk --latency -c4 -d2m -s putReplicasTwoThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.57ms    3.34ms 176.13ms   93.89%
    Req/Sec    49.48      6.72    60.00     55.68%
  Latency Distribution
     50%   38.07ms
     75%   38.72ms
     90%   39.96ms
     99%   42.95ms
  11865 requests in 2.00m, 1.06MB read
Requests/sec:     98.80
Transfer/sec:      9.07KB
```

---

#### 4 threads and 4 connections:
```sh
wrk --latency -t4 -c4 -d2m -s putReplicasTwoThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.56ms    2.47ms 146.56ms   90.87%
    Req/Sec    24.74      5.01    30.00     53.36%
  Latency Distribution
     50%   38.10ms
     75%   38.66ms
     90%   40.20ms
     99%   42.94ms
  11868 requests in 2.00m, 1.06MB read
Requests/sec:     98.83
Transfer/sec:      9.07KB
```
---

### PUT без перезаписи с replicas=3/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s putReplicasThreeThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.63ms    3.63ms 157.69ms   93.06%
    Req/Sec    47.15      7.93    60.00     77.57%
  Latency Distribution
     50%   39.32ms
     75%   42.04ms
     90%   42.92ms
     99%   50.64ms
  11306 requests in 2.00m, 1.01MB read
Requests/sec:     94.19
Transfer/sec:      8.65KB
```
---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s putReplicasThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    41.04ms    3.91ms 174.11ms   91.31%
    Req/Sec    23.41      4.82    30.00     65.00%
  Latency Distribution
     50%   41.35ms
     75%   42.36ms
     90%   43.41ms
     99%   50.83ms
  11232 requests in 2.00m, 1.01MB read
Requests/sec:     93.54
Transfer/sec:      8.59KB
```
---

### На имеющихся данных GET без повторов с replicas=2/3:

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasTwoThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.73ms    3.13ms 167.26ms   88.22%
    Req/Sec    49.09      7.70    60.00     41.49%
  Latency Distribution
     50%   39.94ms
     75%   40.02ms
     90%   43.90ms
     99%   48.01ms
  11769 requests in 2.00m, 1.08MB read
  Non-2xx or 3xx responses: 11769
Requests/sec:     98.06
Transfer/sec:      9.20KB
```
---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s getReplicasTwoThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.72ms    3.09ms 161.36ms   86.99%
    Req/Sec    24.55      5.03    30.00     53.92%
  Latency Distribution
     50%   39.93ms
     75%   40.02ms
     90%   43.90ms
     99%   48.06ms
  11772 requests in 2.00m, 1.08MB read
  Non-2xx or 3xx responses: 11772
Requests/sec:     98.08
Transfer/sec:      9.20KB
```

---

### На имеющихся данных GET без повторов с replicas=3/3:

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasThreeThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.54ms    2.85ms 159.68ms   88.11%
    Req/Sec    49.32      6.86    60.00     53.76%
  Latency Distribution
     50%   39.92ms
     75%   39.99ms
     90%   43.83ms
     99%   47.92ms
  11822 requests in 2.00m, 1.08MB read
  Non-2xx or 3xx responses: 11822
Requests/sec:     98.48
Transfer/sec:      9.24KB
```

---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s getReplicasThreeThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.67ms    3.00ms 159.21ms   86.15%
    Req/Sec    24.58      5.03    30.00     53.59%
  Latency Distribution
     50%   39.93ms
     75%   40.01ms
     90%   43.88ms
     99%   48.00ms
  11788 requests in 2.00m, 1.08MB read
  Non-2xx or 3xx responses: 11788
Requests/sec:     98.20
Transfer/sec:      9.21KB
```
---

### PUT c перезаписью с replicas=2/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s putReplicasRepeatTwoThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.88ms    2.60ms 141.57ms   89.55%
    Req/Sec    49.31      6.84    60.00     53.19%
  Latency Distribution
     50%   38.46ms
     75%   38.84ms
     90%   41.69ms
     99%   43.34ms
  11823 requests in 2.00m, 1.06MB read
Requests/sec:     98.47
Transfer/sec:      9.04KB
```
---
#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s putReplicasRepeatTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.99ms    3.68ms 192.10ms   96.57%
    Req/Sec    24.66      5.02    30.00     53.69%
  Latency Distribution
     50%   38.53ms
     75%   38.84ms
     90%   41.86ms
     99%   42.94ms
  11828 requests in 2.00m, 1.06MB read
Requests/sec:     98.51
Transfer/sec:      9.05KB
```
---

### PUT c перезаписью с replicas=3/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s putReplicasRepeatThreeThree.lua http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.91ms    3.36ms 139.93ms   91.12%
    Req/Sec    46.92      7.22    60.00     83.80%
  Latency Distribution
     50%   40.78ms
     75%   42.44ms
     90%   43.56ms
     99%   50.43ms
  11252 requests in 2.00m, 1.01MB read
Requests/sec:     93.69
Transfer/sec:      8.60KB
```
---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s putReplicasRepeatThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    41.37ms    5.48ms 243.49ms   96.93%
    Req/Sec    23.29      4.80    30.00     65.99%
  Latency Distribution
     50%   41.50ms
     75%   42.53ms
     90%   45.34ms
     99%   54.19ms
  11174 requests in 2.00m, 1.00MB read
Requests/sec:     93.04
Transfer/sec:      8.54KB
```
---

### GET c перезаписью с replicas=2/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasRepeatTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.96ms    3.55ms 182.40ms   96.88%
    Req/Sec    48.83      6.94    60.00     52.00%
  Latency Distribution
     50%   39.94ms
     75%   40.06ms
     90%   43.93ms
     99%   50.43ms
  11707 requests in 2.00m, 1.07MB read
  Non-2xx or 3xx responses: 11707
Requests/sec:     97.54
Transfer/sec:      9.15KB
```
---

#### 4 threads and 4 connections
```sh 
wrk --latency -t4 -c4 -d2m -s getReplicasRepeatTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.91ms    3.55ms 180.64ms   96.99%
    Req/Sec    24.44      5.01    30.00     54.86%
  Latency Distribution
     50%   39.94ms
     75%   40.05ms
     90%   43.93ms
     99%   48.07ms
  11721 requests in 2.00m, 1.07MB read
  Non-2xx or 3xx responses: 11721
Requests/sec:     97.65
Transfer/sec:      9.16KB
```
---

### GET c перезаписью с replicas=3/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasRepeatThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.82ms    3.19ms 170.29ms   95.57%
    Req/Sec    48.98      7.19    60.00     48.25%
  Latency Distribution
     50%   39.94ms
     75%   40.04ms
     90%   43.92ms
     99%   48.00ms
  11741 requests in 2.00m, 1.08MB read
  Non-2xx or 3xx responses: 11741
Requests/sec:     97.83
Transfer/sec:      9.17KB
```
---
#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s getReplicasRepeatThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    41.16ms    3.92ms 190.14ms   95.59%
    Req/Sec    24.29      5.01    30.00     55.93%
  Latency Distribution
     50%   39.95ms
     75%   40.13ms
     90%   43.96ms
     99%   51.98ms
  11654 requests in 2.00m, 1.07MB read
  Non-2xx or 3xx responses: 11654
Requests/sec:     97.06
Transfer/sec:      9.10KB
```

## ПОСЛЕ ОПТИМИЗАЦИЙ


### PUT без перезаписи с replicas=2/3

#### 2 threads and 4 connections:
```sh
wrk --latency -c4 -d2m -s putReplicasTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.68ms    3.91ms 185.91ms   97.89%
    Req/Sec    49.28      6.50    60.00     58.79%
  Latency Distribution
     50%   38.06ms
     75%   38.61ms
     90%   41.48ms
     99%   42.98ms
  11813 requests in 2.00m, 1.06MB read
Requests/sec:     98.39
Transfer/sec:      9.03KB
```
---

#### 4 threads and 4 connections:
```sh
wrk --latency -c4 -d2m -s putReplicasTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.70ms    2.75ms 147.37ms   89.43%
    Req/Sec    49.22      7.00    60.00     51.21%
  Latency Distribution
     50%   38.09ms
     75%   38.66ms
     90%   41.54ms
     99%   44.15ms
  11805 requests in 2.00m, 1.06MB read
Requests/sec:     98.30
Transfer/sec:      9.03KB
```
---

### PUT без перезаписи с replicas=3/3:

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s putReplicasThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    41.40ms    5.37ms 229.78ms   97.27%
    Req/Sec    45.89      8.09    60.00     79.73%
  Latency Distribution
     50%   41.40ms
     75%   42.22ms
     90%   44.47ms
     99%   50.54ms
  10999 requests in 2.00m, 0.99MB read
Requests/sec:     91.62
Transfer/sec:      8.41KB
```
---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s putReplicasThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    41.41ms    3.69ms 158.71ms   88.83%
    Req/Sec    23.11      4.68    30.00     68.19%
  Latency Distribution
     50%   41.46ms
     75%   42.34ms
     90%   45.23ms
     99%   50.69ms
  11090 requests in 2.00m, 0.99MB read
Requests/sec:     92.35
Transfer/sec:      8.48KB
```
---

### На имеющихся данных GET без повторов с replicas=2/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.97ms    3.38ms 165.94ms   96.71%
    Req/Sec    48.83      6.99    60.00     51.42%
  Latency Distribution
     50%   39.94ms
     75%   40.07ms
     90%   43.94ms
     99%   49.12ms
  11707 requests in 2.00m, 1.07MB read
  Non-2xx or 3xx responses: 11707
Requests/sec:     97.49
Transfer/sec:      9.14KB
```
---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s getReplicasTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.87ms    3.23ms 153.86ms   96.51%
    Req/Sec    24.46      5.01    30.00     54.97%
  Latency Distribution
     50%   39.94ms
     75%   40.05ms
     90%   43.92ms
     99%   50.24ms
  11735 requests in 2.00m, 1.07MB read
  Non-2xx or 3xx responses: 11735
Requests/sec:     97.72
Transfer/sec:      9.16KB
```

---

### На имеющихся данных GET без повторов с replicas=3/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.95ms    3.42ms 171.79ms   96.44%
    Req/Sec    48.84      6.85    60.00     53.17%
  Latency Distribution
     50%   39.94ms
     75%   40.06ms
     90%   43.93ms
     99%   51.87ms
  11710 requests in 2.00m, 1.07MB read
  Non-2xx or 3xx responses: 11710
Requests/sec:     97.52
Transfer/sec:      9.15KB
```
---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s getReplicasThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.56ms    1.89ms 109.43ms   85.13%
    Req/Sec    24.62      4.99    30.00     53.45%
  Latency Distribution
     50%   39.94ms
     75%   40.03ms
     90%   43.87ms
     99%   47.04ms
  11811 requests in 2.00m, 1.08MB read
  Non-2xx or 3xx responses: 11811
Requests/sec:     98.38
Transfer/sec:      9.23KB
```
---

### PUT c перезаписью с replicas=2/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s putReplicasRepeatTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.90ms    6.48ms 275.64ms   99.36%
    Req/Sec    49.20      6.98    60.00     52.84%
  Latency Distribution
     50%   38.09ms
     75%   38.71ms
     90%   41.70ms
     99%   43.58ms
  11790 requests in 2.00m, 1.06MB read
Requests/sec:     98.18
Transfer/sec:      9.02KB
```

---

#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s putReplicasRepeatTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.56ms    1.77ms 115.19ms   87.48%
    Req/Sec    24.68      4.99    30.00     53.73%
  Latency Distribution
     50%   38.09ms
     75%   38.56ms
     90%   41.44ms
     99%   42.83ms
  11848 requests in 2.00m, 1.06MB read
Requests/sec:     98.68
Transfer/sec:      9.06KB
```
---

### PUT c перезаписью с replicas=3/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s putReplicasRepeatThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.53ms    1.89ms 119.32ms   86.40%
    Req/Sec    49.26      6.81    60.00     53.21%
  Latency Distribution
     50%   38.03ms
     75%   38.46ms
     90%   41.48ms
     99%   42.96ms
  11818 requests in 2.00m, 1.06MB read
Requests/sec:     98.44
Transfer/sec:      9.04KB
```
---
#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s putReplicasRepeatThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.55ms    1.81ms 113.00ms   86.94%
    Req/Sec    24.66      4.98    30.00     54.04%
  Latency Distribution
     50%   38.06ms
     75%   38.54ms
     90%   41.46ms
     99%   42.98ms
  11837 requests in 2.00m, 1.06MB read
Requests/sec:     98.58
Transfer/sec:      9.05KB
```
---

### GET c перезаписью с replicas=2/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasRepeatTwoThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.33ms    1.36ms  51.85ms   88.08%
    Req/Sec    49.54      7.84    60.00     38.36%
  Latency Distribution
     50%   39.92ms
     75%   40.00ms
     90%   43.56ms
     99%   44.09ms
  11877 requests in 2.00m, 4.97MB read
  Non-2xx or 3xx responses: 7882
Requests/sec:     98.94
Transfer/sec:     42.36KB
```
---
#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s getReplicasRepeatTwoThree.lua http://localhost:8080
 http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.37ms    1.74ms  92.07ms   87.55%
    Req/Sec    24.74      5.00    30.00     52.48%
  Latency Distribution
     50%   39.93ms
     75%   40.00ms
     90%   43.81ms
     99%   44.08ms
  11866 requests in 2.00m, 12.47MB read
Requests/sec:     98.84
Transfer/sec:    106.38KB
```
---

### GET c перезаписью с replicas=3/3

#### 2 threads and 4 connections
```sh
wrk --latency -c4 -d2m -s getReplicasRepeatThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.29ms    1.46ms  62.53ms   88.63%
    Req/Sec    49.58      7.48    60.00     44.03%
  Latency Distribution
     50%   39.92ms
     75%   39.99ms
     90%   42.86ms
     99%   44.04ms
  11887 requests in 2.00m, 12.49MB read
Requests/sec:     99.03
Transfer/sec:    106.57KB
```
---
#### 4 threads and 4 connections
```sh
wrk --latency -t4 -c4 -d2m -s getReplicasRepeatThreeThree.lua http://localhost:8080
http://localhost:8080
Running 2m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.37ms    1.79ms 113.89ms   86.99%
    Req/Sec    24.73      5.01    30.00     51.90%
  Latency Distribution
     50%   39.93ms
     75%   40.00ms
     90%   43.82ms
     99%   44.06ms
  11870 requests in 2.00m, 12.48MB read
Requests/sec:     98.84
Transfer/sec:    106.38KB
```

### итоги
В основном было добавлено кеширование часто используемых объектов и проведена работа над DAO хранилищем. Как результат видно повышение количества обработанных запросов и RPS в целом. Тесты проводились по 2 минуты каждый.


