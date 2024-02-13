package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.request.ArbeidsgiverMutationRequest;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.request.Variables;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.MutationStatus;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.nyBeskjed.NyBeskjedResponse;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.nyOppgave.NyOppgaveResponse;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.oppgaveUtfoertByEksternId.OppgaveUtfoertResponse;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.softDeleteNotifikasjonByEksternId.Data;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.softDeleteNotifikasjonByEksternId.SoftDeleteNotifikasjonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Slf4j
@Component
@ConditionalOnProperty("tiltaksgjennomforing.notifikasjoner.enabled")
public class NotifikasjonService {
    private final RestTemplate restTemplate;
    private final NotifikasjonHandler handler;
    private final NotifikasjonerProperties notifikasjonerProperties;
    private final NotifikasjonParser notifikasjonParser;

    public NotifikasjonService(
            @Qualifier("notifikasjonerRestTemplate") RestTemplate restTemplate,
            @Autowired NotifikasjonParser notifikasjonParser,
            NotifikasjonerProperties properties,
            @Autowired NotifikasjonHandler handler
    ) {
        this.restTemplate = restTemplate;
        this.notifikasjonerProperties = properties;
        this.notifikasjonParser = notifikasjonParser;
        this.handler = handler;
    }

    private HttpEntity<String> createRequestEntity(ArbeidsgiverMutationRequest arbeidsgiverMutationRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity(arbeidsgiverMutationRequest, headers);
    }

    public String opprettNotifikasjon(ArbeidsgiverMutationRequest arbeidsgiverMutationRequest) {
        try {
            return restTemplate.postForObject(
                    notifikasjonerProperties.getUri(),
                    createRequestEntity(arbeidsgiverMutationRequest),
                    String.class);
        } catch (RestClientException exception) {
            log.error("Feil med sending av notifikasjon: ", exception);
            throw exception;
        }
    }

    public String getAvtaleLenke(Avtale avtale) {
        return notifikasjonerProperties.getLenke().concat(avtale.getId().toString())
                .concat("/?bedrift=").concat(avtale.getBedriftNr().asString());
    }

    private String opprettNyMutasjon(ArbeidsgiverNotifikasjon notifikasjon, String mutation, String merkelapp, String tekst) {
        Variables variables = new Variables();
        variables.setEksternId(notifikasjon.getId());
        variables.setVirksomhetsnummer(notifikasjon.getVirksomhetsnummer().asString());
        variables.setLenke(notifikasjon.getLenke());
        variables.setServiceCode(notifikasjon.getServiceCode().toString());
        variables.setMerkelapp(merkelapp);
        variables.setTekst(tekst);
        variables.setServiceEdition(notifikasjon.getServiceEdition().toString());
        variables.setGrupperingsId(notifikasjon.getAvtaleId());
        ArbeidsgiverMutationRequest request = new ArbeidsgiverMutationRequest(
                mutation,
                variables);
        return opprettNotifikasjon(request);
    }

    public void opprettNyBeskjed(
            ArbeidsgiverNotifikasjon notifikasjon,
            NotifikasjonMerkelapp merkelapp,
            NotifikasjonTekst tekst) {
        notifikasjon.setOperasjonType(NotifikasjonOperasjonType.SEND_BESKJED);
        final String response = opprettNyMutasjon(
                notifikasjon,
                notifikasjonParser.getNyBeskjed(),
                merkelapp.getValue(),
                tekst.getTekst());
        final NyBeskjedResponse beskjed = handler.readResponse(response, NyBeskjedResponse.class);
        if (beskjed.getData() != null) {
            handler.sjekkOgSettStatusResponse(
                    notifikasjon,
                    handler.konverterResponse(beskjed.getData().getNyBeskjed()),
                    MutationStatus.NY_BESKJED_VELLYKKET);
        } else {
            handler.logErrorOgSettFeilmelding(response, notifikasjon);
        }
    }

