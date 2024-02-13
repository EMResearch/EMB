package no.nav.tag.tiltaksgjennomforing.datadeling;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import org.junit.jupiter.api.Test;

public class AvtaleHendelseSchemaTest {

    @Test
    public void generer_json_schema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .without(Option.DEFINITIONS_FOR_ALL_OBJECTS);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(AvtaleMelding.class);

        System.out.println(jsonSchema.toPrettyString());
    }
}
