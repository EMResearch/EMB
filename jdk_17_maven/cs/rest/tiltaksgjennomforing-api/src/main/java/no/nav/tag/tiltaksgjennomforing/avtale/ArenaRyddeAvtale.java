package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class ArenaRyddeAvtale {
    @Id
    private UUID id = UUID.randomUUID();
    @OneToOne
    @JoinColumn(name = "avtale")
    private Avtale avtale;
    private LocalDate migreringsdato;
}
