package no.nav.tag.tiltaksgjennomforing.journalfoering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.anyIterable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.stream.Collectors;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggingService;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleInnhold;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleInnholdRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.exceptions.TilgangskontrollException;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InternalAvtaleControllerTest {

    private static final UUID AVTALE_ID_1 = UUID.randomUUID();
    private static final UUID AVTALE_ID_2 = UUID.randomUUID();
    private static final UUID AVTALE_ID_3 = UUID.randomUUID();
    private List<AvtaleInnhold> avtaleInnholdList = treAvtalerSomSkalJournalføres().stream().map(avtale -> avtale.getGjeldendeInnhold()).collect(Collectors.toList());

    private List<AvtaleInnhold> avtaleInnholdListMedTilskuddsPerioder = enAvtaleMedTilskudsPerioderSomSkalJournalføres().stream().map(avtale -> avtale.getGjeldendeInnhold()).collect(Collectors.toList());

    @InjectMocks
    private InternalAvtaleController internalAvtaleController;

    @Mock
    private InnloggingService innloggingService;

    @Mock
    private AvtaleRepository avtaleRepository;

    @Mock
    private AvtaleInnholdRepository avtaleInnholdRepository;

    @Test
    public void henterAvtalerMedTilskuddsperioderTilJournalfoering() {
        doNothing().when(innloggingService).validerSystembruker();
        when(avtaleInnholdRepository.finnAvtaleVersjonerTilJournalfoering()).thenReturn(avtaleInnholdListMedTilskuddsPerioder);
        List<AvtaleTilJournalfoering> avtalerTilJournalfoering = internalAvtaleController.hentIkkeJournalfoerteAvtaler();
        assertThat(avtalerTilJournalfoering).hasSize(1);
        assertThat(avtalerTilJournalfoering.get(0).getTilskuddsPerioder()).hasSize(1);
        avtalerTilJournalfoering.forEach(avtaleTilJournalfoering -> assertNotNull(avtaleTilJournalfoering.getAvtaleId()));
    }

    @Test
    public void henterAvtalerTilJournalfoering() {
        doNothing().when(innloggingService).validerSystembruker();
        when(avtaleInnholdRepository.finnAvtaleVersjonerTilJournalfoering()).thenReturn(avtaleInnholdList);
        List<AvtaleTilJournalfoering> avtalerTilJournalfoering = internalAvtaleController.hentIkkeJournalfoerteAvtaler();
        assertThat(avtalerTilJournalfoering).hasSize(3);
        avtalerTilJournalfoering.forEach(avtaleTilJournalfoering -> assertNotNull(avtaleTilJournalfoering.getAvtaleId()));
    }

    @Test
    public void henterIkkeAvtalerTilJournalfoering() {
        doThrow(TilgangskontrollException.class).when(innloggingService).validerSystembruker();

        assertThatThrownBy(internalAvtaleController::hentIkkeJournalfoerteAvtaler).isInstanceOf(TilgangskontrollException.class);
        verify(avtaleRepository, never()).findAll();
    }

    @Test
    public void journalfoererAvtaler() {
        List<AvtaleInnhold> godkjenteAvtaleVersjoner = treAvtalerSomSkalJournalføres().stream().map(avtale -> avtale.getGjeldendeInnhold()).collect(Collectors.toList());
        Map<UUID, String> map = new HashMap<>();
        godkjenteAvtaleVersjoner.forEach(avtaleInnhold -> map.put(avtaleInnhold.getId(), "1"));

        doNothing().when(innloggingService).validerSystembruker();
        when(avtaleInnholdRepository.findAllById(map.keySet())).thenReturn(godkjenteAvtaleVersjoner);
        internalAvtaleController.journalfoerAvtaler(map);
        verify(avtaleInnholdRepository, atLeastOnce()).saveAll(anyIterable());
    }

    @Test
    public void journalfoererIkkeAvtaler() {
        doThrow(TilgangskontrollException.class).when(innloggingService).validerSystembruker();
        assertThatThrownBy(() -> internalAvtaleController.hentIkkeJournalfoerteAvtaler()).isInstanceOf(TilgangskontrollException.class);
        verify(avtaleRepository, never()).findAllById(anyIterable());
        verify(avtaleRepository, never()).saveAll(anyIterable());
    }

    private static List<Avtale> treAvtalerSomSkalJournalføres() {
        Avtale avtale = TestData.enAvtaleMedAltUtfyltGodkjentAvVeileder();
        avtale.setId(AVTALE_ID_1);
        Avtale avtale2 = TestData.enAvtaleMedFlereVersjoner();
        avtale2.getGjeldendeInnhold().setGodkjentAvVeileder(Now.localDateTime());
        avtale2.setId(AVTALE_ID_2);
        Avtale avtale3 = TestData.enAvtaleMedFlereVersjoner();
        avtale3.getGjeldendeInnhold().setGodkjentAvVeileder(Now.localDateTime());
        avtale3.setId(AVTALE_ID_3);

        return Arrays.asList(avtale, avtale2, avtale3);
    }

    private static List<Avtale> enAvtaleMedTilskudsPerioderSomSkalJournalføres() {
        Avtale avtale4 = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(Now.localDate(), Now.localDate());
        avtale4.getGjeldendeInnhold().setGodkjentAvVeileder(Now.localDateTime());
        avtale4.setId(AVTALE_ID_3);
        return Arrays.asList(avtale4);
    }
}

