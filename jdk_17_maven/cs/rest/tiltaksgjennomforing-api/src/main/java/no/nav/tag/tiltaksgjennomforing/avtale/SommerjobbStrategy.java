package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilLonnstilskuddsprosentException;

public class SommerjobbStrategy extends LonnstilskuddStrategy {

    public SommerjobbStrategy(AvtaleInnhold avtaleInnhold) {
        super(avtaleInnhold);
    }

    @Override
    public void endreAvtaleInnholdMedKvalifiseringsgruppe(EndreAvtale endreAvtale, Kvalifiseringsgruppe kvalifiseringsgruppe) {
        if (kvalifiseringsgruppe != null) {
            final EndreAvtale endreAvtaleMedOppdatertProsentsats = settTilskuddsprosentSats(endreAvtale, kvalifiseringsgruppe);
            super.endre(endreAvtaleMedOppdatertProsentsats);
        } else {
            sjekkSommerjobbProsentsats(endreAvtale);
            super.endre(endreAvtale);
        }
    }

    @Override
    public void endre(EndreAvtale endreAvtale) {
        endreAvtale.setStillingstype(Stillingstype.MIDLERTIDIG);
        sjekkSommerjobbProsentsats(endreAvtale);
        avtaleInnhold.setLonnstilskuddProsent(endreAvtale.getLonnstilskuddProsent());
        super.endre(endreAvtale);
    }

    private void sjekkSommerjobbProsentsats(EndreAvtale endreAvtale) {
        Integer lonnstilskuddProsent = endreAvtale.getLonnstilskuddProsent();
        if (lonnstilskuddProsent != null && !(lonnstilskuddProsent == 75 || lonnstilskuddProsent == 50)) {
            throw new FeilLonnstilskuddsprosentException();
        }
    }

    private EndreAvtale settTilskuddsprosentSats(EndreAvtale endreAvtale, Kvalifiseringsgruppe kvalifiseringsgruppe) {
        final Integer sats = kvalifiseringsgruppe.finnLonntilskuddProsentsatsUtifraKvalifiseringsgruppe(50, 75);
        endreAvtale.setLonnstilskuddProsent(sats);
        return endreAvtale;
    }

}
