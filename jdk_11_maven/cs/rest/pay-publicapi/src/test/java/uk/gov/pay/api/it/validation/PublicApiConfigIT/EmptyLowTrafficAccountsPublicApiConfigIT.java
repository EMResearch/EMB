package uk.gov.pay.api.it.validation.PublicApiConfigIT;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RateLimiterConfig;

import java.util.Collections;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EmptyLowTrafficAccountsPublicApiConfigIT {

    @Rule
    public final DropwizardAppRule<PublicApiConfig> RULE = new DropwizardAppRule<>(PublicApi.class,
            resourceFilePath("config/empty-low-traffic-accounts-test-config.yaml"));

    @Test
    public void shouldParseRateLimitConfigurationForLowTrafficAccounts() {
        RateLimiterConfig rateLimiterConfig = RULE.getConfiguration().getRateLimiterConfig();
        assertThat(rateLimiterConfig.getNoOfReqForLowTrafficAccounts(), is(4500));
        assertThat(rateLimiterConfig.getNoOfPostReqForLowTrafficAccounts(), is(2));
        assertThat(rateLimiterConfig.getIntervalInMillisForLowTrafficAccounts(), is(60000));
        assertThat(rateLimiterConfig.getLowTrafficAccounts(), is(Collections.emptyList()));
    }
}
