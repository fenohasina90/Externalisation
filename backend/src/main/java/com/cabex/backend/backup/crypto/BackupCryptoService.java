package com.cabex.backend.backup.crypto;

import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.BackupException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Chiffrement optionnel des sauvegardes en AES-256-GCM.
 * La clé est dérivée de la phrase secrète par PBKDF2-HMAC-SHA256 (210 000 itérations).
 * Chaque fichier porte un en-tête : magie, version, itérations, sel et IV.
 */
@Service
public class BackupCryptoService {

    private static final byte[] MAGIE = "CABEXBK1".getBytes(StandardCharsets.US_ASCII);
    private static final byte VERSION = 1;
    private static final int LONGUEUR_SEL = 16;
    private static final int LONGUEUR_IV = 12;
    private static final int BITS_TAG = 128;
    private static final int BITS_CLE = 256;
    private static final int ITERATIONS = 210_000;
    private static final String ALGORITHME = "AES/GCM/NoPadding";
    private static final String DERIVATION = "PBKDF2WithHmacSHA256";

    private final SecureRandom aleatoire = new SecureRandom();

    /**
     * Écrit l'en-tête sur le flux de destination et renvoie le flux de chiffrement.
     * Fermer le flux renvoyé finalise le tag d'authentification et ferme la destination.
     */
    public OutputStream ouvrirChiffrement(OutputStream destination, char[] phrase)
            throws IOException, GeneralSecurityException {
        byte[] sel = new byte[LONGUEUR_SEL];
        byte[] iv = new byte[LONGUEUR_IV];
        aleatoire.nextBytes(sel);
        aleatoire.nextBytes(iv);
        SecretKey cle = deriver(phrase, sel);

        DataOutputStream entete = new DataOutputStream(destination);
        entete.write(MAGIE);
        entete.writeByte(VERSION);
        entete.writeInt(ITERATIONS);
        entete.write(sel);
        entete.write(iv);
        entete.flush();

        Cipher cipher = Cipher.getInstance(ALGORITHME);
        cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(BITS_TAG, iv));
        return new CipherOutputStream(destination, cipher);
    }

    /** Chiffre un flux complet (utile pour les tests et les petits volumes). */
    public void chiffrer(InputStream clair, OutputStream chiffre, char[] phrase)
            throws IOException, GeneralSecurityException {
        OutputStream sortie = ouvrirChiffrement(chiffre, phrase);
        clair.transferTo(sortie);
        sortie.close();
    }

    /** Déchiffre un flux produit par {@link #chiffrer}. */
    public void dechiffrer(InputStream chiffre, OutputStream clair, char[] phrase) throws IOException {
        try (DataInputStream entete = new DataInputStream(chiffre)) {
            byte[] magie = new byte[MAGIE.length];
            entete.readFully(magie);
            if (!java.util.Arrays.equals(magie, MAGIE)) {
                throw new BadRequestException("Ce fichier n'est pas une sauvegarde chiffrée CABEX");
            }
            byte version = entete.readByte();
            if (version != VERSION) {
                throw new BadRequestException("Version de chiffrement non prise en charge (" + version + ")");
            }
            int iterations = entete.readInt();
            byte[] sel = new byte[LONGUEUR_SEL];
            byte[] iv = new byte[LONGUEUR_IV];
            entete.readFully(sel);
            entete.readFully(iv);

            SecretKey cle = deriver(phrase, sel, iterations);
            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(BITS_TAG, iv));
            try (CipherInputStream sortie = new CipherInputStream(chiffre, cipher)) {
                sortie.transferTo(clair);
            }
        } catch (GeneralSecurityException e) {
            throw new BackupException("Échec du déchiffrement de la sauvegarde", e);
        } catch (IOException e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (message.contains("tag") || message.contains("bad") || message.contains("decrypt")) {
                throw new BadRequestException("Phrase secrète incorrecte ou sauvegarde corrompue");
            }
            throw e;
        }
    }

    private SecretKey deriver(char[] phrase, byte[] sel) {
        return deriver(phrase, sel, ITERATIONS);
    }

    private SecretKey deriver(char[] phrase, byte[] sel, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(phrase, sel, iterations, BITS_CLE);
            byte[] brut = SecretKeyFactory.getInstance(DERIVATION).generateSecret(spec).getEncoded();
            spec.clearPassword();
            return new SecretKeySpec(brut, "AES");
        } catch (GeneralSecurityException e) {
            throw new BackupException("Impossible de dériver la clé de chiffrement", e);
        }
    }
}
