package no.nav.tag.tiltaksgjennomforing.infrastruktur;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static no.nav.tag.tiltaksgjennomforing.infrastruktur.CorrelationIdSupplier.MDC_CORRELATION_ID_KEY;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
        try {
            Optional.ofNullable(request.getHeader(HEADER_NAME))
                    .filter(StringUtils::isNotBlank)
                    .ifPresentOrElse(CorrelationIdSupplier::set, CorrelationIdSupplier::generateToken);
            request.setAttribute(MDC_CORRELATION_ID_KEY, CorrelationIdSupplier.get());
            response.addHeader(HEADER_NAME, CorrelationIdSupplier.get());
            chain.doFilter(request, response);
        } finally {
            CorrelationIdSupplier.remove();
        }
    }
}