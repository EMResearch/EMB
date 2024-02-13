package uk.gov.pay.api.it.validation.PublicApiConfigIT;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RateLimiterConfig;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PublicApiConfigIT {

    @Rule
    public final DropwizardAppRule<PublicApiConfig> RULE =
            new DropwizardAppRule<>(PublicApi.class, ResourceHelpers.resourceFilePath("config/test-config.yaml"));

    @Test
    public void shouldParseConfiguration() {
        RateLimiterConfig rateLimiterConfig = RULE.getConfiguration().getRateLimiterConfig();
        assertThat(rateLimiterConfig.getNoOfReq(), is(1000));
        assertThat(rateLimiterConfig.getPerMillis(), is(1000));
        assertThat(rateLimiterConfig.getNoOfReqForPost(), is(1000));
        assertThat(rateLimiterConfig.getNoOfReqPerNode(), is(1));
        assertThat(rateLimiterConfig.getNoOfReqForPostPerNode(), is(1));
        assertThat(rateLimiterConfig.getNoOfReqForElevatedAccounts(), is(1000));
        assertThat(rateLimiterConfig.getNoOfPostReqForElevatedAccounts(), is(1000));
        assertThat(rateLimiterConfig.getElevatedAccounts(), is(List.of("1", "2", "3")));
    }
}
