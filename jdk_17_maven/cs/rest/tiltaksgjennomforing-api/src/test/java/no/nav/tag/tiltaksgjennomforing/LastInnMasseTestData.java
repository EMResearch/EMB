package no.nav.tag.tiltaksgjennomforing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("masse-testdata")
public class LastInnMasseTestData implements ApplicationListener<ApplicationReadyEvent> {
    private final AvtaleRepository avtaleRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Laster inn masse testdata");

        for (int i = 0; i < 555; i++) {
            Avtale avtale = TestData.enLonnstilskuddAvtaleGodkjentAvVeilederTilbakeITid();
            avtale.getGjeldendeInnhold().setDeltakerFornavn(new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(5));
            avtaleRepository.save(avtale);
        }
    }
}
