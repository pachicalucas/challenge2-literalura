package br.com.alura.literalura.services;

import br.com.alura.literalura.models.LivroDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataConverter implements IDataConverter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new DataConversionException("Failed to convert JSON to object of type " + clazz.getName(), e);
        }
    }

    @Override
    public List<LivroDTO> fromJsonToListOfBooks(String json) {
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, LivroDTO.class);
            return objectMapper.readValue(json, listType);
        } catch (JsonProcessingException e) {
            throw new DataConversionException("Failed to convert JSON to List<LivroDTO>", e);
        }
    }
}

