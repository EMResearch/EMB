package uk.gov.pay.api.filter.ratelimit;

import java.time.Duration;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.MILLIS;

final class RateLimit {

    private final int noOfReq;
    private final int perMillis;

    private Instant created;
    private int requestCount;

    RateLimit(int noOfReq, int perMillis) {
        this.noOfReq = noOfReq;
        this.perMillis = perMillis;
        this.requestCount = 0;
        this.created = Instant.now().truncatedTo(MILLIS);
    }

    /**
     * This block needs to be synchronous. Each RateLimit object will be shared between requests
     * from the same source (Service), so is not shared across all the requests.
     *
     * @throws RateLimitException
     */
    synchronized void updateAllowance() throws RateLimitException {
        requestCount += 1;
        Instant now = Instant.now().truncatedTo(MILLIS);
        if (Duration.between(created, now).toMillis() >= perMillis) {
            requestCount = 1;
            created = now;
        }
        if (requestCount > noOfReq) {
            throw new RateLimitException();
        }
    }

    public int getNoOfReq() {
        return noOfReq;
    }

    public int getRequestCount() {
        return requestCount;
    }
}
