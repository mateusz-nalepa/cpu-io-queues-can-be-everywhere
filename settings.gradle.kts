rootProject.name = "cpu-io-queues-can-be-everywhere"


include(

    ":apps:commons",
    ":apps:different-endpoints:webmvc-classic-threads",
    ":apps:different-endpoints:webmvc-virtual-threads",
    ":apps:different-endpoints:webflux-netty-server",

    ":apps:endpoint-and-http-client:webflux-netty-server-client-netty",
    ":apps:endpoint-and-http-client:webmvc-virtual-threads-rest-client",

    ":tools:request-sender",
    ":tools:mock-external-service",
)