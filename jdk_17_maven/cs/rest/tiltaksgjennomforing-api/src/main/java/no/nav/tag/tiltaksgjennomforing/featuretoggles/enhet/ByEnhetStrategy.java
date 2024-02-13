package no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet;

import io.getunleash.UnleashContext;
import io.getunleash.strategy.Strategy;
import lombok.RequiredArgsConstructor;

import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class ByEnhetStrategy implements Strategy {

    static final String PARAM = "valgtEnhet";
    private final AxsysService axsysService;

    @Override
    public String getName() {
        return "byEnhet";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        return false;
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters, UnleashContext unleashContext) {
        return unleashContext.getUserId()
                .flatMap(currentUserId -> Optional.ofNullable(parameters.get(PARAM))
                        .map(enheterString -> Set.of(enheterString.split(",\\s?")))
                        .map(enabledeEnheter -> !Collections.disjoint(enabledeEnheter, brukersEnheter(currentUserId))))
                .orElse(false);
    }

    private List<String> brukersEnheter(String currentUserId) {
        if (!NavIdent.erNavIdent(currentUserId)) {
            return List.of();
        }
        return axsysService.hentEnheterNavAnsattHarTilgangTil(new NavIdent(currentUserId)).stream()
                .map(enhet -> enhet.getVerdi()).collect(toList());
    }

}
