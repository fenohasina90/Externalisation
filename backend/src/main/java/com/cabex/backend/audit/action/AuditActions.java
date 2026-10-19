package com.cabex.backend.audit.action;

public final class AuditActions {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String ARCHIVE = "ARCHIVE";
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String PAYMENT = "PAYMENT";
    public static final String STATUS_CHANGE = "STATUS_CHANGE";
    public static final String RESET_REQUEST = "RESET_REQUEST";
    public static final String RESET_APPROVE = "RESET_APPROVE";
    public static final String RESET_SEND = "RESET_SEND";
    public static final String RESET_REJECT = "RESET_REJECT";
    public static final String RESET_DONE = "RESET_DONE";
    public static final String BACKUP_CREATE = "BACKUP_CREATE";
    public static final String BACKUP_AUTO = "BACKUP_AUTO";
    public static final String BACKUP_DOWNLOAD = "BACKUP_DOWNLOAD";
    public static final String BACKUP_DELETE = "BACKUP_DELETE";
    public static final String BACKUP_RESTORE = "BACKUP_RESTORE";

    private AuditActions() {
    }
}
