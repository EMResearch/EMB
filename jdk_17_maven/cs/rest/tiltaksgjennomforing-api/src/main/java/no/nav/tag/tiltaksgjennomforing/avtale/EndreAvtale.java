package no.nav.tag.tiltaksgjennomforing.avtale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndreAvtale {

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
    private String arbeidsoppgaver;
    private String stillingstittel;
    private Integer stillingStyrk08;
    private Integer stillingKonseptId;
    private Integer antallDagerPerUke;

    private String refusjonKontaktpersonFornavn;
    private String refusjonKontaktpersonEtternavn;
    private String refusjonKontaktpersonTlf;
    private Boolean ønskerVarslingOmRefusjon;

    // Arbeidstreningsfelter
    private List<Maal> maal = new ArrayList<>();

    // Inkluderingstilskuddsfelter
    private List<Inkluderingstilskuddsutgift> inkluderingstilskuddsutgift = new ArrayList<>();
    private String inkluderingstilskuddBegrunnelse;

    // Lønnstilskuddsfelter
    private String arbeidsgiverKontonummer;
    private Integer lonnstilskuddProsent;
    private Integer manedslonn;
    private BigDecimal feriepengesats;
    private BigDecimal arbeidsgiveravgift;
    private Double otpSats;
    private Boolean harFamilietilknytning;
    private String familietilknytningForklaring;
    private Stillingstype stillingstype;

    // Mentorfelter
    private String mentorFornavn;
    private String mentorEtternavn;
    private String mentorOppgaver;
    private Double mentorAntallTimer;
    private String mentorTlf;
    private Integer mentorTimelonn;


    public RefusjonKontaktperson getRefusjonKontaktperson(){
        if(refusjonKontaktpersonTlf == null && refusjonKontaktpersonFornavn == null && refusjonKontaktpersonEtternavn == null) {
            return null;
        }

     return new RefusjonKontaktperson(refusjonKontaktpersonFornavn, refusjonKontaktpersonEtternavn, refusjonKontaktpersonTlf,
         ønskerVarslingOmRefusjon);
    }

    public void setRefusjonKontaktperson(RefusjonKontaktperson refusjonKontaktperson) {
        if(refusjonKontaktperson == null) { return; }
        this.refusjonKontaktpersonFornavn = refusjonKontaktperson.getRefusjonKontaktpersonFornavn();
        this.refusjonKontaktpersonEtternavn = refusjonKontaktperson.getRefusjonKontaktpersonEtternavn();
        this.refusjonKontaktpersonTlf = refusjonKontaktperson.getRefusjonKontaktpersonTlf();
        this.ønskerVarslingOmRefusjon = refusjonKontaktperson.getØnskerVarslingOmRefusjon();
    }
}
