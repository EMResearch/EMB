package uk.gov.pay.api.resources;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonassert.JsonAssert;
import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthCheckResourceTest {
    @Mock
    private Environment environment;

    @Mock
    private HealthCheckRegistry healthCheckRegistry;

    private HealthCheckResource resource;

    @BeforeEach
    public void setup() {
        when(environment.healthChecks()).thenReturn(healthCheckRegistry);
        resource = new HealthCheckResource(environment);
    }

    @Test
    public void checkHealthCheck_isUnHealthy() throws JsonProcessingException {
        SortedMap<String,HealthCheck.Result> map = new TreeMap<>();
        map.put("ping", HealthCheck.Result.unhealthy("application is unavailable"));
        map.put("deadlocks", HealthCheck.Result.unhealthy("no new threads available"));

        when(healthCheckRegistry.runHealthChecks()).thenReturn(map);

        Response response = resource.healthCheck();

        assertThat(response.getStatus(), is(503));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.getEntity());
        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.ping.healthy", is(false))
                .assertThat("$.deadlocks.healthy", is(false));
    }

    @Test
    public void checkHealthCheck_isHealthy() throws JsonProcessingException {
        SortedMap<String,HealthCheck.Result> map = new TreeMap<>();
        map.put("ping", HealthCheck.Result.healthy());
        map.put("deadlocks", HealthCheck.Result.healthy());

        when(healthCheckRegistry.runHealthChecks()).thenReturn(map);

        Response response = resource.healthCheck();

        assertThat(response.getStatus(), is(200));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.getEntity());

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.ping.healthy", is(true))
                .assertThat("$.deadlocks.healthy", is(true));
    }

    @Test
    public void checkHealthCheck_isUnhealthyIfPartiallyHealthy() throws JsonProcessingException {
        SortedMap<String,HealthCheck.Result> map = new TreeMap<>();
        map.put("ping", HealthCheck.Result.healthy());
        map.put("deadlocks", HealthCheck.Result.unhealthy("no new threads available"));

        when(healthCheckRegistry.runHealthChecks()).thenReturn(map);

        Response response = resource.healthCheck();

        assertThat(response.getStatus(), is(503));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.getEntity());

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.ping.healthy", is(true))
                .assertThat("$.deadlocks.healthy", is(false));
    }

}
