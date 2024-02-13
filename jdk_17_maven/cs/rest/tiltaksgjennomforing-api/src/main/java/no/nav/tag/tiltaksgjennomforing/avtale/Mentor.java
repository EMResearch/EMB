package no.nav.tag.tiltaksgjennomforing.avtale;

import java.util.UUID;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetBruker;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetMentor;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;
import no.nav.tag.tiltaksgjennomforing.exceptions.TilgangskontrollException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class Mentor extends Avtalepart<Fnr> {

    public Mentor(Fnr identifikator) {
        super(identifikator);
    }

    @Override
    public boolean harTilgangTilAvtale(Avtale avtale) {
        return avtale.getMentorFnr().equals(getIdentifikator());
    }

    @Override
    public Avtale hentAvtale(AvtaleRepository avtaleRepository, UUID avtaleId) {
        Avtale avtale = super.hentAvtale(avtaleRepository,avtaleId);
        if(!avtale.erGodkjentTaushetserklæringAvMentor()) throw new FeilkodeException(Feilkode.IKKE_TILGANG_TIL_AVTALE);
        return skjulDeltakerFødselsnummer(avtale);
    }

    @Override
    Page<Avtale> hentAlleAvtalerMedMuligTilgang(AvtaleRepository avtaleRepository, AvtalePredicate queryParametre, Pageable pageable) {
        Page<Avtale> avtaler = avtaleRepository.findAllByMentorFnr(getIdentifikator(), pageable);
        Page<Avtale> avtalerMedMuligTilgang = avtaler
                .map(this::gjemInnholdOmMentorIkkeHarSignertErklæring)
                .map(this::skjulDeltakerFødselsnummer);
        return avtalerMedMuligTilgang;
    }

    private Avtale gjemInnholdOmMentorIkkeHarSignertErklæring(Avtale avtale){
        if(!avtale.erGodkjentTaushetserklæringAvMentor()) {
            AvtaleInnhold innhold = AvtaleInnhold.nyttTomtInnhold(avtale.getTiltakstype());
            innhold.setBedriftNavn(avtale.getGjeldendeInnhold().getBedriftNavn());
            innhold.setAvtale(avtale);
            avtale.setGjeldendeInnhold(innhold);
            avtale.setDeltakerFnr(null);
            avtale.setVeilederNavIdent(null);
        }
        return avtale;
    }

    @Override
    public void godkjennForAvtalepart(Avtale avtale) {
        avtale.godkjennForMentor(getIdentifikator());
    }

    @Override
    public boolean kanEndreAvtale() {
        return false;
    }

    @Override
    public boolean erGodkjentAvInnloggetBruker(Avtale avtale) {
        return avtale.getMentorFnr().equals(getIdentifikator().asString()) && avtale.erGodkjentTaushetserklæringAvMentor();
    }

    @Override
    boolean kanOppheveGodkjenninger(Avtale avtale) {
        return false;
    }

    @Override
    void opphevGodkjenningerSomAvtalepart(Avtale avtale) {
        throw new TilgangskontrollException("Deltaker kan ikke oppheve godkjenninger");
    }

    private Avtale skjulDeltakerFødselsnummer(Avtale avtale){
        avtale.setDeltakerFnr(null);
        return avtale;
    }

    @Override
    protected Avtalerolle rolle() {
        return Avtalerolle.MENTOR;
    }

    @Override
    public InnloggetBruker innloggetBruker() {
        return new InnloggetMentor(getIdentifikator());
    }
}
