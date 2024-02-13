package no.nav.tag.tiltaksgjennomforing.datavarehus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.leader.LeaderPodCheck;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Profile("!local")
@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class DvhStatusendringJobb {
    private final DvhMeldingEntitetRepository dvhMeldingRepository;
    private final AvtaleRepository avtaleRepository;
    private final LeaderPodCheck leaderPodCheck;

    @Scheduled(fixedDelayString = "${tiltaksgjennomforing.dvh-melding.fixed-delay}")
    public void sjekkOmStatusendring() {

        if (!leaderPodCheck.isLeaderPod()) {
            log.info("Pod er ikke leader, så kjører ikke jobb for å finne avtaler med statusendring");
            return;
        }

        int antallNyeMeldinger = 0;
        for (DvhMeldingEntitet dvhMeldingEntitet : dvhMeldingRepository.findNyesteDvhMeldingForAvtaleSomKanEndreStatus()) {
            UUID avtaleId = dvhMeldingEntitet.getAvtaleId();
            Avtale avtale = avtaleRepository.findById(avtaleId).orElseThrow();

            if (avtale.statusSomEnum() != dvhMeldingEntitet.getTiltakStatus()) {
                LocalDateTime tidspunkt = Now.localDateTime();
                AvroTiltakHendelse avroTiltakHendelse = AvroTiltakHendelseFabrikk.konstruer(avtale, tidspunkt, UUID.randomUUID(), DvhHendelseType.STATUSENDRING, "system");
                dvhMeldingRepository.save(new DvhMeldingEntitet(UUID.randomUUID(), avtaleId, tidspunkt, avtale.statusSomEnum(), avroTiltakHendelse));
                log.info("Avtale med id {} har byttet status til {}, siste melding har status {}, så sender melding med den nye statusen til datavarehus", avtale.getId(), avtale.statusSomEnum(), dvhMeldingEntitet.getTiltakStatus());
                antallNyeMeldinger++;
            }
        }

        log.info("Jobb for å finne avtaler med statusendring har kjørt og sendte {} nye meldinger til datavarehus", antallNyeMeldinger);
    }
}
