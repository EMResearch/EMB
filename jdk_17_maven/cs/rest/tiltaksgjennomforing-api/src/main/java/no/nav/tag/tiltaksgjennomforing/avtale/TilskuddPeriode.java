package no.nav.tag.tiltaksgjennomforing.avtale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
public class TilskuddPeriode implements Comparable<TilskuddPeriode> {

    @Id
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "avtale_id")
    @JsonIgnore
    @ToString.Exclude
    private Avtale avtale;

    @NonNull
    private Integer beløp;
    @NonNull
    private LocalDate startDato;
    @NonNull
    private LocalDate sluttDato;

    @Convert(converter = NavIdentConverter.class)
    private NavIdent godkjentAvNavIdent;

    private LocalDateTime godkjentTidspunkt;

    private String enhet;
    private String enhetsnavn;

    @NonNull
    private Integer lonnstilskuddProsent;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.LAZY)
    private Set<Avslagsårsak> avslagsårsaker = EnumSet.noneOf(Avslagsårsak.class);

    private String avslagsforklaring;
    @Convert(converter = NavIdentConverter.class)
    private NavIdent avslåttAvNavIdent;
    private LocalDateTime avslåttTidspunkt;
    private Integer løpenummer = 1;

    @Enumerated(EnumType.STRING)
    private TilskuddPeriodeStatus status = TilskuddPeriodeStatus.UBEHANDLET;

    @Enumerated(EnumType.STRING)
    private RefusjonStatus refusjonStatus = null;

    private boolean aktiv = true;

    public TilskuddPeriode deaktiverOgLagNyUbehandlet() {
        this.aktiv = false;
        TilskuddPeriode kopi = new TilskuddPeriode();
        kopi.id = UUID.randomUUID();
        kopi.løpenummer = this.løpenummer;
        kopi.beløp = this.beløp;
        kopi.lonnstilskuddProsent = this.lonnstilskuddProsent;
        kopi.startDato = this.startDato;
        kopi.sluttDato = this.sluttDato;
        kopi.avtale = this.avtale;
        kopi.aktiv = true;
        kopi.status = TilskuddPeriodeStatus.UBEHANDLET;
        return kopi;
    }

    private void sjekkOmKanBehandles() {
        if (status != TilskuddPeriodeStatus.UBEHANDLET) {
            throw new FeilkodeException(Feilkode.TILSKUDDSPERIODE_ER_ALLEREDE_BEHANDLET);
        }
        if (Now.localDate().isBefore(kanBesluttesFom())) {
            throw new FeilkodeException(Feilkode.TILSKUDDSPERIODE_BEHANDLE_FOR_TIDLIG);
        }
    }

    @JsonProperty
    private LocalDate kanBesluttesFom() {
        // TODO: DENNE KODEN MÅ FJERNES NÅR VI FÅR BESKJED OM AT DET ER OK Å HOLDE AV PENGER FOR NESTE ÅR
        if (LocalDate.now().getYear() == 2023 && startDato.getYear() == 2024) {
            if (startDato.minusMonths(3).getYear() == 2023) {
                // Setter kun 01-01-2024 hvis den opprinnelig hadde blitt satt til 2023.
                return LocalDate.of(2024, 01, 1);
            }
        }

        if (løpenummer == 1) {
            return LocalDate.MIN;
        }
        return startDato.minusMonths(3);
    }

    void godkjenn(NavIdent beslutter, String enhet) {
        sjekkOmKanBehandles();

        setGodkjentTidspunkt(Now.localDateTime());
        setGodkjentAvNavIdent(beslutter);
        setEnhet(enhet);
        setStatus(TilskuddPeriodeStatus.GODKJENT);
    }

    void avslå(NavIdent beslutter, EnumSet<Avslagsårsak> avslagsårsaker, String avslagsforklaring) {
        sjekkOmKanBehandles();
        if (avslagsforklaring.isBlank()) {
            throw new FeilkodeException(Feilkode.TILSKUDDSPERIODE_AVSLAGSFORKLARING_PAAKREVD);
        }
        if (avslagsårsaker.isEmpty()) {
            throw new FeilkodeException(Feilkode.TILSKUDDSPERIODE_INGEN_AVSLAGSAARSAKER);
        }

        setAvslåttTidspunkt(Now.localDateTime());
        setAvslåttAvNavIdent(beslutter);
        this.avslagsårsaker.addAll(avslagsårsaker);
        setAvslagsforklaring(avslagsforklaring);
        setStatus(TilskuddPeriodeStatus.AVSLÅTT);
    }

    public boolean kanBehandles() {
        try {
            sjekkOmKanBehandles();
            return true;
        } catch (FeilkodeException e) {
            return false;
        }
    }

    @Override
    public int compareTo(@NotNull TilskuddPeriode o) {
        return new CompareToBuilder()
                .append(this.getStartDato(), o.getStartDato())
                .append(this.isAktiv(), o.isAktiv())
                .append(this.getStatus(), o.getStatus())
                .append(this.getId(), o.getId())
                .toComparison();
    }

    public boolean erUtbetalt() {
        return refusjonStatus == RefusjonStatus.UTBETALT || refusjonStatus == RefusjonStatus.KORRIGERT;
    }

    public boolean erRefusjonGodkjent() {
        return refusjonStatus == RefusjonStatus.SENDT_KRAV || refusjonStatus == RefusjonStatus.GODKJENT_MINUSBELØP || refusjonStatus == RefusjonStatus.GODKJENT_NULLBELØP;
    }

}
