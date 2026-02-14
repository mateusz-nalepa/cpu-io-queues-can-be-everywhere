# CPU-Bound, I/O Bound: Queues can be everywhere

A repository demonstrating how thread‑pool tasks queue wait time appears across different types of tasks and
how easily it hides in plain sight. It contains also some examples of how to reduce response times when queue wait time is high.
Some un-expected things about thread pools are also included.

# TL;DR

CPU usage is misleading when it comes to thread pool tasks queue wait time.
Queues can be present at any CPU utilization level.

What to do when queue wait time > 0?

- just add more threads
  - absolutely simplest, really
    - does every app truly need to be ultra‑fast and hyper‑optimized?
    - probably not
    - it's like a soft version of `add more instances` :D
- Thread Pool Isolation -> aka Bulkhead
  - It protects from the "noisy neighbour"
  - request A -> Thread Pool A
  - request B -> Thread Pool B
- Thread Stage Isolation -> aka Stage Event-Driven Architecture (SEDA)
  - request -> IO Pool 1 -> CPU Pool 1 -> IO Pool 2 - CPU Pool 2 etc
  - maximize cpu, minimize queue wait time
- Hybrid: Bulkhead + SEDA
- maybe just more threads/instances
- other things

And what if queue size is almost 0?

- maybe cache is needed
- maybe async calls (http, database) can be done
- maybe deep dive with e.g. async-profiler
- maybe fewer threads/instances are needed ❤️
- maybe some other things? 🤔

# Short introduction
Many different systems behave surprisingly similarly: 
they rely on threads or thread pools, 
make HTTP requests in blocking or non‑blocking mode, 
and parse JSON while executing business logic. 
But sooner or later they begin to slow down - 
like driving through a city during rush hour, 
when every street hides another traffic jam.

Let's say, that regular application has some `I/O-bound tasks`
and some `CPU-bound tasks`.
For example, getting data from http-client is an `I/O-bound task`,
and doing JSON serialization/deserialization 
and probably everything else is a `CPU-bound task`.
When the traffic is low, then response times are flat, and everything is fine.
But when the traffic grows, then response times are increasing, 
and the system becomes slow.
On metric, it looks like a sinusoidal wave, with peaks(e.q. day) 
and valleys(e.q. night).
The more traffic, the higher peaks.
And it may happen on a daily basis.
One of the reason for that are `queues`. 
They store info about pending tasks waiting for execution.


Let's use given analogy to understand this better:
`a cashier scanning groceries`. In this example, two metrics matter:

- queue wait time - how long customers stand in line
- scanning time

Based on that store owner can make some decisions, e.g. open new checkout line.

But what if... there is only one metric... 
what does it mean? Honestly, no idea 😀
If time is e.g. 10 seconds, then:

- maybe X seconds standing in line + 10 seconds scanning time
- maybe 8 seconds standing in line + 2 seconds scanning time
- maybe 10 seconds standing in line + X seconds scanning time

Another important thing to mention is that customers (tasks in the queue) 
may not be aware of the cashier utilization (CPU usage). 
Cause a queue can appear at any CPU utilization level.

So it's good to monitor standing in line time and scanning time as two separate metrics.

`Bonus`
> If the queue is really high, it’s probably worth calculating a `Time to Consume Queue` metric :D
>
> It can be useful when doing migrations for millions of records :D


# How to use this repo

Treat this repository as a Sandbox - an educational repo,
rather than production‑ready code.
The examples are intentionally minimal.
They focus solely on showing thread‑pool tasks queue wait time.

Programming principles such as SOLID, KISS, DRY, or Hexagonal Architecture were not considered when creating this repo.
Tracing is also not included.

