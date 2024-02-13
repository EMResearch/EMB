package uk.gov.pay.api.agreement.resource;

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
import uk.gov.pay.api.agreement.model.Agreement;
import uk.gov.pay.api.agreement.model.AgreementSearchResults;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.agreement.service.AgreementsService;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.resources.error.ApiErrorResponse;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_200_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_201_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_400_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_401_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_404_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_422_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_429_DESCRIPTION;
import static uk.gov.pay.api.common.ResponseConstants.RESPONSE_500_DESCRIPTION;

@Path("/")
@Tag(name = "Agreements")
@Produces({"application/json"})
public class AgreementsApiResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgreementsApiResource.class);

    private final AgreementsService agreementsService;

    @Inject
    public AgreementsApiResource(AgreementsService agreementsService) {
        this.agreementsService = agreementsService;
    }

    @POST
    @Path("/v1/agreements")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Create an agreement",
            summary = "Create an agreement for recurring payments",
            description = "You can use this endpoint to create a new agreement.",
            responses = {
                    @ApiResponse(responseCode = "201", description = RESPONSE_201_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = Agreement.class))),
                    @ApiResponse(responseCode = "400", description = RESPONSE_400_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "422",
                            description = RESPONSE_422_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public Response createAgreement(
            @Parameter(hidden = true) @Auth Account account,
            @Parameter(required = true, description = "requestPayload")
            @Valid CreateAgreementRequest createAgreementRequest)
    {
        LOGGER.info("Creating new agreement for reference {} and gateway accountID {}", 
                createAgreementRequest.getReference(), account.getAccountId());
        var agreementCreatedResponse = agreementsService.createAgreement(account, createAgreementRequest);
        var agreementLedgerResponse = agreementsService.getAgreement(account, agreementCreatedResponse.getAgreementId());
        
        LOGGER.info("Agreement returned (created): [ {} ]", agreementCreatedResponse);
        return Response.status(SC_CREATED).entity(Agreement.from(agreementLedgerResponse)).build();
    }

    @GET
    @Path("/v1/agreements/{agreementId}")
    @Produces(APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Get an agreement",
            summary = "Get information about a single agreement for recurring payments",
            description = "You can use this endpoint to get information about a single recurring payments agreement.",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = Agreement.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public Agreement getAgreement(
            @Parameter(hidden = true) @Auth Account account,
            @PathParam("agreementId") 
            @Parameter(name = "agreementId", 
                    description = "Returns the agreement with the matching `agreement_id`. " +
                            "GOV.UK Pay generated an `agreement_id` when you created the agreement.", 
                    example = "cgc1ocvh0pt9fqs0ma67r42l58") String agreementId)
    {
        LOGGER.info("Get agreement {} request", agreementId);
        var agreementLedgerResponse = agreementsService.getAgreement(account, agreementId);
        return Agreement.from(agreementLedgerResponse);
    }

    @GET
    @Timed
    @Path("/v1/agreements")
    @Produces(APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Search agreements",
            summary = "Search agreements for recurring payments",
            description = "You can use this endpoint to search for recurring payments agreements. " +
                    "The agreements are sorted by date, with the most recently-created agreements appearing first.",
            responses = {
                    @ApiResponse(responseCode = "200", description = RESPONSE_200_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = AgreementSearchResults.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "422",
                            description = RESPONSE_422_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public AgreementSearchResults getAgreements(@Parameter(hidden = true) @Auth Account account, @BeanParam AgreementSearchParams searchParams) {
        return agreementsService.searchAgreements(account, searchParams);
    }

    @POST
    @Path("/v1/agreements/{agreementId}/cancel")
    @Produces(APPLICATION_JSON)
    @Operation(security = {@SecurityRequirement(name = "BearerAuth")},
            operationId = "Cancel an agreement",
            summary = "Cancel an agreement for recurring payments",
            description = "You can use this endpoint to cancel a recurring payments agreement in the `active` status.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content"),
                    @ApiResponse(responseCode = "400", description = "Cancellation of agreement failed",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "401",
                            description = RESPONSE_401_DESCRIPTION),
                    @ApiResponse(responseCode = "404", description = RESPONSE_404_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "429", description = RESPONSE_429_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RESPONSE_500_DESCRIPTION,
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public Response cancelAgreement(@Parameter(hidden = true) @Auth Account account, @PathParam("agreementId") @Parameter(name = "agreementId",
            description = "The `agreement_id` of the agreement you are cancelling",
            example = "cgc1ocvh0pt9fqs0ma67r42l58") String agreementId)
    {
        agreementsService.cancelAgreement(account, agreementId);
        return Response.status(SC_NO_CONTENT).build();
    }

}
