package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import no.nav.tag.tiltaksgjennomforing.avtale.TilskuddPeriode;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class TilskuddsperiodeGodkjentMelding {

    UUID avtaleId;
    UUID tilskuddsperiodeId;
    UUID avtaleInnholdId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate avtaleFom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate avtaleTom;
    Tiltakstype tiltakstype;
    String deltakerFornavn;
    String deltakerEtternavn;
    Identifikator deltakerFnr;
    String arbeidsgiverFornavn;
    String arbeidsgiverEtternavn;
    String arbeidsgiverTlf;
    NavIdent veilederNavIdent;
    String bedriftNavn;
    BedriftNr bedriftNr;
    Integer tilskuddsbeløp;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate tilskuddFom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate tilskuddTom;
    Double feriepengerSats;
    Double otpSats;
    Double arbeidsgiveravgiftSats;
    Integer lønnstilskuddsprosent;
    Integer avtaleNr;
    Integer løpenummer;
    Integer resendingsnummer;
    String enhet;
    NavIdent beslutterNavIdent;
    LocalDateTime godkjentTidspunkt;

    public static TilskuddsperiodeGodkjentMelding create(Avtale avtale, TilskuddPeriode tilskuddsperiode, Integer resendingsnummer) {
        return new TilskuddsperiodeGodkjentMelding(
                avtale.getId(),
                tilskuddsperiode.getId(),
                avtale.getGjeldendeInnhold().getId(),
                avtale.getGjeldendeInnhold().getStartDato(),
                avtale.getGjeldendeInnhold().getSluttDato(),
                avtale.getTiltakstype(),
                avtale.getGjeldendeInnhold().getDeltakerFornavn(),
                avtale.getGjeldendeInnhold().getDeltakerEtternavn(),
                avtale.getDeltakerFnr(),
                avtale.getGjeldendeInnhold().getArbeidsgiverFornavn(),
                avtale.getGjeldendeInnhold().getArbeidsgiverEtternavn(),
                avtale.getGjeldendeInnhold().getArbeidsgiverTlf(),
                avtale.getVeilederNavIdent(),
                avtale.getGjeldendeInnhold().getBedriftNavn(),
                avtale.getBedriftNr(),
                tilskuddsperiode.getBeløp(),
                tilskuddsperiode.getStartDato(),
                tilskuddsperiode.getSluttDato(),
                avtale.getGjeldendeInnhold().getFeriepengesats().doubleValue(),
                avtale.getGjeldendeInnhold().getOtpSats(),
                avtale.getGjeldendeInnhold().getArbeidsgiveravgift().doubleValue(),
                tilskuddsperiode.getLonnstilskuddProsent(),
                avtale.getAvtaleNr(),
                tilskuddsperiode.getLøpenummer(),
                resendingsnummer,
                tilskuddsperiode.getEnhet(),
                tilskuddsperiode.getGodkjentAvNavIdent(),
                tilskuddsperiode.getGodkjentTidspunkt()
        );
    }
}
