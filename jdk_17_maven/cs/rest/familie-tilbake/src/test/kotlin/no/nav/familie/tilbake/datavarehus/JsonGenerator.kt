package no.nav.familie.tilbake.datavarehus

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNullFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class JsonGenerator {

    // Kommenter ut disabled for å generere json-skjema til datavarehus.
    @Disabled
    @Test
    fun genererSkjemaTilDatavarehus() {
        val jsonSchemaGenerator = JsonSchemaGenerator(objectMapper)
        val behandlingstilstandSchema: JsonNode = jsonSchemaGenerator.generateJsonSchema(Behandlingstilstand::class.java)
        val vedtaksoppsummeringSchema: JsonNode = jsonSchemaGenerator.generateJsonSchema(Vedtaksoppsummering::class.java)

        File("Behandlingstilstand.json").writeText(objectMapper.writeValueAsString(behandlingstilstandSchema))
        File("vedtaksoppsummering.json").writeText(objectMapper.writeValueAsString(vedtaksoppsummeringSchema))
    }
}

/**
 * Based on JsonSchemaGenerator Created by Roee Shlomo on 11/29/2016, which was
 * forked from Scala code by mbknor @ https://github.com/mbknor/mbknor-jackson-jsonSchema
 */
class JsonSchemaGenerator(private val rootObjectMapper: ObjectMapper) {

    private val customType2FormatMapping = mapOf(
        "java.time.LocalDateTime" to "datetime-local",
        "java.time.OffsetDateTime" to "datetime",
        "java.time.LocalDate" to "date",
    )

    companion object {

        @JvmStatic val JSON_SCHEMA_DRAFT_2020_12_URL = "https://json-schema.org/draft/2020-12/schema"
    }

    open class MySerializerProvider {

        private var myProvider: SerializerProvider? = null

        fun setProvider(provider: SerializerProvider?) {
            myProvider = provider
        }

        fun getProvider(): SerializerProvider {
            return myProvider!!
        }
    }

    abstract class EnumSupport {

        abstract val node: ObjectNode
        fun enumTypes(enums: MutableSet<String>?) {
            val enumValuesNode = JsonNodeFactory.instance.arrayNode()
            node.set<JsonNode>("enum", enumValuesNode)
            enums?.forEach {
                enumValuesNode.add(it)
            }
        }
    }

    private fun setFormat(node: ObjectNode, format: String) {
        node.put("format", format)
    }

    data class DefinitionInfo(val ref: String?, val jsonObjectFormatVisitor: JsonObjectFormatVisitor?)

    data class WorkInProgress(val classInProgress: Class<*>, val nodeInProgress: ObjectNode)

    // Class that manages creating new defenitions or getting $refs to existing definitions
    inner class DefinitionsHandler {

        private var class2Ref = HashMap<Class<*>, String>()

        private val definitionsNode = JsonNodeFactory.instance.objectNode()

        // Used when 'combining' multiple invocations to getOrCreateDefinition when processing polymorphism.
        private var workInProgress: WorkInProgress? = null

        private var workInProgressStack: MutableList<WorkInProgress?> = mutableListOf()

        fun pushWorkInProgress() {
            workInProgressStack.add(workInProgressStack.size, workInProgress)
            workInProgress = null
        }

        fun popworkInProgress() {
            val item = workInProgressStack.size - 1
            workInProgress = workInProgressStack.removeAt(item)
        }

        // Either creates new definitions or return $ref to existing one
        fun getOrCreateDefinition(
            clazz: Class<*>,
            objectDefinitionBuilder: (ObjectNode) -> JsonObjectFormatVisitor?,
        ): DefinitionInfo {
            val ref = class2Ref[clazz]
            if (ref != null) {
                if (workInProgress != null) {
                    // this is a recursive polymorphism call
                    if (clazz != workInProgress!!.classInProgress) {
                        throw Exception("Wrong class - working on ${workInProgress!!.classInProgress} - got $clazz")
                    }
                    return DefinitionInfo(null, objectDefinitionBuilder(workInProgress!!.nodeInProgress))
                }
                return DefinitionInfo(ref, null)
            }
            // new one - must build it
            var retryCount = 0
            var shortRef = clazz.simpleName
            var longRef = "#/definitions/$shortRef"
            while (class2Ref.values.contains(longRef)) {
                retryCount += 1
                shortRef = clazz.simpleName + "_" + retryCount
                longRef = "#/definitions/" + clazz.simpleName + "_" + retryCount
            }
            class2Ref[clazz] = longRef

            // create definition
            val node = JsonNodeFactory.instance.objectNode()

            // When processing polymorphism, we might get multiple recursive calls to getOrCreateDefinition -
            // this is a wau to combine them
            workInProgress = WorkInProgress(clazz, node)
            definitionsNode.set<JsonNode>(shortRef, node)
            val jsonObjectFormatVisitor = objectDefinitionBuilder.invoke(node)
            workInProgress = null
            return DefinitionInfo(longRef, jsonObjectFormatVisitor)
        }

        fun getFinalDefinitionsNode(): ObjectNode? {
            if (class2Ref.isEmpty()) {
                return null
            }
            return definitionsNode
        }
    }

