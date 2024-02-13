package no.nav.tag.tiltaksgjennomforing.varsel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.*;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
public class Varsel extends AbstractAggregateRoot<Varsel> {
    @Id
    private UUID id;
    private boolean lest;
    @Convert(converter = IdentifikatorConverter.class)
    private Identifikator identifikator;
    @Convert(converter = IdentifikatorConverter.class)
    private Identifikator utførtAvIdentifikator;
    private String tekst;
    @Enumerated(EnumType.STRING)
    private HendelseType hendelseType;
    private boolean bjelle;
    private UUID avtaleId;
    private LocalDateTime tidspunkt;
    @Enumerated(EnumType.STRING)
    private Avtalerolle mottaker;
    @Enumerated(EnumType.STRING)
    private Avtalerolle utførtAv;

    private static String tilskuddsperiodeAvslåttTekst(Avtale avtale, String hendelseTypeTekst) {
        TilskuddPeriode gjeldendePeriode = avtale.gjeldendeTilskuddsperiode();
        String avslagÅrsaker = gjeldendePeriode.getAvslagsårsaker().stream()
                .map(type -> type.getTekst().toLowerCase()).collect(Collectors.joining(", "));
        return hendelseTypeTekst
                .concat(gjeldendePeriode.getAvslåttAvNavIdent().asString())
                .concat(". Årsak til retur: ")
                .concat(avslagÅrsaker)
                .concat(". Forklaring: ")
                .concat(gjeldendePeriode.getAvslagsforklaring());
    }

    private static String lagVarselTekst(Avtale avtale, HendelseType hendelseType) {
        switch (hendelseType) {
            case TILSKUDDSPERIODE_AVSLATT:
                return tilskuddsperiodeAvslåttTekst(avtale, hendelseType.getTekst());
            case AVTALE_FORKORTET:
                return "Avtale forkortet til " + avtale.getGjeldendeInnhold().getSluttDato().format(DateTimeFormatter.ofPattern("dd.MM.YYYY"));
            case AVTALE_FORLENGET:
                return "Avtale forlenget til " + avtale.getGjeldendeInnhold().getSluttDato().format(DateTimeFormatter.ofPattern("dd.MM.YYYY"));
            default:
                return hendelseType.getTekst();
        }
    }

    public static Varsel nyttVarsel(Identifikator identifikator, boolean bjelle, Avtale avtale, Avtalerolle mottaker, Avtalerolle utførtAv, Identifikator utførtAvIdentifikator, HendelseType hendelseType, UUID avtaleId) {
        Varsel varsel = new Varsel();
        varsel.id = UUID.randomUUID();
        varsel.tidspunkt = Now.localDateTime();
        varsel.identifikator = identifikator;
        varsel.utførtAvIdentifikator = utførtAvIdentifikator;
        varsel.tekst = lagVarselTekst(avtale, hendelseType);
        varsel.hendelseType = hendelseType;
        varsel.avtaleId = avtaleId;
        varsel.bjelle = bjelle;
        varsel.mottaker = mottaker;
        varsel.utførtAv = utførtAv;
        return varsel;
    }

    public void settTilLest() {
        lest = true;
    }
}
