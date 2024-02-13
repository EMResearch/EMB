package no.nav.tag.tiltaksgjennomforing.featuretoggles;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.tag.tiltaksgjennomforing.Miljø.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ByEnvironmentStrategyTest {
    @Test
    public void featureIsEnabledWhenEnvironmentInList() {
        assertThat(new ByEnvironmentStrategy(LOCAL).isEnabled(Map.of("miljø", "local,dev-fss"))).isEqualTo(true);
    }

    @Test
    public void featureIsEnabledWhenLocalEnvironmentInList() {
        assertThat(new ByEnvironmentStrategy(LOCAL).isEnabled(Map.of("miljø", "local"))).isEqualTo(true);
    }

    @Test
    public void featureIsDisabledWhenEnvironmentNotInList() {
        assertThat(new ByEnvironmentStrategy(PROD_FSS).isEnabled(Map.of("miljø", "local"))).isEqualTo(false);
    }

    @Test
    public void skalReturnereFalseHvisParametreErNull() {
        assertThat(new ByEnvironmentStrategy(DEV_FSS).isEnabled(null)).isEqualTo(false);
    }

    @Test
    public void skalReturnereFalseHvisMiljøIkkeErSatt() {
        assertThat(new ByEnvironmentStrategy(DEV_FSS).isEnabled(new HashMap<>())).isEqualTo(false);
    }
}
