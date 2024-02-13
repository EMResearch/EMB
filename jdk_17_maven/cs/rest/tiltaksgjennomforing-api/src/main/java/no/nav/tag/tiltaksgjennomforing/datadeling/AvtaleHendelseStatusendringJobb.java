package no.nav.tag.tiltaksgjennomforing.datadeling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka.Topics;
import no.nav.tag.tiltaksgjennomforing.leader.LeaderPodCheck;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Profile({Miljø.DEV_FSS, Miljø.PROD_FSS, Miljø.LOCAL})
@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class AvtaleHendelseStatusendringJobb {
    private final AvtaleMeldingEntitetRepository avtaleMeldingEntitetRepository;
    private final AvtaleRepository avtaleRepository;
    private final LeaderPodCheck leaderPodCheck;

    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${tiltaksgjennomforing.avtale-hendelse-melding.fixed-delay}", timeUnit = TimeUnit.MINUTES)
    public void sjekkOmStatusendring() {
        if (!leaderPodCheck.isLeaderPod()) {
            log.info("Pod er ikke leader, så kjører ikke jobb for å finne avtaler med statusendring");
            return;
        }

        int antallNyeMeldinger = 0;
        for (AvtaleMeldingEntitet avtaleMeldingEntitet : avtaleMeldingEntitetRepository.findNyesteAvtaleHendelseMeldingForAvtaleSomKanEndreStatus()) {
            UUID avtaleId = avtaleMeldingEntitet.getAvtaleId();
            Avtale avtale = avtaleRepository.findById(avtaleId).orElseThrow();

            if (avtale.statusSomEnum() != avtaleMeldingEntitet.getAvtaleStatus()) {
                LocalDateTime tidspunkt = Now.localDateTime();
                AvtaleMelding avtaleMelding = AvtaleMelding.create(avtale, avtale.getGjeldendeInnhold(), new Identifikator("tiltaksgjennomforing-api"), AvtaleHendelseUtførtAvRolle.SYSTEM, HendelseType.STATUSENDRING);
                try {
                    String meldingSomString = objectMapper.writeValueAsString(avtaleMelding);
                    AvtaleMeldingEntitet entitet = new AvtaleMeldingEntitet(UUID.randomUUID(), avtaleId, tidspunkt, HendelseType.STATUSENDRING, avtale.statusSomEnum(), meldingSomString);
                    avtaleMeldingEntitetRepository.save(entitet);
                    log.info("Avtale med id {} har byttet status til {}, siste melding har status {}, så sender melding med den nye statusen på topic {}",
                            avtale.getId(), avtale.statusSomEnum(), avtaleMeldingEntitet.getAvtaleStatus(), Topics.AVTALE_HENDELSE);
                    antallNyeMeldinger++;
                } catch (JsonProcessingException e) {
                    log.error("Feil ved parsing av AvtaleHendelseMelding i statusendringjobb til json for hendelse med avtaleId {}", avtaleMelding.getAvtaleId());
                    throw new RuntimeException(e);
                }
            }
        }

        log.info("Jobb for å finne avtaler med statusendring har kjørt og sendte {} nye meldinger på topic {}", antallNyeMeldinger, Topics.AVTALE_HENDELSE);
    }
}
