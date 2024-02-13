package no.nav.tag.tiltaksgjennomforing.infrastruktur.cache;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventAndKeyHashEventLogger implements CacheEventListener<Object, Object> {

  @Override
  public void onEvent(
    CacheEvent<? extends Object, ? extends Object> cacheEvent) {
      log.debug("Cacheevent: {}, key-hash: {}", cacheEvent.getType(), cacheEvent.getKey().hashCode());
  }

}