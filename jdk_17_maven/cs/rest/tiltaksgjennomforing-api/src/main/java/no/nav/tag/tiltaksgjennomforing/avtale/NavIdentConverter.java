package no.nav.tag.tiltaksgjennomforing.avtale;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class NavIdentConverter implements AttributeConverter<NavIdent, String> {

    @Override
    public String convertToDatabaseColumn(NavIdent attribute) {
        if (attribute == null) return null;
        return attribute.asString();
    }

    @Override
    public NavIdent convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return new NavIdent(dbData);
    }
}
