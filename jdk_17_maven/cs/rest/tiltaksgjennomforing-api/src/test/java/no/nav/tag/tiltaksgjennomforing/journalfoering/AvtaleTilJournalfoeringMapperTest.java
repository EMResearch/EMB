package no.nav.tag.tiltaksgjennomforing.journalfoering;

import static no.nav.tag.tiltaksgjennomforing.journalfoering.AvtaleTilJournalfoeringMapper.tilJournalfoering;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.UUID;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleInnhold;
import no.nav.tag.tiltaksgjennomforing.avtale.GodkjentPaVegneGrunn;
import no.nav.tag.tiltaksgjennomforing.avtale.Maal;
import no.nav.tag.tiltaksgjennomforing.avtale.MaalKategori;
import no.nav.tag.tiltaksgjennomforing.avtale.Stillingstype;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AvtaleTilJournalfoeringMapperTest {

    private Avtale avtale;
    private AvtaleInnhold avtaleInnhold;
    private AvtaleTilJournalfoering tilJournalfoering;
    private GodkjentPaVegneGrunn grunn;

    @BeforeEach
    public void setUp() {
        avtale = TestData.enAvtaleMedAltUtfyltGodkjentAvVeileder();
        avtaleInnhold = avtale.getGjeldendeInnhold();
        grunn = new GodkjentPaVegneGrunn();
    }

    @AfterEach
    public void tearDown() {
        avtale = null;
        tilJournalfoering = null;
    }

    @Test
    public void mapper() {
        final UUID avtaleId = UUID.randomUUID();
        avtale.setId(avtaleId);
        avtale.getGjeldendeInnhold().setGodkjentPaVegneAv(true);
        avtale.setOpprettetTidspunkt(Now.localDateTime());
        avtale.getGjeldendeInnhold().setStillingstype(Stillingstype.FAST);

        tilJournalfoering = tilJournalfoering(avtaleInnhold, null);

        assertEquals(avtaleId.toString(), tilJournalfoering.getAvtaleId().toString());
        assertEquals(avtaleInnhold.getId().toString(), tilJournalfoering.getAvtaleVersjonId().toString());
        assertEquals(avtale.getDeltakerFnr().asString(), tilJournalfoering.getDeltakerFnr());
        assertEquals(avtale.getBedriftNr().asString(), tilJournalfoering.getBedriftNr());
        assertEquals(avtale.getVeilederNavIdent().asString(), tilJournalfoering.getVeilederNavIdent());
        assertEquals(avtale.getOpprettetTidspunkt().toLocalDate(), tilJournalfoering.getOpprettet());
        assertEquals(avtale.getGjeldendeInnhold().getDeltakerFornavn(), tilJournalfoering.getDeltakerFornavn());
        assertEquals(avtale.getGjeldendeInnhold().getDeltakerEtternavn(), tilJournalfoering.getDeltakerEtternavn());
        assertEquals(avtale.getGjeldendeInnhold().getDeltakerTlf(), tilJournalfoering.getDeltakerTlf());
        assertEquals(avtale.getGjeldendeInnhold().getBedriftNavn(), tilJournalfoering.getBedriftNavn());
        assertEquals(avtale.getGjeldendeInnhold().getArbeidsgiverFornavn(), tilJournalfoering.getArbeidsgiverFornavn());
        assertEquals(avtale.getGjeldendeInnhold().getArbeidsgiverEtternavn(), tilJournalfoering.getArbeidsgiverEtternavn());
        assertEquals(avtale.getGjeldendeInnhold().getArbeidsgiverTlf(), tilJournalfoering.getArbeidsgiverTlf());
        assertEquals(avtale.getGjeldendeInnhold().getVeilederFornavn(), tilJournalfoering.getVeilederFornavn());
        assertEquals(avtale.getGjeldendeInnhold().getVeilederEtternavn(), tilJournalfoering.getVeilederEtternavn());
        assertEquals(avtale.getGjeldendeInnhold().getVeilederTlf(), tilJournalfoering.getVeilederTlf());
        assertEquals(avtale.getGjeldendeInnhold().getOppfolging(), tilJournalfoering.getOppfolging());
        assertEquals(avtale.getGjeldendeInnhold().getTilrettelegging(), tilJournalfoering.getTilrettelegging());
        assertEquals(avtale.getGjeldendeInnhold().getStartDato(), tilJournalfoering.getStartDato());
        assertEquals(avtale.getGjeldendeInnhold().getSluttDato(), tilJournalfoering.getSluttDato());
        assertEquals(avtale.getGjeldendeInnhold().getStillingprosent(), tilJournalfoering.getStillingprosent());
        assertEquals(avtale.getGjeldendeInnhold().getAntallDagerPerUke(), tilJournalfoering.getAntallDagerPerUke());
        assertEquals(avtale.getGjeldendeInnhold().getGodkjentAvDeltaker().toLocalDate(), tilJournalfoering.getGodkjentAvDeltaker());
        assertEquals(avtale.getGjeldendeInnhold().getGodkjentAvArbeidsgiver().toLocalDate(), tilJournalfoering.getGodkjentAvArbeidsgiver());
        assertEquals(avtale.getGjeldendeInnhold().getGodkjentAvVeileder().toLocalDate(), tilJournalfoering.getGodkjentAvVeileder());
        assertEquals(avtale.getGjeldendeInnhold().isGodkjentPaVegneAv(), tilJournalfoering.isGodkjentPaVegneAv());
        assertEquals(avtale.getGjeldendeInnhold().getVersjon(), tilJournalfoering.getVersjon());
        assertEquals(avtale.getTiltakstype(), tilJournalfoering.getTiltakstype());
        assertEquals(avtale.getGjeldendeInnhold().getArbeidsgiverKontonummer(), tilJournalfoering.getArbeidsgiverKontonummer());
        assertEquals(avtale.getGjeldendeInnhold().getStillingstittel(), tilJournalfoering.getStillingstittel());
        assertEquals(avtale.getGjeldendeInnhold().getArbeidsoppgaver(), tilJournalfoering.getArbeidsoppgaver());
        assertEquals(avtale.getGjeldendeInnhold().getLonnstilskuddProsent(), tilJournalfoering.getLonnstilskuddProsent());
        assertEquals(avtale.getGjeldendeInnhold().getManedslonn(), tilJournalfoering.getManedslonn());
        assertEquals(avtale.getGjeldendeInnhold().getFeriepengesats(), tilJournalfoering.getFeriepengesats());
        assertEquals(avtale.getGjeldendeInnhold().getArbeidsgiveravgift(), tilJournalfoering.getArbeidsgiveravgift());
        assertEquals(avtale.getGjeldendeInnhold().getManedslonn100pst(), tilJournalfoering.getManedslonn100pst());
        assertNotNull(avtaleInnhold.getStillingstype());
        assertEquals(avtaleInnhold.getStillingstype(), tilJournalfoering.getStillingstype());
    }

    @Test
    public void paaVegneGrunnErIkkeBankId() {
        grunn.setIkkeBankId(true);
        avtale.getGjeldendeInnhold().setGodkjentPaVegneGrunn(grunn);
        tilJournalfoering = tilJournalfoering(avtaleInnhold, null);
        assertTrue(tilJournalfoering.getGodkjentPaVegneGrunn().isIkkeBankId());
        assertFalse(tilJournalfoering.getGodkjentPaVegneGrunn().isDigitalKompetanse());
        assertFalse(tilJournalfoering.getGodkjentPaVegneGrunn().isReservert());
    }

    @Test
    public void paaVegneGrunnErDigitalKompetanse() {
        grunn.setDigitalKompetanse(true);
        avtale.getGjeldendeInnhold().setGodkjentPaVegneGrunn(grunn);
        tilJournalfoering = tilJournalfoering(avtaleInnhold, null);
        assertFalse(tilJournalfoering.getGodkjentPaVegneGrunn().isIkkeBankId());
        assertTrue(tilJournalfoering.getGodkjentPaVegneGrunn().isDigitalKompetanse());
        assertFalse(tilJournalfoering.getGodkjentPaVegneGrunn().isReservert());
    }

    @Test
    public void paaVegneGrunnErReservert() {
        grunn.setReservert(true);
        avtale.getGjeldendeInnhold().setGodkjentPaVegneGrunn(grunn);
        tilJournalfoering = tilJournalfoering(avtaleInnhold, null);
        assertFalse(tilJournalfoering.getGodkjentPaVegneGrunn().isIkkeBankId());
        assertFalse(tilJournalfoering.getGodkjentPaVegneGrunn().isDigitalKompetanse());
        assertTrue(tilJournalfoering.getGodkjentPaVegneGrunn().isReservert());

        avtale.getGjeldendeInnhold().setGodkjentPaVegneGrunn(null);
        tilJournalfoering = tilJournalfoering(avtaleInnhold, null);
        assertNull(tilJournalfoering.getGodkjentPaVegneGrunn());
    }

    @Test
    public void ingenPaaVegneGrunn() {
        avtale.getGjeldendeInnhold().setGodkjentPaVegneGrunn(null);
        tilJournalfoering = tilJournalfoering(avtaleInnhold, null);
        assertNull(tilJournalfoering.getGodkjentPaVegneGrunn());
    }

    @Test
    public void mapperMaal() {
        Maal maal = new Maal();
        maal.setKategori(MaalKategori.FÅ_JOBB_I_BEDRIFTEN);
        maal.setBeskrivelse("Beskrivelse");

        Maal maal2 = new Maal();
        maal2.setKategori(MaalKategori.UTPRØVING);
        maal2.setBeskrivelse("Beskrivelse-2");

        avtaleInnhold.setMaal(Arrays.asList(maal, maal2));

        tilJournalfoering = tilJournalfoering(avtaleInnhold, null);

        tilJournalfoering.getMaal().forEach(maalet -> {
            if (maalet.getKategori().equals(MaalKategori.FÅ_JOBB_I_BEDRIFTEN.getVerdi())) {
                assertEquals("Beskrivelse", maalet.getBeskrivelse());
            } else if (maalet.getKategori().equals(MaalKategori.UTPRØVING.getVerdi())) {
                assertEquals("Beskrivelse-2", maalet.getBeskrivelse());
            } else {
                fail("Mål; " + maalet);
            }
        });
    }

}
