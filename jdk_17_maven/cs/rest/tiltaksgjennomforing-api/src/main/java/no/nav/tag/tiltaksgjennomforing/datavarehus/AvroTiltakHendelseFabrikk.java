package no.nav.tag.tiltaksgjennomforing.datavarehus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;

@UtilityClass
public class AvroTiltakHendelseFabrikk {
    public static AvroTiltakHendelse konstruer(Avtale avtale, LocalDateTime tidspunkt, UUID meldingId, DvhHendelseType hendelseType, String utførtAv) {
        AvroTiltakHendelse hendelse = new AvroTiltakHendelse();
        hendelse.setMeldingId(meldingId.toString());
        hendelse.setTidspunkt(toInstant(tidspunkt));
        hendelse.setAvtaleId(avtale.getId().toString());
        hendelse.setAvtaleInnholdId(avtale.getGjeldendeInnhold().getId().toString());
        hendelse.setTiltakstype(TiltakType.valueOf(avtale.getTiltakstype().name()));
        hendelse.setTiltakskodeArena(avtale.getTiltakstype().getTiltakskodeArena() != null ? TiltakKodeArena.valueOf(avtale.getTiltakstype().getTiltakskodeArena()) : null);
        hendelse.setHendelseType(hendelseType.name());
        hendelse.setTiltakStatus(avtale.statusSomEnum().name());
        hendelse.setDeltakerFnr(avtale.getDeltakerFnr().asString());
        hendelse.setBedriftNr(avtale.getBedriftNr().asString());
        hendelse.setVeilederNavIdent(avtale.getVeilederNavIdent().asString());
        hendelse.setHarFamilietilknytning(avtale.getGjeldendeInnhold().getHarFamilietilknytning());
        hendelse.setStartDato(avtale.getGjeldendeInnhold().getStartDato());
        hendelse.setSluttDato(avtale.getGjeldendeInnhold().getSluttDato());
        hendelse.setStillingprosent(avtale.getGjeldendeInnhold().getStillingprosent());
        hendelse.setAntallDagerPerUke(avtale.getGjeldendeInnhold().getAntallDagerPerUke());
        hendelse.setStillingstittel(avtale.getGjeldendeInnhold().getStillingstittel());
        hendelse.setStillingstype(avtale.getGjeldendeInnhold().getStillingstype() != null ? StillingType.valueOf(avtale.getGjeldendeInnhold().getStillingstype().name()) : null);
        hendelse.setStillingStyrk08(avtale.getGjeldendeInnhold().getStillingStyrk08());
        hendelse.setStillingKonseptId(avtale.getGjeldendeInnhold().getStillingKonseptId());
        hendelse.setLonnstilskuddProsent(avtale.getGjeldendeInnhold().getLonnstilskuddProsent());
        hendelse.setManedslonn(avtale.getGjeldendeInnhold().getManedslonn());
        hendelse.setFeriepengesats(avtale.getGjeldendeInnhold().getFeriepengesats() != null ? avtale.getGjeldendeInnhold().getFeriepengesats().floatValue() : null);
        hendelse.setFeriepengerBelop(avtale.getGjeldendeInnhold().getFeriepengerBelop());
        hendelse.setArbeidsgiveravgift(avtale.getGjeldendeInnhold().getArbeidsgiveravgift() != null ? avtale.getGjeldendeInnhold().getArbeidsgiveravgift().floatValue() : null);
        hendelse.setArbeidsgiveravgiftBelop(avtale.getGjeldendeInnhold().getFeriepengerBelop());
        hendelse.setOtpSats(avtale.getGjeldendeInnhold().getOtpSats() != null ? avtale.getGjeldendeInnhold().getOtpSats().floatValue() : null);
        hendelse.setOtpBelop(avtale.getGjeldendeInnhold().getOtpBelop());
        hendelse.setSumLonnsutgifter(avtale.getGjeldendeInnhold().getSumLonnsutgifter());
        hendelse.setSumLonnstilskudd(avtale.getGjeldendeInnhold().getSumLonnstilskudd());
        hendelse.setSumLonnstilskuddRedusert(avtale.getGjeldendeInnhold().getSumLønnstilskuddRedusert());
        hendelse.setDatoForRedusertProsent(avtale.getGjeldendeInnhold().getDatoForRedusertProsent());
        hendelse.setGodkjentPaVegneAv(avtale.getGjeldendeInnhold().isGodkjentPaVegneAv());
        hendelse.setIkkeBankId(avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn() != null && avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn().isIkkeBankId());
        hendelse.setReservert(avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn() != null && avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn().isReservert());
        hendelse.setDigitalKompetanse(avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn() != null && avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn().isDigitalKompetanse());
        hendelse.setArenaMigreringDeltaker(avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn() != null && avtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn().isArenaMigreringDeltaker());
        hendelse.setGodkjentAvDeltaker(toInstant(avtale.getGjeldendeInnhold().getGodkjentAvDeltaker()));
        hendelse.setGodkjentAvArbeidsgiver(toInstant(avtale.getGjeldendeInnhold().getGodkjentAvArbeidsgiver()));
        hendelse.setGodkjentAvArbeidsgiver(toInstant(avtale.getGjeldendeInnhold().getGodkjentAvArbeidsgiver()));
        hendelse.setGodkjentAvVeileder(toInstant(avtale.getGjeldendeInnhold().getGodkjentAvVeileder()));
        hendelse.setGodkjentAvBeslutter(toInstant(avtale.getGjeldendeInnhold().getGodkjentAvBeslutter()));
        hendelse.setAvtaleInngaatt(toInstant(avtale.getGjeldendeInnhold().getAvtaleInngått()));
        hendelse.setUtfortAv(utførtAv);
        hendelse.setEnhetOppfolging(avtale.getEnhetOppfolging());
        hendelse.setEnhetGeografisk(avtale.getEnhetGeografisk());
        hendelse.setOpprettetAvArbeidsgiver(avtale.isOpprettetAvArbeidsgiver());
        hendelse.setAnnullertTidspunkt(avtale.getAnnullertTidspunkt());
        hendelse.setAnnullertGrunn(avtale.getAnnullertGrunn());
        hendelse.setMaster(erMaster(avtale));
        return hendelse;
    }

    private Boolean erMaster(Avtale avtale) {
        if(avtale.getTiltakstype() == Tiltakstype.SOMMERJOBB || avtale.getTiltakstype() == Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD || avtale.getTiltakstype() == Tiltakstype.VARIG_LONNSTILSKUDD) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private static Instant toInstant(LocalDateTime tidspunkt) {
        if (tidspunkt == null) {
            return null;
        }
        return tidspunkt.atZone(ZoneId.systemDefault()).toInstant();
    }
}
