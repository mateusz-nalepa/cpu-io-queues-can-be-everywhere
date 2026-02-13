## Spring Boot WebFlux with Netty

- Scenario:
    - send first batch of requests `Runtime.getRuntime().availableProcessors()` on `dummy endpoint`
        - mock app will return response after 0s
        - and then app under test will do heavyCpuCode for 10 seconds
        - wait 2 seconds
    - send second batch of requests `Runtime.getRuntime().availableProcessors()` on `dummy endpoint`
        - mock app will return response after 9s
        - and then app under test will do heavyCpuCode for 10 seconds

#### HighLevel Overview

![netty_http_client.png](images/netty_http_client.png)


<details>
  <summary>Click to learn How To Run</summary>

1. Run `docker-compose up`
2. Run `RequestSenderApp`
3. Run `SEDA_05_WebFLUX_PlatformThreads`
4. Run `MockExternalServiceApp`
5. Run one of those:

```shell
curl http://localhost:8080/send-requests-app-with-client/scenario/defaults
#or 
curl http://localhost:8080/send-requests-app-with-client/scenario/dedicatedCpuPool
```

6. Open Grafana and look for metrics http://localhost:3000

</details>

#### Results, when endpoint is executed only on Netty thread pools

In this example, metric `http.client.requests` is not telling the truth

|                  Element                  | Expected response times |      Metrics from App       | Metrics from RequestSender |
|:-----------------------------------------:|:-----------------------:|:---------------------------:|:--------------------------:|
|      `first batch` on dummy endpoint      |           10s           | 27s (no info about batch) ✅ |           10s ✅            |
|     `second batch` on dummy endpoint      |           19s           | 27s (no info about batch) ✅ |           27s ✅            |
|            Http Client metric             |      from 0s to 9s      |      from 0s to 18s ❌       |       Not Applicable       |
| Server Queue wait time<br>(custom metric) |           0s            |             0s              |       Not Applicable       |
| Client Queue wait time<br>(custom metric) |           8s            |            8s ✅             |       Not Applicable       |

#### Results, when `heavyCpuCode` is being executed on dedicated thread pool

|                  Element                  | Expected response times |      Metrics from App       | Metrics from RequestSender |
|:-----------------------------------------:|:-----------------------:|:---------------------------:|:--------------------------:|
|      `first batch` on dummy endpoint      |           10s           | 19s (no info about batch) ✅ |           10s ✅            |
|     `second batch` on dummy endpoint      |           19s           | 19s (no info about batch) ✅ |           19s ✅            |
|            Http Client metric             |      from 0s to 9s      |      from 0s to 9s  ✅       |       Not Applicable       |
| Server Queue wait time<br>(custom metric) |           0s            |             0s              |       Not Applicable       |
| Client Queue wait time<br>(custom metric) |           0s            |            0s ✅             |       Not Applicable       |
