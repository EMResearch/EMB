package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Data
@Component
@ConfigurationProperties(prefix = "tiltaksgjennomforing.tilskuddsperioder")
public class TilskuddsperiodeConfig {
    private EnumSet<Tiltakstype> tiltakstyper = EnumSet.allOf(Tiltakstype.class);
}
