# **Horizon (horyzont)**

---

## **Horizon Configuration**

```java
RedisClient redisClient = RedisClient.create("redis://localhost:6379");
Horizon horizon = HorizonConfigurator.configure(configurator ->
    configurator
        .packetBroker(config ->
            config.packetBroker(
                RedisPacketBroker.create(redisClient)))
        .keyValue(config ->
            config.keyValue(
                RedisKeyValueStorage.create(redisClient)))
        .distributedLock(config ->
            config.shouldInitializeDistributedLocks(true))
        .packetCallback(config ->
            config.requestCleanupInterval(Duration.ofSeconds(10L))
                .useCaffeine(Runnable::run))
        // Use Caffeine for request cleanup (optional, will work without it just fine)
        .horizonSerdes(config -> config.horizonSerdes(
            MsgpackPacketCodecFactory.getMsgpackPacketCodec())));

// On shutdown
horizon.

close();
```

### **Example Packet Structure**

Horizon supports flexible packet structures, adaptable for P2P, C2S, or S2C models. Here is a basic
example of a request packet.

```java
public class ExampleRequestPacket extends JacksonPacket {

  private String content;

  private ExampleRequestPacket() {
  }

  public ExampleRequestPacket(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }
}
```

### **Subscriber Example**

Horizon allows defining subscribers with flexible receiving logic. Subscribers are isolated to
specific topics but can be scaled across multiple instances as needed.

```java
import java.util.concurrent.CompletableFuture;

public class ExampleListener implements Subscriber {

  @Subscribe
  public Packet receive(ExampleRequestPacket request) {
    if (condition) {
      return null; // Return null if conditions are not met
    }
    ExampleResponsePacket response = new ExampleResponsePacket(
        request.getContent() + " Pong!");
    return response.dispatchTo(request.getUniqueId());
  }

  @Subscribe
  public void receive(BroadcastPacket packet) {
    System.out.printf("Received P2P packet: %s", packet.getContent());
  }

  @Subscribe
  public CompletableFuture<Packet> receive(BroadcastPacket packet) {
    System.out.printf("Received P2P packet: %s", packet.getContent());
    ExampleResponsePacket response = new ExampleResponsePacket(
        request.getContent() + " Pong!");
    // Return a CompletableFuture for asynchronous processing
    return CompletableFuture.completedFuture(
        response.dispatchTo(request.getUniqueId()));
  }

  @Override
  public String identity() {
    return "tests";
  }
}
```

### **Distributed Locking Example**

Below is an example demonstrating the use of Horizon's distributed locking, ideal for synchronizing
access to shared resources in multi-threaded environments.

```java
DistributedLock lock = horizon.distributedLocks().createLock("my_lock");
lock.

execute(() ->{
    System.out.

println("Thread "+i +" acquired the lock!");
      try{
          Thread.

sleep(100);
// Simulate a long-running task
      }
          catch(
InterruptedException ignored){
    Thread.

currentThread().

interrupt();
      }
          },
          Duration.

ofMillis(10L),
    Duration.

ofSeconds(5L))
    .

whenComplete((unused, throwable) ->
System.out ..

println("Thread "+i +" released the lock!"))
    .

join();
```

---

<p align="center">
  <img height="100em" src="https://count.getloli.com/get/@:horizon?theme=rule33" alt="usage-count"/>
</p>