package no.nav.tag.tiltaksgjennomforing.avtale.startOgSluttDatoStrategy;

import lombok.experimental.UtilityClass;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;

@UtilityClass
public class StartOgSluttDatoStrategyFactory {
    public static StartOgSluttDatoStrategy create(Tiltakstype tiltakstype, Kvalifiseringsgruppe kvalifiseringsgruppe) {
        switch (tiltakstype) {
            case ARBEIDSTRENING:
                return new ArbeidstreningStartOgSluttDatoStrategy();
            case MIDLERTIDIG_LONNSTILSKUDD:
                return new MidlertidigLonnstilskuddStartOgSluttDatoStrategy(kvalifiseringsgruppe);
            case VARIG_LONNSTILSKUDD:
                return new VarigLonnstilskuddStartOgSluttDatoStrategy();
            case MENTOR:
                return new MentorStartOgSluttDatoStrategy(kvalifiseringsgruppe);
            case INKLUDERINGSTILSKUDD:
                return new InkluderingstilskuddStartOgSluttDatoStrategy();
            case SOMMERJOBB:
                return new SommerjobbStartOgSluttDatoStrategy();
        }
        return new StartOgSluttDatoStrategy() {
        };
    }
}
