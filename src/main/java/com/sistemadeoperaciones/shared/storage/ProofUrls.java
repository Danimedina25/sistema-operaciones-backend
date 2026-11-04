package com.sistemadeoperaciones.shared.storage;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Reglas para las URL de comprobantes. Por defecto solo se aceptan URL HTTPS y, para comprobantes de
 * una operación, Firebase Storage dentro de su carpeta. El emulador local de Storage
 * (http://127.0.0.1:9199) solo se acepta con APP_STORAGE_ALLOW_EMULATOR=true, para los entornos
 * locales de QA y tutoriales; ningún perfil lo activa.
 */
public final class ProofUrls {
    static final String ALLOW_EMULATOR_PROPERTY = "app.storage.allow-emulator";
    private static final String ALLOW_EMULATOR_ENV = "APP_STORAGE_ALLOW_EMULATOR";
    private static final String FIREBASE_HOST = "firebasestorage.googleapis.com";
    private static final int EMULATOR_PORT = 9199;
    private static final int MAX_LENGTH = 500;

    private ProofUrls() {
    }

    /** URL HTTPS, o del emulador local cuando está habilitado. */
    public static boolean isAcceptable(String url) {
        return url != null && (url.startsWith("https://") || isEmulator(url));
    }

    /** Comprobante en Firebase Storage (o el emulador habilitado) dentro de la carpeta de la operación. */
    public static boolean isOperationProof(String url, Long operationId) {
        try {
            URI uri = URI.create(url);
            String path = URLDecoder.decode(uri.getRawPath(), StandardCharsets.UTF_8);
            boolean storage = ("https".equals(uri.getScheme()) && FIREBASE_HOST.equals(uri.getHost())) || isEmulator(url);
            return url.length() <= MAX_LENGTH && storage
                    && path.contains("/o/comprobantes/operaciones/" + operationId + "/") && !path.contains("..");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEmulator(String url) {
        if (!Boolean.parseBoolean(System.getProperty(ALLOW_EMULATOR_PROPERTY, System.getenv(ALLOW_EMULATOR_ENV)))) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            return "http".equals(uri.getScheme()) && uri.getPort() == EMULATOR_PORT
                    && ("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()));
        } catch (Exception e) {
            return false;
        }
    }
}
