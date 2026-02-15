rootProject.name = "cpu-io-queues-can-be-everywhere"


include(

    ":01-presentation-blog-examples",

    ":02-thread-pool-un-expected-things",

    ":03-spring-examples:apps:utils",
    ":03-spring-examples:apps:bulkhead:01-webmvc-classic-threads",
    ":03-spring-examples:apps:bulkhead:02-webmvc-virtual-threads",
    ":03-spring-examples:apps:bulkhead:03-webmvc-coroutines-classic-threads",
    ":03-spring-examples:apps:bulkhead:04-webmvc-coroutines-virtual-threads",
    ":03-spring-examples:apps:bulkhead:05-webflux-netty-server",

    ":03-spring-examples:apps:seda:01-webmvc-classic-threads-rest-client",
    ":03-spring-examples:apps:seda:02-webmvc-virtual-threads-rest-client",
    ":03-spring-examples:apps:seda:03-webmvc-coroutines-classic-threads-rest-client",
    ":03-spring-examples:apps:seda:04-webmvc-coroutines-virtual-threads-rest-client",
    ":03-spring-examples:apps:seda:05-webflux-netty-server-client-netty",

    ":03-spring-examples:helpers:request-sender",
    ":03-spring-examples:helpers:mock-external-service",

)