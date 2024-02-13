package no.nav.tag.tiltaksgjennomforing;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.AvtaleRepository;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("testdata")
public class LastInnTestData implements ApplicationListener<ApplicationReadyEvent> {
    private final AvtaleRepository avtaleRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (avtaleRepository.count() > 0) return;
        log.info("Laster testdata");

        avtaleRepository.save(TestData.enLonnstilskuddAvtaleGodkjentAvVeilederUtenTilskuddsperioder());
        avtaleRepository.save(TestData.enArbeidstreningAvtale());
        avtaleRepository.save(TestData.enMentorAvtaleSignert());
        avtaleRepository.save(TestData.enMentorAvtaleUsignert());
        avtaleRepository.save(TestData.enInkluderingstilskuddAvtale());
        avtaleRepository.save(TestData.enInkluderingstilskuddAvtaleUtfyltOgGodkjentAvArbeidsgiver());
        avtaleRepository.save(TestData.enAvtaleMedAltUtfylt());
        avtaleRepository.save(TestData.enAvtaleMedAltUtfyltGodkjentAvVeileder());
        avtaleRepository.save(TestData.enAvtaleMedFlereVersjoner());
        avtaleRepository.save(TestData.enAvtaleKlarForOppstart());
        Avtale lilly = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        lilly.getGjeldendeInnhold().setGodkjentAvArbeidsgiver(Now.localDateTime());
        avtaleRepository.save(lilly);
        avtaleRepository.save(TestData.enLonnstilskuddAvtaleGodkjentAvVeileder());

        avtaleRepository.save(TestData.enLonnstilskuddAvtaleGodkjentAvVeilederTilbakeITid());
        Now.fixedDate(LocalDate.of(2021, 6, 1));
        avtaleRepository.save(TestData.enSommerjobbAvtaleGodkjentAvVeileder());
        avtaleRepository.save(TestData.enSommerjobbAvtaleGodkjentAvBeslutter());
        avtaleRepository.save(TestData.enSommerjobbAvtaleGodkjentAvArbeidsgiver());
        Now.resetClock();
        avtaleRepository.save(TestData.enMidlertidigLonnstilskuddAvtaleMedSpesieltTilpassetInnsatsGodkjentAvVeileder());
        avtaleRepository.save(TestData.enMentorAvtaleMedMedAltUtfylt());
        avtaleRepository.save(TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordelt());
        avtaleRepository.save(TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedGeografiskEnhet());
        avtaleRepository.save(TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedGeografiskEnhet());
        avtaleRepository.save(TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhet());
        avtaleRepository.save(TestData.enAvtaleOpprettetAvArbeidsgiver(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD));
        avtaleRepository.save(TestData.enAvtaleOpprettetAvArbeidsgiver(Tiltakstype.VARIG_LONNSTILSKUDD));
        avtaleRepository.save(TestData.enVarigLonnstilskuddAvtaleMedBehandletIArenaPerioder());




    }
}
