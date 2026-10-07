export const LIBELLES_ACTION_AUDIT: Record<string, string> = {
  LOGIN: 'Connexion',
  LOGOUT: 'Déconnexion',
  CREATE: 'Création',
  UPDATE: 'Modification',
  DELETE: 'Suppression',
  ARCHIVE: 'Archivage',
  PAYMENT: 'Paiement',
  STATUS_CHANGE: 'Changement de statut',
}

export const getLibelleActionAudit = (action: string): string =>
  LIBELLES_ACTION_AUDIT[action] ?? action