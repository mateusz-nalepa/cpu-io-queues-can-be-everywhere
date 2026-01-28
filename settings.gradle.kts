rootProject.name = "cpu-io-queues-can-be-everywhere"


include(

    ":presentation-examples",
    ":apps:commons",
    ":apps:different-endpoints:webmvc-classic-threads",
    ":apps:different-endpoints:webmvc-virtual-threads",
    ":apps:different-endpoints:webflux-netty-server",
    ":apps:different-endpoints:webmvc-coroutines-classic-threads",
    ":apps:different-endpoints:webmvc-coroutines-virtual-threads",

    ":apps:endpoint-and-http-client:webmvc-classic-threads-rest-client",
    ":apps:endpoint-and-http-client:webmvc-virtual-threads-rest-client",
    ":apps:endpoint-and-http-client:webflux-netty-server-client-netty",
    ":apps:endpoint-and-http-client:webmvc-coroutines-classic-threads-rest-client",
    ":apps:endpoint-and-http-client:webmvc-coroutines-virtual-threads-rest-client",


    ":tools:request-sender",
    ":tools:mock-external-service",
)