package uk.gov.pay.api.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PathHelperTest {

    @ParameterizedTest
    @MethodSource("rateLimitParams")
    public void returnsPathType(String path, String method, String pathType) {
        assertThat(PathHelper.getPathType(path, method), is(pathType));
    }

    static Stream<Arguments> rateLimitParams() {
        return Stream.of(
                arguments("/v1/payments", "POST", "create_payment"),
                arguments("/v1/payments/", "POST", "create_payment"),
                arguments("/v1/payments/paymentId/capture", "POST", "capture_payment"),
                arguments("/v1/payments/paymentId/capture/", "POST", "capture_payment"),
                arguments("/v1/payments/paymentId/cancel", "POST", ""),
                arguments("/v1/payments", "GET", "")
        );
    }
}