    public void opprettOppgave(
            ArbeidsgiverNotifikasjon notifikasjon,
            NotifikasjonMerkelapp merkelapp,
            NotifikasjonTekst tekst) {
        notifikasjon.setOperasjonType(NotifikasjonOperasjonType.SEND_OPPGAVE);
        final String response = opprettNyMutasjon(
                notifikasjon,
                notifikasjonParser.getNyOppgave(),
                merkelapp.getValue(),
                tekst.getTekst());
        final NyOppgaveResponse oppgave = handler.readResponse(response, NyOppgaveResponse.class);
        if (oppgave.getData() != null) {
            handler.sjekkOgSettStatusResponse(
                    notifikasjon,
                    handler.konverterResponse(oppgave.getData().getNyOppgave()),
                    MutationStatus.NY_OPPGAVE_VELLYKKET);
        } else {
            handler.logErrorOgSettFeilmelding(response, notifikasjon);
        }
    }

    public void oppgaveUtfoert(
            Avtale avtale,
            HendelseType hendelseTypeSomSkalMerkesUtfoert,
            MutationStatus status,
            HendelseType hendelseTypeForNyNotifikasjon) {

        final List<ArbeidsgiverNotifikasjon> notifikasjonList =
                handler.finnUtfoertNotifikasjon(avtale.getId(), hendelseTypeSomSkalMerkesUtfoert, status.getStatus());

        if (!notifikasjonList.isEmpty()) {
            notifikasjonList.forEach(n -> {
                final NotifikasjonEvent event = handler.finnEllerOpprettNotifikasjonForHendelse(
                        avtale, n.getId(), hendelseTypeForNyNotifikasjon, this, notifikasjonParser,
                        MutationStatus.OPPGAVE_UTFOERT_VELLYKKET, NotifikasjonOperasjonType.SETT_OPPGAVE_UTFOERT);

                if (!event.notifikasjonFerdigBehandlet) {

                    Variables variables = new Variables();
                    variables.setEksternId(n.getId());
                    variables.setMerkelapp(NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()).getValue());

                    final String response = opprettNotifikasjon(new ArbeidsgiverMutationRequest(
                            notifikasjonParser.getOppgaveUtfoertByEksternId(),
                            variables
                    ));

                    final OppgaveUtfoertResponse oppgaveUtfoert = handler.readResponse(response, OppgaveUtfoertResponse.class);
                    if (oppgaveUtfoert.getData() != null) {
                        handler.oppdaterNotifikasjon(event.getNotifikasjon(),
                                n,
                                handler.konverterResponse(oppgaveUtfoert.getData().getOppgaveUtfoertByEksternId()),
                                MutationStatus.OPPGAVE_UTFOERT_VELLYKKET);
                    } else {
                        handler.logErrorOgSettFeilmelding(response, event.getNotifikasjon());
                    }
                }
            });
        }
    }

    public void softDeleteNotifikasjoner(Avtale avtale) {
        final List<ArbeidsgiverNotifikasjon> notifikasjonlist =
                handler.finnNotifikasjonerTilSletting(avtale.getId());

        if (!notifikasjonlist.isEmpty()) {
            notifikasjonlist.forEach(n -> {
                final NotifikasjonEvent event = handler.finnEllerOpprettNotifikasjonForHendelse(
                        avtale, n.getId(), HendelseType.ANNULLERT, this, notifikasjonParser,
                        MutationStatus.SOFT_DELETE_NOTIFIKASJON_VELLYKKET, NotifikasjonOperasjonType.SOFTDELETE_NOTIFIKASJON);

                if (!event.notifikasjonFerdigBehandlet) {

                    Variables variables = new Variables();
                    variables.setEksternId(n.getId());
                    variables.setMerkelapp(NotifikasjonMerkelapp.getMerkelapp(avtale.getTiltakstype().getBeskrivelse()).getValue());

                    final String response = opprettNotifikasjon(new ArbeidsgiverMutationRequest(
                            notifikasjonParser.getSoftDeleteNotifikasjonByEksternId(),
                            variables));
                    final Data data = handler.readResponse(response, SoftDeleteNotifikasjonResponse.class).getData();

                    if (data != null) {
                        handler.oppdaterNotifikasjon(event.getNotifikasjon(),
                                n,
                                handler.konverterResponse(data.getSoftDeleteNotifikasjonByEksternId()),
                                MutationStatus.SOFT_DELETE_NOTIFIKASJON_VELLYKKET);
                    } else {
                        handler.logErrorOgSettFeilmelding(response, event.getNotifikasjon());
                    }
                }
            });
        }
    }
}
