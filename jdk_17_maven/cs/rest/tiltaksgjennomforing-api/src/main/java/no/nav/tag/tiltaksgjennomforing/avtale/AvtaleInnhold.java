package no.nav.tag.tiltaksgjennomforing.avtale;

import static no.nav.tag.tiltaksgjennomforing.utils.Utils.erTom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

// Lombok
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
// Hibernate
@Entity
public class AvtaleInnhold {

    @Id
    @JsonIgnore
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "avtale")
    @JsonIgnore
    @ToString.Exclude
    private Avtale avtale;

    private Integer versjon;

    private String deltakerFornavn;
    private String deltakerEtternavn;
    private String deltakerTlf;
    private String bedriftNavn;
    private String arbeidsgiverFornavn;
    private String arbeidsgiverEtternavn;
    private String arbeidsgiverTlf;
    private String veilederFornavn;
    private String veilederEtternavn;
    private String veilederTlf;
    private String oppfolging;
    private String tilrettelegging;
    private LocalDate startDato;
    private LocalDate sluttDato;
    private Integer stillingprosent;
    private String journalpostId;
    private String arbeidsoppgaver;
    private String stillingstittel;
    private Integer stillingStyrk08;
    private Integer stillingKonseptId;
    private Integer antallDagerPerUke;

     @Embedded
     private RefusjonKontaktperson refusjonKontaktperson;


    // Mentor
    private String mentorFornavn;
    private String mentorEtternavn;
    private String mentorOppgaver;
    private Double mentorAntallTimer;
    private Integer mentorTimelonn;
    private String mentorTlf;

    // Lønnstilskudd
    private String arbeidsgiverKontonummer;
    private Integer lonnstilskuddProsent;
    private Integer manedslonn;
    private BigDecimal feriepengesats;
    private BigDecimal arbeidsgiveravgift;
    private Boolean harFamilietilknytning;
    private String familietilknytningForklaring;
    private Integer feriepengerBelop;
    private Double otpSats;
    private Integer otpBelop;
    private Integer arbeidsgiveravgiftBelop;
    private Integer sumLonnsutgifter;
    private Integer sumLonnstilskudd;
    private Integer manedslonn100pst;
    private Integer sumLønnstilskuddRedusert;
    private LocalDate datoForRedusertProsent;
    @Enumerated(EnumType.STRING)
    private Stillingstype stillingstype;

    // Arbeidstrening
    @OneToMany(mappedBy = "avtaleInnhold", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<Maal> maal = new ArrayList<>();

    // Inkluderingstilskudd
    @OneToMany(mappedBy = "avtaleInnhold", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<Inkluderingstilskuddsutgift> inkluderingstilskuddsutgift = new ArrayList<>();
    private String inkluderingstilskuddBegrunnelse;

    @JsonProperty
    public Integer inkluderingstilskuddTotalBeløp() {
        return inkluderingstilskuddsutgift.stream().map(inkluderingstilskuddsutgift -> inkluderingstilskuddsutgift.getBeløp())
                .collect(Collectors.toList()).stream()
                .reduce(0, Integer::sum);
    }

    // Godkjenning
    private LocalDateTime godkjentAvDeltaker;
    private LocalDateTime godkjentTaushetserklæringAvMentor;
    private LocalDateTime godkjentAvArbeidsgiver;
    private LocalDateTime godkjentAvVeileder;
    private LocalDateTime godkjentAvBeslutter;
    private LocalDateTime avtaleInngått;
    private LocalDateTime ikrafttredelsestidspunkt;
    @Convert(converter = NavIdentConverter.class)
    private NavIdent godkjentAvNavIdent;
    @Convert(converter = NavIdentConverter.class)
    private NavIdent godkjentAvBeslutterNavIdent;

    // Kostnadssted
    private String enhetKostnadssted;
    private String enhetsnavnKostnadssted;

    @Embedded
    private GodkjentPaVegneGrunn godkjentPaVegneGrunn;
    private boolean godkjentPaVegneAv;

    @Embedded
    private GodkjentPaVegneAvArbeidsgiverGrunn godkjentPaVegneAvArbeidsgiverGrunn;
    private boolean godkjentPaVegneAvArbeidsgiver;

    @Enumerated(EnumType.STRING)
    private AvtaleInnholdType innholdType;


    public static AvtaleInnhold nyttTomtInnhold(Tiltakstype tiltakstype) {
        var innhold = new AvtaleInnhold();
        innhold.setId(UUID.randomUUID());
        innhold.setVersjon(1);
        innhold.setInnholdType(AvtaleInnholdType.INNGÅ);
        if (tiltakstype == Tiltakstype.SOMMERJOBB) {
            innhold.setStillingstype(Stillingstype.MIDLERTIDIG);
        }
        return innhold;
    }

    public AvtaleInnhold nyGodkjentVersjon(AvtaleInnholdType innholdType) {
        AvtaleInnhold nyVersjon = toBuilder()
                .id(UUID.randomUUID())
                .maal(kopiAvMål())
                .inkluderingstilskuddsutgift(kopiAvInkluderingstilskuddsutgifer())
                .journalpostId(null)
                .versjon(versjon + 1)
                .innholdType(innholdType)
                .build();
        nyVersjon.getMaal().forEach(m -> m.setAvtaleInnhold(nyVersjon));
        nyVersjon.getInkluderingstilskuddsutgift().forEach(i -> i.setAvtaleInnhold(nyVersjon));
        return nyVersjon;
    }

    private List<Maal> kopiAvMål() {
        return maal.stream().map(m -> new Maal(m)).collect(Collectors.toList());
    }

    private List<Inkluderingstilskuddsutgift> kopiAvInkluderingstilskuddsutgifer() {
        return inkluderingstilskuddsutgift.stream().map(i -> new Inkluderingstilskuddsutgift(i)).collect(Collectors.toList());
    }

    void endreAvtale(EndreAvtale nyAvtale) {
        if (tiltakstypeHarFastsattLonnstilskuddsprosentsatsUtIfraKvalifiseringsgruppe()) {
            innholdStrategi().endreAvtaleInnholdMedKvalifiseringsgruppe(nyAvtale, avtale.getKvalifiseringsgruppe());
        } else {
            innholdStrategi().endre(nyAvtale);
        }
    }

    public Set<String> felterSomIkkeErFyltUt() {
        return innholdStrategi().alleFelterSomMåFyllesUt()
                .entrySet().stream()
                .filter(entry -> erTom(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private AvtaleInnholdStrategy innholdStrategi() {
        return AvtaleInnholdStrategyFactory.create(this, avtale.getTiltakstype());
    }

    private boolean tiltakstypeHarFastsattLonnstilskuddsprosentsatsUtIfraKvalifiseringsgruppe() {
        // Midlertidig skrudd av utleding av lønnstilskuddprosent for Sommerjobb fra kvalifiseringsgruppe for å åpne for etterregistrering.
        return avtale.getTiltakstype() == Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD;
    }

    public boolean skalJournalfores() {
        return this.godkjentAvVeileder != null && this.getJournalpostId() == null;
    }

    public void endreTilskuddsberegning(EndreTilskuddsberegning tilskuddsberegning) {
        innholdStrategi().endreTilskuddsberegning(tilskuddsberegning);
    }

    public void reberegnLønnstilskudd() {
        innholdStrategi().regnUtTotalLonnstilskudd();
    }

    public void reberegnRedusertProsentOgRedusertLonnstilskudd() {
        innholdStrategi().reUtregnRedusertProsentOgSum();
    }

    public void endreKontaktInfo(EndreKontaktInformasjon endreKontaktInformasjon) {
        setDeltakerFornavn(endreKontaktInformasjon.getDeltakerFornavn());
        setDeltakerEtternavn(endreKontaktInformasjon.getDeltakerEtternavn());
        setDeltakerTlf(endreKontaktInformasjon.getDeltakerTlf());
        setVeilederFornavn(endreKontaktInformasjon.getVeilederFornavn());
        setVeilederEtternavn(endreKontaktInformasjon.getVeilederEtternavn());
        setVeilederTlf(endreKontaktInformasjon.getVeilederTlf());
        setArbeidsgiverFornavn(endreKontaktInformasjon.getArbeidsgiverFornavn());
        setArbeidsgiverEtternavn(endreKontaktInformasjon.getArbeidsgiverEtternavn());
        setArbeidsgiverTlf(endreKontaktInformasjon.getArbeidsgiverTlf());
        setRefusjonKontaktperson(endreKontaktInformasjon.getRefusjonKontaktperson());
    }

    public void endreStillingsInfo(EndreStillingsbeskrivelse endreStillingsbeskrivelse) {
        setStillingstittel(endreStillingsbeskrivelse.getStillingstittel());
        setArbeidsoppgaver(endreStillingsbeskrivelse.getArbeidsoppgaver());
        setStillingStyrk08(endreStillingsbeskrivelse.getStillingStyrk08());
        setStillingKonseptId(endreStillingsbeskrivelse.getStillingKonseptId());
        setStillingprosent(endreStillingsbeskrivelse.getStillingprosent());
        setAntallDagerPerUke(endreStillingsbeskrivelse.getAntallDagerPerUke());
    }

    public void endreOppfølgingOgTilretteleggingInfo(EndreOppfølgingOgTilrettelegging endreOppfølgingOgTilrettelegging) {
        setOppfolging(endreOppfølgingOgTilrettelegging.getOppfolging());
        setTilrettelegging(endreOppfølgingOgTilrettelegging.getTilrettelegging());
    }

    public void endreSluttDato(LocalDate nySluttDato) {
        innholdStrategi().endreSluttDato(nySluttDato);
    }

    public void endreOmMentor(EndreOmMentor endreOmMentor) {
        setMentorFornavn(endreOmMentor.getMentorFornavn());
        setMentorEtternavn(endreOmMentor.getMentorEtternavn());
        setMentorTlf(endreOmMentor.getMentorTlf());
        setMentorAntallTimer(endreOmMentor.getMentorAntallTimer());
        setMentorTimelonn(endreOmMentor.getMentorTimelonn());
        setMentorOppgaver(endreOmMentor.getMentorOppgaver());
    }
}


