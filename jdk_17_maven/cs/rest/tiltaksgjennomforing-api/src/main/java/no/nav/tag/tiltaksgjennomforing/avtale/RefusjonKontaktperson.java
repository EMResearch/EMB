package no.nav.tag.tiltaksgjennomforing.avtale;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Embeddable
@AllArgsConstructor
@FieldNameConstants
public class RefusjonKontaktperson {

     String refusjonKontaktpersonFornavn;
     String refusjonKontaktpersonEtternavn;
     String refusjonKontaktpersonTlf;
     Boolean ønskerVarslingOmRefusjon;

     public RefusjonKontaktperson() {
          this.refusjonKontaktpersonFornavn = "";
          this.refusjonKontaktpersonEtternavn = "";
          this.refusjonKontaktpersonTlf = "";
          this.ønskerVarslingOmRefusjon = true;
     }

     public boolean erIkkeTom() {
          return refusjonKontaktpersonEtternavn != null && refusjonKontaktpersonFornavn != null && refusjonKontaktpersonTlf != null && !this.refusjonKontaktpersonEtternavn.isEmpty() && !refusjonKontaktpersonFornavn.isEmpty() && !refusjonKontaktpersonTlf.isEmpty();
     }
}
