package com.jooyeon.app.common.encryption;

import com.jooyeon.app.domain.entity.member.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gender 열거형을 AES-256-GCM으로 암호화하여 저장하는 컨버터
 */
@Component
@Converter
public class EncryptedGenderConverter implements AttributeConverter<Gender, String> {

    private static EncryptionUtil encryptionUtil;

    @Autowired
    public void setEncryptionUtil(EncryptionUtil encryptionUtil) {
        EncryptedGenderConverter.encryptionUtil = encryptionUtil;
    }

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionUtil.encrypt(attribute.name());
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            String decryptedValue = encryptionUtil.decrypt(dbData);
            return Gender.valueOf(decryptedValue);
        } catch (Exception e) {
            throw new RuntimeException("성별 정보 복호화 중 오류 발생", e);
        }
    }
}