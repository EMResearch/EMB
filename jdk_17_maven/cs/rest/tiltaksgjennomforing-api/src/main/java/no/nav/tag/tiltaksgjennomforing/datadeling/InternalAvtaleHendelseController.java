package no.nav.tag.tiltaksgjennomforing.datadeling;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.exceptions.RessursFinnesIkkeException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Profile(value = {Miljø.DEV_FSS})
@RestController
@RequestMapping("/avtale-hendelse")
@RequiredArgsConstructor
@ProtectedWithClaims(issuer = "aad")
@Slf4j
public class InternalAvtaleHendelseController {

    private final AvtaleMeldingEntitetRepository avtaleMeldingEntitetRepository;
    private final AvtaleRepository avtaleRepository;

    private final TokenUtils tokenUtils;

    private void sjekkTilgang() {
        if (!tokenUtils.harAdRolle("access_as_application")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/fnr")
    public List<String> hentSisteHendelseForFnr(@RequestBody AvtaleMeldingForFnr meldingForFnr) {
        sjekkTilgang();
        List<String> hendelser = new ArrayList<>();
        if(Fnr.erGyldigFnr(meldingForFnr.fnr)) {
            List<Avtale> alleAvtalerForDeltaker = avtaleRepository.findAllByDeltakerFnr(new Fnr(meldingForFnr.fnr));
            alleAvtalerForDeltaker.forEach(avtale -> {
                List<AvtaleMeldingEntitet> avtaleMeldingEntiteter = avtaleMeldingEntitetRepository.findAllByAvtaleId(avtale.getId());
                AvtaleMeldingEntitet avtaleMeldingEntitet = avtaleMeldingEntiteter.stream().max(Comparator.comparing(melding -> melding.getTidspunkt())).orElseGet(null);
                if(avtaleMeldingEntitet != null) {
                    hendelser.add(avtaleMeldingEntitet.getJson());
                }
            });
        }

        return hendelser;
    }

    @GetMapping("/{avtaleId}")
    public String hentSisteHendelse(@PathVariable("avtaleId") UUID avtaleId) {
        List<AvtaleMeldingEntitet> avtaleMeldingEntiteter = avtaleMeldingEntitetRepository.findAllByAvtaleId(avtaleId);
        AvtaleMeldingEntitet avtaleMeldingEntitet = avtaleMeldingEntiteter.stream().max(Comparator.comparing(melding -> melding.getTidspunkt())).orElseThrow(RessursFinnesIkkeException::new);

        return avtaleMeldingEntitet.getJson();
    }

    @GetMapping("/skjema")
    @Unprotected
    public String hentJsonSkjema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .without(Option.DEFINITIONS_FOR_ALL_OBJECTS);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(AvtaleMelding.class);

        return jsonSchema.toPrettyString();
    }


    private record AvtaleMeldingForFnr(String fnr) { }

}
