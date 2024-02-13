package no.nav.tag.tiltaksgjennomforing.journalfoering;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OppgaveTilJournalFoering {
    private String tittel;
    private String beskrivelse;
    private String opplaering;
}
