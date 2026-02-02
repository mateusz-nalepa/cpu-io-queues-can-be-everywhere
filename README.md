# CPU-Bound, I/O Bound: Queues can be everywhere

A repository demonstrating how thread‑pool saturation (queue wait time) appears across different types of tasks - and
how easily it hides in plain sight. When queue wait time is high, then system is slow. 

Analogy: a cashier scanning groceries. Two metrics matter:

- queue wait time - how long customers stand in line
- scanning time

If there is only one metric... what does it mean? Honestly, no idea.
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


# About this repo

Treat this repository as a Sandbox - an educational repo,
rather than production‑ready code.
The examples are intentionally minimal.
They focus solely on showing thread‑pool saturation (queue wait time).

Programming principles such as SOLID, KISS, DRY, or Hexagonal Architecture were not considered when creating this repo.
Tracing is also not included.

Check also [The USE Method](https://www.brendangregg.com/usemethod.html) by Brendan Gregg

# Note about programming language / framework

Examples are written in Kotlin, but the rules of queuing and resource isolation are universal.
Whether Go, Rust, Java, or any other language is used,
the hardware limits and queuing effects are probably the same :D

# Quick summary of the repo

What to do when queue wait time > 0?

- just add more threads
    - absolutely simplest, really
    - does every app truly need to be ultra‑fast and hyper‑optimized
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
  maybe deep dive with e.g. async-profiler
- maybe just fewer threads/instances
- other things

# Notes

## Note 1

Different libraries and frameworks handle thread pools in their own ways.  
Because of that, some of them may not expose fully accurate metrics out of the box.  
In some cases, the metrics may also behave differently than expected.
Simply because the library may not be fully aware of the underlying threads and queues.

## Note 2

Queue wait time can happen also, when there is a `Connection Pool` or any `other Pool`.
Rules are exactly the same. But it's out of scope for this repo.
In this repo there is only `Thread Pool`.

## Note 3

Response from endpoint is returned always on server thread.
But it's intentionally omitted in diagrams, to make them easier to read.

## Note 4

If you spot an error, feel free to open an issue or fork the repo and submit a Pull Request with a fix.

# Thread, Thread Pools

When dealing with threads, it's absolutely enough to debug code with given println :D

```kotlin
println("Current thread: ${Thread.currentThread()}")
println("Current thread isVirtual: ${Thread.currentThread().isVirtual}")
```


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

Default Thread Pool Sizes:

- Spring Boot WebMVC + Tomcat  
  Default thread pool size: 200 threads


- Spring Boot WebMVC + Tomcat (Virtual Threads)  
  Default thread pool size: equal to the number of CPU cores


- Spring Boot WebFlux + Netty  
  Default event loop size: equal to the number of CPU cores

## What about Schedulers, Dispatchers?

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

Check [ThreadsPerformanceTest](apps/different-endpoints/webmvc-classic-threads/src/test/kotlin/com/nalepa/demo/common/ThreadsPerformanceTest.kt)

# Fastest way to reduce queue wait

Just add more threads:

- really, absolutely simplest
- does every app truly need to be ultra‑fast and hyper‑optimized
- probably not
- it's like a soft version of `add more instances` :D

What can happen under high load?

- noisy neighbour, one endpoint consumes all resources
- slow app:
  - all threads are doing I/O
  - or all threads are doing CPU
  - other things

# Presentation / Blog examples

Start with the module [presentation-blog-examples](presentation-blog-examples/src/main/kotlin/com/nalepa/demo)
It contains pure Kotlin, zero frameworks, zero magic.
Only the minimal code needed to illustrate the concepts.
Simple files with a `main()` method that can be executed after cloning this repository.


# Spring Boot Examples

> `How to run` is included in every example

There are two different types of examples:

- applications with `fast` and `slow` endpoints
    - bulkhead pattern is used
    - it protects resources
    - check [spring-examples/bulkhead module](spring-examples/apps/bulkhead/readme-bulkhead.md)
  ![bulkhead.png](bulkhead.png)

- applications with only one endpoint, but under the hood, http-client is called
    - Staged Event-Driven Architecture (SEDA) is used
    - it makes resources faster
    - check [spring-examples/seda module](spring-examples/apps/seda/readme-seda.md)
    ![seda.png](seda.png)

# Other

Some unexpected things when dealing with Thread Pools

### Behaviour of `corePoolSize` and `maxPoolSize` it is unintuitive

It minimizes number of threads used.

Let's say that there is config like that:

```yaml
some-thread-pool:
  core-pool-size: 4
  max-pool-size: 10
  queue-size: 1000
```

It looks like, that:
```text
-> 4 tasks are added to the queue, so 4 core threads are working
-> another 4 tasks are added to the queue, so:
    -> IN THEORY 4 additional threads are created
    -> IN PRACTICE, tasks are just added to the queue
-> fifth thread will be created, when queue is full
```

Solutions? 
- the easiest one, set `corePoolSize` and `maxPoolSize` to the same number
- create wrapper for `LinkedBlockingQueue` (or any other queue) and modify method `offer`
  - Tomcat thread pool works like that


