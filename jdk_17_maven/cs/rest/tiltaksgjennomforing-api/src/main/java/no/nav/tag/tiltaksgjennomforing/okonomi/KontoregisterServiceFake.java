package no.nav.tag.tiltaksgjennomforing.okonomi;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.utils.ConditionalOnPropertyNotEmpty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnPropertyNotEmpty("tiltaksgjennomforing.kontoregister.fake")
public class KontoregisterServiceFake implements KontoregisterService {

    public String hentKontonummer(String bedriftNr)  {
        return "10000008162";
    }

}
