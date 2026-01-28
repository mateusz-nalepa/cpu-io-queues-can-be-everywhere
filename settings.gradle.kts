rootProject.name = "cpu-io-queues-can-be-everywhere"


include(

    ":presentation-blog-examples",

    ":apps:commons",
    ":apps:different-endpoints:01-webmvc-classic-threads",
    ":apps:different-endpoints:02-webmvc-virtual-threads",
    ":apps:different-endpoints:03-webmvc-coroutines-classic-threads",
    ":apps:different-endpoints:04-webmvc-coroutines-virtual-threads",
    ":apps:different-endpoints:05-webflux-netty-server",

    ":apps:endpoint-and-http-client:01-webmvc-classic-threads-rest-client",
    ":apps:endpoint-and-http-client:02-webmvc-virtual-threads-rest-client",
    ":apps:endpoint-and-http-client:03-webmvc-coroutines-classic-threads-rest-client",
    ":apps:endpoint-and-http-client:04-webmvc-coroutines-virtual-threads-rest-client",
    ":apps:endpoint-and-http-client:05-webflux-netty-server-client-netty",

    ":tools:request-sender",
    ":tools:mock-external-service",
)