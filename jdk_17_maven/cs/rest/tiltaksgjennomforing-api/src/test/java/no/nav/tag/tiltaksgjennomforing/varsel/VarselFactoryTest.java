package no.nav.tag.tiltaksgjennomforing.varsel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import org.junit.jupiter.api.Test;

class VarselFactoryTest {

  @Test
  public void skal_returnere_4_parter_Mentor_Deltaker_Arbeidsgiver_Veileder_Ventor_I_VarselListe(){
    VarselFactory factory = new VarselFactory(TestData.enMentorAvtaleUsignert(), Avtalerolle.MENTOR, TestData.enNavIdent() , HendelseType.OPPRETTET);
    assertEquals(4,factory.alleParter().toArray().length);
  }

  @Test
  public void skal_returnere_3_parter_Deltaker_Arbeidsgiver_Veileder_Ventor_I_VarselListe(){
    VarselFactory factory = new VarselFactory(TestData.enArbeidstreningAvtale(), Avtalerolle.ARBEIDSGIVER, TestData.enNavIdent(), HendelseType.OPPRETTET);
    assertEquals(3,factory.alleParter().toArray().length);
  }

}