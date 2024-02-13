package no.nav.tag.tiltaksgjennomforing.autorisasjon.abac;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.PoaoTilgangService;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter.AbacAdapter;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.InternBruker;

@Service
@Slf4j
@RequiredArgsConstructor
public class TilgangskontrollServiceImpl implements TilgangskontrollService {

    private final AbacAdapter abacAdapter;
    private final PoaoTilgangService poaoTilgangService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public boolean harSkrivetilgangTilKandidat(InternBruker internBruker, Fnr fnr) {
        var harAbacTilgang = abacAdapter.harSkriveTilgang(internBruker.getNavIdent().asString(), fnr.asString());
        var contextMap = MDC.getCopyOfContextMap();
        executorService.submit(() -> {
            MDC.setContextMap(contextMap);
            try {
                if (internBruker.getAzureOid() != null && fnr.asString() != null) {
                    var harPoaoTilgang = poaoTilgangService.harSkriveTilgang(internBruker.getAzureOid(), fnr.asString());
                    if (harPoaoTilgang != harAbacTilgang) {
                        log.warn("Tilgangskontroll: ulikt utfall i abac ({}) og poao ({})", harAbacTilgang, harPoaoTilgang);
                    }
                }
            } catch (Exception e) {
                log.error("Feil ved tilgangskontroll-sammenligning", e);
            } finally {
                MDC.clear();
            }
        });
        return harAbacTilgang;
    }

    public Map<Fnr, Boolean> skriveTilganger(InternBruker internBruker, Set<Fnr> fnrListe) {
        return fnrListe.stream()
                .map(fnr -> Map.entry(fnr, harSkrivetilgangTilKandidat(internBruker, fnr)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}