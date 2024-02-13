package no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AxsysEnhet {
    private String enhetId;
    private String navn;
}