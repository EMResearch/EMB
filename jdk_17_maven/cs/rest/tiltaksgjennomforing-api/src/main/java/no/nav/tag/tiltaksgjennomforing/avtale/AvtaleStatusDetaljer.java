package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvtaleStatusDetaljer {
    boolean godkjentAvInnloggetBruker;
    String header;
    String infoDel1;
    String infoDel2;
    String part1;
    Boolean part1Status;
    String part2;
    Boolean part2Status;
    void setInnloggetBrukerStatus( String header,String infoDel1,String infoDel2){
        this.header=header;
        this.infoDel1=infoDel1;
        this.infoDel2=infoDel2;
    }
    void setPart1Detaljer(String part1,boolean part1Status){
        this.part1=part1;
        this.part1Status=part1Status;
    }
    void setPart2Detaljer(String part2, boolean part2Status){
        this.part2=part2;
        this.part2Status=part2Status;
    }
}
