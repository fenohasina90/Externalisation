import type { NotificationUser } from '../types'

export type TypeNotification =
  | 'PAIEMENT_RETARD'
  | 'ECHEANCE_PROCHE'
  | 'NOUVELLE_DEMANDE'
  | 'CHANGEMENT_STATUT'
  | 'PAIEMENT_ENREGISTRE'
  | 'ALERTE_SYSTEME'

export const TYPE_META: Record<TypeNotification, { label: string; tone: 'red' | 'orange' | 'blue' | 'green' | 'gray' }> = {
  PAIEMENT_RETARD: { label: 'Paiement en retard', tone: 'red' },
  ECHEANCE_PROCHE: { label: 'Échéance proche', tone: 'orange' },
  NOUVELLE_DEMANDE: { label: 'Nouvelle demande', tone: 'blue' },
  CHANGEMENT_STATUT: { label: 'Changement de statut', tone: 'blue' },
  PAIEMENT_ENREGISTRE: { label: 'Paiement enregistré', tone: 'green' },
  ALERTE_SYSTEME: { label: 'Alertes système', tone: 'gray' },
}

function notif(
  id: number,
  typeNotification: TypeNotification,
  titre: string,
  message: string,
  dateCreation: string,
  lu = true,
): NotificationUser {
  return {
    id,
    typeNotification,
    titre,
    message,
    referenceType: null,
    referenceId: null,
    lu,
    dateLecture: lu ? dateCreation : null,
    dateCreation,
  }
}

/** 8 premières notifications visibles sur la maquette (3 non lues + 5 lues). */
const TETE: NotificationUser[] = [
  notif(1, 'PAIEMENT_RETARD', 'Paiement en retard', 'Échéance n°1 de REF-2026-011 (Matrizel SARL) en retard de 15 jours', 'Il y a 2 h', false),
  notif(2, 'ECHEANCE_PROCHE', 'Échéance proche', 'Échéance n°2 de REF-2026-014 (Atlas Conseil) : 100 000 MGA à régler le 10/10/2026', 'Il y a 5 h', false),
  notif(3, 'NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-021 créée par Cobalt Consulting', 'Il y a 7 h', false),
  notif(4, 'PAIEMENT_ENREGISTRE', 'Paiement enregistré', 'Paiement de 200 000 MGA enregistré pour REF-2026-014 (Atlas Conseil)', 'Hier, 15:10'),
  notif(5, 'CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-010 (Ammontech SAS) : En cours → Terminée', '30/09/2026'),
  notif(6, 'PAIEMENT_RETARD', 'Paiement en retard', 'Échéance de REF-2026-008 (Novacore Services) en retard de 30 jours', '29/09/2026'),
  notif(7, 'ALERTE_SYSTEME', 'Alertes système', 'Sauvegarde quotidienne terminée avec succès', '28/09/2026, 02:00'),
  notif(8, 'ECHEANCE_PROCHE', 'Échéance proche', 'Échéance n°3 de REF-2026-009 (Cobalt Consulting) dans 7 jours', '26/09/2026'),
]

/** Complément statique pour atteindre 36 notifications (7 retards, 5 échéances, 1 alerte). */
const SUITE: Array<[TypeNotification, string, string, string]> = [
  ['NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-020 créée par Hélios Groupe', '25/09/2026'],
  ['PAIEMENT_ENREGISTRE', 'Paiement enregistré', 'Paiement de 150 000 MGA enregistré pour REF-2026-011 (Matrizel SARL)', '24/09/2026'],
  ['CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-009 (Cobalt Consulting) : Validation → En cours', '23/09/2026'],
  ['PAIEMENT_RETARD', 'Paiement en retard', 'Échéance de REF-2026-007 (Globex France) en retard de 12 jours', '22/09/2026'],
  ['ECHEANCE_PROCHE', 'Échéance proche', 'Échéance n°1 de REF-2026-015 (Innovatech SARL) dans 5 jours', '21/09/2026'],
  ['NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-019 créée par Atlas Conseil', '20/09/2026'],
  ['PAIEMENT_ENREGISTRE', 'Paiement enregistré', 'Paiement de 300 000 MGA enregistré pour REF-2026-008 (Novacore Services)', '19/09/2026'],
  ['CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-007 (Globex France) : En attente → Validation', '18/09/2026'],
  ['PAIEMENT_RETARD', 'Paiement en retard', 'Échéance de REF-2026-006 (Matrizel SARL) en retard de 8 jours', '17/09/2026'],
  ['ECHEANCE_PROCHE', 'Échéance proche', 'Échéance n°2 de REF-2026-012 (Hélios Groupe) dans 6 jours', '16/09/2026'],
  ['NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-018 créée par Novacore Services', '15/09/2026'],
  ['PAIEMENT_ENREGISTRE', 'Paiement enregistré', 'Paiement de 100 000 MGA enregistré pour REF-2026-012 (Hélios Groupe)', '14/09/2026'],
  ['CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-006 (Matrizel SARL) : En cours → Terminée', '13/09/2026'],
  ['PAIEMENT_RETARD', 'Paiement en retard', 'Échéance de REF-2026-005 (Cobalt Consulting) en retard de 21 jours', '12/09/2026'],
  ['NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-017 créée par Globex France', '11/09/2026'],
  ['CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-005 (Cobalt Consulting) : Validation → En cours', '10/09/2026'],
  ['PAIEMENT_ENREGISTRE', 'Paiement enregistré', 'Paiement de 250 000 MGA enregistré pour REF-2026-010 (Ammontech SAS)', '09/09/2026'],
  ['ECHEANCE_PROCHE', 'Échéance proche', 'Échéance n°1 de REF-2026-013 (Ammontech SAS) dans 7 jours', '08/09/2026'],
  ['NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-016 créée par Matrizel SARL', '07/09/2026'],
  ['CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-004 (Hélios Groupe) : En attente → Validation', '06/09/2026'],
  ['PAIEMENT_RETARD', 'Paiement en retard', 'Échéance de REF-2026-004 (Hélios Groupe) en retard de 5 jours', '05/09/2026'],
  ['PAIEMENT_ENREGISTRE', 'Paiement enregistré', 'Paiement de 200 000 MGA enregistré pour REF-2026-009 (Cobalt Consulting)', '04/09/2026'],
  ['NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-015 créée par Innovatech SARL', '03/09/2026'],
  ['CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-003 (Atlas Conseil) : En cours → Terminée', '02/09/2026'],
  ['PAIEMENT_RETARD', 'Paiement en retard', 'Échéance de REF-2026-002 (Innovatech SARL) en retard de 18 jours', '01/09/2026'],
  ['NOUVELLE_DEMANDE', 'Nouvelle demande', 'Nouvelle demande REF-2026-014 créée par Atlas Conseil', '28/08/2026'],
  ['CHANGEMENT_STATUT', 'Changement de statut', 'REF-2026-002 (Innovatech SARL) : Validation → En cours', '27/08/2026'],
  ['PAIEMENT_ENREGISTRE', 'Paiement enregistré', 'Paiement de 120 000 MGA enregistré pour REF-2026-007 (Globex France)', '26/08/2026'],
]

/**
 * 36 notifications statiques : 3 non lues, 7 paiements en retard,
 * 5 échéances proches, 1 alerte système.
 */
export const MOCK_NOTIFICATIONS: NotificationUser[] = [
  ...TETE,
  ...SUITE.map(([type, titre, message, date], i) => notif(9 + i, type, titre, message, date)),
]

export type OngletNotifications = 'toutes' | 'non-lues'
