# Eureka Server -- Self-Preservation

## 1. What is Eureka Self-Preservation?

In a distributed system using Eureka, each service instance periodically
sends a **heartbeat** to the Eureka Server to indicate that it is alive
and functioning.

Normally:

``` text
Service Instance
      |
      | Heartbeat
      ↓
Eureka Server
```

If Eureka does not receive heartbeats from an instance within the
configured timeframe, it can consider that instance unavailable and
eventually evict it from the registry.

### The Network Problem

A temporary network problem or system delay can cause Eureka to miss
heartbeats even though the service itself is still healthy.

This can create a **false-positive failure detection**:

``` text
Healthy Service
      |
      X  Heartbeat lost because of network issue
      |
      ↓
Eureka may think the service is unavailable
```

If Eureka immediately removed every instance whose heartbeat was missed,
temporary network problems could cause unnecessary service removals and
instability.

------------------------------------------------------------------------

## 2. What Self-Preservation Does

**Self-preservation mode** is a protection mechanism in Eureka.

When Eureka detects that it is receiving fewer heartbeats than expected,
it can enter self-preservation mode instead of aggressively evicting
service instances.

The main idea is:

> **Do not assume that missing heartbeats always mean that services are
> actually down.**

While self-preservation is active, Eureka avoids removing registered
instances simply because it is temporarily receiving fewer heartbeats.

This helps protect the registry from losing healthy service instances
during temporary network glitches or delays.

------------------------------------------------------------------------

## 3. Heartbeat and Lease Concepts

Each Eureka client periodically sends a heartbeat to the server.

Two important settings control this behavior:

### `eureka.instance.lease-renewal-interval-in-seconds = 30`

Defines how frequently the client sends a heartbeat to Eureka.

``` text
Every 30 seconds
      ↓
Client sends heartbeat
      ↓
Eureka Server
```

### `eureka.instance.lease-expiration-duration-in-seconds = 90`

Defines the duration Eureka waits after the last received heartbeat
before an instance's lease can expire and the instance can become
eligible for eviction.

Conceptually:

``` text
Last heartbeat
      |
      |------ 90 seconds ------|
                              ↓
                     Lease can expire
```

------------------------------------------------------------------------

## 4. Eureka Eviction Interval

### `eureka.server.eviction-interval-timer-in-ms = 60 * 1000`

The Eureka Server runs its eviction task at this interval.

With the configured value:

``` text
60 * 1000 ms = 60 seconds
```

The eviction task checks for instances whose leases have expired.

Before eviction, Eureka also considers whether the system has entered
**self-preservation mode**, based on the actual and expected heartbeat
levels.

So eviction is not simply:

``` text
No heartbeat → Immediately remove instance
```

Instead, Eureka considers the lease status and self-preservation state.

------------------------------------------------------------------------

## 5. Renewal Percentage Threshold

### `eureka.server.renewal-percent-threshold = 0.85`

This value is used when calculating the expected percentage of
heartbeats that Eureka should be receiving.

``` text
0.85 = 85%
```

The threshold is used as part of Eureka's self-preservation calculation.

The basic idea is to compare:

``` text
Expected heartbeats
        vs
Actual heartbeats received
```

If the actual heartbeat level falls below the expected threshold, Eureka
can enter self-preservation mode.

------------------------------------------------------------------------

## 6. Renewal Threshold Update Interval

### `eureka.server.renewal-threshold-update-interval-ms = 15 * 60 * 1000`

The renewal threshold is recalculated periodically using this interval.

``` text
15 * 60 * 1000 ms
        ↓
15 minutes
```

The scheduler uses this interval to calculate/update the expected
heartbeat level.

------------------------------------------------------------------------

## 7. Enabling Self-Preservation

### `eureka.server.enable-self-preservation = true`

Self-preservation is enabled with this setting.

When enabled, Eureka can protect the registry from mass eviction when it
detects that the number of received heartbeats has dropped below the
expected level.

It can be disabled by changing the value to:

``` text
false
```

For our learning setup, the important concept is understanding **why
self-preservation exists and how the heartbeat-related properties
influence it**.

------------------------------------------------------------------------

## 8. Overall Flow

``` text
Accounts ──────┐
Cards ─────────┼──→ Heartbeats ──→ Eureka Server
Loans ─────────┘

                 Eureka monitors
                       |
                       ↓
             Expected vs Actual
                 heartbeats
                       |
          ┌────────────┴────────────┐
          ↓                         ↓
    Normal heartbeat          Heartbeats drop
          ↓                         ↓
    Normal operation         Self-preservation
                                    |
                                    ↓
                         Avoid unnecessary eviction
```

------------------------------------------------------------------------

## 9. Important Properties

  -----------------------------------------------------------------------------------------------------------
  Property                                                                        Value Purpose
  -------------------------------------------------------- ---------------------------- ---------------------
  `eureka.instance.lease-renewal-interval-in-seconds`                              `30` Frequency of client
                                                                                        heartbeats

  `eureka.instance.lease-expiration-duration-in-seconds`                           `90` Time after the last
                                                                                        heartbeat before the
                                                                                        lease can expire

  `eureka.server.eviction-interval-timer-in-ms`                             `60 * 1000` Frequency of the
                                                                                        Eureka eviction task

  `eureka.server.renewal-percent-threshold`                                      `0.85` Threshold used for
                                                                                        expected heartbeat
                                                                                        calculation

  `eureka.server.renewal-threshold-update-interval-ms`                 `15 * 60 * 1000` Interval for updating
                                                                                        the expected
                                                                                        heartbeat threshold

  `eureka.server.enable-self-preservation`                                       `true` Enables Eureka
                                                                                        self-preservation
  -----------------------------------------------------------------------------------------------------------

## 10. Key Takeaway

Eureka Self-Preservation is designed to prevent the Eureka Server from
treating a temporary **network problem** as a widespread **service
failure**.

``` text
Network glitch
     ↓
Some heartbeats are missed
     ↓
Actual heartbeats fall below expected level
     ↓
Eureka enters self-preservation
     ↓
Avoid unnecessary eviction of registered instances
```

The purpose is to maintain a more stable service registry during
temporary network interruptions.
