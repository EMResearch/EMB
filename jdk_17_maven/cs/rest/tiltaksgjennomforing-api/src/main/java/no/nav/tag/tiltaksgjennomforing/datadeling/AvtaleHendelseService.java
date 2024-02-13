package no.nav.tag.tiltaksgjennomforing.datadeling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
@Service
public class AvtaleHendelseService {

    private final AvtaleRepository avtaleRepository;
    private final AvtaleMeldingEntitetRepository avtaleMeldingEntitetRepository;

    private final ObjectMapper objectMapper;

    public void sendAvtaleHendelseMeldingPåEnAvtale(Avtale avtale) {
        lagMelding(avtale);
        log.info("Send melding om avtale {}", avtale.getId());
    }

    @Async
    public void sendAvtaleHendelseMeldingPåAlleAvtaler() {
        AtomicInteger antallSendt = new AtomicInteger();

        log.info("Henter ALLE avtaler for å lage patchehendelsemeldinger");
        List<Avtale> alleAvtaler = avtaleRepository.findAll(); // /o\ \o/
        log.info("Alle avtaler er hentet, totalt {} antall avtaler, looper og sender hendelsemeldinger", alleAvtaler.size());

        alleAvtaler.forEach(avtale -> {
            if(skalSendes(avtale)) {
                lagMelding(avtale);
                antallSendt.getAndIncrement();
            }
            if(antallSendt.get() % 100 == 0) {
                log.info("Gått igjennom {} antall avtaler", antallSendt.get());
            }
        });
        log.info("Sendt totalt {} antall hendelsemeldinger", antallSendt.get());
    }

    @Async
    public void sendAvtaleHendelseMeldingPåAlleAvtalerDRYRun() {
        AtomicInteger antallSendt = new AtomicInteger();

        log.info("DRY RUN - Henter ALLE avtaler for å lage patchehendelsemeldinger");
        List<Avtale> alleAvtaler = avtaleRepository.findAll(); // /o\ \o/
        log.info("DRY RUN - Alle avtaler er hentet, det ble hele {} avtaler. looper og sender hendelsemeldinger", alleAvtaler.size());

        alleAvtaler.forEach(avtale -> {
            if(skalSendes(avtale)) {
                lagMeldingDRYRun(avtale);
                antallSendt.getAndIncrement();
            }
            if(antallSendt.get() % 100 == 0) {
                log.info("Gått igjennom {} antall avtaler", antallSendt.get());
            }
        });
        log.info("DRY RUN - Sendt totalt {} antall hendelsemeldinger", antallSendt.get());
    }

    // Skal alt sendes egentlig?
    private boolean skalSendes(Avtale avtale) {
        return true;
    }

    private void lagMelding(Avtale avtale) {
        var melding = AvtaleMelding.create(avtale, avtale.getGjeldendeInnhold(), new Identifikator("tiltaksgjennomforing-api"), AvtaleHendelseUtførtAvRolle.SYSTEM, HendelseType.STATUSENDRING);
        UUID meldingId = UUID.randomUUID();
        LocalDateTime tidspunkt = Now.localDateTime();
        try {
            String meldingSomString = objectMapper.writeValueAsString(melding);
            AvtaleMeldingEntitet entitet = new AvtaleMeldingEntitet(meldingId, avtale.getId(), tidspunkt, HendelseType.STATUSENDRING, avtale.statusSomEnum(), meldingSomString);
            avtaleMeldingEntitetRepository.save(entitet);
        } catch (JsonProcessingException e) {
            log.error("Feil ved parsing av AvtaleHendelseMelding til json for avtale med id: {}", avtale.getId());
        }
    }

    private void lagMeldingDRYRun(Avtale avtale) {
        var melding = AvtaleMelding.create(avtale, avtale.getGjeldendeInnhold(), new Identifikator("system"), AvtaleHendelseUtførtAvRolle.SYSTEM, HendelseType.STATUSENDRING);
        try {
            String meldingSomString = objectMapper.writeValueAsString(melding);
            if(meldingSomString == null ) {
                log.info("Melding ble ikke generert");
            }
        } catch (JsonProcessingException e) {
            log.error("Feil ved parsing av AvtaleHendelseMelding til json for avtale med id: {}", avtale.getId());
        }
    }
}
