package com.languageschool.backend.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StringAttributeEncryptorConverter implements AttributeConverter<String, String> {

    private static volatile FieldEncryptor DELEGATE;

    public static void setDelegate(FieldEncryptor enc) {
        DELEGATE = enc;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        FieldEncryptor d = DELEGATE;
        return d == null ? attribute : d.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        FieldEncryptor d = DELEGATE;
        return d == null ? dbData : d.decrypt(dbData);
    }
}
