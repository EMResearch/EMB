package no.nav.tag.tiltaksgjennomforing.journalfoering;

import java.util.ArrayList;
import java.util.List;

import no.nav.tag.tiltaksgjennomforing.avtale.*;

public class AvtaleTilJournalfoeringMapper {

    public static AvtaleTilJournalfoering tilJournalfoering(AvtaleInnhold avtaleInnhold, Avtalerolle avtalerolle) {
        Avtale avtale = avtaleInnhold.getAvtale();

        AvtaleTilJournalfoering avtaleTilJournalfoering = new AvtaleTilJournalfoering();
        avtaleTilJournalfoering.setTiltakstype(avtale.getTiltakstype());
        avtaleTilJournalfoering.setArbeidsgiverKontonummer(avtale.getGjeldendeInnhold().getArbeidsgiverKontonummer());
        avtaleTilJournalfoering.setStillingstittel(avtale.getGjeldendeInnhold().getStillingstittel());
        avtaleTilJournalfoering.setArbeidsoppgaver(avtale.getGjeldendeInnhold().getArbeidsoppgaver());
        avtaleTilJournalfoering.setLonnstilskuddProsent(avtale.getGjeldendeInnhold().getLonnstilskuddProsent());
        avtaleTilJournalfoering.setManedslonn(avtale.getGjeldendeInnhold().getManedslonn());
        avtaleTilJournalfoering.setFeriepengesats(avtale.getGjeldendeInnhold().getFeriepengesats());
        avtaleTilJournalfoering.setArbeidsgiveravgift(avtale.getGjeldendeInnhold().getArbeidsgiveravgift());
        avtaleTilJournalfoering.setMentorFornavn(avtale.getGjeldendeInnhold().getMentorFornavn());
        avtaleTilJournalfoering.setMentorEtternavn(avtale.getGjeldendeInnhold().getMentorEtternavn());
        avtaleTilJournalfoering.setMentorOppgaver(avtale.getGjeldendeInnhold().getMentorOppgaver());
        avtaleTilJournalfoering.setMentorAntallTimer(avtale.getGjeldendeInnhold().getMentorAntallTimer());
        avtaleTilJournalfoering.setMentorTimelonn(avtale.getGjeldendeInnhold().getMentorTimelonn());
        avtaleTilJournalfoering.setGodkjentAvArbeidsgiver(avtaleInnhold.getGodkjentAvArbeidsgiver().toLocalDate());
        avtaleTilJournalfoering.setGodkjentAvVeileder(avtaleInnhold.getGodkjentAvVeileder().toLocalDate());
        avtaleTilJournalfoering.setGodkjentAvDeltaker(avtaleInnhold.getGodkjentAvDeltaker().toLocalDate());
        avtaleTilJournalfoering.setOpprettet(avtale.getOpprettetTidspunkt().toLocalDate());
        avtaleTilJournalfoering.setAvtaleId(avtale.getId());
        avtaleTilJournalfoering.setAvtaleVersjonId(avtaleInnhold.getId());
        avtaleTilJournalfoering.setDeltakerFnr(identifikatorAsString(avtale.getDeltakerFnr()));
        avtaleTilJournalfoering.setMentorFnr(identifikatorAsString(avtale.getMentorFnr()));
        avtaleTilJournalfoering.setBedriftNr(identifikatorAsString(avtale.getBedriftNr()));
        avtaleTilJournalfoering.setVeilederNavIdent(identifikatorAsString(avtale.getVeilederNavIdent()));
        avtaleTilJournalfoering.setEnhetOppfolging(avtale.getEnhetOppfolging());
        avtaleTilJournalfoering.setDeltakerFornavn(avtaleInnhold.getDeltakerFornavn());
        avtaleTilJournalfoering.setDeltakerEtternavn(avtaleInnhold.getDeltakerEtternavn());
        avtaleTilJournalfoering.setDeltakerTlf(avtaleInnhold.getDeltakerTlf());
        avtaleTilJournalfoering.setBedriftNavn(avtaleInnhold.getBedriftNavn());
        avtaleTilJournalfoering.setArbeidsgiverFornavn(avtaleInnhold.getArbeidsgiverFornavn());
        avtaleTilJournalfoering.setArbeidsgiverEtternavn(avtaleInnhold.getArbeidsgiverEtternavn());
        avtaleTilJournalfoering.setArbeidsgiverTlf(avtaleInnhold.getArbeidsgiverTlf());
        avtaleTilJournalfoering.setVeilederFornavn(avtaleInnhold.getVeilederFornavn());
        avtaleTilJournalfoering.setVeilederEtternavn(avtaleInnhold.getVeilederEtternavn());
        avtaleTilJournalfoering.setVeilederTlf(avtaleInnhold.getVeilederTlf());
        avtaleTilJournalfoering.setOppfolging(avtaleInnhold.getOppfolging());
        avtaleTilJournalfoering.setTilrettelegging(avtaleInnhold.getTilrettelegging());
        avtaleTilJournalfoering.setStartDato(avtaleInnhold.getStartDato());
        avtaleTilJournalfoering.setSluttDato(avtaleInnhold.getSluttDato());
        avtaleTilJournalfoering.setStillingprosent(avtaleInnhold.getStillingprosent());
        avtaleTilJournalfoering.setAntallDagerPerUke(avtaleInnhold.getAntallDagerPerUke());
        avtaleTilJournalfoering.setMaal(maalListToMaalTilJournalfoeringList(avtaleInnhold.getMaal()));
        avtaleTilJournalfoering.setGodkjentPaVegneGrunn(godkjentPaVegneGrunn(avtaleInnhold.getGodkjentPaVegneGrunn()));
        avtaleTilJournalfoering.setGodkjentPaVegneAv(avtaleInnhold.isGodkjentPaVegneAv());
        avtaleTilJournalfoering.setVersjon(avtaleInnhold.getVersjon());
        avtaleTilJournalfoering.setHarFamilietilknytning(avtaleInnhold.getHarFamilietilknytning());
        avtaleTilJournalfoering.setFamilietilknytningForklaring((avtaleInnhold.getFamilietilknytningForklaring()));
        avtaleTilJournalfoering.setFeriepengerBelop(avtaleInnhold.getFeriepengerBelop());
        avtaleTilJournalfoering.setOtpSats(avtaleInnhold.getOtpSats());
        avtaleTilJournalfoering.setOtpBelop(avtaleInnhold.getOtpBelop());
        avtaleTilJournalfoering.setArbeidsgiveravgiftBelop(avtaleInnhold.getArbeidsgiveravgiftBelop());
        avtaleTilJournalfoering.setSumLonnsutgifter(avtaleInnhold.getSumLonnsutgifter());
        avtaleTilJournalfoering.setSumLonnstilskudd(avtaleInnhold.getSumLonnstilskudd());
        avtaleTilJournalfoering.setStillingstype(avtaleInnhold.getStillingstype());
        avtaleTilJournalfoering.setManedslonn100pst(avtaleInnhold.getManedslonn100pst());
        avtaleTilJournalfoering.setRefusjonKontaktperson(avtaleInnhold.getRefusjonKontaktperson());
        avtaleTilJournalfoering.setInkluderingstilskuddsutgift(avtaleInnhold.getInkluderingstilskuddsutgift());
        avtaleTilJournalfoering.setInkluderingstilskuddBegrunnelse(avtaleInnhold.getInkluderingstilskuddBegrunnelse());

        if(avtalerolle != null) {
            avtaleTilJournalfoering.setAvtalerolle(avtalerolle) ;
        }
        if(avtaleInnhold.getGodkjentTaushetserklæringAvMentor() != null){
            avtaleTilJournalfoering.setGodkjentTaushetserklæringAvMentor(avtaleInnhold.getGodkjentTaushetserklæringAvMentor().toLocalDate());
        }

        return avtaleTilJournalfoering;
    }

