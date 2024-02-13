package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

import java.util.HashMap;
import java.util.Map;

public class InkluderingstilskuddStrategy extends BaseAvtaleInnholdStrategy {

    public InkluderingstilskuddStrategy(AvtaleInnhold avtaleInnhold){
        super(avtaleInnhold);
    }

    @Override
    public void endre(EndreAvtale nyAvtale) {
        sjekkTotalBeløp();

        avtaleInnhold.getInkluderingstilskuddsutgift().clear();
        avtaleInnhold.getInkluderingstilskuddsutgift().addAll(nyAvtale.getInkluderingstilskuddsutgift());
        avtaleInnhold.getInkluderingstilskuddsutgift().forEach(i -> i.setAvtaleInnhold(avtaleInnhold));
        avtaleInnhold.setInkluderingstilskuddBegrunnelse(nyAvtale.getInkluderingstilskuddBegrunnelse());
        avtaleInnhold.setHarFamilietilknytning(nyAvtale.getHarFamilietilknytning());
        avtaleInnhold.setFamilietilknytningForklaring(nyAvtale.getFamilietilknytningForklaring());

        super.endre(nyAvtale);
    }

    @Override
    public Map<String, Object> alleFelterSomMåFyllesUt() {
        var alleFelter = new HashMap<String, Object>();
        alleFelter.putAll(super.alleFelterSomMåFyllesUt());

        alleFelter.put(AvtaleInnhold.Fields.inkluderingstilskuddsutgift, avtaleInnhold.getInkluderingstilskuddsutgift());
        alleFelter.put(AvtaleInnhold.Fields.inkluderingstilskuddBegrunnelse, avtaleInnhold.getInkluderingstilskuddBegrunnelse());
        alleFelter.put(AvtaleInnhold.Fields.harFamilietilknytning, avtaleInnhold.getHarFamilietilknytning());
        if (avtaleInnhold.getHarFamilietilknytning() != null && avtaleInnhold.getHarFamilietilknytning()) {
            alleFelter.put(AvtaleInnhold.Fields.familietilknytningForklaring, avtaleInnhold.getFamilietilknytningForklaring());
        }
        return alleFelter;
    }

    private void sjekkTotalBeløp() {
        Integer MAX_SUM = 136700;
        Integer sum = avtaleInnhold.inkluderingstilskuddTotalBeløp();
        if (sum > MAX_SUM) {
            throw new FeilkodeException(Feilkode.INKLUDERINGSTILSKUDD_SUM_FOR_HØY);
        }
    }

}
