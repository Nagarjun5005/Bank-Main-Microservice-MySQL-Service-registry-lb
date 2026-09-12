# Spring Cloud - Service Discovery, Registration & Load Balancing 
https://spring.io/projects/spring-cloud

This project is created to understand how **Spring Cloud** helps implement:

- Service Registration
- Service Discovery
- Client-side Load Balancing
- Service-to-Service Communication
- Fault Tolerance and Resilience

> **Note:** The topic in this slide is **Spring Cloud Service Discovery**, not **Spring Cloud Bus**. Spring Cloud Bus is a separate component mainly used for propagating events/configuration changes across services.

---

## 1. What Problem Are We Solving?

In a microservices architecture, we may have multiple services:

```text
User Service
Order Service
Payment Service
Notification Service
```

Each service can run on a different:

- IP address
- Host
- Port

For example:

```text
User Service     → 192.168.1.10:8081
Order Service    → 192.168.1.11:8082
Payment Service  → 192.168.1.12:8083
```

If `Order Service` needs to communicate with `Payment Service`, we could hard-code:

```text
http://192.168.1.12:8083
```

However, the IP address or port may change.

There may also be multiple instances:

```text
Payment Service
    ├── Instance 1 → 192.168.1.12:8083
    ├── Instance 2 → 192.168.1.13:8083
    └── Instance 3 → 192.168.1.14:8083
```

We therefore need a mechanism that allows services to **find each other dynamically**.

This is where **Service Discovery** comes in.

---

# 2. Service Discovery

## What is Service Discovery?

Service Discovery is the process through which one microservice finds the location of another microservice without having to know its IP address and port beforehand.

Instead of:

```text
Order Service
      |
      ↓
http://192.168.1.12:8083
```

we use:

```text
Order Service
      |
      ↓
Service Registry
      |
      ↓
Payment Service
```

The Service Registry maintains information about available service instances.

---

# 3. Service Registration

Before a service can be discovered, it needs to **register itself** with the Service Registry.

For example:

```text
Payment Service starts
        |
        ↓
Registers with Eureka
        |
        ↓
PAYMENT-SERVICE
IP: 192.168.1.12
PORT: 8083
```

If multiple instances are running:

```text
PAYMENT-SERVICE
    |
    ├── 192.168.1.12:8083
    ├── 192.168.1.13:8083
    └── 192.168.1.14:8083
```

The registry keeps track of these instances.

---

# 4. Eureka

This project uses **Netflix Eureka** as the Service Discovery mechanism.

Eureka acts as a **Service Registry**.

```text
                  Eureka Server
                 Service Registry
                       |
          +------------+------------+
          |            |            |
          ↓            ↓            ↓
     User Service  Order Service  Payment Service
```

Each service registers itself with Eureka.

Other services can then ask Eureka:

```text
"Where is PAYMENT-SERVICE?"
```

Eureka returns the available instances.

---

# 5. Eureka Server

The **Eureka Server** maintains the registry of services.

Example:

```text
                  Eureka Server
                -----------------
                Service Registry
                -----------------
                       |
        +--------------+--------------+
        |              |              |
        ↓              ↓              ↓
   USER-SERVICE   ORDER-SERVICE   PAYMENT-SERVICE
```

The Eureka Server knows:

```text
USER-SERVICE
    → 192.168.1.10:8081

ORDER-SERVICE
    → 192.168.1.11:8082

PAYMENT-SERVICE
    → 192.168.1.12:8083
```

---

# 6. Eureka Client

A microservice that registers itself with Eureka acts as an **Eureka Client**.

```text
Payment Service
      |
      | Registration
      ↓
Eureka Server
```

The service provides information such as:

```text
Service Name
IP Address
Port
Instance Information
```

The service can also use Eureka to discover other services.

---

# 7. Service Discovery Flow

Consider:

```text
Order Service
Payment Service
Eureka Server
```

The flow is:

```text
1. Payment Service starts
          |
          ↓
2. Payment Service registers with Eureka
          |
          ↓
3. Order Service needs Payment Service
          |
          ↓
4. Order Service asks Eureka
          |
          ↓
5. Eureka returns available Payment instances
          |
          ↓
6. Order Service selects an instance
          |
          ↓
7. Request is sent to Payment Service
```

---

# 8. Spring Cloud Load Balancer

Service Discovery tells us **where the service instances are**.

But what happens if there are multiple instances?

```text
PAYMENT-SERVICE

Instance 1 → 10.0.0.1:8081
Instance 2 → 10.0.0.2:8081
Instance 3 → 10.0.0.3:8081
```

Which instance should receive the request?

This is where **Spring Cloud Load Balancer** comes in.

