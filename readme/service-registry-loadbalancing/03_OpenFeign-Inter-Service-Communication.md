# OpenFeign – Inter-Service Communication

## 1. Overview

In our microservices project, the **Accounts Service** needs to communicate with other microservices such as **Cards** and **Loans**.

For this communication, we use **Spring Cloud OpenFeign**.

OpenFeign allows Accounts to call APIs exposed by other services through a simple Java interface instead of manually creating HTTP requests.

## 2. OpenFeign in Our Project

The Accounts Service has Feign clients for:

- Cards Service
- Loans Service

The Feign clients describe the APIs that Accounts needs to call.

The Cards Feign client uses the service name **`cards`**, which matches:

`spring.application.name: cards`

The Loans Feign client similarly uses **`loans`**, matching:

`spring.application.name: loans`

## 3. How Feign Works with Eureka

Our application does not provide a fixed URL for the Cards or Loans Feign clients.

Instead, the Feign client uses the **service name**.

```text
Accounts Service
       |
       | Feign Client
       v
   Service Name
   (cards / loans)
       |
       v
     Eureka
       |
       v
Find registered service instance
       |
       v
Spring Cloud Load Balancer
       |
       v
Cards / Loans Service
```

The Accounts startup log confirms this behavior:

```text
For 'cards' URL not provided.
Will try picking an instance via load-balancing.
```

This is expected in our setup.

Accounts does not need to know the actual host and port of the Cards or Loans service.

## 4. Feign + Eureka + Load Balancer

These components have different responsibilities:

| Component | Responsibility |
|---|---|
| **OpenFeign** | Defines and performs HTTP communication |
| **Eureka** | Finds where the requested service is running |
| **Spring Cloud Load Balancer** | Selects an available instance |

A simple way to remember this:

```text
Feign          -> How do I communicate?
Eureka         -> Where is the service?
Load Balancer  -> Which instance should I use?
```

## 5. Accounts as an Aggregator

In our project, the Accounts Service acts as an **aggregator** when retrieving customer details.

```text
Client
  |
  | GET customer details
  v
Accounts Service
  |
  +-------------------+
  |                   |
  v                   v
Account Data       Cards Service
                    (Feign)
  |
  v
Loans Service
(Feign)
  |
  v
Combined CustomerDetailsDto
```

The final response combines:

- Customer information
- Account information
- Card information obtained from Cards
- Loan information obtained from Loans

This allows the client to obtain the required customer information through a single Accounts API.

## 6. Why Use Service Names Instead of URLs?

A direct URL tightly couples Accounts to a particular service instance:

```text
Accounts -> http://localhost:9000
```

With service discovery, Accounts only knows the logical service name:

```text
Accounts -> cards
```

Eureka maintains the actual location of the Cards instances.

This becomes useful when:

- Service instances move to different hosts or ports
- Multiple instances of a service are running
- Services are scaled horizontally
- The application is deployed across different environments

## 7. Enabling Feign

The Accounts application enables Feign client support using:

`@EnableFeignClients`

This tells Spring to discover the Feign client interfaces and create their implementations.

The Feign clients can then be injected and used as Spring beans.

## 8. Request Flow in Our Example

```text
1. Client calls Accounts
          |
2. Accounts retrieves customer/account data
          |
3. Accounts uses CardsFeignClient
          |
4. Feign uses service name "cards"
          |
5. Eureka provides the Cards instance
          |
6. Load Balancer selects an instance
          |
7. Cards API is called
          |
8. Accounts uses LoansFeignClient
          |
9. Loans service is discovered and called
          |
10. Accounts combines the responses
          |
11. CustomerDetailsDto is returned
```

## 9. Key Takeaways

- **OpenFeign** simplifies HTTP communication between microservices.
- Feign clients use the **logical service name** instead of hard-coded host/port URLs.
- The service name must match the service's `spring.application.name`.
- **Eureka** provides service discovery.
- **Spring Cloud Load Balancer** selects a service instance.
- Accounts uses Feign to communicate with Cards and Loans.
- Accounts acts as an **aggregator** by combining data from multiple services into `CustomerDetailsDto`.

### Core Concept

```text
OpenFeign
    |
Service Name
    |
Eureka Service Discovery
    |
Spring Cloud Load Balancer
    |
Target Microservice
```