Check also [The USE Method](https://www.brendangregg.com/usemethod.html) by Brendan Gregg

Steps for this repo:
- start with reading `this readme.md` file
- jump to [presentation-blog-examples module](presentation-blog-examples) and run examples
- jump to [spring-examples module](spring-examples) and run examples & see results in Grafana
- jump to [thread-pool-un-expected-things module](thread-pool-un-expected-things) and learn about some (un)expected things

# Notes

## Note 1 - Language / framework

Examples are written in Kotlin, but the rules of queuing and resource isolation are universal.
Whether Go, Rust, Java, or any other language is used,
the hardware limits and queuing effects are probably the same :D

## Note 2 - Metrics

Different libraries and frameworks handle thread pools in their own ways.  
Because of that, some of them may not expose fully accurate metrics out of the box.  
In some cases, the metrics may also behave differently than expected.
Simply because the library may not be fully aware of the underlying threads and queues.

## Note 3 - Other pools

Queue wait time can happen also, when there is a `Connection Pool` or any `other Pool`.
Rules are exactly the same. But it's out of scope for this repo.
In this repo there is only `Thread Pool`.

## Note 4 - Thread returning response from server

The response from an endpoint is always returned on a server thread.
But it's intentionally omitted in diagrams, to make them easier to read.

## Note 5 - Thread pool saturation

When all threads in thread pool are busy, then thread pool is saturated.
In this repository, the term `queue wait time` 
is used to describe the time of the saturation.

# Thread, Thread Pools

There is always some `Thread` - like a cashier at the checkout:

```kotlin
fun main() {
    Thread.ofPlatform()
        .name("some-thread")
        .start {
            println("${Thread.currentThread()} : Hello world!")
        }
        .join()
}
```

Or a `Thread Pool`, like many cashiers working in parallel:

```kotlin
fun main() {
    val threadPoolExecutor = ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue(10))
    threadPoolExecutor
        .submit {
            println("${Thread.currentThread()} : Hello world!")
        }
        .get()
}
```

What, if customers are waiting in a line?

- measure queue wait time as one metric
- measure scanning time as another metric

#### Debugging thread pools
When dealing with threads, it's `ABSOLUTELY GOOD IDEA` to check which thread executes code.
Given code is simple enough to check this :D

```kotlin
println("Current thread: ${Thread.currentThread()}")
println("Current thread isVirtual: ${Thread.currentThread().isVirtual}")
```

#### Some default thread pools sizes
Default Thread Pool Sizes:

- Spring Boot WebMVC + Tomcat  
  Default thread pool size: 200 threads


- Spring Boot WebMVC + Tomcat (Virtual Threads)  
  Default thread pool size: equal to the number of CPU cores


- Spring Boot WebFlux + Netty  
  Default event loop size: equal to the number of CPU cores

#### What about Schedulers, Dispatchers?

Under the hood: just threads.

Project Reactor:

```kotlin
private val scheduler =
    Schedulers.fromExecutor(
        ThreadPoolExecutor(
            1, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue(10)
        )
    )
```

Kotlin Coroutines:

```kotlin
private val dispatcher =
    ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue(10))
        .asCoroutineDispatcher()
```

For any other library... it's probably the same 😄

# When queue wait time happens

`Queue wait time` may occur when all threads in thread pool are:
- busy 
  - e.q: JSON serialization/deserialization 
  - high cpu utilization
  - thousands of `cpu bound tasks`? probably bad
- blocked 
  - e.q: waiting for response from http-client
  - low cpu utilization
  - thousands of `blocking tasks`? probably we can live with that

![cpu-usage.png](images/cpu-usage.png)

# How to measure queue wait time

In order to measure queue wait time (and more) use:
```kotlin
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics

val threadPoolExecutor = 
    ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue(10))

val monitoredThreadPoolExecutor = 
    ExecutorServiceMetrics
        .monitor(
            meterRegistry,
            threadPoolExecutor,
            threadPoolName,
        )
```

Thanks to this code, the metric `executor.idle` will be available - yes, this is literally queue wait time. The name may be misleading, but the behavior is not.

Check [Micrometer JVM Metrics for more](https://docs.micrometer.io/micrometer/reference/reference/jvm.html)

# Fastest way to reduce queue wait

Just add more threads:

- really, absolutely simplest
  - does every app truly need to be ultra‑fast and hyper‑optimized?
  - probably not
  - it's like a soft version of `add more instances` :D

What can happen under high load?

- noisy neighbour, one endpoint consumes all resources
- slow app:
  - all threads are doing I/O
  - or all threads are doing CPU
  - other things

# A little bit slower way to reduce response times

There are two patterns (maybe there is some more?) that can be used to reduce response times, when queue wait time is high:
- bulkhead pattern
  - it protects resources
  - many thread pools, many endpoints
  ![bulkhead.png](images/bulkhead.png)
- Staged Event-Driven Architecture (SEDA)
  - it makes resources faster
  - many thread pools, one endpoint
  ![seda.png](images/seda.png)

In those approaches, additional thread pools may be needed,
thus more context-switching. But context-switching is not a problem, 
when it is not excessive.

# Presentation / Blog examples

The module [presentation-blog-examples](presentation-blog-examples/src/main/kotlin/com/nalepa/demo)
contains pure Kotlin, zero frameworks, zero magic.
Only the minimal code needed to illustrate the concepts.
Simple files with a `main()` method that can be executed after cloning this repository.


# Spring Boot Examples

> Each example includes instructions on how to run it,
as well as a detailed threads diagram.

There are two different types of examples:

- applications with `fast` and `slow` endpoints
    - bulkhead pattern is used
    - it protects resources
    - check [spring-examples/bulkhead module](spring-examples/apps/bulkhead/readme-bulkhead.md)

- applications with only one endpoint, but under the hood, http-client is called
    - Staged Event-Driven Architecture (SEDA) is used
    - it makes resources faster
    - check [spring-examples/seda module](spring-examples/apps/seda/readme-seda.md)

`Nalepa TODO`: Make sure one again, that data in module readme files are valid

# Thread Pool (Un)Expected Things

Sometimes things does not work as expected.
Knowing about them, can save a lot of debugging hours.

Please check [thread-pool-un-expected-things](thread-pool-un-expected-things/thread-pool-un-expected-things.md)

# Contributing

If you spot an error, feel free to open an issue or fork the repo and submit a Pull Request with a fix.
