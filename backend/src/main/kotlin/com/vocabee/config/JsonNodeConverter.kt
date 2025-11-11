package com.vocabee.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class JsonNodeConverter : AttributeConverter<JsonNode, String> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: JsonNode?): String? {
        return attribute?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): JsonNode? {
        return if (dbData.isNullOrBlank()) {
            null
        } else {
            objectMapper.readTree(dbData)
        }
    }
}
