export type TypeNotification =
  | 'PAIEMENT_RETARD'
  | 'ECHEANCE_PROCHE'
  | 'NOUVELLE_DEMANDE'
  | 'PAIEMENT_ENREGISTRE'
  | 'CHANGEMENT_STATUT'
  | 'ALERTE_SYSTEME'
  | 'DEMANDE_RESET_MDP'

export interface NotificationUser {
  id: number
  typeNotification: TypeNotification | string
  titre: string
  message: string
  referenceType: string | null
  referenceId: number | null
  lu: boolean
  dateLecture: string | null
  dateCreation: string
}

export interface NotificationsNonLues {
  notifications: NotificationUser[]
  totalNonLues: number
  parType: Record<string, number>
}

export type OngletNotifications = 'toutes' | 'non-lues'
