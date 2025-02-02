# **Horizon**

Horizon is a **distributed messaging framework** built on top of Redis, providing developers with a robust toolkit for inter-process communication, synchronization, and message handling. It simplifies the complexities of distributed systems by offering the following core functionalities:

- **Packet handling**
- **Async communication**
- **Publish-subscribe (pub-sub) messaging**
- **Key-value storage**
- **Distributed locking**

---

## **Table of Contents**

- [Features](#features)
- [Installation](#installation)
- [Getting Started](#getting-started)
    - [Quick Setup](#quick-setup)
    - [Code Example](#code-example)
- [Core API Overview](#core-api-overview)
    - [Publishing Messages](#1-publishing-messages)
    - [Subscribing to Topics](#2-subscribing-to-topics)
    - [Request-Response Communication](#3-request-response-communication)
    - [Distributed Locking](#4-distributed-locking)
        - [Basic Lock Example](#distributed-lock-basic-example)
        - [Retries and Backoff Example](#distributed-lock-retries-and-backoff)
    - [Using Horizon Cache](#5-horizon-cache)
        - [Storing Data](#horizon-cache-storing-data)
        - [Retrieving Data](#horizon-cache-retrieving-data)
        - [Managing Cache Entries](#horizon-cache-managing-entries)
- [How It Works](#how-it-works)
- [Contributing](#contributing)
- [License](#license)

---

## **Features**

### **Core Functionalities**
Horizon provides an array of powerful features:
- **Pub-Sub** for asynchronous messaging between distributed components.
- **Packet Request/Response** for building efficient RPC-like interactions.
- **Distributed Locks** for creating fault-tolerant synchronization mechanisms.
- **Caching** for managing distributed key-value pairs.
- **Custom Packet Handlers** with annotations for clean code.

---

## **Getting Started**

### **Quick Setup**

1. Ensure Redis is installed and running.
2. Add the dependency to your project.
3. Initialize Horizon via `HorizonCreator.creator()`.

### **Code Example**

```java
public static void main(String[] args) {
    // Initialize Horizon instance
    try (Horizon horizon = HorizonCreator.creator(RedisClient.create()).create()) {
        // Subscribe to topics
        horizon.subscribe("example_topic", new ExamplePacketHandler());
        
        // Publish a message
        horizon.publish("example_topic", new ExamplePacket("Hello, Horizon!"));

        // Distributed locking
        DistributedLock lock = horizon.getLock("example_lock");
        lock.execute(() -> System.out.println("Critical section executed."), 
                      Duration.ofMillis(100), 
                      Duration.ofSeconds(5));
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

For detailed examples, see the usage sections below.

---

## **Core API Overview**

### **1. Publishing Messages**

Send a message to a Redis pub-sub channel using the `publish` method.

```java
horizon.publish("channel_name", new ExamplePacket("Hello, World!"));
```

---

### **2. Subscribing to Topics**

Register a topic listener using the `subscribe` method. Define handlers with `@PacketHandler`.

```java
horizon.subscribe("topic_name", new MyPacketHandler());
```

Example handler:
```java
public static class MyPacketHandler {
    @PacketHandler
    public void onPacket(ExamplePacket packet) {
        System.out.printf("Received: %s%n", packet.getContent());
    }
}
```

---

### **3. Request-Response Communication**

Send a packet and handle asynchronous responses.

```java
horizon.<ExampleResponsePacket>request("request_topic", new ExampleRequestPacket("Request data"))
       .thenAccept(response -> 
           System.out.println("Received response: " + response.getContent()));
```

In packet handler. You can return response wrapped into CompletableFuture, or not doesn't matter but we support it!

```java
  @PacketHandler
  public CompletableFuture<Packet> receive(ExampleRequestPacket packet) {
  ExampleResponsePacket response = new ExampleResponsePacket(
        request.getContent() + " Pong!");
    // Return a CompletableFuture for asynchronous processing
    return CompletableFuture.completedFuture(
        response.pointAt(request.getUniqueId()));
  }
```

---

### **4. Distributed Locking**

Distributed locks allow synchronizing tasks across processes. You can acquire/execute critical tasks using `DistributedLock`.

#### **Distributed Lock: Basic Example**

```java
DistributedLock lock = horizon.getLock("resource_lock");

try {
    // Acquire the lock with 5-second TTL
    if (lock.acquire(Duration.ofSeconds(5))) {
        System.out.println("Lock acquired. Performing critical operation...");
    } else {
        System.out.println("Unable to acquire lock.");
    }
} finally {
    // Always release the lock
    lock.release();
}
```

#### **Distributed Lock: Retries and Backoff**

```java
lock.execute(() -> {
    System.out.println("Safely executing a critical section with retries.");
}, Duration.ofMillis(50), Duration.ofSeconds(10));
```

---

### **5. Horizon Cache**

Horizon Cache is a Redis-backed mechanism to manage key-value pairs using Redis hashes. It simplifies data storage, retrieval, and removal while working seamlessly with **HorizonCodec**. It is particularly useful for distributed systems requiring scalable caching solutions.

When using Horizon's caching mechanism, **ensure the object being encoded/decoded has a no-args constructor**, especially when using JSON-based codecs like Jackson.

#### **Key Features**:
- Serialize/deserialize objects automatically via `HorizonCodec`.
- Each cache entry is stored under a unique Redis `key`.
- Supports operations for storing, retrieving, removing, and clearing entries.
- Allows handling nested/complex objects as long as they comply with serialization rules of the used codec.

---

#### **Storing Data**

Adding data to the cache is straightforward with `set()`. You can store objects mapped to a unique `field` for a given cache `key`.

```java
HorizonCache cache = horizon.getCache("users_cache");

// Add an object to the cache
cache.set("user123", new User("john_doe"));
System.out.println("Data stored in cache successfully.");
```

> **Note:** Ensure your object (e.g., `User`) has a valid no-args constructor for serialization and deserialization.

---

#### **Retrieving Data**

You can retrieve cached data using the `get()` method, specifying the field and the expected type.

```java
User user = cache.get("user123", User.class);
if (user != null) {
    System.out.println("Retrieved user from cache: " + user.name());
} else {
    System.out.println("User not found in cache!");
}
```

Horizon ensures objects are automatically deserialized to the specified type using the underlying codec.

---

#### **Removing Entries**

If you no longer need specific entries in the cache, you can remove them using their `field`. This avoids redundancy and ensures efficient resource usage.

```java
// Remove a specific entry from the cache
boolean removed = cache.remove("user123");
if (removed) {
    System.out.println("Cache entry removed.");
} else {
    System.out.println("Failed to remove entry or it doesn't exist.");
}
```

---

#### **Clearing Entire Cache**

To clear all data stored in a cache instance, use the `clear()` method. This operation deletes all fields associated with the Redis key representing the cache.

```java
// Clear all entries in the cache
long clearedCount = cache.clear();
System.out.println("Total entries cleared: " + clearedCount);
```

---

#### **Enhanced Usage Example**

Below is an end-to-end usage example of Horizon Cache:

```java
HorizonCache cache = horizon.getCache("product_cache");

// Store an object in the cache
Product product = new Product("P123", "Laptop", 1200.00);
cache.set("productId123", product);

// Retrieve and use the data
Product cachedProduct = cache.get("productId123", Product.class);
if (cachedProduct != null) {
    System.out.printf("Product Name: %s, Price: %.2f%n", cachedProduct.name(), cachedProduct.price());
}

// Remove specific data
cache.remove("productId123");

// Clear the entire cache
cache.clear();
System.out.println("Product cache is now empty.");
```

---

#### **Error Handling**

All cache operations (`set`, `get`, `remove`, `clear`) throw `HorizonCacheException`, allowing users to handle errors gracefully. The most common scenarios include:
- Redis connection issues.
- Serialization/deserialization errors.

Example:
```java
try {
    cache.set("testKey", new TestObject());
} catch (HorizonCacheException e) {
    System.err.println("Failed to store data in cache: " + e.getMessage());
}
```

By ensuring proper error handling, applications using Horizon Cache can remain robust and reliable.

---

## **How It Works**

Horizon leverages Redis as its backbone, utilizing its features to implement:
1. **Pub-Sub**: Efficient message broadcasting with a publish-subscribe pattern.
2. **Key-based Storage**: High-performance key-value caching using Redis hashes.
3. **Asynchronous Request-Handling**: Non-blocking operations with completion callbacks.
4. **Distributed Locks**: Ensures process synchronization using atomic Redis operations.

### **Key Components**
- **Packets**: Represents the unit of communication (custom serializable objects).
- **Listeners**: Handle incoming packets via annotations like `@PacketHandler`.
- **DistributedLock**: Used for synchronization of distributed tasks.
- **HorizonCache**: Provides a simple interface for Redis-backed key-value storage.

---

## **Contributing**

We welcome contributions to Horizon! Whether it's reporting a bug, suggesting new features, or improving documentation:
- Fork the repository
- Create a new branch
- Submit a pull request

For any questions or discussions, feel free to open an issue.

---

## **License**

Horizon is licensed under the **MIT License**. For more details, see the [LICENSE](LICENSE) file.

---

<p align="center">
  <img src="https://count.getloli.com/get/@a?theme=rule33" alt="Usage Counter"/>
</p>