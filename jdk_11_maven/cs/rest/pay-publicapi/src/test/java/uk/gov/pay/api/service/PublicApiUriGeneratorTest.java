package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.config.PublicApiConfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PublicApiUriGeneratorTest {

    @Mock
    PublicApiConfig publicApiConfig;
    
    PublicApiUriGenerator publicApiUriGenerator;
    private String publicApiTestBaseUrl = "http://publicapi.test";
    
    @BeforeEach
    public void setUp() {
        when(publicApiConfig.getBaseUrl()).thenReturn(publicApiTestBaseUrl);
        publicApiUriGenerator = new PublicApiUriGenerator(publicApiConfig);
    }
    
    @Test
    public void convertHostToPublicAPI_with_http() {
        String originalHost = "http://something.test";
        String pathAndQuery = "/v1/api/events?queryParam1=value1&queryParam2=value2";
        assertEquals(publicApiTestBaseUrl + pathAndQuery, publicApiUriGenerator.convertHostToPublicAPI(originalHost + pathAndQuery) );
    }

    @Test
    public void convertHostToPublicAPI_with_https() {
        String originalHost = "https://something.test";
        String pathAndQuery = "/v1/api/events?queryParam1=value1&queryParam2=value2";
        assertEquals(publicApiTestBaseUrl + pathAndQuery, publicApiUriGenerator.convertHostToPublicAPI(originalHost + pathAndQuery) );
    }
}
