package uk.gov.pay.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;

@Path("/")
public class SecuritytxtResource {

    // https://gds-way.cloudapps.digital/standards/vulnerability-disclosure.html
    private static final URI CABINET_OFFICE_SECURITY_TXT = URI.create("https://vdp.cabinetoffice.gov.uk/.well-known/security.txt");

    @GET
    @Path("/.well-known/security.txt")
    public Response redirectFromWellKnownSecuritytxt() {
        return redirectToCabinetOfficeSecuritytxt();
    }

    @GET
    @Path("/security.txt")
    public Response redirectFromSecuritytxt() {
        return redirectToCabinetOfficeSecuritytxt();
    }

    private static Response redirectToCabinetOfficeSecuritytxt() {
        return Response
                .status(Response.Status.FOUND)
                .location(CABINET_OFFICE_SECURITY_TXT)
                .cacheControl(CacheControl.valueOf("no-cache"))
                .expires(new Date())
                .build();
    }

}
