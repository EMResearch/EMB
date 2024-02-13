package no.nav.tag.tiltaksgjennomforing.datavarehus;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/utvikler-admin/dvh-melding")
@RequiredArgsConstructor
@ProtectedWithClaims(issuer = "aad")
@Slf4j
public class InternalDvhMeldingProdusentController {
    private final AvtaleRepository avtaleRepository;
    private final DvhMeldingEntitetRepository dvhMeldingRepository;
    private final TokenUtils tokenUtils;
    private final DvhMeldingProperties dvhMeldingProperties;
    private final DvhAvtalePatchService dvhAvtalePatchService;

    @PostMapping("/patch")
    public void patcheAvtale(@RequestBody PatchRequest request) {
        log.info("Patcher avtaler til dvh");
        sjekkTilgang();
        avtaleRepository.findAllById(request.avtaleIder()).forEach(avtale -> {
            UUID meldingId = UUID.randomUUID();
            String utførtAv = tokenUtils.hentBrukerOgIssuer().map(TokenUtils.BrukerOgIssuer::getBrukerIdent).orElse("patch");
            AvroTiltakHendelse avroTiltakHendelse = AvroTiltakHendelseFabrikk.konstruer(avtale, Now.localDateTime(), meldingId, DvhHendelseType.PATCHING, utførtAv);
            dvhMeldingRepository.save(new DvhMeldingEntitet(meldingId, avtale.getId(), Now.localDateTime(), avtale.statusSomEnum(), avroTiltakHendelse));
            log.info("Patchet avtale {}, sendt melding med id {} til datavarehus", avtale.getId(), meldingId);
        });
    }

    @PostMapping("patchalleavtaler")
    public void patchAlleAvtaler() {
        log.info("Patcher alle avtaler til dvh");
        sjekkTilgang();
        dvhAvtalePatchService.lagDvhPatchMeldingForAlleAvtaler();
    }

    private void sjekkTilgang() {
        if (!tokenUtils.harAdGruppe(dvhMeldingProperties.getGruppeTilgang())) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }
    }

    private record PatchRequest(List<UUID> avtaleIder) {
    }

}
