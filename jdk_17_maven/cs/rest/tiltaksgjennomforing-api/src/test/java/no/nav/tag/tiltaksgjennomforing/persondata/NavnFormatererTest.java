package no.nav.tag.tiltaksgjennomforing.persondata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NavnFormatererTest {
    @Test
    void bare_fornavn_og_etternavn() {
        NavnFormaterer navnFormaterer = new NavnFormaterer(new Navn("FOO", null, "BAR"));
        assertThat(navnFormaterer.getFornavn()).isEqualTo("Foo");
        assertThat(navnFormaterer.getEtternavn()).isEqualTo("Bar");
    }

    @Test
    void fornavn_mellomnavn_og_etternavn() {
        NavnFormaterer navnFormaterer = new NavnFormaterer(new Navn("FOO", "BAR", "BAZ"));
        assertThat(navnFormaterer.getFornavn()).isEqualTo("Foo Bar");
        assertThat(navnFormaterer.getEtternavn()).isEqualTo("Baz");
    }

    @Test
    void navn_med_bindestrek() {
        NavnFormaterer navnFormaterer = new NavnFormaterer(new Navn("FOO-BAR", "BARNEY BOO", "BAZZ-Y BAG"));
        assertThat(navnFormaterer.getFornavn()).isEqualTo("Foo-Bar Barney Boo");
        assertThat(navnFormaterer.getEtternavn()).isEqualTo("Bazz-Y Bag");
    }
}