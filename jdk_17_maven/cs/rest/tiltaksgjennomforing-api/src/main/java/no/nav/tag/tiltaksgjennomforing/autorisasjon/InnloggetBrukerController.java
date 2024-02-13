package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import no.nav.security.token.support.core.api.Protected;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.exceptions.IkkeValgtPartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Protected
@RestController
@RequestMapping("/innlogget-bruker")
public class InnloggetBrukerController {
    private final InnloggingService innloggingService;

    @Autowired
    public InnloggetBrukerController(InnloggingService innloggingService) {
        this.innloggingService = innloggingService;
    }

    @GetMapping
    public ResponseEntity<InnloggetBruker> hentInnloggetBruker(@CookieValue("innlogget-part") Optional<Avtalerolle> innloggetPart) {
        return ResponseEntity.ok(innloggingService.hentInnloggetBruker(innloggetPart.orElseThrow(IkkeValgtPartException::new)));
    }
}
