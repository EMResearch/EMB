package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.MutationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"tiltaksgjennomforing.notifikasjoner.enabled=true"})
@ActiveProfiles({Miljø.LOCAL, "wiremock"})
@DirtiesContext
public class NotifikasjonServiceTest {

    @Autowired
    NotifikasjonService notifikasjonService;

    @Autowired
    NotifikasjonParser parser;

    @Autowired
    ArbeidsgiverNotifikasjonRepository arbeidsgiverNotifikasjonRepository;

    @Autowired
    AvtaleRepository avtaleRepository;

    Avtale avtale;
    ArbeidsgiverNotifikasjon notifikasjon;

    @BeforeEach
    public void init() {
        avtale = TestData.enArbeidstreningAvtale();
        avtaleRepository.save(avtale);
        notifikasjon = ArbeidsgiverNotifikasjon.nyHendelse(
                avtale,
                HendelseType.OPPRETTET,
                notifikasjonService,
                parser);
    }

    private List<ArbeidsgiverNotifikasjon> finnAntallNotifikasjonerMedGittMutasjonStatus(
            List<ArbeidsgiverNotifikasjon> notifikasjonList, MutationStatus onsketStatus) {
        return notifikasjonList.stream().
                filter(n -> n.getStatusResponse().equals(onsketStatus.getStatus())).collect(Collectors.toList());
    }

    @Test
    public void opprettNyBeskjedTest() {
        arbeidsgiverNotifikasjonRepository.deleteAll();
        notifikasjonService.opprettNyBeskjed(
                notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);

        assertThat(arbeidsgiverNotifikasjonRepository.findAllByAvtaleId(avtale.getId())).isNotEmpty();
    }

    @Test
    public void opprettNyOppgaveTest() {
        arbeidsgiverNotifikasjonRepository.deleteAll();
        notifikasjonService.opprettOppgave(
                notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);

        assertThat(arbeidsgiverNotifikasjonRepository.
                findArbeidsgiverNotifikasjonByAvtaleIdAndVarselSendtVellykketAndNotifikasjonAktiv(
                        avtale.getId(),
                        true,
                        true))
                .isNotEmpty();
    }

    @Test
    public void findArbeidsgiverNotifikasjonByIdAndHendelseTypeAndStatusResponseTest() {
        arbeidsgiverNotifikasjonRepository.deleteAll();
        notifikasjonService.opprettOppgave(
                notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);

        List<ArbeidsgiverNotifikasjon> notifikasjonList =
                arbeidsgiverNotifikasjonRepository.
                        findArbeidsgiverNotifikasjonByAvtaleIdAndHendelseTypeAndStatusResponse(
                                avtale.getId(),
                                this.notifikasjon.getHendelseType(),
                                MutationStatus.NY_OPPGAVE_VELLYKKET.getStatus());
        ArbeidsgiverNotifikasjon notifikasjon = notifikasjonList.get(0);

        assertThat(notifikasjon).isNotNull();
        assertThat(notifikasjon.getAvtaleId()).isEqualTo(avtale.getId());
        assertThat(notifikasjon.getHendelseType()).isEqualTo(HendelseType.OPPRETTET);
        assertThat(notifikasjon.getStatusResponse()).isEqualTo(MutationStatus.NY_OPPGAVE_VELLYKKET.getStatus());
    }

    @Test
    public void settOppgaveUtfoertTest() {
        arbeidsgiverNotifikasjonRepository.deleteAll();
        notifikasjonService.opprettOppgave(
                notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);

        List<ArbeidsgiverNotifikasjon> notifikasjonList =
                arbeidsgiverNotifikasjonRepository.
                        findArbeidsgiverNotifikasjonByAvtaleIdAndVarselSendtVellykketAndNotifikasjonAktiv(
                                avtale.getId(),
                                true,
                                true);
        ArbeidsgiverNotifikasjon notifikasjon = notifikasjonList.get(0);

        assertThat(notifikasjonList.get(0)).isNotNull();
        assertThat(notifikasjon.getAvtaleId()).isEqualTo(avtale.getId());
        assertThat(notifikasjon.getStatusResponse()).isEqualTo(MutationStatus.NY_OPPGAVE_VELLYKKET.getStatus());
        assertThat(notifikasjon.isNotifikasjonAktiv()).isTrue();

        notifikasjonService.oppgaveUtfoert(
                avtale,
                HendelseType.OPPRETTET,
                MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);

        List<ArbeidsgiverNotifikasjon> oppdatertNotifikasjonList =
                arbeidsgiverNotifikasjonRepository.findAllByAvtaleId(avtale.getId());

        assertThat(oppdatertNotifikasjonList.get(0)).isNotNull();
        assertThat(oppdatertNotifikasjonList.get(0).getAvtaleId()).isEqualTo(avtale.getId());
        assertThat(oppdatertNotifikasjonList.get(0).isNotifikasjonAktiv()).isFalse();
        assertThat(oppdatertNotifikasjonList.get(1)).isNotNull();
        assertThat(oppdatertNotifikasjonList.get(1).getAvtaleId()).isEqualTo(avtale.getId());
        assertThat(oppdatertNotifikasjonList.get(1).getStatusResponse())
                .isEqualTo(MutationStatus.OPPGAVE_UTFOERT_VELLYKKET.getStatus());
    }

