package no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring;

import lombok.RequiredArgsConstructor;
import no.nav.security.token.support.core.api.Unprotected;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/be-om-altinn-rettighet-urler")
@Unprotected
@RequiredArgsConstructor
public class BeOmAltinnRettighetUrlerController {
    private final AltinnTilgangsstyringProperties props;

    @GetMapping
    public Map<Tiltakstype, String> beOmRettighetUrler(@RequestParam("orgNr") String orgNr) {
        return Map.of(
                Tiltakstype.ARBEIDSTRENING, beOmRettighetUrl(orgNr),
                Tiltakstype.INKLUDERINGSTILSKUDD, beOmRettighetUrl(orgNr),
                Tiltakstype.MENTOR, beOmRettighetUrl(orgNr),
                Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD, beOmRettighetUrl(orgNr),
                Tiltakstype.VARIG_LONNSTILSKUDD, beOmRettighetUrl(orgNr),
                Tiltakstype.SOMMERJOBB, beOmRettighetUrl(orgNr)
        );
    }

    private String beOmRettighetUrl(String orgNr) {
        return props.getBeOmRettighetBaseUrl() + "&bedrift=" + orgNr;
    }
}
