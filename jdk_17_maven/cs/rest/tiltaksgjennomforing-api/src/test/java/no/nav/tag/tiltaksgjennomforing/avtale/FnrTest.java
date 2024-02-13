package no.nav.tag.tiltaksgjennomforing.avtale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import no.nav.tag.tiltaksgjennomforing.exceptions.TiltaksgjennomforingException;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Test;

public class FnrTest {

    @Test
    public void fnrKanVæreNull(){
        assertThat(new Fnr(null)).isEqualTo(new Fnr(null));
    }
    @Test
    public void fnrSkalIkkeVaereTomt() {
        assertThatThrownBy(() -> new Fnr("")).isExactlyInstanceOf(TiltaksgjennomforingException.class);
    }

    @Test
    public void fnrSkalIkkeHaMindreEnn11Siffer() {
        assertThatThrownBy(() -> new Fnr("123")).isExactlyInstanceOf(TiltaksgjennomforingException.class);
    }

    @Test
    public void fnrSkalIkkeHaMerEnn11Siffer() {
        assertThatThrownBy(() -> new Fnr("1234567890123")).isExactlyInstanceOf(TiltaksgjennomforingException.class);
    }

    @Test
    public void fnrSkalIkkeInneholdeBokstaver() {
        assertThatThrownBy(() -> new Fnr("1234567890a")).isExactlyInstanceOf(TiltaksgjennomforingException.class);
    }

    @Test
    public void fnrSkalIkkeInneholdeAndreTingEnnTall() {
        assertThatThrownBy(() -> new Fnr("12345678900 ")).isExactlyInstanceOf(TiltaksgjennomforingException.class);
    }

    @Test
    public void fnrSkalInneholde11Tall() {
        String gyldigFnr = "01234567890";
        assertThat(new Fnr(gyldigFnr).asString()).isEqualTo(gyldigFnr);
    }

    @Test
    public void testFnr1() {
        Fnr fnrOver16 = new Fnr("29110976648");
        assertThat(fnrOver16.erUnder16år()).isTrue();
        assertThat(fnrOver16.erOver30år()).isFalse();
    }

    @Test
    public void testFnr2() {
        Fnr fnr = new Fnr("19109613897");
        assertThat(fnr.erUnder16år()).isFalse();
        assertThat(fnr.erOver30år()).isFalse();
    }

    @Test
    public void testFnr3() {
        Fnr fnr = new Fnr("25128626630");
        assertThat(fnr.erOver30år()).isTrue();
        assertThat(fnr.erUnder16år()).isFalse();
    }

    @Test
    public void testFnr4() {
        Now.fixedDate(LocalDate.of(2021, 12, 20));
        Fnr fnr = new Fnr("23029149054");
        assertThat(fnr.erOver30årFørsteJanuar()).isFalse();
        assertThat(fnr.erUnder16år()).isFalse();
        Now.resetClock();
    }

    @Test
    public void testFnr5() {
        Now.fixedDate(LocalDate.of(2021, 12, 20));
        final Fnr fnr = new Fnr("23029149054");
        LocalDate startDato = LocalDate.of(2022, 1, 5);
        assertThat(fnr.erOver30årFørsteJanuar()).isFalse();
        assertThat(fnr.erOver30årFraOppstartDato(startDato)).isTrue();
        Now.resetClock();
    }

    @Test
    public void testDnr1() {
        Fnr fnr = new Fnr("49120799125");
        assertThat(fnr.erUnder16år()).isTrue();
        assertThat(fnr.erOver30år()).isFalse();
    }

    @Test
    public void testDnr2() {
        Fnr fnr = new Fnr("64090099076");
        assertThat(fnr.erUnder16år()).isFalse();
        assertThat(fnr.erOver30år()).isFalse();
    }

    @Test
    void testOver67År() {
        Fnr fnr = new Fnr("30015521534");

        Now.fixedDate(LocalDate.of(2022, 1, 29));
        assertThat(fnr.erOver67ÅrFraSluttDato(Now.localDate())).isFalse();

        Now.fixedDate(LocalDate.of(2022, 1, 30));
        assertThat(fnr.erOver67ÅrFraSluttDato(Now.localDate())).isTrue();

        Now.resetClock();
    }

    @Test
    void testAtAldersjekkKanGjøresPåSyntetiskFnr() {
        Fnr fnr = new Fnr("07459742977");
        assertThat(fnr.erUnder16år()).isFalse();
        assertThat(fnr.erOver30år()).isFalse();
    }

    @Test
    void testAtAldersjekkKanGjøresPåSyntetiskFnrFraSkatteEtaten() {
        Fnr fnr = new Fnr("21899797180");
        assertThat(fnr.erUnder16år()).isFalse();
        assertThat(fnr.erOver30år()).isFalse();
    }
}



