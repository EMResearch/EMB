package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.utils.Now;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "avtale_forkortet")
public class AvtaleForkortetEntitet {
    @Id
    private UUID id;
    private UUID avtaleId;
    private UUID avtaleInnholdId;
    private Instant tidspunkt;
    @Convert(converter = NavIdentConverter.class)
    private NavIdent utførtAv;
    private LocalDate nySluttDato;
    private String grunn;
    private String annetGrunn;

    public AvtaleForkortetEntitet() {
    }

    public AvtaleForkortetEntitet(Avtale avtale, AvtaleInnhold avtaleInnhold, NavIdent utførtAv, LocalDate nySluttDato, String grunn, String annetGrunn) {
        this.id = UUID.randomUUID();
        this.avtaleId = avtale.getId();
        this.avtaleInnholdId = avtaleInnhold.getId();
        this.tidspunkt = Now.instant();
        this.utførtAv = utførtAv;
        this.nySluttDato = nySluttDato;
        this.grunn = grunn;
        this.annetGrunn = annetGrunn;
    }


}
