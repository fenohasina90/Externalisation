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