    @Test
    public void oppGaveUtfoertSkalKunlagresEnGangPrHendelse() {
        arbeidsgiverNotifikasjonRepository.deleteAll();
        notifikasjonService.opprettOppgave(
                notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);

        notifikasjonService.oppgaveUtfoert(
                avtale,
                HendelseType.OPPRETTET,
                MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);

        notifikasjonService.oppgaveUtfoert(
                avtale,
                HendelseType.OPPRETTET,
                MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);

        List<ArbeidsgiverNotifikasjon> oppdatertNotifikasjonList =
                arbeidsgiverNotifikasjonRepository.findAllByAvtaleId(avtale.getId());

        assertThat(oppdatertNotifikasjonList.size()).isEqualTo(2);
    }

    @Test
    public void softDeleteNotifikasjonTest() {
        arbeidsgiverNotifikasjonRepository.deleteAll();
        notifikasjonService.opprettOppgave(
                notifikasjon,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);

        List<ArbeidsgiverNotifikasjon> notifikasjonList =
                arbeidsgiverNotifikasjonRepository.findAllByAvtaleId(avtale.getId());
        ArbeidsgiverNotifikasjon notifikasjon = notifikasjonList.get(0);

        assertThat(notifikasjonList.get(0)).isNotNull();
        assertThat(notifikasjon.getStatusResponse()).isEqualTo(MutationStatus.NY_OPPGAVE_VELLYKKET.getStatus());
        assertThat(notifikasjon.isNotifikasjonAktiv()).isTrue();

        notifikasjonService.softDeleteNotifikasjoner(avtale);

        List<ArbeidsgiverNotifikasjon> oppdatertNotifikasjonList =
                arbeidsgiverNotifikasjonRepository.findAllByAvtaleId(avtale.getId());

        assertThat(oppdatertNotifikasjonList.get(0)).isNotNull();
        assertThat(oppdatertNotifikasjonList.get(0).getAvtaleId()).isEqualTo(avtale.getId());
        assertThat(oppdatertNotifikasjonList.get(0).isNotifikasjonAktiv()).isFalse();
        assertThat(oppdatertNotifikasjonList.get(1).getStatusResponse())
                .isEqualTo(MutationStatus.SOFT_DELETE_NOTIFIKASJON_VELLYKKET.getStatus());
    }

    @Test
    public void softDeleteSkalIkkeOverskriveOppgaveUtfoertReferanseId() {
        arbeidsgiverNotifikasjonRepository.deleteAll();

        final ArbeidsgiverNotifikasjon not_avtaleOpprettet =
                ArbeidsgiverNotifikasjon.nyHendelse(avtale, HendelseType.OPPRETTET, notifikasjonService, parser);

        final ArbeidsgiverNotifikasjon not_avtaleInngattBeskjed =
                ArbeidsgiverNotifikasjon.nyHendelse(avtale, HendelseType.AVTALE_INNGÅTT, notifikasjonService, parser);

        notifikasjonService.opprettOppgave(not_avtaleOpprettet,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_OPPRETTET);

        notifikasjonService.oppgaveUtfoert(avtale,
                HendelseType.OPPRETTET, MutationStatus.NY_OPPGAVE_VELLYKKET, HendelseType.AVTALE_INNGÅTT);

        notifikasjonService.opprettNyBeskjed(not_avtaleInngattBeskjed,
                NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()),
                NotifikasjonTekst.TILTAK_AVTALE_INNGATT);

        final List<ArbeidsgiverNotifikasjon> allByAvtaleId =
                arbeidsgiverNotifikasjonRepository.findAllByAvtaleId(avtale.getId());

        assertThat(allByAvtaleId.size()).isEqualTo(3);

        notifikasjonService.softDeleteNotifikasjoner(avtale);

        final List<ArbeidsgiverNotifikasjon> allByAvtaleIdAfterSoftDelete =
                arbeidsgiverNotifikasjonRepository.findAll();

        assertThat(finnAntallNotifikasjonerMedGittMutasjonStatus(allByAvtaleIdAfterSoftDelete, MutationStatus.NY_OPPGAVE_VELLYKKET).size()).
                isEqualTo(1);
        assertThat(finnAntallNotifikasjonerMedGittMutasjonStatus(allByAvtaleIdAfterSoftDelete, MutationStatus.OPPGAVE_UTFOERT_VELLYKKET).size()).
                isEqualTo(1);
        assertThat(finnAntallNotifikasjonerMedGittMutasjonStatus(allByAvtaleIdAfterSoftDelete, MutationStatus.NY_BESKJED_VELLYKKET).size()).
                isEqualTo(1);
        assertThat(finnAntallNotifikasjonerMedGittMutasjonStatus(allByAvtaleIdAfterSoftDelete, MutationStatus.SOFT_DELETE_NOTIFIKASJON_VELLYKKET).size()).
                isEqualTo(2);
    }

}
