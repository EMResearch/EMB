package no.nav.tag.tiltaksgjennomforing.infrastruktur;

import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Unprotected
public class HealthCheckController {
    private final JdbcTemplate jdbcTemplate;

    public HealthCheckController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping(value = "/internal/healthcheck")
    public String healthcheck() {
        return jdbcTemplate.queryForObject("select 'ok'", String.class);
    }
}
