package no.nav.tag.tiltaksgjennomforing.avtale;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.SlettemerkeProperties;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.TilgangskontrollService;
import no.nav.tag.tiltaksgjennomforing.enhet.Norg2Client;
import no.nav.tag.tiltaksgjennomforing.enhet.VeilarbArenaClient;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.AxsysService;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.auditing.AuditConsoleLogger;
import no.nav.tag.tiltaksgjennomforing.persondata.PersondataService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.Cookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.tag.tiltaksgjennomforing.avtale.AvtaleApiTest.AvtaleApiTestUtil.lagTokenForNavIdent;
import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.enArbeidstreningAvtale;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class AvtaleApiTest {

    public AvtaleApiTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    MockMvc mockMvc;
    ObjectMapper mapper = new ObjectMapper();

    @MockBean
    AxsysService axsysService;
    @Mock
    VeilarbArenaClient veilarbArenaClient;
    @Mock
    Norg2Client norg2Client;
    @MockBean
    private AvtaleRepository avtaleRepository;
    @MockBean
    private TilgangskontrollService tilgangskontrollService;
    @Mock
    private PersondataService persondataService;
    @SpyBean
    private AuditConsoleLogger auditConsoleLogger;

    @Test
    public void hentSkalReturnereRiktigAvtale() throws Exception {
        Avtale avtale = enArbeidstreningAvtale();
        var navIdent = TestData.enNavIdent();
        Veileder veileder = new Veileder(
                navIdent,
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Collections.emptySet(),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );
        when(tilgangskontrollService.harSkrivetilgangTilKandidat(eq(veileder), any(Fnr.class))).thenReturn(true);
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(any())).thenReturn(List.of());
        when(avtaleRepository.findById(avtale.getId())).thenReturn(Optional.of(avtale));
        var res = hentAvtaleForVeileder(navIdent, avtale.getId());
        assertEquals(200, res.getStatus());
        assertEquals(avtale.getId().toString(), mapper.readTree(res.getContentAsByteArray()).get("id").asText());

        verify(auditConsoleLogger, times(1)).logg(any());
    }

    private MockHttpServletResponse hentAvtaleForVeileder(NavIdent navIdent, UUID avtaleId) throws Exception {
        var headers = new HttpHeaders();
        headers.put("Authorization", List.of("Bearer " + lagTokenForNavIdent(navIdent)));
        return mockMvc.perform(MockMvcRequestBuilders.get(URI.create("/avtaler/" + avtaleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers)
                        .cookie(new Cookie("innlogget-part", Avtalerolle.VEILEDER.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();
    }

    static class AvtaleApiTestUtil {
        private static String tokenRequest(String url) {
            try {
                return HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder()
                                .GET()
                                .uri(URI.create(url))
                                .build(), HttpResponse.BodyHandlers.ofString()
                ).body();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String lagTokenForFnr(String fnr) {
            return tokenRequest(format("https://tiltak-fakelogin.ekstern.dev.nav.no/token?pid=%s&aud=fake-tokenx&iss=tokenx&acr=Level4", fnr));
        }

        static String lagTokenForNavIdent(NavIdent navIdent) {
            return tokenRequest(format("https://tiltak-fakelogin.ekstern.dev.nav.no/token?NAVident=%s&aud=fake-aad&iss=aad&acr=Level4", navIdent.asString()));
        }
    }
}
