package no.nav.tag.tiltaksgjennomforing.avtale.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GamleVerdier {
    private boolean godkjentAvDeltaker;
    private boolean godkjentAvArbeidsgiver;
}
