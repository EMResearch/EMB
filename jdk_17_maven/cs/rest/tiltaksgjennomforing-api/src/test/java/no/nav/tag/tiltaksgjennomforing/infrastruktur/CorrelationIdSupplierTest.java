package no.nav.tag.tiltaksgjennomforing.infrastruktur;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CorrelationIdSupplierTest {
    @Test
    public void set__egendefinert_token_kan_returneres_flere_ganger() {
        String egendefinertToken = "foo";
        CorrelationIdSupplier.set(egendefinertToken);
        String token1 = CorrelationIdSupplier.get();
        String token2 = CorrelationIdSupplier.get();
        assertThat(token1).isEqualTo(egendefinertToken);
        assertThat(token2).isEqualTo(egendefinertToken);
    }

    @Test
    public void set__blank_fungerer_ikke() {
        assertThatThrownBy(() -> CorrelationIdSupplier.set("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void generate__token_kan_returneres_flere_ganger() {
        CorrelationIdSupplier.generateToken();
        String token1 = CorrelationIdSupplier.get();
        String token2 = CorrelationIdSupplier.get();
        assertThat(token1).isNotNull().isEqualTo(token2);
    }

    @Test
    public void generate__ny_genereres() {
        CorrelationIdSupplier.generateToken();
        String token1 = CorrelationIdSupplier.get();
        CorrelationIdSupplier.generateToken();
        String token2 = CorrelationIdSupplier.get();
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    public void remove__fjerner_token() {
        CorrelationIdSupplier.generateToken();
        String token1 = CorrelationIdSupplier.get();
        CorrelationIdSupplier.remove();
        String token2 = CorrelationIdSupplier.get();
        assertThat(token1).isNotNull();
        assertThat(token2).isNull();
    }
}