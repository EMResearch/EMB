package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriode;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriodeRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriodeStatus;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@ConditionalOnProperty("tiltaksgjennomforing.kafka.enabled")
@Component
@Slf4j
public class RefusjonEndretStatusKafkaConsumer {
    private final TilskuddPeriodeRepository tilskuddPeriodeRepository;


    public RefusjonEndretStatusKafkaConsumer(TilskuddPeriodeRepository tilskuddPeriodeRepository) {
        this.tilskuddPeriodeRepository = tilskuddPeriodeRepository;
    }

    @KafkaListener(topics = Topics.REFUSJON_ENDRET_STATUS, containerFactory = "refusjonEndretStatusContainerFactory")
    public void refusjonEndretStatus(RefusjonEndretStatusMelding melding) {
        TilskuddPeriode tilskuddPeriode = tilskuddPeriodeRepository.findById(UUID.fromString(melding.getTilskuddsperiodeId())).orElseThrow();
        if(tilskuddPeriode.getStatus() == TilskuddPeriodeStatus.UBEHANDLET) {
            log.error("En tilskuddsperiode {} som er ubehandlet har fått statusendring fra refusjon-api", melding.getTilskuddsperiodeId());
        }
        tilskuddPeriode.setRefusjonStatus(melding.getStatus());
        log.info("Setter refusjonstatus til {} på tilskuddsperiode {}", melding.getStatus(), melding.getTilskuddsperiodeId());

        tilskuddPeriodeRepository.save(tilskuddPeriode);

    }
}
