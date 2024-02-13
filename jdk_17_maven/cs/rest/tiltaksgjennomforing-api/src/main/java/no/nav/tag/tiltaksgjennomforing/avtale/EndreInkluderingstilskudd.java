package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Value
@NoArgsConstructor
public class EndreInkluderingstilskudd {
    List<Inkluderingstilskuddsutgift> inkluderingstilskuddsutgift = new ArrayList<>();

    public Integer inkluderingstilskuddTotalBeløp() {
        return inkluderingstilskuddsutgift.stream().map(inkluderingstilskuddsutgift -> inkluderingstilskuddsutgift.getBeløp())
                .collect(Collectors.toList()).stream()
                .reduce(0, Integer::sum);
    }
}
