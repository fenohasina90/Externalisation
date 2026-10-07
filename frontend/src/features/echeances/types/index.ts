export type EtatEcheance = 'A_JOUR' | 'PROCHE' | 'EN_RETARD' | string

export interface Echeance {
  id: number
  proposPaiementId: number
  numeroEcheance: number
  dateEcheance: string
  montantAttendu: number
  montantPaye: number
  reste: number
  statut: EtatEcheance
  commentaire: string | null
}

export interface CreateEcheancePayload {
  numeroEcheance: number
  dateEcheance: string
  montantAttendu: number
  commentaire?: string | null
}