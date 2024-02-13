package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.UtviklerTilgangProperties;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriode;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriodeRepository;
import no.nav.tag.tiltaksgjennomforing.exceptions.RessursFinnesIkkeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@RestController
@RequestMapping("/utvikler-admin/tilskuddsperioder")
@RequiredArgsConstructor
@ProtectedWithClaims(issuer = "aad")
@ConditionalOnProperty("tiltaksgjennomforing.kafka.enabled")
@Slf4j
public class TilskuddsperiodeAdminController {
    private final TilskuddsperiodeKafkaProducer tilskuddsperiodeKafkaProducer;
    private final AvtaleRepository avtaleRepository;
    private final TilskuddPeriodeRepository tilskuddPeriodeRepository;
    private final UtviklerTilgangProperties utviklerTilgangProperties;
    private final TokenUtils tokenUtils;

    private void sjekkTilgang() {
        if (!tokenUtils.harAdGruppe(utviklerTilgangProperties.getGruppeTilgang())) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }
    }

    // Generer en kafkamelding og send den. Oppdaterer ikke statuser eller lignende på perioden
    @PostMapping("/send-tilskuddsperiode-godkjent-melding/{tilskuddsperiodeId}")
    public void sendTilskuddsperiodeGodkjentMelding(@PathVariable("tilskuddsperiodeId") UUID id) {
        sjekkTilgang();
        log.info("Lager og sender tilskuddsperiode godkjent-melding for tilskuddsperiode: {}", id);
        TilskuddPeriode tilskuddPeriode = tilskuddPeriodeRepository.findById(id).orElseThrow(RessursFinnesIkkeException::new);
        Avtale avtale = tilskuddPeriode.getAvtale();
        TilskuddsperiodeGodkjentMelding melding = TilskuddsperiodeGodkjentMelding.create(avtale, tilskuddPeriode, null);
        tilskuddsperiodeKafkaProducer.publiserTilskuddsperiodeGodkjentMelding(melding);

    }

    // Generer en kafkamelding og send den. Oppdaterer ikke statuser eller lignende på perioden
    @PostMapping("/send-tilskuddsperiode-annullert-melding/{tilskuddsperiodeId}")
    public void sendTilskuddsperiodeAnnullertMelding(@PathVariable("tilskuddsperiodeId") UUID id) {
        sjekkTilgang();
        log.info("Lager og sender tilskuddsperiode annullert-melding for tilskuddsperiode: {}", id);
        TilskuddPeriode tilskuddPeriode = tilskuddPeriodeRepository.findById(id).orElseThrow(RessursFinnesIkkeException::new);
        TilskuddsperiodeAnnullertMelding melding = new TilskuddsperiodeAnnullertMelding(tilskuddPeriode.getId(), TilskuddsperiodeAnnullertÅrsak.AVTALE_ANNULLERT);
        tilskuddsperiodeKafkaProducer.publiserTilskuddsperiodeAnnullertMelding(melding);
    }

}
