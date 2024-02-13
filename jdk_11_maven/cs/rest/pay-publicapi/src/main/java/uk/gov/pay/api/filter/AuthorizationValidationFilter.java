package uk.gov.pay.api.filter;

import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.service.payments.logging.LoggingKeys.REMOTE_ADDRESS;

public class AuthorizationValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationValidationFilter.class);

    private static final int HMAC_SHA1_LENGTH = 32;
    private static final String BEARER_PREFIX = "Bearer ";
    
    private static final String[] EXCLUDED_URLS = {
            "/v1/auth"
    };
    
    private String apiKeyHmacSecret;

    @Inject
    public AuthorizationValidationFilter(PublicApiConfig configuration) {
        this.apiKeyHmacSecret = configuration.getApiKeyHmacSecret();
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        String path = ((HttpServletRequest) request).getRequestURI();
        if (Arrays.stream(EXCLUDED_URLS).anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }
        
        final String authorization = ((HttpServletRequest) request).getHeader("Authorization");
        String clientAddress = Optional.ofNullable(((HttpServletRequest) request).getHeader("X-Forwarded-For"))
                .map(forwarded -> forwarded.split(",")[0])
                .orElse(null);

        if (isValidAuthorizationHeader(authorization, clientAddress)) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).sendError(UNAUTHORIZED.getStatusCode(), UNAUTHORIZED.getReasonPhrase());
        }
    }

    @Override
    public void destroy() {
    }

    private boolean isValidAuthorizationHeader(String authorization, String clientAddress) {
        return authorization != null
                && authorization.startsWith(BEARER_PREFIX)
                && isValidTokenIntegrity(authorization.substring(BEARER_PREFIX.length()), clientAddress);
    }

    private boolean isValidTokenIntegrity(String apiKey, String clientAddress) {
        boolean isValid = false;
        if (apiKey.length() >= HMAC_SHA1_LENGTH + 1) {
            int initHmacIndex = apiKey.length() - HMAC_SHA1_LENGTH;
            String hmacFromApiKey = apiKey.substring(initHmacIndex);
            String tokenFromApiKey = apiKey.substring(0, initHmacIndex);
            isValid = tokenMatchesHmac(tokenFromApiKey, hmacFromApiKey);
        }

        if (!isValid) {
            logger.warn("Attempt to authenticate using an API key with an invalid checksum",
                    kv(REMOTE_ADDRESS, clientAddress));
        }

        return isValid;
    }

    private boolean tokenMatchesHmac(String token, String currentHmac) {
        final String hmacCalculatedFromToken = BaseEncoding.base32Hex()
                .lowerCase().omitPadding()
                .encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, apiKeyHmacSecret).hmac(token));
        return MessageDigest.isEqual(hmacCalculatedFromToken.getBytes(StandardCharsets.UTF_8), currentHmac.getBytes(StandardCharsets.UTF_8));
    }
}
