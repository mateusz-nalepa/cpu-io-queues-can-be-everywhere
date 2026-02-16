## Spring Boot app with classic Tomcat with 200 threads with Kotlin Coroutines

- Scenario:
    - send 200 requests on `slow endpoint`
        - every thread will just sleep for 10 seconds
    - wait 2 seconds
    - send 200 requests on `fast endpoint`

#### HighLevel Overview

![tomcat_different_endpoints_200_classic_threads.png](../../../../images/tomcat_different_endpoints_200_classic_threads.png)

<details>
  <summary>Click to learn How To Run</summary>

1. Run `docker-compose up`
2. Run `RequestSenderApp`
3. Run `Bulkhead_03_WebMVC_Coroutines_PlatformThreads`
4. Run one of those:

```shell
curl 'http://localhost:8080/send-requests-on-different-endpoints/scenario/defaults?batchSize=200'
#or 
curl 'http://localhost:8080/send-requests-on-different-endpoints/scenario/dedicatedCpuPool?batchSize=200'
```

5. Open Grafana and look for metrics http://localhost:3000

</details>

#### Results, when endpoints are being executed on Tomcat Thread Pool

In this example, metric `http.server.requests` is not telling the truth

|                Element                 | Expected response times | Metrics from App | Metrics from RequestSender |
|:--------------------------------------:|:-----------------------:|:----------------:|:--------------------------:|
|             Slow endpoint              |           10s           |      10s ✅       |           10s ✅            |
|             Fast endpoint              |        almost 0s        |   almost 0s ❌    |            8s ✅            |
| App queue wait time<br>(custom metric) |           8s            |       8s ✅       |       Not Applicable       |

#### Results, when every endpoint has dedicated thread pool - Tomcat thread pool only accept here requests

|                Element                 | Expected response times | Metrics from App | Metrics from RequestSender |
|:--------------------------------------:|:-----------------------:|:----------------:|:--------------------------:|
|             Slow endpoint              |           10s           |      10s ✅       |           10s ✅            |
|             Fast endpoint              |        almost 0s        |   almost 0s ✅    |        almost 0s ✅         |
| App queue wait time<br>(custom metric) |           0s            |       0s ✅       |       Not Applicable       |
