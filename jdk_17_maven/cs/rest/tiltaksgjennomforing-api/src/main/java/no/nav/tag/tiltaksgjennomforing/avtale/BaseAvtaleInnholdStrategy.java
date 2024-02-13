package no.nav.tag.tiltaksgjennomforing.avtale;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import no.nav.tag.tiltaksgjennomforing.avtale.RefusjonKontaktperson.Fields;
import no.nav.tag.tiltaksgjennomforing.utils.Now;

public abstract class BaseAvtaleInnholdStrategy implements AvtaleInnholdStrategy {
    final AvtaleInnhold avtaleInnhold;

    public BaseAvtaleInnholdStrategy(AvtaleInnhold avtaleInnhold) {
        this.avtaleInnhold = avtaleInnhold;
    }

    @Override
    public void endre(EndreAvtale nyAvtale) {
        avtaleInnhold.setDeltakerFornavn(nyAvtale.getDeltakerFornavn());
        avtaleInnhold.setDeltakerEtternavn(nyAvtale.getDeltakerEtternavn());
        avtaleInnhold.setDeltakerTlf(nyAvtale.getDeltakerTlf());
        avtaleInnhold.setBedriftNavn(nyAvtale.getBedriftNavn());
        avtaleInnhold.setArbeidsgiverFornavn(nyAvtale.getArbeidsgiverFornavn());
        avtaleInnhold.setArbeidsgiverEtternavn(nyAvtale.getArbeidsgiverEtternavn());
        avtaleInnhold.setArbeidsgiverTlf(nyAvtale.getArbeidsgiverTlf());
        avtaleInnhold.setVeilederFornavn(nyAvtale.getVeilederFornavn());
        avtaleInnhold.setVeilederEtternavn(nyAvtale.getVeilederEtternavn());
        avtaleInnhold.setVeilederTlf(nyAvtale.getVeilederTlf());
        avtaleInnhold.setArbeidsoppgaver(nyAvtale.getArbeidsoppgaver());
        avtaleInnhold.setOppfolging(nyAvtale.getOppfolging());
        avtaleInnhold.setTilrettelegging(nyAvtale.getTilrettelegging());
        avtaleInnhold.setStartDato(nyAvtale.getStartDato());
        avtaleInnhold.setSluttDato(nyAvtale.getSluttDato());
        avtaleInnhold.setStillingprosent(nyAvtale.getStillingprosent());
        avtaleInnhold.setAntallDagerPerUke(nyAvtale.getAntallDagerPerUke());
        avtaleInnhold.setRefusjonKontaktperson(nyAvtale.getRefusjonKontaktperson());
    }

    @Override
    public Map<String, Object> alleFelterSomMåFyllesUt() {
        // Felter som er felles for alle tiltakstyper

        Map<String, Object> alleFelter = new HashMap<>();
        alleFelter.put(AvtaleInnhold.Fields.deltakerFornavn, avtaleInnhold.getDeltakerFornavn());
        alleFelter.put(AvtaleInnhold.Fields.deltakerEtternavn, avtaleInnhold.getDeltakerEtternavn());
        alleFelter.put(AvtaleInnhold.Fields.deltakerTlf, avtaleInnhold.getDeltakerTlf());
        alleFelter.put(AvtaleInnhold.Fields.bedriftNavn, avtaleInnhold.getBedriftNavn());
        alleFelter.put(AvtaleInnhold.Fields.arbeidsgiverFornavn, avtaleInnhold.getArbeidsgiverFornavn());
        alleFelter.put(AvtaleInnhold.Fields.arbeidsgiverEtternavn, avtaleInnhold.getArbeidsgiverEtternavn());
        alleFelter.put(AvtaleInnhold.Fields.arbeidsgiverTlf, avtaleInnhold.getArbeidsgiverTlf());
        alleFelter.put(AvtaleInnhold.Fields.veilederFornavn, avtaleInnhold.getVeilederFornavn());
        alleFelter.put(AvtaleInnhold.Fields.veilederEtternavn, avtaleInnhold.getVeilederEtternavn());
        alleFelter.put(AvtaleInnhold.Fields.veilederTlf, avtaleInnhold.getVeilederTlf());
        alleFelter.put(AvtaleInnhold.Fields.startDato, avtaleInnhold.getStartDato());
        alleFelter.put(AvtaleInnhold.Fields.sluttDato, avtaleInnhold.getSluttDato());
        alleFelter.put(AvtaleInnhold.Fields.oppfolging, avtaleInnhold.getOppfolging());
        alleFelter.put(AvtaleInnhold.Fields.tilrettelegging, avtaleInnhold.getTilrettelegging());
        if(avtaleInnhold.getRefusjonKontaktperson() != null){
            alleFelter.put(Fields.refusjonKontaktpersonFornavn, avtaleInnhold.getRefusjonKontaktperson().getRefusjonKontaktpersonFornavn());
            alleFelter.put(Fields.refusjonKontaktpersonEtternavn, avtaleInnhold.getRefusjonKontaktperson().getRefusjonKontaktpersonEtternavn());
            alleFelter.put(Fields.refusjonKontaktpersonTlf, avtaleInnhold.getRefusjonKontaktperson().getRefusjonKontaktpersonTlf());
        }
        return alleFelter;
    }

    @Override
    public void endreSluttDato(LocalDate nySluttDato) {
        avtaleInnhold.setSluttDato(nySluttDato);
        avtaleInnhold.setIkrafttredelsestidspunkt(Now.localDateTime());
    }
}
