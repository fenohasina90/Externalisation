import type { Echeance } from '@/features/echeances/types'

export interface PlanPaiement {
  id: number
  demandeId: number
  referenceDemande: string
  dateDebut: string
  dateFin: string
  montantTotal: number
  restePayer: number
  nombreEcheances: number
  commentaire: string | null
  statut: string
  /** Échéances du plan, déjà triées par numéro et avec leur état calculé. */
  echeances: Echeance[]
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

export interface UpdatePlanPaiementPayload {
  dateDebut: string
  dateFin: string
  montantTotal: number
  commentaire?: string | null
}