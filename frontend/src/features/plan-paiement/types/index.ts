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

export interface EcheancePlanSaisie {
  dateEcheance: string
  montantAttendu: number
}

export interface CreatePlanPaiementPayload {
  dateDebut: string
  dateFin: string
  montantTotal: number
  nombreEcheances: number
  commentaire?: string | null
  /** Échéances manuelles (date + montant) ; sinon répartition automatique. */
  echeances?: EcheancePlanSaisie[] | null
}