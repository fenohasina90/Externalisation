/** État d'une échéance (valeurs produites par EcheanceMapper.calculerStatutEcheance). */
export type EtatEcheance = 'PAYEE' | 'EN_RETARD' | 'PROCHE' | 'PARTIELLE' | 'A_VENIR'

export interface Echeance {
  id: number
  proposPaiementId: number
  numeroEcheance: number
  dateEcheance: string
  montantAttendu: number
  montantPaye: number
  reste: number
  statut: EtatEcheance | string
  commentaire: string | null
}

/** Demande cible d'une échéance (navigation depuis une notification). */
export interface EcheanceCible {
  id: number
  numeroEcheance: number
  demandeId: number
  referenceDemande: string
}

export interface CreateEcheancePayload {
  numeroEcheance: number
  dateEcheance: string
  montantAttendu: number
  commentaire?: string | null
}

export interface UpdateEcheancePayload {
  dateEcheance: string
  montantAttendu: number
  commentaire?: string | null
}