package com.jooyeon.app.common.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionUtil 테스트")
class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil();
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", "TestEncryptionKey123456789012345");
        encryptionUtil.init();
    }


    @Test
    @DisplayName("텍스트 암호화/복호화 - 성공")
    void encrypt_decrypt_test_Success() {
        // given
        String plainText = "안녕하세요! 한글 테스트입니다.";

        // when
        String encrypted = encryptionUtil.encrypt(plainText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("긴 텍스트 암호화/복호화 - 성공")
    void encrypt_decrypt_longText_Success() {
        // given
        String plainText = "This is a very long text that contains multiple sentences. " +
                          "It should be properly encrypted and decrypted without any issues. " +
                          "This test verifies that the encryption utility can handle longer content.";

        // when
        String encrypted = encryptionUtil.encrypt(plainText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }


    @Test
    @DisplayName("동일한 평문의 암호화 결과가 매번 다름 (IV 랜덤성)")
    void encrypt_SamePlainText_DifferentResults() {
        // given
        String plainText = "Same text for encryption";

        // when
        String encrypted1 = encryptionUtil.encrypt(plainText);
        String encrypted2 = encryptionUtil.encrypt(plainText);

        // then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(encryptionUtil.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptionUtil.decrypt(encrypted2)).isEqualTo(plainText);
    }


    @Test
    @DisplayName("조작된 암호화 데이터 복호화 시 예외 발생")
    void decrypt_TamperedData_ThrowsException() {
        // given
        String plainText = "Original text";
        String encrypted = encryptionUtil.encrypt(plainText);

        // 암호화된 데이터를 조작
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        encryptedBytes[encryptedBytes.length - 1] = (byte) (encryptedBytes[encryptedBytes.length - 1] ^ 1);
        String tamperedEncrypted = Base64.getEncoder().encodeToString(encryptedBytes);

        // when & then
        assertThatThrownBy(() -> encryptionUtil.decrypt(tamperedEncrypted))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error decrypting data");
    }


    @Test
    @DisplayName("16바이트 키로 초기화 - 성공")
    void init_16ByteKey_Success() {
        // given
        EncryptionUtil util = new EncryptionUtil();
        ReflectionTestUtils.setField(util, "encryptionKey", "1234567890123456"); // 16 bytes

        // when & then
        assertDoesNotThrow(util::init);
    }

    @Test
    @DisplayName("24바이트 키로 초기화 - 성공")
    void init_24ByteKey_Success() {
        // given
        EncryptionUtil util = new EncryptionUtil();
        ReflectionTestUtils.setField(util, "encryptionKey", "123456789012345678901234"); // 24 bytes

        // when & then
        assertDoesNotThrow(util::init);
    }

    @Test
    @DisplayName("32바이트 키로 초기화 - 성공")
    void init_32ByteKey_Success() {
        // given
        EncryptionUtil util = new EncryptionUtil();
        ReflectionTestUtils.setField(util, "encryptionKey", "12345678901234567890123456789012"); // 32 bytes

        // when & then
        assertDoesNotThrow(util::init);
    }


}