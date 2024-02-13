package no.nav.tag.tiltaksgjennomforing.datadeling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.UtviklerTilgangProperties;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.exceptions.RessursFinnesIkkeException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@RestController
@RequestMapping("/utvikler-admin/avtale-hendelse")
@RequiredArgsConstructor
@ProtectedWithClaims(issuer = "aad")
@Slf4j
public class AvtaleHendelseController {

    private final UtviklerTilgangProperties utviklerTilgangProperties;
    private final TokenUtils tokenUtils;
    private final AvtaleHendelseService avtaleHendelseService;
    private final AvtaleRepository avtaleRepository;
    private void sjekkTilgang() {
        if (!tokenUtils.harAdGruppe(utviklerTilgangProperties.getGruppeTilgang())) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("send-melding-en-avtale/{avtaleId}")
    public void sendMeldingForEnAvtale(@PathVariable("avtaleId") UUID avtaleId) {
        log.info("Sender hendelsemelding for en avtale {}", avtaleId);
        Avtale avtale = avtaleRepository.findById(avtaleId).orElseThrow(RessursFinnesIkkeException::new);
        avtaleHendelseService.sendAvtaleHendelseMeldingPåEnAvtale(avtale);
    }

    @PostMapping("dry-send-melding-alle-avtaler")
    public void drySendMeldingAlleAvtaler() {
        log.info("DRY - Sender alle avtaler som hendelsemeldinger på topic");
        sjekkTilgang();
        avtaleHendelseService.sendAvtaleHendelseMeldingPåAlleAvtalerDRYRun();
    }


    @PostMapping("send-melding-alle-avtaler")
    @Transactional
    public void sendMeldingAlleAvtaler() {
        log.info("Sender alle avtaler som hendelsemeldinger på topic");
        sjekkTilgang();
        avtaleHendelseService.sendAvtaleHendelseMeldingPåAlleAvtaler();
    }
}