```text
Order Service
      |
      ↓
Spring Cloud Load Balancer
      |
      +------→ Payment Instance 1
      |
      +------→ Payment Instance 2
      |
      +------→ Payment Instance 3
```

The Load Balancer selects an available service instance.

---

# 9. Service Discovery vs Load Balancing

These are two different responsibilities.

### Service Discovery

Answers:

> Where are the available instances of Payment Service?

```text
PAYMENT-SERVICE

10.0.0.1:8081
10.0.0.2:8081
10.0.0.3:8081
```

### Load Balancing

Answers:

> Which Payment Service instance should I call?

```text
Request
   |
   ↓
Load Balancer
   |
   ↓
10.0.0.2:8081
```

Therefore:

```text
Service Discovery
        ↓
Find available instances

Load Balancer
        ↓
Choose an instance
```

---

# 10. Netflix Feign Client

Another component discussed is the **Netflix Feign Client**.

Feign makes communication between microservices easier.

Without Feign, we might use:

```text
RestTemplate
WebClient
```

With Feign, we can define an interface:

```java
@FeignClient(name = "payment-service")
public interface PaymentClient {

    @GetMapping("/payments/{id}")
    Payment getPayment(@PathVariable Long id);
}
```

The application can then call:

```java
paymentClient.getPayment(100);
```

Instead of manually writing:

```text
http://192.168.1.12:8083/payments/100
```

The service name can be used instead.

---

# 11. Feign + Service Discovery

Feign can work together with Service Discovery.

The overall flow becomes:

```text
Order Service
      |
      ↓
Feign Client
      |
      ↓
Service Discovery
      |
      ↓
Find PAYMENT-SERVICE instances
      |
      ↓
Spring Cloud Load Balancer
      |
      ↓
Select an instance
      |
      ↓
Payment Service
```

This allows services to communicate without hard-coding IP addresses.

---

# 12. Complete Architecture

```text
                         Eureka Server
                       Service Registry
                              |
             +----------------+----------------+
             |                |                |
             ↓                ↓                ↓
       User Service      Order Service    Payment Service
                              |
                              |
                         Feign Client
                              |
                              ↓
                    Service Discovery
                              |
                              ↓
                 Spring Cloud Load Balancer
                              |
                    +---------+---------+
                    |         |         |
                    ↓         ↓         ↓
               Payment 1  Payment 2  Payment 3
```

---

# 13. Other Service Registries

Although this project uses Eureka, Eureka is not the only option.

## Consul

Consul is provided by HashiCorp.

It can be used for:

- Service Discovery
- Service Registration
- Health Checks
- Configuration

## etcd

`etcd` is a distributed key-value store.

It is commonly used for:

- Configuration
- Service Discovery
- Distributed coordination

## Apache Zookeeper

Apache Zookeeper is a distributed coordination system.

It has also been used for:

- Service Discovery
- Configuration
- Distributed coordination

The underlying concept remains the same:

> Maintain information about available service instances so that other services can discover them dynamically.

---

# 14. Ribbon vs Spring Cloud Load Balancer

Older Spring Cloud applications commonly used:

```text
Netflix Ribbon
```

Ribbon provided **client-side load balancing**.

The older architecture could look like:

```text
Feign
  |
  ↓
Ribbon
  |
  ↓
Eureka
  |
  ↓
Service Instance
```

Ribbon entered **maintenance mode** and is no longer the preferred solution for new Spring Cloud applications.

The modern approach is:

```text
Feign
  |
  ↓
Spring Cloud Load Balancer
  |
  ↓
Service Discovery
  |
  ↓
Service Instance
```

For this project, focus on:

```text
Spring Cloud Load Balancer
```

rather than Ribbon.

---

# 15. Advantages of Service Discovery

## 15.1 Dynamic Service Locations

Services don't need to hard-code IP addresses.

Instead of:

```text
http://192.168.1.12:8083
```

we can use:

```text
PAYMENT-SERVICE
```

The registry determines where the service is running.

---

## 15.2 Multiple Service Instances

We can run multiple instances:

```text
PAYMENT-SERVICE

Instance 1
Instance 2
Instance 3
```

This allows the application to scale horizontally.

---

## 15.3 Load Balancing

Requests can be distributed across service instances.

Example:

```text
Request 1 → Instance 1
Request 2 → Instance 2
Request 3 → Instance 3
Request 4 → Instance 1
```

The exact selection strategy depends on the load-balancer configuration.

---

## 15.4 Dynamically Managed IP Addresses

Suppose Payment Service initially runs at:

```text
10.0.0.10:8080
```

Later it moves to:

```text
10.0.0.25:8080
```

The consuming service doesn't need to be changed to the new IP address.

