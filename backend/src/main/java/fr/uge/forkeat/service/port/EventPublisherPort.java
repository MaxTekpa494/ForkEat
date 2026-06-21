package fr.uge.forkeat.service.port;


import fr.uge.forkeat.service.event.DomainEvent;

/* Moi :
Alors pourquoi une classe parametrée ici ? Parce que :
- https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/ApplicationEventPublisher.html
- publishEvent : void publishEvent(Object event) prend un Object en parametre et c'est pas forcement notre tasse de thé par consequent
- On aurait pu mettre directement :  void publish(RecipePublishedEvent) mais cela lierait fortement notre interface à
-  RecipePublishedEvent, notre interface ne sera donc plus open-closed (oh joie).
- Pourquoi on voudrait l'open close ici ? Parce qu'il peut y avoir d'autres types Event à l'avenir et on ne va
- S'amuser à créer une interface pour chaque event... Voilà
- Par ailleurs on extends DomainEvent pour eviter de prendre en parametre les Integer, String ...
  */
public interface EventPublisherPort<E extends DomainEvent> {
  void publish(E event);
}
