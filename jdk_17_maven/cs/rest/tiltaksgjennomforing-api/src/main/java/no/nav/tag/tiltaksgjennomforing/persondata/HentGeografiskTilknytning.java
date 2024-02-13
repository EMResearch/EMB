package no.nav.tag.tiltaksgjennomforing.persondata;

import lombok.Value;

@Value
public class HentGeografiskTilknytning {
   private final String gtKommune;
   private final String gtBydel;
   private final String gtLand;
   private final String regel;

   String getGeoTilknytning(){
        if(gtBydel == null){
            return gtKommune;
        }
        return gtBydel;
    }
}
