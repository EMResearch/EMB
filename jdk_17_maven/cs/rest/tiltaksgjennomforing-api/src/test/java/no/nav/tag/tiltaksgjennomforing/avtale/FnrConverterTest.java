package no.nav.tag.tiltaksgjennomforing.avtale;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FnrConverterTest {

  @Test
  public void skalReturnereNullOmVerdiErNull(){
    assertNull(new FnrConverter().convertToDatabaseColumn(null));
  }

}