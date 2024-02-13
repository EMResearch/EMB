package uk.gov.pay.api.utils;

import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

public class ApiKeyGenerator {

    public static String apiKeyValueOf(String token, String secret) {
        byte[] hmacBytes = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, secret).hmac(token);
        String encodedHmac = BaseEncoding.base32Hex().lowerCase().omitPadding().encode(hmacBytes);
        return token + encodedHmac;
    }
}