    data class PolymorphismInfo(val typePropertyName: String, val subTypeName: String)

    data class PropertyNode(val main: ObjectNode, val meta: ObjectNode)

    inner class MyJsonFormatVisitorWrapper(
        val objectMapper: ObjectMapper,
        private val level: Int = 0,
        val node: ObjectNode = JsonNodeFactory.instance.objectNode(),
        val definitionsHandler: DefinitionsHandler,
        // This property may represent the BeanProperty when we're directly processing beneath the property
        val currentProperty: BeanProperty?,
    ) : JsonFormatVisitorWrapper,
        MySerializerProvider() {

        open inner class MyJsonObjectFormatVisitor(
            private val thisObjectNode: ObjectNode,
            private val propertiesNode: ObjectNode,
        ) : JsonObjectFormatVisitor,
            MySerializerProvider() {

            private fun myPropertyHandler(
                propertyName: String,
                propertyType: JavaType,
                prop: BeanProperty?,
                jsonPropertyRequired: Boolean,
            ) {
                if (propertiesNode.get(propertyName) != null) {
                    return
                }

                val thisPropertyNode1 = JsonNodeFactory.instance.objectNode()
                propertiesNode.set<JsonNode>(propertyName, thisPropertyNode1)
                val thisPropertyNode = PropertyNode(thisPropertyNode1, thisPropertyNode1)

                // Continue processing this property

                val childVisitor = createChild(thisPropertyNode.main, currentProperty = prop)

                definitionsHandler.pushWorkInProgress()
                objectMapper.acceptJsonFormatVisitor(propertyType, childVisitor)
                definitionsHandler.popworkInProgress()

                // Check if we should set this property as required
                val rawClass = propertyType.rawClass
                val requiredProperty = when {
                    rawClass.isPrimitive -> true
                    jsonPropertyRequired -> true
                    else -> prop?.getAnnotation(NotNull::class.java) != null
                }

                if (requiredProperty) {
                    getRequiredArrayNode(thisObjectNode).add(propertyName)
                }

                if (prop != null) {
                    resolvePropertyFormat(prop)?.let {
                        setFormat(thisPropertyNode.main, it)
                    }

                    prop.getAnnotation(JsonPropertyDescription::class.java)?.let {
                        thisPropertyNode.meta.put("description", it.value)
                    }
                }
            }

            override fun property(writer: BeanProperty?) {
                if (writer != null) {
                    myPropertyHandler(writer.name, writer.type, writer, jsonPropertyRequired = true)
                }
            }

            override fun property(name: String, handler: JsonFormatVisitable?, propertyTypeHint: JavaType) {
                myPropertyHandler(name, propertyTypeHint, null, jsonPropertyRequired = true)
            }

            override fun optionalProperty(writer: BeanProperty?) {
                if (writer != null) {
                    myPropertyHandler(writer.name, writer.type, writer, jsonPropertyRequired = false)
                }
            }

            override fun optionalProperty(name: String, handler: JsonFormatVisitable?, propertyTypeHint: JavaType) {
                myPropertyHandler(name, propertyTypeHint, null, jsonPropertyRequired = false)
            }
        }

        fun createChild(childNode: ObjectNode, currentProperty: BeanProperty?): MyJsonFormatVisitorWrapper {
            return MyJsonFormatVisitorWrapper(
                objectMapper,
                level + 1,
                node = childNode,
                definitionsHandler = definitionsHandler,
                currentProperty = currentProperty,
            )
        }

        override fun expectStringFormat(type: JavaType?): JsonStringFormatVisitor {
            node.put("type", "string")

            if (currentProperty != null) {
                // Look for @Pattern
                currentProperty.getAnnotation(Pattern::class.java)?.let {
                    node.put("pattern", it.regexp)
                }

                // Look for @Size
                currentProperty.getAnnotation(Size::class.java)?.let {
                    if (it.min > 0) {
                        node.put("minLength", it.min)
                    }
                    if (it.max != Integer.MAX_VALUE) {
                        node.put("maxLength", it.max)
                    }
                }
            }

            return object : JsonStringFormatVisitor, EnumSupport() {
                override val node: ObjectNode
                    get() = this@MyJsonFormatVisitorWrapper.node

                override fun format(format: JsonValueFormat?) {
                    setFormat(this@MyJsonFormatVisitorWrapper.node, format.toString())
                }
            }
        }

        override fun expectArrayFormat(type: JavaType): JsonArrayFormatVisitor {
            node.put("type", "array")

            val itemsNode = JsonNodeFactory.instance.objectNode()
            node.set<JsonNode>("items", itemsNode)

            // We get improved result while processing kotlin-collections by getting elementType this way
            // instead of using the one which we receive in JsonArrayFormatVisitor.itemsFormat
            // This approach also works for Java
            val preferredElementType: JavaType? = type.contentType

            return object : JsonArrayFormatVisitor, MySerializerProvider() {

                override fun itemsFormat(handler: JsonFormatVisitable?, elementType: JavaType?) {
                    objectMapper.acceptJsonFormatVisitor(
                        preferredElementType ?: elementType,
                        createChild(itemsNode, currentProperty = null),
                    )
                }

                override fun itemsFormat(format: JsonFormatTypes?) {
                    if (format != null) {
                        itemsNode.put("type", format.value())
                    }
                }
            }
        }

        override fun expectNullFormat(type: JavaType?): JsonNullFormatVisitor {
            return object : JsonNullFormatVisitor {}
        }

        override fun expectNumberFormat(type: JavaType?): JsonNumberFormatVisitor {
            node.put("type", "number")

            // Look for @Min, @Max => minumum, maximum
            currentProperty?.let { property ->
                property.getAnnotation(Min::class.java)?.let {
                    node.put("minimum", it.value)
                }
                property.getAnnotation(Max::class.java)?.let {
                    node.put("maximum", it.value)
                }
            }

            return object : JsonNumberFormatVisitor, EnumSupport() {
                override val node: ObjectNode
                    get() = this@MyJsonFormatVisitorWrapper.node

                override fun format(format: JsonValueFormat?) {
                    setFormat(this@MyJsonFormatVisitorWrapper.node, format.toString())
                }

                override fun numberType(type: JsonParser.NumberType?) {
                }
            }
        }

        override fun expectAnyFormat(type: JavaType?): JsonAnyFormatVisitor {
            return object : JsonAnyFormatVisitor {}
        }

        override fun expectMapFormat(type: JavaType?): JsonMapFormatVisitor {
            // There is no way to specify map in jsonSchema,
            // So we're going to treat it as type=object with additionalProperties = true,
            // so that it can hold whatever the map can hold

            val additionalPropsObject = JsonNodeFactory.instance.objectNode()

            node.put("type", "object")
            node.set<JsonNode>("additionalProperties", additionalPropsObject)

            // TODO: this is from latest mbknor - is it better?
//            definitionsHandler.pushWorkInProgress()
//            val childVisitor = createChild(additionalPropsObject, null)
//            objectMapper.acceptJsonFormatVisitor(type!!.containedType(1), childVisitor)
//            definitionsHandler.popworkInProgress()

            return object : JsonMapFormatVisitor, MySerializerProvider() {
                override fun valueFormat(handler: JsonFormatVisitable?, valueType: JavaType?) {
                    objectMapper.acceptJsonFormatVisitor(valueType, createChild(additionalPropsObject, currentProperty = null))
                }

                override fun keyFormat(handler: JsonFormatVisitable?, keyType: JavaType?) {
                    if (keyType != null) {
                        if (!keyType.isTypeOrSubTypeOf(String::class.java)) {
                            node.put("additionalProperties", true)
                        }
                    }
                }
            }
        }

        override fun expectIntegerFormat(type: JavaType?): JsonIntegerFormatVisitor {
            node.put("type", "integer")

            // Look for @Min, @Max => minumum, maximum
            currentProperty?.let { property ->
                property.getAnnotation(Min::class.java)?.let {
                    node.put("minimum", it.value)
                }
                property.getAnnotation(Max::class.java)?.let {
                    node.put("maximum", it.value)
                }
            }
            return object : JsonIntegerFormatVisitor, EnumSupport() {
                override val node: ObjectNode
                    get() = this@MyJsonFormatVisitorWrapper.node

                override fun format(format: JsonValueFormat?) {
                    setFormat(this@MyJsonFormatVisitorWrapper.node, format.toString())
                }

                override fun numberType(type: JsonParser.NumberType?) {
                }
            }
        }

        override fun expectBooleanFormat(type: JavaType?): JsonBooleanFormatVisitor {
            node.put("type", "boolean")

            return object : JsonBooleanFormatVisitor, EnumSupport() {
                override val node: ObjectNode
                    get() = this@MyJsonFormatVisitorWrapper.node

                override fun format(format: JsonValueFormat?) {
                    setFormat(this@MyJsonFormatVisitorWrapper.node, format.toString())
                }
            }
        }

        private fun getRequiredArrayNode(objectNode: ObjectNode): ArrayNode {
            if (objectNode.has("required")) {
                val node = objectNode.get("required")
                if (node is ArrayNode) {
                    return node
                }
            }
            val rn = JsonNodeFactory.instance.arrayNode()
            objectNode.set<JsonNode>("required", rn)
            return rn
        }

        private fun extractPolymorphismInfo(_type: JavaType): PolymorphismInfo? {
            // look for @JsonTypeInfo
            val ac = AnnotatedClassResolver.resolve(objectMapper.deserializationConfig, _type, objectMapper.deserializationConfig)
            val jsonTypeInfo: JsonTypeInfo? = ac.annotations?.get(JsonTypeInfo::class.java)

            if (jsonTypeInfo != null) {
                if (jsonTypeInfo.include != JsonTypeInfo.As.PROPERTY) {
                    throw Exception("We only support polymorphism using jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY")
                }
                if (jsonTypeInfo.use != JsonTypeInfo.Id.NAME) {
                    throw Exception("We only support polymorphism using jsonTypeInfo.use == JsonTypeInfo.Id.NAME")
                }

                val propertyName = jsonTypeInfo.property
                val subTypeName: String = objectMapper.subtypeResolver
                    .collectAndResolveSubtypesByClass(objectMapper.deserializationConfig, ac)
                    .first { it.type == _type.rawClass } // find first
                    .name
                return PolymorphismInfo(propertyName, subTypeName)
            }
            return null
        }

        override fun expectObjectFormat(_type: JavaType): JsonObjectFormatVisitor? {
            val ac = AnnotatedClassResolver.resolve(objectMapper.deserializationConfig, _type, objectMapper.deserializationConfig)
            val resolvedSubTypes =
                objectMapper.subtypeResolver.collectAndResolveSubtypesByClass(objectMapper.deserializationConfig, ac)

            val subTypes = resolvedSubTypes.map { it.type }.filter {
                _type.rawClass.isAssignableFrom(it) && _type.rawClass != it
            }

            if (subTypes.isNotEmpty()) {
                val anyOfArrayNode = JsonNodeFactory.instance.arrayNode()
                node.set<JsonNode>("oneOf", anyOfArrayNode)

                subTypes.forEach { clazz ->

                    val definitionInfo: DefinitionInfo = definitionsHandler.getOrCreateDefinition(clazz) {
                        val childVisitor = createChild(it, currentProperty = null)
                        objectMapper.acceptJsonFormatVisitor(clazz, childVisitor)
                        null
                    }

                    val thisOneOfNode = JsonNodeFactory.instance.objectNode()
                    thisOneOfNode.put("\$ref", definitionInfo.ref)
                    anyOfArrayNode.add(thisOneOfNode)
                }

                return null // Returning null to stop jackson from visiting this object since we have done it manually
            } else {
                val objectBuilder: (ObjectNode) -> JsonObjectFormatVisitor? = { thisObjectNode ->

                    thisObjectNode.put("type", "object")
                    thisObjectNode.put("additionalProperties", false)

                    // If class is annotated with com.dr.ktjsonschema.JsonSchemaFormat, we should add it
                    val annotatedClass = AnnotatedClassResolver.resolve(
                        objectMapper.deserializationConfig,
                        _type,
                        objectMapper.deserializationConfig,
                    )
                    resolvePropertyFormat(_type)?.let {
                        setFormat(thisObjectNode, it)
                    }

                    annotatedClass.annotations.get(JsonPropertyDescription::class.java)?.let {
                        thisObjectNode.put("description", it.value)
                    }

                    val propertiesNode = JsonNodeFactory.instance.objectNode()
                    thisObjectNode.set<JsonNode>("properties", propertiesNode)

                    extractPolymorphismInfo(_type)?.let {
                        val pi = it

                        // This class is a child in a polymorphism config.
                        // Set the title = subTypeName
                        thisObjectNode.put("title", pi.subTypeName)

                        // must inject the 'type'-param and value as enum with only one possible value
                        val enumValuesNode = JsonNodeFactory.instance.arrayNode()
                        enumValuesNode.add(pi.subTypeName)

                        val enumObjectNode = JsonNodeFactory.instance.objectNode()
                        enumObjectNode.put("type", "string")
                        enumObjectNode.set<JsonNode>("enum", enumValuesNode)
                        enumObjectNode.put("default", pi.subTypeName)

                        // Make sure the editor hides this polymorphism-specific property
                        val optionsNode = JsonNodeFactory.instance.objectNode()
                        enumObjectNode.set<JsonNode>("options", optionsNode)
                        optionsNode.put("hidden", true)

                        propertiesNode.set<JsonNode>(pi.typePropertyName, enumObjectNode)

                        getRequiredArrayNode(thisObjectNode).add(pi.typePropertyName)
                    }

                    MyJsonObjectFormatVisitor(thisObjectNode, propertiesNode)
                }

                return if (level == 0) {
                    // This is the first level - we must not use definitions
                    objectBuilder(node)
                } else {
                    val definitionInfo: DefinitionInfo = definitionsHandler.getOrCreateDefinition(_type.rawClass, objectBuilder)

                    definitionInfo.ref?.let {
                        // Must add ref to def at "this location"
                        node.put("\$ref", it)
                    }

                    definitionInfo.jsonObjectFormatVisitor
                }
            }
        }
    }

