package no.nav.tag.tiltaksgjennomforing.orgenhet;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import no.nav.security.token.support.core.api.Protected;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
@RequestMapping("/organisasjoner")
@Timed
@RequiredArgsConstructor
public class OrganisasjonController {
    private final EregService eregService;

    @GetMapping
    public Organisasjon hentVirksomhet(@RequestParam("bedriftNr") BedriftNr bedriftNr) {
        return eregService.hentVirksomhet(bedriftNr);
    }
}
