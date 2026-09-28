package fr.uge.forkeat.infrastructure.event;

import fr.uge.forkeat.service.event.DomainEvent;
import fr.uge.forkeat.service.port.EventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventPublisherAdapter<E extends DomainEvent> implements EventPublisherPort<E> {

  private final ApplicationEventPublisher applicationEventPublisher;
  public EventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(E event) {
    applicationEventPublisher.publishEvent(event);
  }
}
