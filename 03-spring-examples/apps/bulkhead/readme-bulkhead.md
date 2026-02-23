# Bulkhead

In those endpoints, `bulkhead` pattern is used.

It has many thread pools for many endpoints.

It protects one resources from another, e.g. `noisy neighbour`

> In graphical examples there is a simplification,
> that there is only one thread in a thread pool.

![bulkhead.png](../../../images/bulkhead.png)

When `Thread Pool A` is busy, then `Thread Pool B` is still able to process requests and vice versa.

## Examples with `fast` and `slow` endpoints

For `fast endpoints` it's always:

```kotlin
ResponseEntity.ok((SomeResponse("fast")))
```

For `slow endpoints` it depends on types of threads:

```kotlin
Operations.someBlockingIO(blockingIODelaySeconds)
// or
Operations.heavyCpuCode(cpuOperationDelaySeconds)
// and then
ResponseEntity.ok((SomeResponse("slow")))
```

## Architecture of the applications

![img.png](img.png)

## Application examples
- every one of them includes `how to run` and detailed threads diagram
- [01-webmvc-classic-threads](01-webmvc-classic-threads/01-webmvc-classic-threads.md)
- [02-webmvc-virtual-threads](02-webmvc-virtual-threads/02-webmvc-virtual-threads.md)
- [03-webmvc-coroutines-classic-threads](03-webmvc-coroutines-classic-threads/03-webmvc-coroutines-classic-threads.md)
- [04-webmvc-coroutines-virtual-threads](04-webmvc-coroutines-virtual-threads/04-webmvc-coroutines-virtual-threads.md)
- [05-webflux-netty-server](05-webflux-netty-server/05-webflux-netty-server.md)

