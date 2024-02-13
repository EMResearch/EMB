package no.nav.tag.tiltaksgjennomforing.varsel.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class RefusjonVarselMelding {
    UUID avtaleId;
    UUID tilskuddsperiodeId;
    VarselType varselType;
    LocalDate fristForGodkjenning;
}