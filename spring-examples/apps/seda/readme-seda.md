# Staged Event-Driven Architecture (SEDA) here

## Examples with app using http client

In those examples, Staged Event-Driven Architecture (SEDA) is being used.

When threads are busy/blocked, then it may happen that from application perspective:

- app is doing CPU work, eq: JSON deserialization for request X
- and then send http-client request for request X+1

We would probably like to change that to:

- app is doing CPU work, eq: JSON deserialization for request X
- at the same time, app is waiting for response from http-client for request X+1

There is only one `dummy endpoint` in apps:

- `httpClient.getData()`
- and then `heavyCpuCode(cpuOperationDelaySeconds)`

### 1/2 - Spring Boot app with Tomcat on Virtual Threads

<details>
  <summary>Click to learn How To Run</summary>

1. Run `docker-compose up`
2. Run `RequestSenderApp`
3. Run `VirtualAppWithHttpClient`
4. Run `MockExternalServiceApp`
5. Run one of those:

```shell
curl http://localhost:8080/send-requests-app-with-client/scenario/defaults
#or 
curl http://localhost:8080/send-requests-app-with-client/scenario/dedicatedCpuPool
```

6. Open Grafana and look for metrics http://localhost:3000

</details>

- Scenario:
    - send first batch of requests `Runtime.getRuntime().availableProcessors()` on `dummy endpoint`
        - mock app will return response after 0s
    - wait 2 seconds
    - send second batch of requests `Runtime.getRuntime().availableProcessors()` on `dummy endpoint`
        - mock app will return response after 9s

#### HighLevel Overview

![tomcat_http_client_virtual_threads.png](images/tomcat_http_client_virtual_threads.png)

#### Results, when endpoint is executed only on Virtual Thread Pool

In this example, metric `http.server.requests` is not telling the truth

|                  Element                  | Expected response times |      Metrics from App       | Metrics from RequestSender |
|:-----------------------------------------:|:-----------------------:|:---------------------------:|:--------------------------:|
|      `first batch` on dummy endpoint      |           10s           | 19s (no info about batch) ❌ |           10s ✅            |
|     `second batch` on dummy endpoint      |           19s           | 19s (no info about batch) ❌ |           27s ✅            |
|            Http Client metric             |      from 0s to 9s      |        from 0s to 9s        |       Not Applicable       |
| Server Saturation time<br>(custom metric) |           8s            |            8s ✅             |       Not Applicable       |

#### Results, when `heavyCpuCode` is being executed on dedicated thread pool

|                  Element                  | Expected response times |      Metrics from App       | Metrics from RequestSender |
|:-----------------------------------------:|:-----------------------:|:---------------------------:|:--------------------------:|
|      `first batch` on dummy endpoint      |           10s           | 19s (no info about batch) ❌ |           10s ✅            |
|     `second batch` on dummy endpoint      |           19s           | 19s (no info about batch) ❌ |           19s ✅            |
|            Http Client metric             |      from 0s to 9s      |        from 0s to 9s        |       Not Applicable       |
| Server Saturation time<br>(custom metric) |           0s            |            0s ✅             |       Not Applicable       |

### 2/2 - Spring Boot WebFlux with Netty

<details>
  <summary>Click to learn How To Run</summary>

1. Run `docker-compose up`
2. Run `RequestSenderApp`
3. Run `NettyServerAppWithHttpClient`
4. Run `MockExternalServiceApp`
5. Run one of those:

```shell
curl http://localhost:8080/send-requests-app-with-client/scenario/defaults
#or 
curl http://localhost:8080/send-requests-app-with-client/scenario/dedicatedCpuPool
```

6. Open Grafana and look for metrics http://localhost:3000

</details>

- Scenario:
    - send first batch of requests `Runtime.getRuntime().availableProcessors()` on `dummy endpoint`
        - mock app will return response after 0s
    - wait 2 seconds
    - send second batch of requests `Runtime.getRuntime().availableProcessors()` on `dummy endpoint`
        - mock app will return response after 9s

#### HighLevel Overview

![netty_http_client.png](images/netty_http_client.png)

#### Results, when endpoint is executed only on Netty thread pools

In this example, metric `http.client.requests` is not telling the truth

|                  Element                   | Expected response times |      Metrics from App       | Metrics from RequestSender |
|:------------------------------------------:|:-----------------------:|:---------------------------:|:--------------------------:|
|      `first batch` on dummy endpoint       |           10s           | 27s (no info about batch) ✅ |           10s ✅            |
|      `second batch` on dummy endpoint      |           19s           | 27s (no info about batch) ✅ |           27s ✅            |
|             Http Client metric             |      from 0s to 9s      |      from 0s to 18s ❌       |       Not Applicable       |
| Server Saturation time<br>(missing metric) |         Missing         |           Missing           |       Not Applicable       |
| Client Saturation time<br>(custom metric)  |           8s            |            8s ✅             |       Not Applicable       |

#### Results, when `heavyCpuCode` is being executed on dedicated thread pool

|                  Element                   | Expected response times |      Metrics from App       | Metrics from RequestSender |
|:------------------------------------------:|:-----------------------:|:---------------------------:|:--------------------------:|
|      `first batch` on dummy endpoint       |           10s           | 19s (no info about batch) ✅ |           10s ✅            |
|      `second batch` on dummy endpoint      |           19s           | 19s (no info about batch) ✅ |           19s ✅            |
|             Http Client metric             |      from 0s to 9s      |      from 0s to 9s  ✅       |       Not Applicable       |
| Server Saturation time<br>(missing metric) |         Missing         |           Missing           |       Not Applicable       |
| Client Saturation time<br>(custom metric)  |           0s            |            0s ✅             |       Not Applicable       |
