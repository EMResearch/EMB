package no.nav.tag.tiltaksgjennomforing;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("wiremock")
@Slf4j
@Component
public class IntegrasjonerMockServer implements DisposableBean {
    private final WireMockServer server;

    public IntegrasjonerMockServer() {
        log.info("Starter mockserver for eksterne integrasjoner.");
        server = new WireMockServer(WireMockConfiguration.options().usingFilesUnderClasspath(".").port(8090));
        server.start();
    }

    public WireMockServer getServer() {
        return server;
    }

    @Override
    public void destroy() {
        log.info("Stopper mockserver.");
        server.stop();
    }
}
