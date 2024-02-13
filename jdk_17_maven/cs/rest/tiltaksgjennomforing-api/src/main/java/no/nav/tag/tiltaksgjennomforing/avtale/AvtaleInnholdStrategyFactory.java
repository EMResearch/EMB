package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AvtaleInnholdStrategyFactory {
    public AvtaleInnholdStrategy create(AvtaleInnhold avtaleInnhold, Tiltakstype tiltakstype) {
        switch (tiltakstype) {
            case ARBEIDSTRENING:
                return new ArbeidstreningStrategy(avtaleInnhold);
            case MIDLERTIDIG_LONNSTILSKUDD:
                return new MidlertidigLonnstilskuddStrategy(avtaleInnhold);
            case VARIG_LONNSTILSKUDD:
                return new VarigLonnstilskuddStrategy(avtaleInnhold);
            case MENTOR:
                return new MentorStrategy(avtaleInnhold);
            case INKLUDERINGSTILSKUDD:
                return new InkluderingstilskuddStrategy(avtaleInnhold);
            case SOMMERJOBB:
                return new SommerjobbStrategy(avtaleInnhold);
        }
        throw new IllegalStateException();
    }
}
