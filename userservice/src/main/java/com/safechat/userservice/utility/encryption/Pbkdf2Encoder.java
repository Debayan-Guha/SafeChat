package com.safechat.userservice.utility.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;

@Configuration
public class Pbkdf2Encoder {

    private final String SECRET_KEY;

    public Pbkdf2Encoder(@Value("${encryption.pbkdf2.secretKey}") String SECRET_KEY) {
        this.SECRET_KEY = SECRET_KEY;
    }
    @Bean
    public PasswordEncoder pbkd2Encoder() {
        Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder(SECRET_KEY, 16, 60000, 512);
        encoder.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
        encoder.setEncodeHashAsBase64(true);
        return encoder;
    }
}