The Service Registry maintains the current service information.

---

## 15.5 Fault Tolerance

Suppose we have:

```text
PAYMENT-SERVICE

Instance 1 → DOWN
Instance 2 → UP
Instance 3 → UP
```

The system can avoid routing requests to unavailable instances when the discovery/health information reflects their status.

This improves system availability.

---

## 15.6 Resilience

Service Discovery combined with multiple instances and load balancing can make the overall system more resilient.

```text
Payment Instance 1 ❌
        |
        ↓
Load Balancer
        |
        ↓
Payment Instance 2 ✅
```

Instead of the entire application failing because one instance is unavailable, another instance can potentially serve the request.

---

# 16. Important Terminology

| Term | Meaning |
|---|---|
| **Service Registry** | Maintains information about available service instances |
| **Service Registration** | Process of a service registering itself |
| **Service Discovery** | Process of finding another service dynamically |
| **Eureka Server** | Service Registry used in this project |
| **Eureka Client** | Microservice that registers/discovers services |
| **Service Instance** | A running instance of a microservice |
| **Feign Client** | Simplifies HTTP communication between services |
| **Spring Cloud Load Balancer** | Selects a service instance |
| **Ribbon** | Older Netflix client-side load balancer |
| **Consul** | Alternative service discovery solution |
| **etcd** | Distributed key-value store that can support discovery |
| **Zookeeper** | Distributed coordination system |

---

# 17. The Most Important Concept

Remember this simple relationship:

```text
             SERVICE DISCOVERY
                    |
                    ↓
             "Where is the service?"
                    |
                    ↓
                 Eureka
                    |
                    ↓
       "Here are the available instances"
                    |
                    ↓
          Spring Cloud Load Balancer
                    |
                    ↓
          "Use this instance"
                    |
                    ↓
              Feign Client
                    |
                    ↓
          HTTP request to service
```

---

# 18. What We Will Build in This Project

```text
                       ┌─────────────────┐
                       │  Eureka Server  │
                       │ Service Registry│
                       └────────┬────────┘
                                │
               ┌────────────────┼────────────────┐
               │                │                │
               ↓                ↓                ↓
        ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
        │ User        │  │ Order       │  │ Payment     │
        │ Service     │  │ Service     │  │ Service     │
        └─────────────┘  └──────┬──────┘  └─────────────┘
                                │
                                ↓
                         ┌─────────────┐
                         │ Feign       │
                         │ Client      │
                         └──────┬──────┘
                                │
                                ↓
                    ┌──────────────────────┐
                    │ Service Discovery    │
                    └──────────┬───────────┘
                               │
                               ↓
                    ┌──────────────────────┐
                    │ Spring Cloud         │
                    │ Load Balancer        │
                    └──────────┬───────────┘
                               │
                     ┌─────────┼─────────┐
                     ↓         ↓         ↓
                  Payment   Payment   Payment
                  Instance  Instance  Instance
                     1         2         3
```

---

# 19. Learning Flow

Recommended order for learning this project:

```text
1. Microservices Basics
        ↓
2. Service Registration
        ↓
3. Eureka Server
        ↓
4. Eureka Client
        ↓
5. Service Discovery
        ↓
6. Multiple Service Instances
        ↓
7. Spring Cloud Load Balancer
        ↓
8. Feign Client
        ↓
9. Feign + Service Discovery
        ↓
10. Load Balancing + Service Discovery
        ↓
11. Failure of Service Instances
        ↓
12. Resilience and Fault Tolerance
```

---

# 20. Key Takeaways

1. **Microservices should not depend on hard-coded IP addresses.**

2. **Service Registry maintains information about running services.**

3. **Eureka is the Service Registry used in this project.**

4. **Service Registration happens when a service registers itself with Eureka.**

5. **Service Discovery allows one service to find another service dynamically.**

6. **Spring Cloud Load Balancer selects an instance when multiple instances are available.**

7. **Feign simplifies service-to-service HTTP communication.**

8. **Ribbon is an older solution and is no longer the preferred choice.**

9. **Consul, etcd and Zookeeper are alternatives that can be used for service discovery/coordination.**

10. **Service Discovery + Load Balancing helps improve scalability, availability, fault tolerance and resilience.**

---

# Core Mental Model

```text
             Eureka
               |
               | "Where is Payment Service?"
               ↓
        Available Instances
               |
               ↓
     Spring Cloud Load Balancer
               |
               | "Which instance?"
               ↓
       Payment Service Instance
               |
               ↑
         Feign Client
               |
               ↑
         Calling Service
```

## Remember

```text
Eureka
  = FIND

Load Balancer
  = CHOOSE

Feign
  = CALL
```
