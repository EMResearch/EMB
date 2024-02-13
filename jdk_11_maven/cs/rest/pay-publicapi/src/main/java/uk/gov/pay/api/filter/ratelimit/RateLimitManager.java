package uk.gov.pay.api.filter.ratelimit;

import uk.gov.pay.api.app.config.RateLimiterConfig;
import uk.gov.pay.api.filter.RateLimiterKey;

import javax.ws.rs.HttpMethod;

public class RateLimitManager {

    private RateLimiterConfig configuration;

    public RateLimitManager(RateLimiterConfig config) {
        configuration = config;
    }

    public int getAllowedNumberOfRequests(RateLimiterKey rateLimiterKey, String account) {
        if (configuration.getElevatedAccounts().contains(account)) {
            if (HttpMethod.POST.equals(rateLimiterKey.getMethod())) {
                return configuration.getNoOfPostReqForElevatedAccounts();
            }

            return configuration.getNoOfReqForElevatedAccounts();
        }

        if (configuration.getLowTrafficAccounts().contains(account)) {
            if (HttpMethod.POST.equals(rateLimiterKey.getMethod())) {
                return configuration.getNoOfPostReqForLowTrafficAccounts();
            }
            return configuration.getNoOfReqForLowTrafficAccounts();
        }

        if (HttpMethod.POST.equals(rateLimiterKey.getMethod())) {
            return configuration.getNoOfReqForPost();
        }

        return configuration.getNoOfReq();
    }

    public int getRateLimitInterval(String account) {
        if (configuration.getLowTrafficAccounts().contains(account)) {
            return configuration.getIntervalInMillisForLowTrafficAccounts();
        }

        return configuration.getPerMillis();
    }
}
