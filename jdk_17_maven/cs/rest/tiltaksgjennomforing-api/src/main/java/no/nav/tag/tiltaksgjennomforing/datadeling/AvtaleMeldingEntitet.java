package no.nav.tag.tiltaksgjennomforing.datadeling;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.Status;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "avtale_melding")
public class AvtaleMeldingEntitet extends AbstractAggregateRoot<AvtaleMeldingEntitet> {

    @Id
    private UUID meldingId;
    private UUID avtaleId;
    @Enumerated(EnumType.STRING)
    private HendelseType hendelseType;

    @Enumerated(EnumType.STRING)
    private Status avtaleStatus;
    private LocalDateTime tidspunkt;
    private String json;
    private boolean sendt;
    private boolean sendtCompacted;

    public AvtaleMeldingEntitet(UUID meldingId, UUID avtaleId, LocalDateTime tidspunkt, HendelseType hendelseType, Status avtaleStatus, String meldingAsJson) {
        this.meldingId = meldingId;
        this.avtaleId = avtaleId;
        this.hendelseType = hendelseType;
        this.tidspunkt = tidspunkt;
        this.json = meldingAsJson;
        this.avtaleStatus = avtaleStatus;

        registerEvent(new AvtaleMeldingOpprettet(this));
    }

}
