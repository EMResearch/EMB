package no.nav.tag.tiltaksgjennomforing;

import static org.assertj.core.api.Assertions.assertThat;
import no.nav.tag.tiltaksgjennomforing.avtale.EndreAvtale;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import org.junit.jupiter.api.Test;

class TestDataTest {

  @Test
  void endring_på_alle_TestData_endre_felter_så_ingen_er_Null_felter() {
    EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
    TestData.endreMaalInfo(endreAvtale);
    TestData.endreMentorInfo(endreAvtale);
    TestData.endreInkluderingstilskuddInfo(endreAvtale);
    assertThat(endreAvtale.getMaal()).isNotEmpty();
    assertThat(endreAvtale.getInkluderingstilskuddsutgift()).isNotEmpty();
    assertThat(endreAvtale).hasNoNullFieldsOrProperties();
  }
}