package no.nav.tag.tiltaksgjennomforing.sporingslogg;

import lombok.Data;
import lombok.EqualsAndHashCode;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.utils.Now;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class Sporingslogg {
    @Id
    private UUID id;
    private LocalDateTime tidspunkt;
    private UUID avtaleId;
    @Enumerated(EnumType.STRING)
    private HendelseType hendelseType;

    public static Sporingslogg nyHendelse(Avtale avtale, HendelseType hendelseType) {
        Sporingslogg sporingslogg = new Sporingslogg();
        sporingslogg.id = UUID.randomUUID();
        sporingslogg.tidspunkt = Now.localDateTime();
        sporingslogg.avtaleId = avtale.getId();
        sporingslogg.hendelseType = hendelseType;
        return sporingslogg;
    }
}
