package com.sistemadeoperaciones.shared.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProofUrlsTest {
    private static final String FIREBASE = "https://firebasestorage.googleapis.com/v0/b/app/o/comprobantes%2Foperaciones%2F7%2Fcheque.png?alt=media";
    private static final String EMULATOR = "http://127.0.0.1:9199/v0/b/demo/o/comprobantes%2Foperaciones%2F7%2Fcheque.png?alt=media";

    @AfterEach
    void clear() {
        System.clearProperty(ProofUrls.ALLOW_EMULATOR_PROPERTY);
    }

    @Test
    void byDefaultOnlyHttpsAndFirebaseAreAccepted() {
        assertThat(ProofUrls.isAcceptable("https://example.com/comprobante.png")).isTrue();
        assertThat(ProofUrls.isAcceptable(EMULATOR)).isFalse();
        assertThat(ProofUrls.isAcceptable(null)).isFalse();
        assertThat(ProofUrls.isOperationProof(FIREBASE, 7L)).isTrue();
        assertThat(ProofUrls.isOperationProof(EMULATOR, 7L)).isFalse();
        assertThat(ProofUrls.isOperationProof("https://example.com/o/comprobantes/operaciones/7/x.png", 7L)).isFalse();
        assertThat(ProofUrls.isOperationProof(null, 7L)).isFalse();
    }

    @Test
    void enabledEmulatorIsAcceptedOnlyOnItsLocalHostAndPort() {
        System.setProperty(ProofUrls.ALLOW_EMULATOR_PROPERTY, "true");
        assertThat(ProofUrls.isAcceptable(EMULATOR)).isTrue();
        assertThat(ProofUrls.isAcceptable("http://localhost:9199/v0/b/demo/o/x.png")).isTrue();
        assertThat(ProofUrls.isAcceptable("http://127.0.0.1:8080/x.png")).isFalse();
        assertThat(ProofUrls.isAcceptable("http://example.com:9199/x.png")).isFalse();
    }

    @Test
    void operationFolderIsStillRequiredWithTheEmulator() {
        System.setProperty(ProofUrls.ALLOW_EMULATOR_PROPERTY, "true");
        assertThat(ProofUrls.isOperationProof(EMULATOR, 7L)).isTrue();
        assertThat(ProofUrls.isOperationProof(EMULATOR, 8L)).isFalse();
        assertThat(ProofUrls.isOperationProof("http://127.0.0.1:9199/v0/b/demo/o/comprobantes%2Foperaciones%2F7%2F..%2Fx.png", 7L)).isFalse();
    }
}
