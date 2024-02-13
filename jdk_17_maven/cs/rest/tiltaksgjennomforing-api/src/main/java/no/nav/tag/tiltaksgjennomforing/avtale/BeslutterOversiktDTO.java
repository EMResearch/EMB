package no.nav.tag.tiltaksgjennomforing.avtale;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface BeslutterOversiktDTO {
        String getId();
        Integer getAvtaleNr();
        Tiltakstype getTiltakstype();
        NavIdent getVeilederNavIdent();
        String getDeltakerFornavn();
        String getDeltakerEtternavn();
        Fnr getDeltakerFnr();
        String getBedriftNavn();
        BedriftNr getBedriftNr();
        LocalDate getStartDato();
        LocalDate getSluttDato();
        String getStatus();
        String getAntallUbehandlet();
        LocalDateTime getOpprettetTidspunkt();
        LocalDateTime getSistEndret();
}
