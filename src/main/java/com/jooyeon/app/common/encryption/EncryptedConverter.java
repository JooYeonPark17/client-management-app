package com.jooyeon.app.common.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionUtil.getInstance().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionUtil.getInstance().decrypt(dbData);
    }
}