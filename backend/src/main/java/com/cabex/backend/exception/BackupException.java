package com.cabex.backend.exception;

/**
 * Échec opérationnel du module de sauvegarde (mysqldump/mysql introuvable,
 * exécution en erreur, écriture impossible…). Le message est présenté à
 * l'utilisateur afin de faciliter le diagnostic.
 */
public class BackupException extends RuntimeException {

    public BackupException(String message) {
        super(message);
    }

    public BackupException(String message, Throwable cause) {
        super(message, cause);
    }
}
