import type { Echeance } from '@/features/echeances/types'

export type { Echeance }

/** États calculés par le backend pour une échéance (EcheanceMapper.calculerStatutEcheance). */
export type EtatEcheance = 'PAYEE' | 'EN_RETARD' | 'PROCHE' | 'PARTIELLE' | 'A_VENIR'

/** Situation de règlement d'un plan, déduite des états de ses échéances. */
export type SituationPlan = 'PAYE' | 'PARTIEL' | 'EN_RETARD' | 'EN_ATTENTE' | 'NON_PLANIFIE'

export interface ModePaiement {
  id: number
  code: string
  libelle: string
  actif: boolean
}

/** Plan de paiement rattaché à une ligne de service (GET /lignes/{id}/plan-paiement). */
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
  echeances: Echeance[]
}

export interface Paiement {
  id: number
  referencePaiement: string
  demandeId: number
  referenceDemande: string
  societeId: number | null
  nomSociete: string | null
  echeanceId: number | null
  modePaiementId: number
  libelleMode: string
  datePaiement: string
  montantPaye: number
  referenceVirement: string | null
  commentaire: string | null
  pieceJustificative: string | null
  createdById: number | null
  createdByName: string | null
}

export interface CreatePaiementPayload {
  demandeId: number
  echeanceId: number
  modePaiementId: number
  datePaiement: string
  montantPaye: number
  referenceVirement?: string | null
  commentaire?: string | null
  pieceJustificative?: string | null
}

export interface UpdatePaiementPayload {
  echeanceId?: number | null
  datePaiement: string
  montantPaye: number
  referenceVirement?: string | null
  commentaire?: string | null
}

/** Échéance présentée dans le formulaire de règlement, rattachée à sa ligne de service. */
export interface EcheanceReglable {
  echeance: Echeance
  ligneId: number
  ligneLabel: string
  demandeId: number
  referenceDemande: string
  nomSociete: string | null
  /** Période de service : le backend borne la date du paiement à cet intervalle. */
  debutService: string | null
  finService: string | null
}

/** Plan d'une ligne de service avec son rattachement (utilisé par les écrans d'échéancier). */
export interface PlanParLigne {
  ligneId: number
  ligneLabel: string
  debutService: string | null
  finService: string | null
  plan: PlanPaiement | null
}

/** Ligne d'un échéancier affichée dans la liste des échéanciers. */
export interface EcheancierLigne {
  demandeId: number
  referenceDemande: string
  nomSociete: string | null
  nbServices: number
  nbEcheances: number
  montantTotal: number
  reste: number
  situation: SituationPlan
}