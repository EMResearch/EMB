package no.nav.tag.tiltaksgjennomforing.avtale;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class FnrConverter implements AttributeConverter<Fnr, String> {

    @Override
    public String convertToDatabaseColumn(Fnr attribute) {
        if(attribute == null) return null;
        return attribute.asString();
    }

    @Override
    public Fnr convertToEntityAttribute(String dbData) {
        return new Fnr(dbData);
    }
}
