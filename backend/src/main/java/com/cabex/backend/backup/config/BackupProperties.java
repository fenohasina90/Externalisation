package com.cabex.backend.backup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Paramètres du module de sauvegarde (dossier de stockage, rétention,
 * planification et accès au client MySQL/mysqldump).
 */
@Component
public class BackupProperties {

    private final String dir;
    private final int retentionDays;
    private final String cron;
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    private final String mysqldumpPath;
    private final String mysqlPath;
    private final long uploadMaxSize;

    public BackupProperties(
            @Value("${backup.dir:./data/backups}") String dir,
            @Value("${backup.retention-days:14}") int retentionDays,
            @Value("${backup.cron:0 0 2 * * *}") String cron,
            @Value("${backup.db.host:127.0.0.1}") String dbHost,
            @Value("${backup.db.port:3306}") int dbPort,
            @Value("${backup.db.name:gestion_externalisation}") String dbName,
            @Value("${backup.db.username:root}") String dbUsername,
            @Value("${backup.db.password:}") String dbPassword,
            @Value("${backup.db.mysqldump-path:mysqldump}") String mysqldumpPath,
            @Value("${backup.db.mysql-path:mysql}") String mysqlPath,
            @Value("${backup.upload.max-size:268435456}") long uploadMaxSize) {
        this.dir = dir;
        this.retentionDays = retentionDays;
        this.cron = cron;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.mysqldumpPath = mysqldumpPath;
        this.mysqlPath = mysqlPath;
        this.uploadMaxSize = uploadMaxSize;
    }

    public String getDir() {
        return dir;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getCron() {
        return cron;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getMysqldumpPath() {
        return mysqldumpPath;
    }

    public String getMysqlPath() {
        return mysqlPath;
    }

    public long getUploadMaxSize() {
        return uploadMaxSize;
    }
}
