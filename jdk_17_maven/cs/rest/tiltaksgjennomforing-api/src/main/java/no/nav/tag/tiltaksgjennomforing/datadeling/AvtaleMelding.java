package no.nav.tag.tiltaksgjennomforing.datadeling;

import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.avtale.*;
import no.nav.tag.tiltaksgjennomforing.enhet.Formidlingsgruppe;
import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class AvtaleMelding {
    HendelseType hendelseType;
    Status avtaleStatus;

    Identifikator deltakerFnr;
    Identifikator mentorFnr;
    Identifikator bedriftNr;
    Identifikator veilederNavIdent;
    Tiltakstype tiltakstype;
    LocalDateTime opprettetTidspunkt;
    UUID avtaleId;
    Integer avtaleNr;
    Instant sistEndret;
    Instant annullertTidspunkt;
    String annullertGrunn;
    boolean slettemerket;
    boolean opprettetAvArbeidsgiver;
    String enhetGeografisk;
    String enhetsnavnGeografisk;
    String enhetOppfolging;
    String enhetsnavnOppfolging;
    boolean godkjentForEtterregistrering;
    Kvalifiseringsgruppe kvalifiseringsgruppe;
    Formidlingsgruppe formidlingsgruppe;
    SortedSet<TilskuddPeriode> tilskuddPeriode = new TreeSet<>();
    boolean feilregistrert;

    // Innhold
    Integer versjon;
    String deltakerFornavn;
    String deltakerEtternavn;
    String deltakerTlf;
    String bedriftNavn;
    String arbeidsgiverFornavn;
    String arbeidsgiverEtternavn;
    String arbeidsgiverTlf;
    String veilederFornavn;
    String veilederEtternavn;
    String veilederTlf;
    String oppfolging;
    String tilrettelegging;
    LocalDate startDato;
    LocalDate sluttDato;
    Integer stillingprosent;
    String journalpostId;
    String arbeidsoppgaver;
    String stillingstittel;
    Integer stillingStyrk08;
    Integer stillingKonseptId;
    Integer antallDagerPerUke;
    RefusjonKontaktperson refusjonKontaktperson;
    // Mentor
    String mentorFornavn;
    String mentorEtternavn;
    String mentorOppgaver;
    Double mentorAntallTimer;
    Integer mentorTimelonn;
    String mentorTlf;

    // Lønnstilskudd
    String arbeidsgiverKontonummer;
    Integer lonnstilskuddProsent;
    Integer manedslonn;
    BigDecimal feriepengesats;
    BigDecimal arbeidsgiveravgift;
    Boolean harFamilietilknytning;
    String familietilknytningForklaring;
    Integer feriepengerBelop;
    Double otpSats;
    Integer otpBelop;
    Integer arbeidsgiveravgiftBelop;
    Integer sumLonnsutgifter;
    Integer sumLonnstilskudd;
    Integer manedslonn100pst;
    Integer sumLønnstilskuddRedusert;
    LocalDate datoForRedusertProsent;
    Stillingstype stillingstype;

    List<Maal> maal = new ArrayList<>();

    // Inkluderingstilskudd
    List<Inkluderingstilskuddsutgift> inkluderingstilskuddsutgift = new ArrayList<>();
    String inkluderingstilskuddBegrunnelse;
    Integer inkluderingstilskuddTotalBeløp;

    // Godkjenning
    LocalDateTime godkjentAvDeltaker;
    LocalDateTime godkjentTaushetserklæringAvMentor;
    LocalDateTime godkjentAvArbeidsgiver;
    LocalDateTime godkjentAvVeileder;
    LocalDateTime godkjentAvBeslutter;
    LocalDateTime avtaleInngått;
    LocalDateTime ikrafttredelsestidspunkt;
    Identifikator godkjentAvNavIdent;
    Identifikator godkjentAvBeslutterNavIdent;

    // Kostnadssted
    String enhetKostnadssted;
    String enhetsnavnKostnadssted;
    GodkjentPaVegneGrunn godkjentPaVegneGrunn;
    boolean godkjentPaVegneAv;
    GodkjentPaVegneAvArbeidsgiverGrunn godkjentPaVegneAvArbeidsgiverGrunn;
    boolean godkjentPaVegneAvArbeidsgiver;
    AvtaleInnholdType innholdType;
    Identifikator utførtAv;
    AvtaleHendelseUtførtAvRolle utførtAvRolle;

    public static AvtaleMelding create(Avtale avtale, AvtaleInnhold avtaleInnhold, Identifikator utførtAv, AvtaleHendelseUtførtAvRolle utførtAvAvtaleRolle, HendelseType hendelseType) {

        AvtaleMelding avtaleMelding = new AvtaleMelding();
        avtaleMelding.setHendelseType(hendelseType);
        avtaleMelding.setAvtaleStatus(avtale.statusSomEnum());
        avtaleMelding.setDeltakerFnr(avtale.getDeltakerFnr());
        avtaleMelding.setMentorFnr(avtale.getMentorFnr());
        avtaleMelding.setBedriftNr(avtale.getBedriftNr());
        avtaleMelding.setVeilederNavIdent(avtale.getVeilederNavIdent());
        avtaleMelding.setTiltakstype(avtale.getTiltakstype());
        avtaleMelding.setOpprettetTidspunkt(avtale.getOpprettetTidspunkt());
        avtaleMelding.setAvtaleId(avtale.getId());
        avtaleMelding.setAvtaleNr(avtale.getAvtaleNr());
        avtaleMelding.setSistEndret(avtale.getSistEndret());
        avtaleMelding.setAnnullertTidspunkt(avtale.getAnnullertTidspunkt());
        avtaleMelding.setAnnullertGrunn(avtale.getAnnullertGrunn());
        avtaleMelding.setSlettemerket(avtale.isSlettemerket());
        avtaleMelding.setOpprettetAvArbeidsgiver(avtale.isOpprettetAvArbeidsgiver());
        avtaleMelding.setEnhetGeografisk(avtale.getEnhetGeografisk());
        avtaleMelding.setEnhetsnavnGeografisk(avtale.getEnhetsnavnGeografisk());
        avtaleMelding.setEnhetOppfolging(avtale.getEnhetOppfolging());
        avtaleMelding.setEnhetsnavnOppfolging(avtale.getEnhetsnavnOppfolging());
        avtaleMelding.setGodkjentForEtterregistrering(avtale.isGodkjentForEtterregistrering());
        avtaleMelding.setKvalifiseringsgruppe(avtale.getKvalifiseringsgruppe());
        avtaleMelding.setFormidlingsgruppe(avtale.getFormidlingsgruppe());
        avtaleMelding.setFeilregistrert(avtale.isFeilregistrert());
        avtaleMelding.setVersjon(avtaleInnhold.getVersjon());
        avtaleMelding.setDeltakerFornavn(avtaleInnhold.getDeltakerFornavn());
        avtaleMelding.setDeltakerEtternavn(avtaleInnhold.getDeltakerEtternavn());
        avtaleMelding.setDeltakerTlf(avtaleInnhold.getDeltakerTlf());
        avtaleMelding.setBedriftNavn(avtaleInnhold.getBedriftNavn());
        avtaleMelding.setArbeidsgiverFornavn(avtaleInnhold.getArbeidsgiverFornavn());
        avtaleMelding.setArbeidsgiverEtternavn(avtaleInnhold.getArbeidsgiverEtternavn());
        avtaleMelding.setArbeidsgiverTlf(avtaleInnhold.getArbeidsgiverTlf());
        avtaleMelding.setVeilederFornavn(avtaleInnhold.getVeilederFornavn());
        avtaleMelding.setVeilederEtternavn(avtaleInnhold.getVeilederEtternavn());
        avtaleMelding.setVeilederTlf(avtaleInnhold.getVeilederTlf());
        avtaleMelding.setOppfolging(avtaleInnhold.getOppfolging());
        avtaleMelding.setTilrettelegging(avtaleInnhold.getTilrettelegging());
        avtaleMelding.setStartDato(avtaleInnhold.getStartDato());
        avtaleMelding.setSluttDato(avtaleInnhold.getSluttDato());
        avtaleMelding.setStillingprosent(avtaleInnhold.getStillingprosent());
        avtaleMelding.setJournalpostId(avtaleInnhold.getJournalpostId());
        avtaleMelding.setArbeidsoppgaver(avtaleInnhold.getArbeidsoppgaver());
        avtaleMelding.setStillingstittel(avtaleInnhold.getStillingstittel());
        avtaleMelding.setStillingStyrk08(avtaleInnhold.getStillingStyrk08());
        avtaleMelding.setStillingKonseptId(avtaleInnhold.getStillingKonseptId());
        avtaleMelding.setAntallDagerPerUke(avtaleInnhold.getAntallDagerPerUke());
        avtaleMelding.setRefusjonKontaktperson(avtaleInnhold.getRefusjonKontaktperson());
        avtaleMelding.setMentorFornavn(avtaleInnhold.getMentorFornavn());
        avtaleMelding.setMentorEtternavn(avtaleInnhold.getMentorEtternavn());
        avtaleMelding.setMentorOppgaver(avtaleInnhold.getMentorOppgaver());
        avtaleMelding.setMentorAntallTimer(avtaleInnhold.getMentorAntallTimer());
        avtaleMelding.setMentorTimelonn(avtaleInnhold.getMentorTimelonn());
        avtaleMelding.setMentorTlf(avtaleInnhold.getMentorTlf());
        avtaleMelding.setArbeidsgiverKontonummer(avtaleInnhold.getArbeidsgiverKontonummer());
        avtaleMelding.setLonnstilskuddProsent(avtaleInnhold.getLonnstilskuddProsent());
        avtaleMelding.setManedslonn(avtaleInnhold.getManedslonn());
        avtaleMelding.setFeriepengesats(avtaleInnhold.getFeriepengesats());
        avtaleMelding.setArbeidsgiveravgift(avtaleInnhold.getArbeidsgiveravgift());
        avtaleMelding.setHarFamilietilknytning(avtaleInnhold.getHarFamilietilknytning());
        avtaleMelding.setFamilietilknytningForklaring(avtaleInnhold.getFamilietilknytningForklaring());
        avtaleMelding.setFeriepengerBelop(avtaleInnhold.getFeriepengerBelop());
        avtaleMelding.setOtpSats(avtaleInnhold.getOtpSats());
        avtaleMelding.setOtpBelop(avtaleInnhold.getOtpBelop());
        avtaleMelding.setArbeidsgiveravgiftBelop(avtaleInnhold.getArbeidsgiveravgiftBelop());
        avtaleMelding.setSumLonnsutgifter(avtaleInnhold.getSumLonnsutgifter());
        avtaleMelding.setSumLonnstilskudd(avtaleInnhold.getSumLonnstilskudd());
        avtaleMelding.setManedslonn100pst(avtaleInnhold.getManedslonn100pst());
        avtaleMelding.setSumLønnstilskuddRedusert(avtaleInnhold.getSumLønnstilskuddRedusert());
        avtaleMelding.setDatoForRedusertProsent(avtaleInnhold.getDatoForRedusertProsent());
        avtaleMelding.setStillingstype(avtaleInnhold.getStillingstype());
        avtaleMelding.setInkluderingstilskuddBegrunnelse(avtaleInnhold.getInkluderingstilskuddBegrunnelse());
        avtaleMelding.setInkluderingstilskuddTotalBeløp(avtaleInnhold.inkluderingstilskuddTotalBeløp());
        avtaleMelding.setGodkjentAvDeltaker(avtaleInnhold.getGodkjentAvDeltaker());
        avtaleMelding.setGodkjentTaushetserklæringAvMentor(avtaleInnhold.getGodkjentTaushetserklæringAvMentor());
        avtaleMelding.setGodkjentAvArbeidsgiver(avtaleInnhold.getGodkjentAvArbeidsgiver());
        avtaleMelding.setGodkjentAvVeileder(avtaleInnhold.getGodkjentAvVeileder());
        avtaleMelding.setGodkjentAvBeslutter(avtaleInnhold.getGodkjentAvBeslutter());
        avtaleMelding.setAvtaleInngått(avtaleInnhold.getAvtaleInngått());
        avtaleMelding.setIkrafttredelsestidspunkt(avtaleInnhold.getIkrafttredelsestidspunkt());
        avtaleMelding.setGodkjentAvNavIdent(avtaleInnhold.getGodkjentAvNavIdent());
        avtaleMelding.setGodkjentAvBeslutterNavIdent(avtaleInnhold.getGodkjentAvBeslutterNavIdent());
        avtaleMelding.setEnhetKostnadssted(avtaleInnhold.getEnhetKostnadssted());
        avtaleMelding.setEnhetsnavnKostnadssted(avtaleInnhold.getEnhetsnavnKostnadssted());
        avtaleMelding.setGodkjentPaVegneGrunn(avtaleInnhold.getGodkjentPaVegneGrunn());
        avtaleMelding.setGodkjentPaVegneAv(avtaleInnhold.isGodkjentPaVegneAv());
        avtaleMelding.setGodkjentPaVegneAvArbeidsgiverGrunn(avtaleInnhold.getGodkjentPaVegneAvArbeidsgiverGrunn());
        avtaleMelding.setGodkjentPaVegneAvArbeidsgiver(avtaleInnhold.isGodkjentPaVegneAvArbeidsgiver());
        avtaleMelding.setInnholdType(avtaleInnhold.getInnholdType());
        avtaleMelding.setUtførtAv(utførtAv);
        avtaleMelding.setUtførtAvRolle(utførtAvAvtaleRolle);

        //Lister
        avtaleMelding.setTilskuddPeriode(Collections.emptySortedSet());
        avtaleMelding.setMaal(avtaleInnhold.getMaal());
        avtaleMelding.setInkluderingstilskuddsutgift(avtaleInnhold.getInkluderingstilskuddsutgift());

        return avtaleMelding;
    }

}
