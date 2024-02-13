package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.RefusjonStatus;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriode;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriodeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefusjonEndretStatusTest {

    @Test
    public void skal_kunne_finne_riktig_tilskuddsperiode_og_lagre_status_uten_å_kaste_en_feil() throws JsonProcessingException {
        // GITT
        TilskuddPeriodeRepository tilskuddPeriodeRepository = mock(TilskuddPeriodeRepository.class);
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        when(tilskuddPeriodeRepository.findById(any())).thenReturn(Optional.of(tilskuddPeriode));

        // NÅR
        RefusjonEndretStatusKafkaConsumer consumer = new RefusjonEndretStatusKafkaConsumer(tilskuddPeriodeRepository);

        consumer.refusjonEndretStatus(new RefusjonEndretStatusMelding("1234", "1234", "1234", RefusjonStatus.UTBETALT, tilskuddPeriode.getId().toString()));

        // SÅ
        verify(tilskuddPeriodeRepository).save(any());
    }

}
