package no.nav.tag.tiltaksgjennomforing.avtale;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class BeslutterOversikt {

    private String id;
    private String veilederNavIdent;
    private String deltakerFornavn;
    private String deltakerEtternavn;
    private String deltakerFnr;
    private String bedriftNavn;
    private String bedriftNr;
    private LocalDate StartDato;
    private LocalDate sluttDato;
    private String Status;
    private String antallUbehandlet;
    private LocalDateTime OpprettetTidspunkt;


    protected static List<BeslutterOversikt> getBeslutterOversikt(Page<BeslutterOversiktDTO> beslutterOversikt) {
        return beslutterOversikt.getContent().stream().map(listElement -> new BeslutterOversikt(
                listElement.getId(),
                listElement.getVeilederNavIdent().asString(),
                listElement.getDeltakerFornavn(),
                listElement.getDeltakerEtternavn(),
                listElement.getDeltakerFnr().asString(),
                listElement.getBedriftNavn(),
                listElement.getBedriftNr() != null ? listElement.getBedriftNr().asString() : null,
                listElement.getStartDato(),
                listElement.getSluttDato(),
                listElement.getStatus(),
                listElement.getAntallUbehandlet(),
                listElement.getOpprettetTidspunkt()
        )).collect(Collectors.toList());
    }

}
