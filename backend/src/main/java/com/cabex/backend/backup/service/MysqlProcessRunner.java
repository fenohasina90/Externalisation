package com.cabex.backend.backup.service;

import com.cabex.backend.backup.config.BackupProperties;
import com.cabex.backend.backup.crypto.BackupCryptoService;
import com.cabex.backend.exception.BackupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Exécution des clients externes MySQL : {@code mysqldump} (sauvegarde compressée,
 * éventuellement chiffrée) et {@code mysql} (restauration d'un script SQL).
 * Le mot de passe est transmis par la variable d'environnement {@code MYSQL_PWD}
 * afin de ne jamais apparaître dans la ligne de commande.
 */
@Service
public class MysqlProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(MysqlProcessRunner.class);

    private final BackupProperties proprietes;
    private final BackupCryptoService crypto;

    public MysqlProcessRunner(BackupProperties proprietes, BackupCryptoService crypto) {
        this.proprietes = proprietes;
        this.crypto = crypto;
    }

    /**
     * Sauvegarde la base : le flux de {@code mysqldump} est compressé (gzip) vers
     * la destination, puis chiffré si une phrase secrète est fournie.
     */
    public void dump(OutputStream destination, boolean chiffre, char[] phrase) {
        List<String> arguments = argumentsDump();
        ProcessBuilder constructeur = constructeur(arguments);
        Process processus;
        try {
            processus = constructeur.start();
        } catch (IOException e) {
            throw new BackupException(
                    "Impossible de lancer '" + proprietes.getMysqldumpPath()
                            + "'. Vérifiez que le client MySQL est installé et que le chemin est correct.", e);
        }

        StringBuilder erreur = new StringBuilder();
        Thread lecteurErreur = Thread.ofVirtual().start(() -> lire(processus, erreur));

        try {
            OutputStream cible = destination;
            if (chiffre) {
                cible = crypto.ouvrirChiffrement(destination, phrase);
            }
            try (GZIPOutputStream gzip = new GZIPOutputStream(cible)) {
                processus.getInputStream().transferTo(gzip);
                gzip.finish();
            }
        } catch (GeneralSecurityException e) {
            processus.destroyForcibly();
            throw new BackupException("Échec du chiffrement de la sauvegarde", e);
        } catch (IOException e) {
            processus.destroyForcibly();
            throw new BackupException("Erreur pendant la génération de la sauvegarde : " + e.getMessage(), e);
        }

        int code = attendre(processus);
        joindre(lecteurErreur);
        if (code != 0) {
            throw new BackupException("La sauvegarde a échoué (code " + code + ") : " + extrait(erreur.toString()));
        }
    }

    /** Restaure un script SQL dans la base configurée. */
    public void restaurer(Path fichierSql) {
        List<String> arguments = List.of(
                proprietes.getMysqlPath(),
                "--protocol=TCP",
                "--host=" + hoteEffectif(),
                "--port=" + proprietes.getDbPort(),
                "--user=" + proprietes.getDbUsername(),
                "--default-character-set=utf8mb4",
                proprietes.getDbName());
        ProcessBuilder constructeur = constructeur(arguments);
        constructeur.redirectInput(fichierSql.toFile());
        constructeur.redirectErrorStream(true);

        Process processus;
        try {
            processus = constructeur.start();
        } catch (IOException e) {
            throw new BackupException(
                    "Impossible de lancer '" + proprietes.getMysqlPath()
                            + "'. Vérifiez que le client MySQL est installé et que le chemin est correct.", e);
        }

        StringBuilder sortie = new StringBuilder();
        Thread lecteur = Thread.ofVirtual().start(() -> lire(processus, sortie));
        int code = attendre(processus);
        joindre(lecteur);
        if (code != 0) {
            throw new BackupException("La restauration a échoué (code " + code + ") : " + extrait(sortie.toString()));
        }
    }

    private List<String> argumentsDump() {
        List<String> arguments = new ArrayList<>();
        arguments.add(proprietes.getMysqldumpPath());
        arguments.add("--protocol=TCP");
        arguments.add("--host=" + hoteEffectif());
        arguments.add("--port=" + proprietes.getDbPort());
        arguments.add("--user=" + proprietes.getDbUsername());
        arguments.add("--single-transaction");
        if (routinesDisponibles()) {
            arguments.add("--routines");
        } else {
            log.warn("Procédures/fonctions non accessibles : la sauvegarde est générée sans '--routines'");
        }
        arguments.add("--triggers");
        if (evenementsDisponibles()) {
            arguments.add("--events");
        } else {
            log.warn("Planificateur d'événements MySQL inactif : la sauvegarde est générée sans '--events'");
        }
        arguments.add("--no-tablespaces");
        arguments.add("--default-character-set=utf8mb4");
        arguments.add(proprietes.getDbName());
        return arguments;
    }

    /**
     * Vérifie si le planificateur d'événements est actif. Certains SGBD (MariaDB)
     * refusent {@code --events} lorsqu'il est désactivé, ce qui ferait échouer le dump.
     */
    private boolean evenementsDisponibles() {
        String resultat = executerRequete("SELECT @@event_scheduler");
        return resultat != null && "ON".equalsIgnoreCase(resultat.trim());
    }

    /**
     * Vérifie que les procédures et fonctions sont accessibles. Sur certaines
     * installations (tables système non migrées) ou pour un utilisateur restreint,
     * {@code --routines} échoue : on l'omet alors pour produire une sauvegarde valide.
     */
    private boolean routinesDisponibles() {
        String base = proprietes.getDbName().replace("'", "''");
        return executerRequete("SHOW FUNCTION STATUS WHERE Db = '" + base + "'") != null;
    }

    /** Exécute une requête via le client mysql ; retourne la sortie, ou null en cas d'échec. */
    private String executerRequete(String requete) {
        List<String> arguments = List.of(
                proprietes.getMysqlPath(),
                "--protocol=TCP",
                "--host=" + hoteEffectif(),
                "--port=" + proprietes.getDbPort(),
                "--user=" + proprietes.getDbUsername(),
                "--skip-column-names",
                "--batch",
                "--execute=" + requete);
        ProcessBuilder constructeur = constructeur(arguments);
        constructeur.redirectErrorStream(true);
        try {
            Process processus = constructeur.start();
            String resultat = new String(processus.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return processus.waitFor() == 0 ? resultat : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Les clients MySQL interprètent {@code localhost} comme une connexion par socket Unix,
     * en ignorant le port ; on force donc l'adresse IPv4 pour rester en TCP (XAMPP, Docker,
     * hôtes distants) et éviter l'erreur 2002 sur {@code /var/run/mysqld/mysqld.sock}.
     */
    private String hoteEffectif() {
        String hote = proprietes.getDbHost();
        if (hote == null || hote.isBlank()) {
            return "127.0.0.1";
        }
        String valeur = hote.trim();
        return "localhost".equalsIgnoreCase(valeur) ? "127.0.0.1" : valeur;
    }

    private ProcessBuilder constructeur(List<String> arguments) {
        ProcessBuilder constructeur = new ProcessBuilder(arguments);
        Map<String, String> environnement = constructeur.environment();
        environnement.put("MYSQL_PWD", proprietes.getDbPassword() == null ? "" : proprietes.getDbPassword());
        return constructeur;
    }

    private void lire(Process processus, StringBuilder destination) {
        try {
            String contenu = new String(processus.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            destination.append(contenu);
        } catch (IOException e) {
            log.debug("Lecture du flux d'erreur interrompue : {}", e.getMessage());
        }
    }

    private int attendre(Process processus) {
        try {
            return processus.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processus.destroyForcibly();
            throw new BackupException("Opération interrompue");
        }
    }

    private void joindre(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String extrait(String sortie) {
        if (sortie == null || sortie.isBlank()) {
            return "aucun détail fourni par le client MySQL";
        }
        String nettoye = sortie.strip();
        return nettoye.length() > 500 ? nettoye.substring(nettoye.length() - 500) : nettoye;
    }
}
