package uk.gov.pay.api.it;

import com.google.common.collect.ImmutableMap;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class ResourcesFilterAuthorisationIT extends ResourcesFilterITestBase {

    @Test
    public void createPayment_whenInvalidAuthorizationHeader_shouldReturn401Response() throws Exception {

        List<Callable<ValidatableResponse>> tasks = Arrays.asList(
                () -> postPaymentResponse("InvalidToken", PAYLOAD),
                () -> postPaymentResponse("InvalidToken", PAYLOAD)
        );

        List<ValidatableResponse> finishedTasks = invokeAll(tasks);

        finishedTasks.get(0).statusCode(401);
        finishedTasks.get(1).statusCode(401);
    }

    @Test
    public void getPayment_whenInvalidAuthorizationHeader_shouldReturn401Response() throws Exception {

        List<Callable<ValidatableResponse>> tasks = Arrays.asList(
                () -> getPaymentResponse("InvalidToken2"),
                () -> getPaymentResponse("InvalidToken2")
        );

        List<ValidatableResponse> finishedTasks = invokeAll(tasks);

        finishedTasks.get(0).statusCode(401);
        finishedTasks.get(1).statusCode(401);
    }

    @Test
    public void getPaymentEvents_whenInvalidAuthorizationHeader_shouldReturn401Response() throws Exception {

        List<Callable<ValidatableResponse>> tasks = Arrays.asList(
                () -> getPaymentEventsResponse("InvalidToken3"),
                () -> getPaymentEventsResponse("InvalidToken3")
        );

        List<ValidatableResponse> finishedTasks = invokeAll(tasks);

        finishedTasks.get(0).statusCode(401);
        finishedTasks.get(1).statusCode(401);
    }

    @Test
    public void searchPayments_whenInvalidAuthorizationHeader_shouldReturn401Response() throws Exception {

        List<Callable<ValidatableResponse>> tasks = Arrays.asList(
                () -> searchPayments("InvalidToken5", ImmutableMap.of("reference", REFERENCE)),
                () -> searchPayments("InvalidToken5", ImmutableMap.of("reference", REFERENCE))
        );

        List<ValidatableResponse> finishedTasks = invokeAll(tasks);

        finishedTasks.get(0).statusCode(401);
        finishedTasks.get(1).statusCode(401);
    }

    @Test
    public void cancelPayment_whenInvalidAuthorizationHeader_shouldReturn401Response() throws Exception {

        List<Callable<ValidatableResponse>> tasks = Arrays.asList(
                () -> postCancelPaymentResponse("InvalidToken6"),
                () -> postCancelPaymentResponse("InvalidToken6")
        );

        List<ValidatableResponse> finishedTasks = invokeAll(tasks);

        finishedTasks.get(0).statusCode(401);
        finishedTasks.get(1).statusCode(401);
    }

}
