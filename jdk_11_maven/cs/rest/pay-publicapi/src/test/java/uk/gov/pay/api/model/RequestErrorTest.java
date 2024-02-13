package uk.gov.pay.api.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class RequestErrorTest {

    @Test
    void shouldGetExpectedValuesWhenToStringIsCalled() {
        RequestError requestError = aRequestError(RequestError.Code.CREATE_PAYMENT_ACCOUNT_ERROR);
        assertThat(requestError.toString(), is("RequestError{field=null, code=P0199, name=CREATE_PAYMENT_ACCOUNT_ERROR, description='There is an error with this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ .'}"));
    }

    @Test
    void shouldGetExpectedValuesForAgreementNotFoundError() {
        RequestError requestError = aRequestError("set_up_agreement", RequestError.Code.CREATE_PAYMENT_AGREEMENT_ID_ERROR);
        assertThat(requestError.toString(), is("RequestError{field=set_up_agreement, code=P0103, name=CREATE_PAYMENT_AGREEMENT_ID_ERROR, description='Invalid attribute value: set_up_agreement. Agreement ID does not exist'}"));
    }

}
