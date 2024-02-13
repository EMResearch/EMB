package no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class AxsysRespons {
    private List<AxsysEnhet> enheter;

    List<NavEnhet> tilEnheter() {
        return enheter.stream().map(enhet -> new NavEnhet(enhet.getEnhetId(), enhet.getNavn())).collect(Collectors.toList());
    }
}
