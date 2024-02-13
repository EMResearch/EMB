package uk.gov.pay.api.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static uk.gov.pay.api.model.RequestError.Code.REQUEST_DENIED_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

@Path("/")
public class RequestDeniedResource {

    private static final Logger logger = LoggerFactory.getLogger(RequestDeniedResource.class);

    @GET
    @Timed
    @Path("request-denied")
    @Produces(APPLICATION_JSON)
    public Response requestDeniedGet(@HeaderParam("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    @POST
    @Timed
    @Path("request-denied")
    @Produces(APPLICATION_JSON)
    public Response requestDeniedPost(@HeaderParam("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    @PUT
    @Timed
    @Path("request-denied")
    @Produces(APPLICATION_JSON)
    public Response requestDeniedPut(@HeaderParam("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    @DELETE
    @Timed
    @Path("request-denied")
    @Produces(APPLICATION_JSON)
    public Response requestDeniedDelete(@HeaderParam("x-naxsi_sig") String naxsiViolatedRules) {
        return requestDenied(naxsiViolatedRules);
    }

    private Response requestDenied(@HeaderParam("x-naxsi_sig") String naxsiViolatedRules) {
        logger.info("Naxsi rules violated - [ {} ]", naxsiViolatedRules);
        return Response.status(BAD_REQUEST).entity(aRequestError(REQUEST_DENIED_ERROR)).build();
    }
}
