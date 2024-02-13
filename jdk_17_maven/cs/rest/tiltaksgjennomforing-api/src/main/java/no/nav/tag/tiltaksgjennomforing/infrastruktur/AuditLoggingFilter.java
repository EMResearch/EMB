package no.nav.tag.tiltaksgjennomforing.infrastruktur;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.auditing.AuditEntry;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.auditing.AuditLogger;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.auditing.EventType;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import static no.nav.tag.tiltaksgjennomforing.ApiBeskriver.API_BESKRIVELSE_ATTRIBUTT;
import static no.nav.tag.tiltaksgjennomforing.infrastruktur.CorrelationIdSupplier.MDC_CORRELATION_ID_KEY;

/**
 * Dette filteret fanger opp alle responser fra APIet.
 * Dersom en person (arbeidsgiver, saksbehandler) har gjort et oppslag
 * og får returnert en JSON som inneholder "deltakerFnr" så vil dette
 * resultere i en audit-hendelse.
 */
@Slf4j
@Component
class AuditLoggingFilter extends OncePerRequestFilter {
    private final TokenUtils tokenUtils;
    private final AuditLogger auditLogger;
    private static final String classname = AuditLoggingFilter.class.getName();

    public AuditLoggingFilter(TokenUtils tokenUtils, AuditLogger auditLogger) {
        this.tokenUtils = tokenUtils;
        this.auditLogger = auditLogger;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);
        String correlationId;
        if (request.getAttribute(MDC_CORRELATION_ID_KEY) != null)
            correlationId = (String) request.getAttribute(MDC_CORRELATION_ID_KEY);
        else correlationId = null;

        if (correlationId == null) {
            log.error("{}: feilet pga manglende correlationId.", classname);
        }
        if (response.getContentType() != null && response.getContentType().startsWith("application/json") && correlationId != null) {
            try {
                List<String> fnrListe = JsonPath.read(wrapper.getContentInputStream(), "$..deltakerFnr");
                var utførtTid = Now.instant();
                String brukerId = tokenUtils.hentBrukerOgIssuer().map(TokenUtils.BrukerOgIssuer::getBrukerIdent).orElse(null);
                var uri = URI.create(request.getRequestURI());
                // Logger kun oppslag dersom en innlogget bruker utførte oppslaget
                if (brukerId != null) {
                    fnrListe.stream().filter(Objects::nonNull).distinct().forEach(deltakerFnr -> {
                        var apiBeskrivelse = (String) request.getAttribute(API_BESKRIVELSE_ATTRIBUTT);
                        if (apiBeskrivelse == null) {
                            log.warn("Manglende @ApiBeskrivelse for api-endepunkt {}", uri);
                        }
                        // Ikke logg at en bruker slår opp sin egen informasjon
                        if (!brukerId.equals(deltakerFnr)) {
                            var entry = new AuditEntry(
                                    "tiltaksgjennomforing-api",
                                    brukerId,
                                    deltakerFnr,
                                    EventType.READ,
                                    true,
                                    utførtTid,
                                    apiBeskrivelse != null ? apiBeskrivelse
                                            : "Oppslag i løsning for arbeidsmarkedstiltak",
                                    uri,
                                    HttpMethod.valueOf(request.getMethod()),
                                    correlationId
                            );
                            auditLogger.logg(entry);
                        }
                    });
                }
            } catch (IOException ex) {
                log.warn("{}: Klarte ikke dekode responsen. Var det ikke gyldig JSON?", classname);
            } catch (Exception ex) {
                log.error("{}: Logging feilet", classname, ex);
            }
        }
        wrapper.copyBodyToResponse();
    }
}