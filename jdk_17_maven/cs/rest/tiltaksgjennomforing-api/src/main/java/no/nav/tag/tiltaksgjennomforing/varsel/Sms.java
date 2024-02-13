package no.nav.tag.tiltaksgjennomforing.varsel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import no.nav.tag.tiltaksgjennomforing.avtale.IdentifikatorConverter;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import no.nav.tag.tiltaksgjennomforing.varsel.events.SmsSendt;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
public class Sms extends AbstractAggregateRoot<Sms> {
    @Id
    private UUID smsVarselId;
    private String telefonnummer;
    @Convert(converter = IdentifikatorConverter.class)
    private Identifikator identifikator;
    private String meldingstekst;
    private UUID avtaleId;
    private LocalDateTime tidspunkt;
    @Enumerated(EnumType.STRING)
    private HendelseType hendelseType;
    private String avsenderApplikasjon;

    public static Sms nyttVarsel(String telefonnummer,
                                 Identifikator identifikator,
                                 String meldingstekst,
                                 HendelseType hendelseType,
                                 UUID avtaleId) {
        Sms sms = new Sms();
        sms.smsVarselId = UUID.randomUUID();
        sms.telefonnummer = telefonnummer;
        sms.identifikator = identifikator;
        sms.meldingstekst = meldingstekst;
        sms.hendelseType = hendelseType;
        sms.tidspunkt = Now.localDateTime();
        sms.avtaleId = avtaleId;
        sms.avsenderApplikasjon = "tiltaksgjennomforing-api";
        sms.registerEvent(new SmsSendt(sms));
        return sms;
    }
}
