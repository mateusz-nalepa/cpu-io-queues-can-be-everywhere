# Some (un)expected things when dealing with Thread Pools

At the end of every section, there is info `why this matter`

## Table of Contents

- [Adding more threads for CPU‑bound tasks may make things worse](#adding-more-threads-for-cpubound-tasks-may-make-things-worse)
- [Blocking factor](#blocking-factor)
- [Threads created with Thread.ofVirtual().factory() are scheduled on ForkJoinPool](#threads-created-with-threadofvirtualfactory-are-scheduled-on-forkjoinpool)
- [Only first subscribeOn does matter](#only-first-subscribeon-does-matter)
- [Behavior of corePoolSize and maxPoolSize is unintuitive](#behavior-of-corepoolsize-and-maxpoolsize-is-unintuitive)


### Adding more threads for CPU‑bound tasks may make things worse

When all threads are busy doing CPU‑bound work, then adding more threads may make things worse.
Of course CPU can execute many tasks in parallel, but they will be slower.

From the other hand, adding more threads for `blocking tasks` may be a good idea,
when queue wait time is high. It happens because, well, CPU usage for them is near 0%.

There is a code, which is intentionally doing CPU‑bound work, and given number of threads is used to execute it.
When more threads are added, then the total number of iterations per thread is slower

`Note`: This code was executed on notebook with 8 cores.
```java
Number of iterations per thread number: 8: 	 3 018 674
Number of iterations per thread number: 16: 	 1 603 543
Number of iterations per thread number: 32: 	 855 639
Number of iterations per thread number: 128: 	 240 104
```

Check
[ThreadsPerformance](src/main/java/com/nalepa/demo/example01/ThreadsPerformance.java)
for more

##### Why this matter?

Processing thousand of elements at the same time may cause:
- all customers will be waiting for the response which can cause `OutOfMemoryError`
- none of them will be finished in case of e.g. `OutOfMemoryError`

### Blocking factor

The blocking factor describes the proportion of time 
a task spends blocked compared to its total execution time.

Blocking Factor = Blocking Time / (Blocking Time + CPU Time)

Examples:

- Task: 3s I/O, 3s CPU
  - Blocking Factor = 3/(3+3)=0.5
- Task: 9s I/O, 3s CPU
  - Blocking Factor = 9/(9+3)=0.75

But it may be very hard to find out blocking factor for given task, because it can be different for each execution, and it can be different for each task.

What if blocking factor is always near 0 or 1?

This leads to the following guidelines:

- I/O pool -> Blocking Factor ≈ 1
  - getting data from database, calling external service, etc.
  - tasks spend most of their time waiting, so a larger number of threads is acceptable, and virtual threads are often suitable.
  - tasks queue wait time should be here 0, as app should wait request as soon as possible and wait for the response

- CPU pool -> Blocking Factor ≈ 0
  - e.g. parsing JSON 
  - tasks primarily use CPU, so the number of threads should be close to the number of CPU cores
  - tasks queue wait time can be here greater than 0, as maybe there is microburst which will disappear for a moment, or maybe creating a new instances is in progress

But hey, not every app needs to be hyper‑optimized. 
For example, Tomcat default 200 request threads probably works surprisingly well 
`as long as the workload is mostly I/O‑bound`. 

Most servlet‑based applications spend the majority of time waiting for: 
- database responses
- external services
- network I/O

With a blocking factor close to 1, having many threads is perfectly reasonable and easy to maintain. 

However, if request processing becomes CPU‑heavy, those 200 threads can suddenly turn into 200 competing CPU tasks, causing contention, GC pressure, and a drop in throughput.

##### Why this matter?

Incorrect thread‑pool sizing may result in:

- excessive CPU contention and reduced throughput
- long queueing delays
- unnecessarily large thread pools for CPU‑bound workloads
- performance issues that are difficult to diagnose

### Threads created with Thread.ofVirtual().factory() are scheduled on ForkJoinPool

When creating multiple executors with virtual thread factories,
full isolation may be expected. However, it does not work like that.

> NOTE: That's for Java 24, maybe in the future, there will be some changes in that area.

```java
static void virtualThreadFactoryThreadsAreScheduledOnForkJoinPool() throws Exception {
    var executor1 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("one").factory());
    var executor2 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("two").factory());
    var executor3 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("three").factory());

    // CurrentThread: VirtualThread[#37,one]/runnable@ForkJoinPool-1-worker-1
    Future<?> future1 = executor1.submit(() -> System.out.println("CurrentThread: " + Thread.currentThread()));

    // CurrentThread: VirtualThread[#39,two]/runnable@ForkJoinPool-1-worker-2
    Future<?> future2 = executor2.submit(() -> System.out.println("CurrentThread: " + Thread.currentThread()));
  
    // CurrentThread: VirtualThread[#41,three]/runnable@ForkJoinPool-1-worker-3
    Future<?> future3 = executor3.submit(() -> System.out.println("CurrentThread: " + Thread.currentThread()));
    
    future1.get();
    future2.get();
    future3.get();
}
```

Check
[ThreadOfVirtualFactory](src/main/java/com/nalepa/demo/example02/ThreadOfVirtualFactory.java)
for more

##### Why this matter?

Multiple executors created with virtual‑thread factories still share the same underlying thread pool, which can lead to incorrect assumptions about isolation. Using:
```java
var executor = Executors.newVirtualThreadPerTaskExecutor();
```
keeps the model simple and avoids unnecessary complexity.

Of course, there are scenarios where virtual‑thread factories are intentionally preferred.

### Only first `subscribeOn` does matter

In order to use another thread pool for [Project Reactor](https://projectreactor.io/docs/core/release/reference/coreFeatures/schedulers.html#the-subscribeon-method), given operators can be used:
- subscribeOn
- publishOn

But it turns out, that only the first `subscribeOn` does matter.

Check [Project Reactor - subscribeOn method documentation](https://projectreactor.io/docs/core/release/reference/coreFeatures/schedulers.html#the-subscribeon-method)

```java
static void anotherSubscribeOnHasNoEffect() {
    Scheduler firstScheduler = Schedulers.newParallel("first-Scheduler");
    Scheduler secondScheduler = Schedulers.newParallel("second-Scheduler");

    Mono.just("value")
        .subscribeOn(firstScheduler)
        // Thread[#26,first-Scheduler-2,5,main] : first-Value   
        .doOnNext(v -> System.out.println(Thread.currentThread() + " : first-Value"))
            
        .subscribeOn(secondScheduler)
        // Thread[#26,first-Scheduler-2,5,main] : second-Value  
        .doOnNext(v -> System.out.println(Thread.currentThread() + " : second-Value"))
        .block();

    firstScheduler.dispose();
    secondScheduler.dispose();
}
```

If there's a need to switch code execution to another thread, then `publishOn` can be used:

```java
static void subscribeOnThenPublishOn() {
    Scheduler firstScheduler = Schedulers.newParallel("first-Scheduler");
    Scheduler secondScheduler = Schedulers.newParallel("second-Scheduler");

    Mono.just("value")
        .subscribeOn(firstScheduler)
        // Thread[#25,first-Scheduler-1,5,main] : first-Value
        .doOnNext(v -> System.out.println(Thread.currentThread() + " : first-Value"))
        
        .publishOn(secondScheduler)
        // Thread[#26,second-Scheduler-2,5,main] : second-Value
        .doOnNext(v -> System.out.println(Thread.currentThread() + " : second-Value"))
        .block();

    firstScheduler.dispose();
    secondScheduler.dispose();
}
```

Check
[ReactorSubscribeOnPublishOn.java](src/main/java/com/nalepa/demo/example03/ReactorSubscribeOnPublishOn.java)
for more

##### Why this matter?

Reactive code, like e.g. `WebClient from Spring` by default have configured `Thread Pool.` So using `subscribeOn` operator may have no effect.

```java
private final Scheduler someScheduler =
        Schedulers.newParallel("some-scheduler");

private Mono<byte[]> getDummyData() {
    return webClient
            .get()
            .uri("http://some-service/some-endpoint")
            .retrieve()
            .bodyToMono(byte[].class)
            
            // this may have no effect :D
            // check which thread will be next to "some-message"
            // maybe publishOn operator should be used
            .subscribeOn(someScheduler)
            .doOnNext(bytes -> System.out.println(Thread.currentThread() + " : some message"));
}
```

### Behavior of `corePoolSize` and `maxPoolSize` is unintuitive

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

This behavior minimizes thread creation but is unintuitive.

Solutions?
- the easiest one, set `corePoolSize` and `maxPoolSize` to the same number
- create wrapper for `LinkedBlockingQueue` (or any other queue) and modify method `offer`
    - Tomcat thread pool works like that

Check
[ThreadPoolCoreSizeMaxSize](src/main/java/com/nalepa/demo/example04/ThreadPoolCoreSizeMaxSize.java)
for more

##### Why this matter?

There can be a performance problem and knowing about default Thread Pool Behavior can save a lot of hours
