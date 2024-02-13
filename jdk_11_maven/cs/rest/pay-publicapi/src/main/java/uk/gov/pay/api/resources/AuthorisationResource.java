package uk.gov.pay.api.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.model.AuthorisationRequest;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.resources.error.ApiErrorResponse;
import uk.gov.pay.api.service.AuthorisationService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
@Produces({"application/json"})
@Tag(name = "Authorise card payments")
public class AuthorisationResource {

    private static final Logger logger = LoggerFactory.getLogger(AuthorisationResource.class);
    private final AuthorisationService authorisationService;

    @Inject
    public AuthorisationResource(AuthorisationService authorisationService) {
        this.authorisationService = authorisationService;
    }

    @POST
    @Timed
    @Path("/v1/auth")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "Authorise a MOTO payment",
            summary = "Send card details to authorise a MOTO payment",
            description = "You can use this endpoint to [authorise payments](https://docs.payments.service.gov.uk/moto_payments/moto_send_card_details_api/) you have created with `authorisation_mode` set to `moto_api`.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Your authorisation request was successful."),
                    @ApiResponse(responseCode = "400", description = "Your request is invalid. Check the `code` and `description` in the response to find out why your request failed.",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "402",
                            description = "The `card_number` you sent is not a valid card number or you chose not to accept this card type. Check the `code` and `description` fields in the response to find out why your request failed.",
                            content = @Content(schema = @Schema(implementation = RequestError.class))),
                    @ApiResponse(responseCode = "422", description = "A value you sent is invalid or missing. Check the `code` and `description` in the response to find out why your request failed.",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "There is something wrong with GOV.UK Pay. If there are no issues on our status page (https://payments.statuspage.io), you can contact us with your error code and we'll investigate.",
                            content = @Content(schema = @Schema(implementation = RequestError.class)))
            }
    )
    public Response authorisePayment(@Parameter(required = true)
                                     @Valid AuthorisationRequest authorisationRequest) {
        return authorisationService.authoriseRequest(authorisationRequest);
    }
}
