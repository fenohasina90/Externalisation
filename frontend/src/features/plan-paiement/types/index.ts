export interface PlanPaiement {
  id: number
  demandeId: number
  referenceDemande: string
  dateDebut: string
  dateFin: string
  montantTotal: number
  restePayer: number
  nombreEcheances: number
  nombreEcheancesPayees: number
  statut: string
  commentaire: string | null
}

export interface CreatePlanPaiementPayload {
  dateDebut: string
  dateFin: string
  montantTotal: number
  nombreEcheances: number
  commentaire?: string | null
}