package br.com.alura.literalura.services;

import br.com.alura.literalura.models.LivroDTO;

import java.util.List;

public interface IDataConverter {
    <T> T fromJson(String json, Class<T> clazz);

    List<LivroDTO> fromJsonToListOfBooks(String json);
}