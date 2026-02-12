rootProject.name = "cpu-io-queues-can-be-everywhere"


include(

    ":presentation-blog-examples",

    ":spring-examples:apps:utils",
    ":spring-examples:apps:bulkhead:01-webmvc-classic-threads",
    ":spring-examples:apps:bulkhead:02-webmvc-virtual-threads",
    ":spring-examples:apps:bulkhead:03-webmvc-coroutines-classic-threads",
    ":spring-examples:apps:bulkhead:04-webmvc-coroutines-virtual-threads",
    ":spring-examples:apps:bulkhead:05-webflux-netty-server",

    ":spring-examples:apps:seda:01-webmvc-classic-threads-rest-client",
    ":spring-examples:apps:seda:02-webmvc-virtual-threads-rest-client",
    ":spring-examples:apps:seda:03-webmvc-coroutines-classic-threads-rest-client",
    ":spring-examples:apps:seda:04-webmvc-coroutines-virtual-threads-rest-client",
    ":spring-examples:apps:seda:05-webflux-netty-server-client-netty",

    ":spring-examples:helpers:request-sender",
    ":spring-examples:helpers:mock-external-service",



    ":thread-pool-un-expected-things",
)