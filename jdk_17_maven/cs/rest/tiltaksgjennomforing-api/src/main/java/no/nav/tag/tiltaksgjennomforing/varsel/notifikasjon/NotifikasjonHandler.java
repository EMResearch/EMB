package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.FeilVedSendingResponse.FeilVedSendingResponse;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.FellesResponse;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response.MutationStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifikasjonHandler {
    private final ObjectMapper objectMapper;
    private final ArbeidsgiverNotifikasjonRepository arbeidsgiverNotifikasjonRepository;


    public <T> T readResponse(String json, Class<T> contentClass) {
        try {
            return objectMapper.readValue(json, contentClass);
        } catch (IOException exception) {
            log.error("objectmapper feilet med lesing av data: ", exception);
        }
        return null;
    }

    public FellesResponse konverterResponse(Object data) {
        try {
            if (data != null) {
                return objectMapper.convertValue(data, FellesResponse.class);
            }
        } catch (Exception e) {
            log.error("feilet med convertering av data til FellesMutationResponse klasse: ", e);
        }
        return null;
    }

    public void sjekkOgSettStatusResponse(
            ArbeidsgiverNotifikasjon notifikasjon,
            FellesResponse response,
            MutationStatus vellykketStatus) {
        if (response != null) {
            if (response.get__typename().equals(vellykketStatus.getStatus())) {
                notifikasjon.setVarselSendtVellykket(true);
                if (response.get__typename().equals(MutationStatus.NY_OPPGAVE_VELLYKKET.getStatus())) {
                    notifikasjon.setNotifikasjonAktiv(true);
                }
            }
            notifikasjon.setStatusResponse(response.get__typename());
            arbeidsgiverNotifikasjonRepository.save(notifikasjon);
        }
    }

    public void saveNotifikasjon(ArbeidsgiverNotifikasjon notifikasjon) {
        arbeidsgiverNotifikasjonRepository.save(notifikasjon);
    }

    public List<ArbeidsgiverNotifikasjon> finnNotifikasjonerTilSletting(UUID id) {
        return arbeidsgiverNotifikasjonRepository.findAllByAvtaleIdForDeleting(id);
    }

    protected void oppdaterNotifikasjon(
            ArbeidsgiverNotifikasjon notifikasjon,
            ArbeidsgiverNotifikasjon notifikasjonReferanse,
            FellesResponse response,
            MutationStatus onsketStatus
    ) {
        final String typename = response.get__typename();
        if (typename.equals(onsketStatus.getStatus())) {
            notifikasjonReferanse.setNotifikasjonAktiv(false);
            notifikasjon.setVarselSendtVellykket(true);
        }
        notifikasjon.setStatusResponse(typename);
        this.saveNotifikasjon(notifikasjon);
        this.saveNotifikasjon(notifikasjonReferanse);
    }

    public void logErrorOgSettFeilmelding(String response, ArbeidsgiverNotifikasjon notifikasjon) {
        log.error("Feilet med henting av data response. Response: {}", response);
        final FeilVedSendingResponse feilmelding = readResponse(response, FeilVedSendingResponse.class);
        if (feilmelding.getErrors() != null && feilmelding.getErrors().length > 0) {
            final String message = feilmelding.getErrors()[0].getMessage();
            if (message != null) {
                notifikasjon.setStatusResponse(message);
                this.saveNotifikasjon(notifikasjon);
            }
        }
    }

    protected NotifikasjonEvent finnEllerOpprettNotifikasjonForHendelse(
            Avtale avtale,
            UUID notifikasjonReferanseId,
            HendelseType hendelseTypeForNyNotifikasjon,
            NotifikasjonService service,
            NotifikasjonParser parser,
            MutationStatus onsketStatus,
            NotifikasjonOperasjonType operasjonType) {

        NotifikasjonEvent event = new NotifikasjonEvent();
        try {
            ArbeidsgiverNotifikasjon notifikasjon = arbeidsgiverNotifikasjonRepository.
                    findArbeidsgiverNotifikasjonsByAvtaleIdAndNotifikasjonReferanseIdAndOperasjonType(avtale.getId(), notifikasjonReferanseId.toString(), operasjonType);
            if (notifikasjon != null) {
                event.setNotifikasjon(notifikasjon);
                event.setNotifikasjonFerdigBehandlet(notifikasjon.getStatusResponse() != null && notifikasjon.getStatusResponse().equals(onsketStatus.getStatus()));
                return event;
            }
        } catch (Exception e) {
            log.warn("Feilet med henting av arbeidsgiverNotifikasjon med avtaleId {} og unik NotifikasjonReferanseId {} og OperasjonType {}", avtale.getId(), notifikasjonReferanseId.toString(), operasjonType);
            event.setNotifikasjon(null);
            event.setNotifikasjonFerdigBehandlet(true);
            return event;
        }

        ArbeidsgiverNotifikasjon utfoertNotifikasjon = ArbeidsgiverNotifikasjon.nyHendelse(avtale,
                hendelseTypeForNyNotifikasjon, service, parser);
        utfoertNotifikasjon.setNotifikasjonReferanseId(notifikasjonReferanseId.toString());
        utfoertNotifikasjon.setOperasjonType(operasjonType);

        event.setNotifikasjon(utfoertNotifikasjon);
        event.setNotifikasjonFerdigBehandlet(false);
        return event;
    }

    protected List<ArbeidsgiverNotifikasjon> finnUtfoertNotifikasjon(UUID id, HendelseType hendelsetype, String statusResponse) {
        return arbeidsgiverNotifikasjonRepository
                .findArbeidsgiverNotifikasjonByAvtaleIdAndHendelseTypeAndStatusResponse(id, hendelsetype, statusResponse);
    }
}
