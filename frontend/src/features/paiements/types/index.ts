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
  factureValideeId: number | null
  referenceFactureValidee: string | null
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

/** Détail d'un paiement : informations du reçu PDF (facture validée, montants, montant en lettres). */
export interface PaiementDetail extends Paiement {
  numeroEcheance: number | null
  dateEcheance: string | null
  nomService: string | null
  /** Montant de la facture validée payée. */
  montantFactureValidee: number | null
  /** Total des paiements actifs sur cette facture (ce paiement inclus). */
  montantPayeSurFacture: number | null
  /** Reste à payer sur la facture (tient compte de tous les paiements). */
  resteAPayerFacture: number | null
  montantEnLettres: string | null
}

export interface CreatePaiementPayload {
  demandeId: number
  /** Facture validée liée au paiement (toujours requise). */
  factureValideeId: number
  /** Échéance réglée (EXTERNALISATION avec plan de paiement). NULL sinon. */
  echeanceId?: number | null
  modePaiementId: number
  datePaiement: string
  montantPaye: number
  referenceVirement?: string | null
  commentaire?: string | null
  pieceJustificative?: string | null
}

export interface UpdatePaiementPayload {
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
  /** Facture validée de la ligne (proforma validée), liée au paiement. */
  factureValideeId: number | null
  /** Référence de la facture validée (pour affichage). */
  referenceFactureValidee: string | null
}

/** Plan d'une ligne de service avec son rattachement (utilisé par les écrans d'échéancier). */
export interface PlanParLigne {
  ligneId: number
  ligneLabel: string
  typePrestation: string | null
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