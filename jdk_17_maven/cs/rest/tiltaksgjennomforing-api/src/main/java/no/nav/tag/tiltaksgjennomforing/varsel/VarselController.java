package no.nav.tag.tiltaksgjennomforing.varsel;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import no.nav.security.token.support.core.api.Protected;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggingService;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalepart;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Protected
@RestController
@RequestMapping("/varsler")
@Timed
@RequiredArgsConstructor
public class VarselController {
    private final InnloggingService innloggingService;
    private final VarselRepository varselRepository;
    private final AvtaleRepository avtaleRepository;

    @GetMapping("/oversikt")
    public List<Varsel> hentVarslerMedBjelleForOversikt(
            @CookieValue("innlogget-part") Avtalerolle innloggetPart) {
        Avtalepart avtalepart = innloggingService.hentAvtalepart(innloggetPart);
        return varselRepository.findAllByLestIsFalseAndBjelleIsTrueAndIdentifikatorIn(avtalepart.identifikatorer());
    }

    @GetMapping("/avtale-modal")
    public List<Varsel> hentVarslerMedBjelleForAvtale(
            @RequestParam(value = "avtaleId") UUID avtaleId, @CookieValue("innlogget-part") Avtalerolle innloggetPart) {
        Avtalepart avtalepart = innloggingService.hentAvtalepart(innloggetPart);
        return varselRepository.findAllByLestIsFalseAndBjelleIsTrueAndAvtaleIdAndIdentifikatorIn(avtaleId, avtalepart.identifikatorer());
    }

    @GetMapping("/avtale-logg")
    public List<Varsel> hentAlleVarslerForAvtale(
            @RequestParam(value = "avtaleId") UUID avtaleId, @CookieValue("innlogget-part") Avtalerolle innloggetPart) {
        Avtalepart avtalepart = innloggingService.hentAvtalepart(innloggetPart);
        Avtale avtale = avtaleRepository.findById(avtaleId).orElseThrow();
        avtalepart.sjekkTilgang(avtale);
        return varselRepository.findAllByAvtaleIdAndMottaker(avtaleId, innloggetPart);
    }

    @PostMapping("{varselId}/sett-til-lest")
    @Transactional
    public ResponseEntity<?> settTilLest(@PathVariable("varselId") UUID varselId, @CookieValue("innlogget-part") Avtalerolle innloggetPart) {
        Avtalepart avtalepart = innloggingService.hentAvtalepart(innloggetPart);
        Varsel varsel = varselRepository.findByIdAndIdentifikatorIn(varselId, avtalepart.identifikatorer());
        varsel.settTilLest();
        varselRepository.save(varsel);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sett-alle-til-lest")
    @Transactional
    public ResponseEntity<?> settFlereVarslerTilLest(@RequestBody List<UUID> varselIder, @CookieValue("innlogget-part") Avtalerolle innloggetPart) {
        varselIder.forEach(varselId -> settTilLest(varselId, innloggetPart));
        return ResponseEntity.ok().build();
    }
}
