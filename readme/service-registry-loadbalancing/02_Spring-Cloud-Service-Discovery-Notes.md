# Spring Cloud – Service Discovery, Eureka & Load Balancing

## 1. Project Overview

This project demonstrates a microservices architecture using Spring Cloud.

```text
Microservices Project
│
├── Eureka Server
├── Config Server
├── Accounts Service
├── Loans Service
├── Cards Service
└── RabbitMQ / Spring Cloud Bus
```

Current focus:

- Service Registration
- Service Discovery
- Eureka
- Eureka Client
- Eureka Heartbeats / Leases
- Actuator shutdown
- Introduction to Load Balancing

> **Note:** Spring Cloud Bus is a separate concept. It is primarily used for propagating events/configuration changes between services. It is not responsible for Eureka service registration or load balancing.

---

# 2. Config Server vs Eureka Server

These two components have different responsibilities.

### Config Server

The Config Server provides centralized configuration to the microservices.

```text
GitHub Config Repository
        ↓
   Config Server :8071
        ↓
    Microservices
```

Example configuration repository:

```text
microservices-config
│
├── eurekaserver.yml
├── accounts.yml
├── loans.yml
└── cards.yml
```

Applications retrieve configuration from:

```text
http://localhost:8071/
```

### Eureka Server

Eureka maintains a **service registry**.

```text
Microservices
      ↓
Eureka Server :8070
      ↓
Service Registry
```

The key distinction:

```text
Config Server
= "What configuration should I use?"

Eureka
= "Where are the other services?"
```

---

# 3. Eureka Server

The Eureka Server application is named:

```yaml
spring:
  application:
    name: eurekaserver
```

It is enabled using:

```java
@EnableEurekaServer
```

The Eureka Server runs on:

```text
http://localhost:8070
```

### Eureka Server configuration

```yaml
server:
  port: 8070

eureka:
  instance:
    hostname: localhost
  client:
    fetchRegistry: false
    registerWithEureka: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

For this standalone Eureka Server:

```text
registerWithEureka: false
= Eureka Server does not register itself.

fetchRegistry: false
= Eureka Server does not fetch a registry from another Eureka Server.
```

---

# 4. Eureka Client

A microservice must have the Eureka Client dependency to participate in Eureka service discovery.

For example:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

This dependency was added to:

- Accounts
- Loans
- Cards

### Important

Eureka configuration alone is not sufficient.

The microservice needs:

```text
Eureka Client dependency
        +
Eureka client configuration
        ↓
Service can register with Eureka
```

---

# 5. Eureka Client Configuration

Each microservice has configuration similar to:

```yaml
eureka:
  instance:
    preferIpAddress: true
  client:
    fetchRegistry: true
    registerWithEureka: true
    serviceUrl:
      defaultZone: http://localhost:8070/eureka/
```

### `registerWithEureka`

```yaml
registerWithEureka: true
```

Means:

> "Register me with the Eureka Server."

```text
Accounts → Eureka
Loans    → Eureka
Cards    → Eureka
```

### `fetchRegistry`

```yaml
fetchRegistry: true
```

Means:

> "Allow this service to obtain information about other registered services."

This becomes important for service-to-service communication.

### `defaultZone`

```yaml
defaultZone: http://localhost:8070/eureka/
```

Tells the Eureka client where the Eureka Server is located.

### `preferIpAddress`

```yaml
preferIpAddress: true
```

Tells Eureka to prefer the service's IP address when advertising the instance.

---

# 6. Service Registration

Once the Eureka Client dependency and configuration are added, the microservices automatically register themselves with Eureka.

Current registry:

```text
Eureka Server :8070
       │
       ├── ACCOUNTS → 172.30.240.1:8080 → UP
       ├── LOANS    → 172.30.240.1:8090 → UP
       └── CARDS    → 172.30.240.1:9000 → UP
```

The application names come from:

```yaml
spring:
  application:
    name: accounts
```

```yaml
spring:
  application:
    name: loans
```

```yaml
spring:
  application:
    name: cards
```

Eureka displays them as:

```text
ACCOUNTS
LOANS
CARDS
```

---

# 7. Checking the Eureka Registry

The Eureka dashboard:

```text
http://localhost:8070/
```

The underlying registry:

```text
http://localhost:8070/eureka/apps
```

The `/eureka/apps` endpoint returns the currently registered applications and their instances.

Example:

```text
ACCOUNTS → 172.30.240.1:8080 → UP
LOANS    → 172.30.240.1:8090 → UP
CARDS    → 172.30.240.1:9000 → UP
```

The important information is:

- Application name
- Instance address
- Port
- Status

Internal XML fields such as `countryId`, `dataCenterInfo`, and timestamps are implementation details and are not important for the core architecture.

---

# 8. Service Discovery

**Service registration** and **service discovery** are different concepts.

### Registration

The service tells Eureka:

```text
"I am Accounts and I am running on port 8080."
```

```text
Accounts ──────► Eureka
```

### Discovery

Another service can ask:

```text
"Where is Accounts?"
```

Eureka can provide:

```text
Accounts
   ↓
