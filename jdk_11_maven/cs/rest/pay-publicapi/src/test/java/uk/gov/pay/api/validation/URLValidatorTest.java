package uk.gov.pay.api.validation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.validation.URLValidator.urlValidatorValueOf;

public class URLValidatorTest {


    @Test
    public void whenIsDisabledSecureConnection_httpUrlAreValid() {
        assertThat(urlValidatorValueOf(true).isValid("http://my-valid-url.com"), is(true));
    }

    @Test
    public void whenIsDisabledSecureConnection_allowingLocalUrl() {
        assertThat(urlValidatorValueOf(true).isValid("http://localhost:8080/pay"), is(true));
    }

    @Test
    public void whenIsDisabledSecureConnection_httpsUrlsAreValid() {
        assertThat(urlValidatorValueOf(true).isValid("https://my-valid-url.com"), is(true));
    }

    @Test
    public void whenIsDisabledSecureConnection_doubleSlashUrlsAreValid() {
        assertThat(urlValidatorValueOf(true).isValid("https://www.example.com/path-here//path-there"), is(true));
    }

    @Test
    public void whenIsEnabledSecureConnection_httpUrlsAreNotValid() {
        assertThat(urlValidatorValueOf(false).isValid("http://my-valid-url.com"), is(false));
    }

    @Test
    public void whenIsEnabledSecureConnection_httpsUrlsAreValid() {
        assertThat(urlValidatorValueOf(false).isValid("https://my-valid-url.com"), is(true));
    }

    @Test
    public void whenIsEnabledSecureConnection_allowingLocalUrl() {
        assertThat(urlValidatorValueOf(false).isValid("https://localhost:8080/pay"), is(true));
    }

    @Test
    public void whenIsEnabledSecureConnection_allowingLocalUrl_shouldFailForHttp() {
        assertThat(urlValidatorValueOf(false).isValid("http://localhost:8080/pay"), is(false));
    }

    @Test
    public void whenIsEnabledSecureConnection_allowingInternalDomains() {
        assertThat(urlValidatorValueOf(false).isValid("https://staging.service.core.internal/claim/pay/id/receiver"), is(true));
    }

    @Test
    public void whenIsEnabledSecureConnection_disallowingEvilDomains() {
        assertThat(urlValidatorValueOf(false).isValid("https://an.evil/claim/pay/id/receiver"), is(false));
    }

    @Test
    public void whenIsEnabledSecureConnection_doubleSlashUrlsAreValid() {
        assertThat(urlValidatorValueOf(false).isValid("https://www.example.com/path-here//path-there"), is(true));
    }

    @Test
    public void whenUrlIsBlank_shouldFailValidation_whenDisabledSecureConnection() {
        assertThat(urlValidatorValueOf(true).isValid("   "), is(false));
    }

    @Test
    public void whenUrlIsBlank_shouldFailValidation_whenEnabledSecureConnection() {
        assertThat(urlValidatorValueOf(false).isValid("   "), is(false));
    }

    @Test
    public void whenUrlTldIsLocal_shouldAllowAsValid() {
        assertThat(urlValidatorValueOf(false).isValid("https://a-fake-test-env.fakeservice.local/public/web/govuk-return"), is(true));
    }

    @Test
    public void whenUrlIsNotAnAcceptedProtocol_disabledSecureConnection_shouldFailValidation() {
        assertThat(urlValidatorValueOf(true).isValid("ftp://ftp.funet.fi/pub/standards/RFC/rfc959.txt"), is(false));
    }

    @Test
    public void whenUrlIsNotAnAcceptedProtocol_enabledSecureConnection_shouldFailValidation() {
        assertThat(urlValidatorValueOf(false).isValid("ftp://ftp.funet.fi/pub/standards/RFC/rfc959.txt"), is(false));
    }
}

