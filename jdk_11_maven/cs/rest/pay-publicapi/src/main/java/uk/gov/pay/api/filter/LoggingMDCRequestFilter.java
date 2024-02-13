package uk.gov.pay.api.filter;

import org.slf4j.MDC;
import uk.gov.pay.api.auth.Account;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.security.Principal;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.service.payments.logging.LoggingKeys.AGREEMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.REFUND_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.REMOTE_ADDRESS;

@Provider
@Priority(Priorities.USER)
public class LoggingMDCRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Optional<Account> mayBeAccount = getAccount(requestContext);
        MDC.put(GATEWAY_ACCOUNT_ID, mayBeAccount.map(Account::getName).orElse(EMPTY));
        mayBeAccount.ifPresent(account -> MDC.put("token_link", account.getTokenLink()));

        String clientAddress = getClientAddress(requestContext);
        MDC.put(REMOTE_ADDRESS, clientAddress);

        getPathParameterFromRequest("paymentId", requestContext)
                .ifPresent(paymentId -> MDC.put(PAYMENT_EXTERNAL_ID, paymentId));
        getPathParameterFromRequest("refundId", requestContext)
                .ifPresent(refundId -> MDC.put(REFUND_EXTERNAL_ID, refundId));
        getPathParameterFromRequest("agreementId", requestContext)
                .ifPresent(agreementId -> MDC.put(AGREEMENT_EXTERNAL_ID, agreementId));
    }

    private String getClientAddress(ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getHeaderString("X-Forwarded-For"))
                .map(forwarded -> forwarded.split(",")[0])
                .orElse(null);
    }

    private Optional<String> getPathParameterFromRequest(String parameterName, ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getUriInfo().getPathParameters().getFirst(parameterName));
    }

    private Optional<Account> getAccount(ContainerRequestContext requestContext) {
        Optional<Principal> userPrincipal = Optional.ofNullable(requestContext.getSecurityContext().getUserPrincipal());
        return userPrincipal.map(principal -> (Account) principal);
    }
}