    private fun generateTitleFromPropertyName(propertyName: String): String {
        // Code found here:
        // http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
        val s = propertyName.replace(
            Regex(
                String.format(
                    "%s|%s|%s",
                    "(?<=[A-Z])(?=[A-Z][a-z])",
                    "(?<=[^A-Z])(?=[A-Z])",
                    "(?<=[A-Za-z])(?=[^A-Za-z])",
                ),
            ),
            " ",
        )

        // Make the first letter uppercase
        return s.substring(0, 1).uppercase() + s.substring(1)
    }

    fun resolvePropertyFormat(_type: JavaType): String? {
        return resolvePropertyFormat(_type.rawClass.name)
    }

    fun resolvePropertyFormat(prop: BeanProperty): String? {
        return resolvePropertyFormat(prop.type.rawClass.name)
    }

    private fun resolvePropertyFormat(rawClassName: String): String? {
        return customType2FormatMapping[rawClassName]
    }

    fun <T> generateJsonSchema(clazz: Class<T>): JsonNode {
        val rootNode = JsonNodeFactory.instance.objectNode()

        // Specify that this is a v2020-12 json schema
        rootNode.put("\$schema", JSON_SCHEMA_DRAFT_2020_12_URL)

        // Add schema title
        rootNode.put("title", generateTitleFromPropertyName(clazz.simpleName))

        val definitionsHandler = DefinitionsHandler()
        val rootVisitor = MyJsonFormatVisitorWrapper(
            rootObjectMapper,
            node = rootNode,
            definitionsHandler = definitionsHandler,
            currentProperty = null,
        )
        rootObjectMapper.acceptJsonFormatVisitor(clazz, rootVisitor)

        definitionsHandler.getFinalDefinitionsNode()?.let {
            rootNode.set<JsonNode>("definitions", it)
        }

        return rootNode
    }
}
