# **Horizon (horyzont)**

Horizon is a **distributed messaging framework** designed for:
- **Packet handling**
- **Async communication**
- **Publish-subscribe (pub-sub) messaging**
- **Key-value storage**
- **Distributed locking**

It is built on top of Redis and provides a simple and powerful API for developers to work with inter-process communication and synchronization.

---

## **Features**
- **Publish & Subscribe**: Efficient pub-sub messaging using Redis.
- **Packet Request/Response**: Easy handling of request/response message patterns.
- **Distributed Locks**: Simplified implementation of distributed locks for synchronization.
- **Caching**: Powerful key-based caching using Redis.
- **Custom Packet Handlers**: Seamlessly handle custom packets using annotations.

---

## **Installation**
Add the Horizon dependency to your Java project via your dependency manager. Ensure you have Redis running and accessible to your application.

---

## **Getting Started**

Here is an example to demonstrate the core functionalities of Horizon:

### **Example Code**
```java
public static void main(String[] args) {
  try (Horizon horizon = HorizonCreator.creator(RedisClient.create()).create()) {

    // Subscribing to a topic
    horizon.subscribe("tests", new ExampleListener());
    horizon.publish("tests", new ExamplePacket("Hello, world!"));

    // Async packet request/response
    horizon
        .<ExampleResponse>request("tests", new ExampleRequest("Hello, world!"))
        .thenAccept(response -> System.out.println("Received response: " + response.getContent()))
        .join();

    // Distributed locking example
    AtomicInteger counter = new AtomicInteger(0);
    for (int j = 0; j < 10; j++) {
      final int i = j;
      final DistributedLock lock = horizon.getLock("my_lock");
      lock.execute(
              () -> {
                System.out.println("Thread " + i + " acquired the lock!");
                Thread.sleep(100); // Simulate task
              },
              Duration.ofMillis(10L),
              Duration.ofSeconds(5L))
          .whenComplete(
              (unused, throwable) -> {
                counter.incrementAndGet();
                System.out.println("Thread " + i + " released the lock!");
              });
    }

    // Wait for all threads to finish
    while (counter.get() < 10) {
      Thread.sleep(100);
      System.out.println("waiting for all threads to finish (" + counter.get() + "/10)");
    }
  } catch (final Exception exception) {
    exception.printStackTrace();
    System.exit(1);
  }
}
```

### **Custom Packets and Handlers**
```java
public static class ExamplePacket extends Packet {
  private String content;

  @JsonCreator
  public ExamplePacket(final String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }
}

public static class ExampleListener {

  @PacketHandler
  public void handle(final ExamplePacket packet) {
    System.out.printf(
        "Received packet with content %s and uid %s%n", packet.getContent(), packet.getUniqueId());
  }

  @PacketHandler
  public Packet handle(final ExampleRequest request) {
    return new ExampleResponse("Response to: " + request.getContent())
        .pointAt(request.getUniqueId());
  }
}
```

---

## **Core API Overview**

### 1. **Creating a Horizon Instance**
To create an instance of Horizon with default settings:
```java
Horizon horizon = HorizonCreator.creator(RedisClient.create()).create();
```

### 2. **Publishing Messages**
Publish a packet to a specific channel:
```java
horizon.publish("channel_name", new ExamplePacket("Hello, Horizon!"));
```

### 3. **Subscribing to Topics**
Subscribe to a topic and define a listener:
```java
horizon.subscribe("topic_name", new ExampleListener());
```

### 4. **Request-Response Communication**
Send a request and handle the response asynchronously:
```java
horizon.<ExampleResponse>request("channel_name", new ExampleRequest("Request Data"))
       .thenAccept(response -> System.out.println(response.getContent()));
```

### 5. **Distributed Locking**
Use distributed locks for synchronization:
```java
DistributedLock lock = horizon.getLock("my_lock");
lock.execute(() -> {
    // Critical section
}, Duration.ofMillis(10), Duration.ofSeconds(5));
```

---

## **How It Works**
Horizon is built on top of Redis, leveraging its:
- **Pub-Sub**: Communication between distributed components.
- **Key-based Storage**: Used for caching and managing locks.
- **Asynchronous APIs**: Non-blocking, efficient communication.

### **Key Components**
1. **Packets**: Represent messages exchanged between components.
2. **Listeners**: Handle incoming messages, using `@PacketHandler` annotations.
3. **DistributedLock**: Manages synchronization across distributed systems.
4. **HorizonCodec**: Provides serialization/deserialization of packets.

---

## **Contributing**
Contributions are welcome! Feel free to fork the repository and submit pull requests for new features, bug fixes, or documentation improvements.

---

## **License**
Horizon is licensed under the [MIT License](LICENSE).

---

<p align="center">
  <img src="https://count.getloli.com/get/@:horizon?theme=rule33" alt="Usage Counter"/>
</p>