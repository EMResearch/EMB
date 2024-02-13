package no.nav.familie.ba.sak.common

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(stringList: List<String>): String {
        return java.lang.String.join(SPLIT_CHAR, stringList)
    }

    override fun convertToEntityAttribute(string: String?): List<String> {
        return if (string.isNullOrBlank()) emptyList() else string.split(SPLIT_CHAR)
    }

    companion object {

        private const val SPLIT_CHAR = ";"
    }
}
