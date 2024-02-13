package no.nav.tag.tiltaksgjennomforing.avtale;

import java.time.LocalDate;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilLonnstilskuddsprosentException;

import java.util.Map;

/**
 * TODO: Kalkulering av redusert prosent og redusert dato bør kun skje her og ikke i @see avtale.java og heller ikke i frontend
 *
 */
public class VarigLonnstilskuddStrategy extends LonnstilskuddStrategy {

    public static final int GRENSE_68_PROSENT_ETTER_12_MND = 68;
    public static final int MAX_67_PROSENT_ETTER_12_MND = 67;

    @Override
    public Map<String, Object> alleFelterSomMåFyllesUt() {
        Map<String, Object> felterSomMåFyllesUt = super.alleFelterSomMåFyllesUt();
        felterSomMåFyllesUt.put(AvtaleInnhold.Fields.lonnstilskuddProsent, avtaleInnhold.getLonnstilskuddProsent());
        return felterSomMåFyllesUt;
    }

    public VarigLonnstilskuddStrategy(AvtaleInnhold avtaleInnhold) {
        super(avtaleInnhold);
    }

    @Override
    public void endre(EndreAvtale endreAvtale) {
        if (endreAvtale.getLonnstilskuddProsent() != null && (
            endreAvtale.getLonnstilskuddProsent() < 0 || endreAvtale.getLonnstilskuddProsent() > 75)) {
            throw new FeilLonnstilskuddsprosentException();
        }
        avtaleInnhold.setLonnstilskuddProsent(endreAvtale.getLonnstilskuddProsent());
        super.endre(endreAvtale);
    }

    @Override
    public void regnUtTotalLonnstilskudd() {
        super.regnUtTotalLonnstilskudd();
        regnUtDatoOgSumRedusert();
    }

    @Override
    public void reUtregnRedusertProsentOgSum() {
        regnUtDatoOgSumRedusert();
    }

    private LocalDate getDatoForRedusertProsent(LocalDate startDato, LocalDate sluttDato, Integer lonnstilskuddprosent) {
        if (startDato == null || sluttDato == null || lonnstilskuddprosent == null) {
            return null;
        }
        if (startDato.plusYears(1).minusDays(1).isBefore(sluttDato)) {
            return startDato.plusYears(1);
        }
        return null;
    }
    private void regnUtDatoOgSumRedusert() {
        if(avtaleInnhold.getLonnstilskuddProsent() == null || avtaleInnhold.getLonnstilskuddProsent() < GRENSE_68_PROSENT_ETTER_12_MND) {
            avtaleInnhold.setDatoForRedusertProsent(null);
            avtaleInnhold.setSumLønnstilskuddRedusert(null);
            return;
        }
        LocalDate datoForRedusertProsent = getDatoForRedusertProsent(avtaleInnhold.getStartDato(), avtaleInnhold.getSluttDato(), avtaleInnhold.getLonnstilskuddProsent());
        avtaleInnhold.setDatoForRedusertProsent(datoForRedusertProsent);
        Integer sumLønnstilskuddRedusert = regnUtRedusertLønnstilskudd();
        avtaleInnhold.setSumLønnstilskuddRedusert(sumLønnstilskuddRedusert);

    }

    private Integer regnUtRedusertLønnstilskudd() {
        if (avtaleInnhold.getDatoForRedusertProsent() != null && avtaleInnhold.getLonnstilskuddProsent() != null) {
            int lonnstilskuddProsent = avtaleInnhold.getLonnstilskuddProsent();
            if(lonnstilskuddProsent >= GRENSE_68_PROSENT_ETTER_12_MND) lonnstilskuddProsent = MAX_67_PROSENT_ETTER_12_MND;
            return getSumLonnsTilskudd(avtaleInnhold.getSumLonnsutgifter(), lonnstilskuddProsent);
        } else {
            return null;
        }
    }

}
