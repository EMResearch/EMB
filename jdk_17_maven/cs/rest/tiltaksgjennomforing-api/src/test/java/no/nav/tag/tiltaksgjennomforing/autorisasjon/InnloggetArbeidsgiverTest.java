package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.*;

import no.nav.tag.tiltaksgjennomforing.avtale.Arbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InnloggetArbeidsgiverTest {

    @Mock
    public AvtaleRepository avtaleRepository;

    Avtale avtale = TestData.enArbeidstreningAvtale();
    Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);

    @BeforeEach
    public void setUp(){
        avtale.setAnnullertGrunn("Hemmelig");
    }

    @Test
    public void hentAvtale_uten_annullertGrunn() {
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        Avtale hentetAvtale = arbeidsgiver.hentAvtale(avtaleRepository, avtale.getId());
        assertThat(hentetAvtale.getAnnullertGrunn()).isNull();
    }

    @Test
    public void hentAvtalerForMinsideArbeidsgiver_uten_annullertGrunn() {
        when(avtaleRepository.findAllByBedriftNr(eq(avtale.getBedriftNr()))).thenReturn(Arrays.asList(avtale));
        List<Avtale> hentetAvtaler = arbeidsgiver.hentAvtalerForMinsideArbeidsgiver(avtaleRepository, avtale.getBedriftNr());
        assertThat(hentetAvtaler.get(0).getAnnullertGrunn()).isNull();
    }
}
