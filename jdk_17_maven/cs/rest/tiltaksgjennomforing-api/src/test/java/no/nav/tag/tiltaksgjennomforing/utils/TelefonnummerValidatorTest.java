package no.nav.tag.tiltaksgjennomforing.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.tag.tiltaksgjennomforing.utils.TelefonnummerValidator.erGyldigMobilnummer;
import static org.assertj.core.api.Assertions.assertThat;

public class TelefonnummerValidatorTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"9", "4", "22222222", "433333333333", "9x999999", "92222222 "})
    void erGyldigMobilnummer__false(String tlf) {
        assertThat(erGyldigMobilnummer(tlf)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"92222222", "44444444"})
    void erGyldigMobilnummer__true(String tlf) {
        assertThat(erGyldigMobilnummer(tlf)).isTrue();
    }
}