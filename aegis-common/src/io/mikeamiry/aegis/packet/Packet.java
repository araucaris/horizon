package io.mikeamiry.aegis.packet;

import io.mikeamiry.aegis.eventbus.Event;
import java.io.Serializable;

/**
 * Represents a base class for data packets that are used in communication systems or event-driven
 * architectures. This class implements the {@code Event} interface, indicating that packets can
 * function as events in an event bus or similar systems. Additionally, it implements {@code
 * Serializable}, allowing instances of this class or its subclasses to be serialized for transport
 * or storage.
 *
 * <p>A {@code Packet} contains two key properties: - {@code source}: Identifies the origin or the
 * sender of the packet. - {@code target}: Identifies the intended recipient or target of the
 * packet.
 *
 * <p>Key features include: - Ability to set and retrieve the source and target of the packet. - A
 * utility method {@code pointAt} to set the target of the current packet to the source of another
 * packet, supporting chaining by returning the current packet instance.
 *
 * <p>This class is abstract and should be extended to define specific types of packets required for
 * different communications or event-handling systems.
 */
public abstract class Packet implements Event, Serializable {

  private String source;
  private String target;

  protected Packet() {}

  public String source() {
    return source;
  }

  public void source(final String source) {
    this.source = source;
  }

  public String target() {
    return target;
  }

  public <T extends Packet> T pointAt(final Packet request) {
    this.target = request.source;
    //noinspection unchecked
    return (T) this;
  }
}
