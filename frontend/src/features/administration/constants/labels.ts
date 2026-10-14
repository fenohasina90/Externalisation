export const LIBELLES_ACTION_AUDIT: Record<string, string> = {
  LOGIN: 'Connexion',
  LOGOUT: 'Déconnexion',
  CREATE: 'Création',
  UPDATE: 'Modification',
  DELETE: 'Suppression',
  ARCHIVE: 'Archivage',
  PAYMENT: 'Paiement',
  STATUS_CHANGE: 'Changement de statut',
  RESET_REQUEST: 'Demande de réinitialisation',
  RESET_APPROVE: 'Réinitialisation approuvée',
  RESET_SEND: 'Mot de passe envoyé',
  RESET_REJECT: 'Réinitialisation refusée',
  RESET_DONE: 'Réinitialisation terminée',
}

export const getLibelleActionAudit = (action: string): string =>
  LIBELLES_ACTION_AUDIT[action] ?? action

/** Actions proposées dans le filtre (codes backend). */
export const ACTIONS_AUDIT = Object.keys(LIBELLES_ACTION_AUDIT)

/** Modules et entités connus (filtres). */
export const MODULES_AUDIT = [
  'AUTH',
  'UTILISATEUR',
  'CLIENT',
  'COMMERCIAL',
  'SERVICE',
  'DEMANDE',
  'DOSSIER',
  'PAIEMENT',
  'DOCUMENT',
  'RAPPORT',
  'AUDIT',
  'CONFIGURATION',
  'DASHBOARD',
  'NOTIFICATION',
]

export const ENTITES_AUDIT = [
  'Utilisateur',
  'Role',
  'Societe',
  'ClientResponsable',
  'Commercial',
  'Domaine',
  'Service',
  'Tarif',
  'DemandeService',
  'DossierCreation',
  'PlanPaiement',
  'EcheancePaiement',
  'Paiement',
  'ModePaiement',
  'PieceJointe',
  'Notification',
  'Configuration',
]