# **Horizon (horyzont)**

---

## **Horizon Configuration**

```java
RedisClient redisClient = RedisClient.create("redis://localhost:6379");
Horizon horizon = Horizon.newBuilder(redisClient).build();
// On shutdown
horizon.

close();
```

### **Packet Structure**

```java
public class ExampleRequestPacket extends Packet {

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

### **Subscriber**

```java
public class ExampleListener implements Subscriber {

  @Subscribe
  public Packet receive(ExampleRequestPacket request) {
    // Sync response
    if (condition) {
      return null; // Return null if conditions are not met
    }
    ExampleResponsePacket response =
        new ExampleResponsePacket(request.getContent() + " Pong!");
    return response.dispatchTo(request.getUniqueId());
  }

  @Subscribe
  public void receive(BroadcastPacket packet) {
    // Simple listener
    System.out.printf("Received P2P packet: %s", packet.getContent());
  }

  @Subscribe
  public CompletableFuture<Packet> receive(BroadcastPacket packet) {
    // Async response
    ExampleResponsePacket response =
        new ExampleResponsePacket(request.getContent() + " Pong!");
    return completedFuture(response.dispatchTo(request.getUniqueId()));
  }

  @Override
  public String identity() {
    return "tests";
  }
}
```

### **Distributed Locking**

```java
TODO
```

---

<p align="center">
  <img height="100em" src="https://count.getloli.com/get/@:horizon?theme=rule33" alt="usage-count"/>
</p>