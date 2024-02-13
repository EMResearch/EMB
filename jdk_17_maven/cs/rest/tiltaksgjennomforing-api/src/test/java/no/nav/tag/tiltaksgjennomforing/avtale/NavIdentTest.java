package no.nav.tag.tiltaksgjennomforing.avtale;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NavIdentTest {
    @ParameterizedTest
    @CsvSource(value = {
            "xxxxxx,false",
            "X12345,false",
            "00000000000,false",
            "912345,false",
            "X123456,true",
            "x123456,true",
    })
    void test_erNavIdent(String input, boolean expected) {
        assertThat(NavIdent.erNavIdent(input)).isEqualTo(expected);
    }
}