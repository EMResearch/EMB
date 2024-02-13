package no.nav.tag.tiltaksgjennomforing;

import lombok.experimental.UtilityClass;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;

@UtilityClass
public class AssertFeilkode {
    public static void assertFeilkode(Feilkode feilkode, Runnable shouldRaiseThrowable) {
        try {
            shouldRaiseThrowable.run();
            failBecauseExceptionWasNotThrown(FeilkodeException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FeilkodeException.class).extracting(throwable -> ((FeilkodeException) throwable).getFeilkode()).isEqualTo(feilkode);
        }
    }
}
