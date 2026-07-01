package com.languageschool.backend.config;

import com.languageschool.backend.security.crypto.FieldEncryptor;
import com.languageschool.backend.security.crypto.StringAttributeEncryptorConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityCryptoConfig {

    @Bean
    public FieldEncryptor fieldEncryptor(@Value("${security.field-encryption.key}") String base64Key) {
        return new FieldEncryptor(base64Key);
    }

    @Bean
    public InitializingBean wireJpaConverter(FieldEncryptor enc) {
        return () -> StringAttributeEncryptorConverter.setDelegate(enc);
    }
}
