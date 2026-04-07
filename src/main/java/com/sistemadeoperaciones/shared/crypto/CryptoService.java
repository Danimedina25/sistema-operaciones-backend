package com.sistemadeoperaciones.shared.crypto;

public interface CryptoService {

    String encrypt(String plainText);

    String decrypt(String encryptedText);
}