    private static String identifikatorAsString(Identifikator id) {
        return id != null ? id.asString() : "";
    }

    private static GodkjentPaVegneGrunnTilJournalfoering godkjentPaVegneGrunn(GodkjentPaVegneGrunn grunn) {
        if (grunn == null) {
            return null;
        }

        return new GodkjentPaVegneGrunnTilJournalfoering(
                grunn.isIkkeBankId(),
                grunn.isDigitalKompetanse(),
                grunn.isReservert(),
                grunn.isArenaMigreringDeltaker()
        );
    }


    private static MaalTilJournalfoering maalToMaalTilJournalfoering(Maal maal) {
        if (maal == null) {
            return null;
        }

        MaalTilJournalfoering maalTilJournalfoering = new MaalTilJournalfoering();

        maalTilJournalfoering.setKategori(maal.getKategori().getVerdi());
        maalTilJournalfoering.setBeskrivelse(maal.getBeskrivelse());

        return maalTilJournalfoering;
    }

    private static List<MaalTilJournalfoering> maalListToMaalTilJournalfoeringList(List<Maal> list) {
        if (list == null) {
            return null;
        }
        List<MaalTilJournalfoering> list1 = new ArrayList<>(list.size());
        for (Maal maal : list) {
            list1.add(maalToMaalTilJournalfoering(maal));
        }
        return list1;
    }
}
