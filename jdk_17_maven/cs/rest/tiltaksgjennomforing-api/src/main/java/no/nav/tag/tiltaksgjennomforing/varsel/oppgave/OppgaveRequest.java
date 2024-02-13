package no.nav.tag.tiltaksgjennomforing.varsel.oppgave;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.utils.Now;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class OppgaveRequest {

    private final static String BESKRIVELSE = "Avtale er opprettet av arbeidsgiver på tiltak %s. Se avtalen under filteret 'Ufordelte' i https://tiltaksgjennomforing.intern.nav.no/tiltaksgjennomforing";
    private final static String TEMA = "TIL";
    private final static String HOY_PRI = "NORM";
    private final static String OPPG_TYPE = "VURD_HENV";
    private final static String BEHANDLINGSTYPE = "ae0034";

    private final String beskrivelse;
    private final String tema = TEMA;
    private final String prioritet = HOY_PRI;
    private final String oppgavetype = OPPG_TYPE;
    private final String behandlingstype = BEHANDLINGSTYPE;
    private final String behandlingstema;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate aktivDato = Now.localDate();
    private final String aktoerId;

    public OppgaveRequest(String aktørId, Tiltakstype tiltakstype) {
        this.aktoerId = aktørId;
        this.behandlingstema = tiltakstype.getBehandlingstema();
        this.beskrivelse = String.format(BESKRIVELSE, tiltakstype.getBeskrivelse());
    }
}