# **Horizon (horyzont)**

---

## **Horizon Examples**

```java
public static void main(String[] args) {
  try (Horizon horizon =
      Horizon.newBuilder(
              RedisClient.create(
                  RedisURI.builder()
                      .withHost("localhost")
                      .build()))
          .build()) {
    horizon.retrieveStorage("locks").clear();

    horizon.subscribe("tests", new ExampleListener());
    horizon.publish("tests", new ExamplePacket("Hello, world!"));

    horizon
        .<ExampleCallback>request("tests", new ExampleCallback("Hello, world!"))
        .thenAccept(response -> System.out.println("Received response: " + response.getContent()))
        .join();

    AtomicInteger counter = new AtomicInteger(0);
    for (int j = 0; j < 10; j++) {
      int i = j;
      DistributedLock lock = horizon.retrieveLock("my_lock");
      lock.execute(
              () -> {
                System.out.println("Thread " + i + " acquired the lock!");

                try {
                  Thread.sleep(100);

                  // Simulate a long-running task
                } catch (InterruptedException ignored) {
                  Thread.currentThread().interrupt();
                }
              },
              Duration.ofMillis(10L),
              Duration.ofSeconds(5L))
          .whenComplete(
              (unused, throwable) -> {
                counter.incrementAndGet();
                System.out.println("Thread " + i + " released the lock!");
              });
    }

    while (counter.get() < 10) {
      Thread.sleep(100);
      System.out.println("waiting for all threads to finish (" + counter.get() + "/10)");
    }
  } catch (Exception e) {
    e.printStackTrace();
    System.exit(1);
  }
}

public static class ExamplePacket extends Packet {

  private String content;

  @JsonCreator
  private ExamplePacket() {}

  public ExamplePacket(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }
}

public static class ExampleCallback extends Packet {

  private String content;

  @JsonCreator
  private ExampleCallback() {}

  public ExampleCallback(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }
}

private static class ExampleListener {

  @PacketHandler
  public void handle(ExamplePacket packet) {
    System.out.printf(
        "Received packet with content %s and uid %s%n", packet.getContent(), packet.getUniqueId());
  }

  @PacketHandler
  public Packet handle(ExampleCallback request) {
    return new ExampleCallback("HIII " + request.getContent()).pointAt(request.getUniqueId());
  }
}
```

---

<p align="center">
  <img height="100em" src="https://count.getloli.com/get/@:horizon?theme=rule33" alt="usage-count"/>
</p>