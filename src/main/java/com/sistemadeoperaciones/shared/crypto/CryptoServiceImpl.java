package com.sistemadeoperaciones.shared.crypto;

import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class CryptoServiceImpl implements CryptoService {

    private static final String ALGORITHM = "AES";

    private final SecretKeySpec secretKeySpec;

    public CryptoServiceImpl(@Value("${app.crypto.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("La clave AES debe tener 16, 24 o 32 caracteres");
        }

        this.secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public String encrypt(String plainText) {
        try {
            if (plainText == null) {
                return null;
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new BadRequestException("Error al cifrar la información sensible");
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        try {
            if (encryptedText == null) {
                return null;
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BadRequestException("Error al descifrar la información sensible");
        }
    }
}