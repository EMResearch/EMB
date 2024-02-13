package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvbruttInfo {
    private LocalDate avbruttDato;
    private String avbruttGrunn;

    public void grunnErOppgitt() {
        if (avbruttGrunn == null || avbruttGrunn.isEmpty()) {
            throw new FeilkodeException(Feilkode.GRUNN_TIL_AVBRYTELSE);
        }
    }
}
