# Some (un)expected things when dealing with Thread Pools

At the end of every section, there is info `why this matter`

### Adding more threads for CPU‑bound tasks may make things worse

When all threads are busy doing CPU‑bound work, then adding more threads may make things worse.
Off course CPU can execute many tasks in parallel, but they will be slower.

From the other hand, adding more threads for `blocking tasks` may be a good idea,
when queue wait time is high. It happens because, well, CPU usage for them is near 0%.

There is a code, which is intentionally doing CPU‑bound work, and given number of threads is used to execute it.
When more threads are added, then the total number of iterations per thread is slower

`Note`: This code was executed on notebook with 8 cores.
```kotlin
Number of iterations per thread number: 8: 	 3 018 674
Number of iterations per thread number: 16: 	 1 603 543
Number of iterations per thread number: 32: 	 855 639
Number of iterations per thread number: 128: 	 240 104
```

Check
[ThreadsPerformance](src/main/kotlin/com/nalepa/demo/example01/ThreadsPerformance.kt)
for more

##### Why this matter?

Processing thousand of elements at the same time may cause:
- all customers will be waiting for the response which can cause `OutOfMemoryError`
- none of them will be finished in case of e.q. `OutOfMemoryError`


### Behavior of `corePoolSize` and `maxPoolSize` it is unintuitive

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

Check
[ThreadPoolCoreSizeMaxSize.kt](src/main/kotlin/com/nalepa/demo/example04/ThreadPoolCoreSizeMaxSize.kt)
for more

##### Why this matter?

There can be a performance problem and knowing about default Thread Pool Behavior can save a lot of hours

### Threads created with Thread.ofVirtual().factory() are scheduled on ForkJoinPool

```kotlin
fun virtualThreadFactoryThreadsAreScheduledOnForkJoinPool() {
    val executor1 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("one").factory())
    val executor2 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("two").factory())
    val executor3 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("three").factory())

    // CurrentThread: VirtualThread[#37,one]/runnable@ForkJoinPool-1-worker-1
    val future1 = executor1.submit { println("CurrentThread: " + Thread.currentThread()) }

    // CurrentThread: VirtualThread[#39,two]/runnable@ForkJoinPool-1-worker-2
    val future2 = executor2.submit { println("CurrentThread: " + Thread.currentThread()) }

    // CurrentThread: VirtualThread[#41,three]/runnable@ForkJoinPool-1-worker-3
    val future3 = executor3.submit { println("CurrentThread: " + Thread.currentThread()) }

    future1.get()
    future2.get()
    future3.get()
}
```

Check
[ThreadOfVirtualFactory](src/main/kotlin/com/nalepa/demo/example02/ThreadOfVirtualFactory.kt)
for more

##### Why this matter?

Multiple executors created with virtual‑thread factories still share the same underlying thread pool, which can lead to incorrect assumptions about isolation. Using:
```kotlin
val executor = Executors.newVirtualThreadPerTaskExecutor()
```
keeps the model simple and avoids unnecessary complexity.

Of course, there are scenarios where virtual‑thread factories are intentionally preferred.

### Only first `subscribeOn` does matter

In order to use another thread pool for Project Reactor, given operators can be used:
- subscribeOn
- publishOn

But it turns out, that only the first `subscribeOn` does matter.

Check [Project Reactor - subscribeOn method documentation](https://projectreactor.io/docs/core/release/reference/coreFeatures/schedulers.html#the-subscribeon-method)

```kotlin
fun anotherSubscribeOnHasNoEffect() {
    val firstScheduler = Schedulers.newParallel("first-Scheduler")
    val secondScheduler = Schedulers.newParallel("second-Scheduler")
  
    Mono.just("value")
        // Thread[#26,first-Scheduler-2,5,main] : first-Value     
        .subscribeOn(firstScheduler)
        .doOnNext { println("${Thread.currentThread()} : first-Value") }

        // Thread[#26,first-Scheduler-2,5,main] : second-Value    
        .subscribeOn(secondScheduler)
        .doOnNext { println("${Thread.currentThread()} : second-Value") }
        .block()
  
    firstScheduler.dispose()
    secondScheduler.dispose()
}
```

If there's a need to switch code execution to another thread, then `publishOn` can be used:

```kotlin
fun subscribeOnThenPublishOn() {
    val firstScheduler = Schedulers.newParallel("first-Scheduler")
    val secondScheduler = Schedulers.newParallel("second-Scheduler")

    Mono.just("value")
        // Thread[#25,first-Scheduler-1,5,main] : first-Value
        .subscribeOn(firstScheduler)
        .doOnNext { println("${Thread.currentThread()} : first-Value") }

        // Thread[#26,second-Scheduler-2,5,main] : second-Value
        .publishOn(secondScheduler)
        .doOnNext { println("${Thread.currentThread()} : second-Value") }
        .block()

    firstScheduler.dispose()
    secondScheduler.dispose()
}
```

Check
[ReactorSubscribeOnPublishOn](src/main/kotlin/com/nalepa/demo/example03/ReactorSubscribeOnPublishOn.kt)
for more

##### Why this matter?

Reactive code, like e.q. `WebClient from Spring` by default have configured `Thread Pool.` So using `subscribeOn` operator may have no effect.

```kotlin
val someScheduler = Schedulers.newBoundedElastic(10, 100, "some-scheduler")

private fun getDummyData(index: String, mockDelaySeconds: Long): Mono<ByteArray> {
    return webClient
        .get()
        .uri("http://some-service/some-endpoint")
        .retrieve()
        .bodyToMono(ByteArray::class.java)
        
        // this may have no effect :D 
        // check which thread will be next to "some-message"
        // maybe publishOn operator should be used
        .subscribeOn(someScheduler) 
        .doOnNext { println("${Thread.currentThread()} : some message") }
}
```
