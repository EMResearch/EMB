package no.nav.tag.tiltaksgjennomforing.varsel;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.sporingslogg.Sporingslogg;
import no.nav.tag.tiltaksgjennomforing.sporingslogg.SporingsloggRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles(Miljø.LOCAL)
@DirtiesContext
public class SporingsloggRepositoryTest {
    @Autowired
    private SporingsloggRepository sporingsloggRepository;
    @Autowired
    private AvtaleRepository avtaleRepository;

    @Test
    public void save__lagrer_alle_felter() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        avtaleRepository.save(avtale);
        Sporingslogg sporingslogg = TestData.enHendelse(avtale);
        Sporingslogg lagretSporingslogg = sporingsloggRepository.save(sporingslogg);
        assertThat(lagretSporingslogg.getId()).isNotNull();
        assertThat(lagretSporingslogg.getTidspunkt()).isNotNull();
        assertThat(lagretSporingslogg).isEqualToIgnoringNullFields(sporingslogg);
    }
}