172.30.240.1:8080
```

Eureka therefore acts as a central service registry, allowing services to discover other services without relying entirely on hard-coded host/port information.

---

# 9. Eureka Heartbeat

After registration, the Eureka client periodically sends a **heartbeat/lease renewal** to Eureka.

Conceptually:

```text
Accounts ── heartbeat ──► Eureka
Accounts ── heartbeat ──► Eureka
Accounts ── heartbeat ──► Eureka
```

The Eureka client library handles this automatically.

No custom heartbeat code needs to be written by the application.

The registry showed:

```text
renewalIntervalInSecs = 30
durationInSecs        = 90
```

The client periodically renews its lease, and Eureka uses the lease to determine whether an instance is still available.

---

# 10. What Happens When a Microservice Shuts Down?

Spring Boot Actuator can be used to shut down a service.

For example:

```yaml
management:
  endpoint:
    shutdown:
      access: unrestricted
```

The shutdown endpoint can be called with:

```text
POST http://localhost:8080/actuator/shutdown
```

The response is:

```json
{
  "message": "Shutting down, bye..."
}
```

This response is generated by the **Spring Boot Actuator shutdown endpoint**, not by a custom Accounts controller.

Shutdown flow:

```text
POST /actuator/shutdown
          ↓
Spring Boot Actuator
          ↓
Accounts application shuts down
          ↓
No more Eureka heartbeats
          ↓
Eureka lease eventually expires
          ↓
Instance can be removed/evicted
```

### Important

Eureka does not necessarily remove the service instantly when the application shuts down. It uses the heartbeat/lease mechanism to determine when the instance is no longer available.

> **Security note:** An unrestricted shutdown endpoint is suitable for local learning/testing, but should not be exposed without proper protection in production.

---

# 11. What Happens When Eureka Server Shuts Down?

If the Eureka Server itself is stopped:

```text
Eureka Server :8070 ❌
```

the registered microservices do **not necessarily stop**.

Their Eureka clients continue attempting to communicate with Eureka:

```text
Accounts ── heartbeat ──X Eureka
Loans    ── heartbeat ──X Eureka
Cards    ── heartbeat ──X Eureka
```

Because Eureka is unavailable, the applications can produce connection/heartbeat exceptions.

The microservices themselves can continue running:

```text
Eureka       ❌
Accounts     ✅ :8080
Loans        ✅ :8090
Cards        ✅ :9000
```

However, **service discovery is affected** because the Eureka Server is unavailable.

---

# 12. Current Architecture

```text
                    ┌─────────────────────┐
                    │    Config Server    │
                    │       :8071         │
                    └──────────┬──────────┘
                               │
                         Configuration
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
        ┌──────────┐     ┌──────────┐     ┌──────────┐
        │ Accounts │     │  Loans   │     │  Cards   │
        │  :8080   │     │  :8090   │     │  :9000   │
        └─────┬────┘     └─────┬────┘     └─────┬────┘
              │                │                │
              │       Registration              │
              └────────────────┼────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │   Eureka Server     │
                    │       :8070         │
                    │                     │
                    │  Service Registry   │
                    └─────────────────────┘
```

---

# 13. Key Takeaways

```text
Config Server
→ Centralized application configuration

Eureka Server
→ Service registry

Eureka Client
→ Allows microservices to register/discover services

registerWithEureka
→ Register myself with Eureka

fetchRegistry
→ Get information about other services

Heartbeat
→ Periodically tell Eureka that the service is still alive

Lease
→ Eureka's mechanism for determining whether an instance is still available

Actuator shutdown
→ Allows a running Spring Boot application to be shut down through an endpoint
```

## Current Progress

```text
✅ Config Server
✅ Eureka Server
✅ Eureka Client dependency
✅ Accounts registration
✅ Loans registration
✅ Cards registration
✅ Service registry verification
✅ Eureka heartbeat concept
✅ Actuator shutdown experiment
✅ Eureka shutdown/heartbeat behavior

➡️ Next:
   Service-to-service communication
   +
   Spring Cloud Load Balancer
```
