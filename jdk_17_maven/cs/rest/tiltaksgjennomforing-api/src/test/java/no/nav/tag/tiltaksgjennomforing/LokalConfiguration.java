package no.nav.tag.tiltaksgjennomforing;

import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.NotifikasjonService;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LokalConfiguration {
  @Bean("azure")
  RestTemplate restTemplate(){
    return new RestTemplateBuilder().build();
  }

  @Bean
  NotifikasjonService notifikasjon() { return Mockito.mock(NotifikasjonService.class);}

}
