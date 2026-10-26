package com.cabex.backend.backup;

import com.cabex.backend.backup.crypto.BackupCryptoService;
import com.cabex.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chiffrement AES-256-GCM des sauvegardes : aller-retour, phrase erronée, en-tête.
 */
class BackupCryptoServiceTest {

    private final BackupCryptoService crypto = new BackupCryptoService();
    private final char[] phrase = "PhraseSecrete123".toCharArray();

    private byte[] chiffrer(String contenu) throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        crypto.chiffrer(new ByteArrayInputStream(contenu.getBytes(StandardCharsets.UTF_8)), sortie, phrase);
        return sortie.toByteArray();
    }

    @Test
    void allerRetourConserveLeContenu() throws Exception {
        String contenu = "CREATE TABLE test (id INT);\nINSERT INTO test VALUES (1);\n";
        byte[] chiffre = chiffrer(contenu);

        assertNotEquals(contenu, new String(chiffre, StandardCharsets.UTF_8));
        assertTrue(new String(chiffre, StandardCharsets.US_ASCII).startsWith("CABEXBK1"));

        ByteArrayOutputStream clair = new ByteArrayOutputStream();
        crypto.dechiffrer(new ByteArrayInputStream(chiffre), clair, phrase);
        assertEquals(contenu, clair.toString(StandardCharsets.UTF_8));
    }

    @Test
    void mauvaisePhraseRefusee() throws Exception {
        byte[] chiffre = chiffrer("donnees sensibles");
        assertThrows(BadRequestException.class, () ->
                crypto.dechiffrer(new ByteArrayInputStream(chiffre),
                        new ByteArrayOutputStream(), "AutrePhrase456".toCharArray()));
    }

    @Test
    void enteteInvalideRefuse() {
        byte[] corrompu = "pas une sauvegarde CABEX".getBytes(StandardCharsets.US_ASCII);
        assertThrows(BadRequestException.class, () ->
                crypto.dechiffrer(new ByteArrayInputStream(corrompu),
                        new ByteArrayOutputStream(), phrase));
    }
}
