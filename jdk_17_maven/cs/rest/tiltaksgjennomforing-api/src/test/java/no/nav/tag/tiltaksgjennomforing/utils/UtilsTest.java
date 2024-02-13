package no.nav.tag.tiltaksgjennomforing.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {
    @Test
    public void erIkkeTomme__med_null() {
        assertThat(Utils.erIkkeTomme(1, "k", null)).isFalse();
    }

    @Test
    public void erIkkeTomme__med_tom_streng() {
        assertThat(Utils.erIkkeTomme(1, "k", "")).isFalse();
    }

    @Test
    public void erIkkeTomme__uten_null() {
        assertThat(Utils.erIkkeTomme(1, "k", new Object())).isTrue();
    }
}