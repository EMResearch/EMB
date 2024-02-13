package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.familie.ba.sak.common.Utils

enum class UtdypendeVilkårsvurdering {
    VURDERING_ANNET_GRUNNLAG,
    VURDERT_MEDLEMSKAP,
    DELT_BOSTED,
    DELT_BOSTED_SKAL_IKKE_DELES,
    OMFATTET_AV_NORSK_LOVGIVNING,
    OMFATTET_AV_NORSK_LOVGIVNING_UTLAND,
    ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING,
    BARN_BOR_I_NORGE,
    BARN_BOR_I_EØS,
    BARN_BOR_I_STORBRITANNIA,
    BARN_BOR_I_NORGE_MED_SØKER,
    BARN_BOR_I_EØS_MED_SØKER,
    BARN_BOR_I_EØS_MED_ANNEN_FORELDER,
    BARN_BOR_I_STORBRITANNIA_MED_SØKER,
    BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER,
    BARN_BOR_ALENE_I_ANNET_EØS_LAND,
}

@Converter
class UtdypendeVilkårsvurderingerConverter : AttributeConverter<List<UtdypendeVilkårsvurdering>, String> {

    override fun convertToDatabaseColumn(enumListe: List<UtdypendeVilkårsvurdering>) =
        Utils.konverterEnumsTilString(enumListe)

    override fun convertToEntityAttribute(string: String?): List<UtdypendeVilkårsvurdering> =
        Utils.konverterStringTilEnums(string)
}
