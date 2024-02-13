package no.nav.tag.tiltaksgjennomforing.varsel;

import java.util.List;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;

public class VarselFactory {
    private final Avtale avtale;
    private final Avtalerolle utførtAv;
    private final Identifikator utførtAvIdentifikator;
    private final HendelseType hendelseType;

    public VarselFactory(Avtale avtale, Avtalerolle utførtAv, Identifikator utførtAvIdentifikator, HendelseType hendelseType) {
        this.avtale = avtale;
        this.hendelseType = hendelseType;
        this.utførtAv = utførtAv;
        this.utførtAvIdentifikator = utførtAvIdentifikator;
    }

    public Varsel deltaker() {
        return Varsel.nyttVarsel(avtale.getDeltakerFnr(), utførtAv != Avtalerolle.DELTAKER, avtale, Avtalerolle.DELTAKER, utførtAv, utførtAvIdentifikator, hendelseType, avtale.getId());
    }

    public Varsel arbeidsgiver() {
        return Varsel.nyttVarsel(avtale.getBedriftNr(), utførtAv != Avtalerolle.ARBEIDSGIVER, avtale, Avtalerolle.ARBEIDSGIVER, utførtAv, utførtAvIdentifikator, hendelseType, avtale.getId());
    }


    //TODO: Hent IDENTEN til beslutter her og ikke bare veileder
    public Varsel veileder() {
        return Varsel.nyttVarsel(avtale.getVeilederNavIdent(), utførtAv != Avtalerolle.VEILEDER, avtale, Avtalerolle.VEILEDER, utførtAv, utførtAvIdentifikator, hendelseType, avtale.getId());
    }

    public Varsel mentor() {
        return Varsel.nyttVarsel(avtale.getMentorFnr(), utførtAv != Avtalerolle.MENTOR, avtale, Avtalerolle.MENTOR, utførtAv, utførtAvIdentifikator, hendelseType, avtale.getId());
    }

    public List<Varsel> alleParter() {
        return switch (avtale.getTiltakstype()){
            case MENTOR ->  List.of(deltaker(), arbeidsgiver(), veileder(), mentor());
            default ->  List.of(deltaker(), arbeidsgiver(), veileder());
        };
    }
}
