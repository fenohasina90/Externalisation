import type { TypeNotification } from './types'

export interface TypeNotificationMeta {
  label: string
  tone: 'red' | 'orange' | 'blue' | 'green' | 'gray'
}

export const TYPE_META: Record<TypeNotification, TypeNotificationMeta> = {
  PAIEMENT_RETARD: { label: 'Paiement en retard', tone: 'red' },
  ECHEANCE_PROCHE: { label: 'Échéance proche', tone: 'orange' },
  NOUVELLE_DEMANDE: { label: 'Nouvelle demande', tone: 'blue' },
  PAIEMENT_ENREGISTRE: { label: 'Paiement enregistré', tone: 'green' },
  CHANGEMENT_STATUT: { label: 'Changement de statut', tone: 'blue' },
  ALERTE_SYSTEME: { label: 'Alertes système', tone: 'gray' },
  DEMANDE_RESET_MDP: { label: 'Réinitialisation de mot de passe', tone: 'gray' },
}

export const TYPES_NOTIFICATION = Object.keys(TYPE_META) as TypeNotification[]

const TYPES_DEMANDE: TypeNotification[] = ['NOUVELLE_DEMANDE', 'CHANGEMENT_STATUT']
const TYPES_PAIEMENT: TypeNotification[] = [
  'PAIEMENT_RETARD',
  'ECHEANCE_PROCHE',
  'PAIEMENT_ENREGISTRE',
]

/** Types que le backend autorise à la lecture selon le rôle connecté. */
export function typesVisibles(role: string | null | undefined): TypeNotification[] {
  if (role === 'ACCUEIL') return TYPES_DEMANDE
  if (role === 'ECONOMAT') return TYPES_PAIEMENT
  return TYPES_NOTIFICATION
}

export const EVENEMENT_NOTIFICATIONS = 'notifications:maj'

export function signalerChangementNotifications(): void {
  window.dispatchEvent(new Event(EVENEMENT_NOTIFICATIONS))
}
