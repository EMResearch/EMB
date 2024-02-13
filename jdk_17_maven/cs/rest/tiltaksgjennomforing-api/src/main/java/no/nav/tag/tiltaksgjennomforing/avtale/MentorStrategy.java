package no.nav.tag.tiltaksgjennomforing.avtale;

import java.util.HashMap;
import java.util.Map;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleInnhold.Fields;

public class MentorStrategy extends BaseAvtaleInnholdStrategy {

    public MentorStrategy(AvtaleInnhold avtaleInnhold) {
        super(avtaleInnhold);
    }

    @Override
    public void endre(EndreAvtale nyAvtale) {
        avtaleInnhold.setMentorFornavn(nyAvtale.getMentorFornavn());
        avtaleInnhold.setMentorEtternavn(nyAvtale.getMentorEtternavn());
        avtaleInnhold.setMentorOppgaver(nyAvtale.getMentorOppgaver());
        avtaleInnhold.setMentorAntallTimer(nyAvtale.getMentorAntallTimer());
        avtaleInnhold.setMentorTlf(nyAvtale.getMentorTlf());
        avtaleInnhold.setMentorTimelonn(nyAvtale.getMentorTimelonn());
        avtaleInnhold.setHarFamilietilknytning(nyAvtale.getHarFamilietilknytning());
        avtaleInnhold.setFamilietilknytningForklaring(nyAvtale.getFamilietilknytningForklaring());
        super.endre(nyAvtale);
    }

    @Override
    public Map<String, Object> alleFelterSomMåFyllesUt() {
        var alleFelter = new HashMap<String, Object>();
        alleFelter.putAll(super.alleFelterSomMåFyllesUt());
        alleFelter.put(AvtaleInnhold.Fields.mentorFornavn, avtaleInnhold.getMentorFornavn());
        alleFelter.put(AvtaleInnhold.Fields.mentorEtternavn, avtaleInnhold.getMentorEtternavn());
        alleFelter.put(AvtaleInnhold.Fields.mentorOppgaver, avtaleInnhold.getMentorOppgaver());
        alleFelter.put(AvtaleInnhold.Fields.mentorAntallTimer, avtaleInnhold.getMentorAntallTimer());
        alleFelter.put(AvtaleInnhold.Fields.mentorTimelonn, avtaleInnhold.getMentorTimelonn());
        alleFelter.put(Fields.mentorTlf, avtaleInnhold.getMentorTlf());
        alleFelter.put(AvtaleInnhold.Fields.harFamilietilknytning, avtaleInnhold.getHarFamilietilknytning());
        if (avtaleInnhold.getHarFamilietilknytning() != null && avtaleInnhold.getHarFamilietilknytning()) {
            alleFelter.put(AvtaleInnhold.Fields.familietilknytningForklaring, avtaleInnhold.getFamilietilknytningForklaring());
        }

        return alleFelter;
    }
}
