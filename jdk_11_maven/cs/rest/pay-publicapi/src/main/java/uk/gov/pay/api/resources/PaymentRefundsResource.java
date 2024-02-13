package uk.gov.pay.api.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.RefundsResponse;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.model.search.card.RefundForSearchResult;
import uk.gov.pay.api.model.search.card.RefundResult;
import uk.gov.pay.api.resources.error.ApiErrorResponse;
import uk.gov.pay.api.service.ConnectorService;
import uk.gov.pay.api.service.CreateRefundService;
import uk.gov.pay.api.service.GetPaymentRefundService;
import uk.gov.pay.api.service.GetPaymentRefundsService;
import uk.gov.pay.api.service.GetPaymentService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_200_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_401_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_404_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_429_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_500_DESCRIPTION;

@Path("/v1/payments/{paymentId}/refunds")
@Tag(name = "Refunding card payments")
@Produces({"application/json"})
public class PaymentRefundsResource {
    private static final Logger logger = LoggerFactory.getLogger(PaymentRefundsResource.class);

    private final GetPaymentRefundsService getPaymentRefundsService;
    private final GetPaymentRefundService getPaymentRefundService;
    private final CreateRefundService createRefundService;

    @Inject
    public PaymentRefundsResource(PublicApiConfig configuration,
                                  GetPaymentRefundsService getPaymentRefundsService,
                                  GetPaymentRefundService getPaymentRefundService,
                                  ConnectorService connectorService,
                                  GetPaymentService getPaymentService,
                                  CreateRefundService createRefundService) {
        this.getPaymentRefundsService = getPaymentRefundsService;
        this.getPaymentRefundService = getPaymentRefundService;
        this.createRefundService = createRefundService;
    }

    @GET
    @Timed
    @Produces(APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get all refunds for a payment",
            summary = "Get information about a payment’s refunds",
            description = "You can use this endpoint to [get a list of refunds for a payment]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments/#get-all-refunds-for-a-single-payment).",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RefundForSearchResult.class))),
                    @ApiResponse(responseCode = "401", description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public RefundsResponse getRefunds(@Parameter(hidden = true) @Auth Account account,
                                      @PathParam("paymentId") @Parameter(name = "paymentId", 
                                              description = "The unique `payment_id` of the payment you want a list of refunds for.") String paymentId,
                                      @Parameter(hidden = true) @HeaderParam("X-Ledger") String strategyName) {

        logger.info("Get refunds for payment request - paymentId={} using strategy={}", paymentId, strategyName);

        GetPaymentRefundsStrategy strategy = new GetPaymentRefundsStrategy(strategyName, account, paymentId, getPaymentRefundsService);
        RefundsResponse refundsResponse = strategy.validateAndExecute();

        logger.debug("refund returned - [ {} ]", refundsResponse);
        return refundsResponse;
    }

    @GET
    @Timed
    @Path("/{refundId}")
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get a payment refund",
            summary = "Check the status of a refund",
            description = "You can use this endpoint to [get details about an individual refund]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments/#checking-the-status-of-a-refund).",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RefundResult.class))),
                    @ApiResponse(responseCode = "401", description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    @Produces(APPLICATION_JSON)
    public RefundResponse getRefundById(@Parameter(hidden = true)
                                        @Auth Account account,
                                        @PathParam("paymentId") @Parameter(name = "paymentId",
                                                description = "The unique `payment_id` of the payment you want to view a refund of.") String paymentId,
                                        @PathParam("refundId") @Parameter(name = "refundId",
                                                description = "The unique `refund_id` of the refund you want to view. " + 
                                                        "If one payment has multiple refunds, each refund has a different `refund_id`.") String refundId,
                                        @Parameter(hidden = true) @HeaderParam("X-Ledger") String strategyName) {

        logger.info("Payment refund request - paymentId={}, refundId={}", paymentId, refundId);

        var strategy = new GetPaymentRefundStrategy(strategyName, account, paymentId, refundId, getPaymentRefundService);
        RefundResponse refundResponse = strategy.validateAndExecute();

        logger.info("refund returned - [ {} ]", refundResponse);

        return refundResponse;
    }

    @POST
    @Timed
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Submit a refund for a payment",
            summary = "Refund a payment",
            description = "You can use this endpoint to [fully or partially refund a payment]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "successful operation",
                            content = @Content(schema = @Schema(implementation = RefundResult.class))),
                    @ApiResponse(responseCode = "202", description = "ACCEPTED"),
                    @ApiResponse(responseCode = "401", description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "412", description = "Refund amount available mismatch"),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public Response submitRefund(@Parameter(hidden = true) @Auth Account account,
                                 @PathParam("paymentId") @Parameter(name = "paymentId",
                                         description = "The unique `payment_id` of the payment you want to refund.") String paymentId,
                                 @Parameter(required = true, description = "requestPayload")
                                 CreatePaymentRefundRequest requestPayload) {

        logger.info("Create a refund for payment request - paymentId={}", paymentId);
        RefundResponse refundResponse = createRefundService.createRefund(account, paymentId, requestPayload);
        return Response.accepted(refundResponse).build();
    }
}
