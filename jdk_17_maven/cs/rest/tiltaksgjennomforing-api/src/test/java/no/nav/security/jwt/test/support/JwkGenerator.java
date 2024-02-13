package no.nav.security.jwt.test.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.IOUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

public class JwkGenerator {

    private static final String DEFAULT_KEYID = "localhost-signer";
    public static final String DEFAULT_JWKSET_FILE = "/jwkset.json";

    public JwkGenerator() {
    }

    public static RSAKey getDefaultRSAKey() {
        return (RSAKey) getJWKSet().getKeyByKeyId(DEFAULT_KEYID);
    }

    public static RSAKey getRSAKey(String keyID) {
        return (RSAKey) getJWKSet().getKeyByKeyId(keyID);
    }

    public static JWKSet getJWKSet() {
        try {
            return JWKSet.parse(IOUtils.readInputStreamToString(JwkGenerator.class.getResourceAsStream(DEFAULT_JWKSET_FILE), Charset.forName("UTF-8")));
        } catch (IOException | ParseException io) {
            throw new RuntimeException(io);
        }
    }

    public static JWKSet getJWKSetFromFile(File file) {
        try {
            JWKSet set = JWKSet.load(file);
            return set;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024); //just for testing so 1024 is ok
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected static RSAKey createJWK(String keyID, KeyPair keyPair) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(keyID)
                .build();
        return jwk;
    }
}
