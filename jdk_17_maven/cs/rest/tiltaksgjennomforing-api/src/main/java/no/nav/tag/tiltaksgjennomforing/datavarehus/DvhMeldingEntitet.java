package no.nav.tag.tiltaksgjennomforing.datavarehus;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.Status;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "dvh_melding")
public class DvhMeldingEntitet extends AbstractAggregateRoot<DvhMeldingEntitet> {
    @Id
    private UUID meldingId;
    private UUID avtaleId;
    private LocalDateTime tidspunkt;
    @Enumerated(EnumType.STRING)
    private Status tiltakStatus;
    private String json;
    private boolean sendt;

    public DvhMeldingEntitet(UUID meldingId, UUID avtaleId, LocalDateTime tidspunkt, Status tiltakStatus, AvroTiltakHendelse avroTiltakHendelse) {
        this.meldingId = meldingId;
        this.avtaleId = avtaleId;
        this.tidspunkt = tidspunkt;
        this.tiltakStatus = tiltakStatus;
        this.json = avroTiltakHendelse.toString();
        registerEvent(new DvhMeldingOpprettet(this, avroTiltakHendelse));
    }
}
