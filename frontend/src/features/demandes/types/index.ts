export type StatutDemande =
  | 'EN_ATTENTE'
  | 'VALIDATION'
  | 'EN_COURS'
  | 'TERMINEE'
  | 'ANNULEE'

export interface Demande {
  id: number
  referenceDemande: string
  dateDemande: string
  debutService: string
  finService: string
  societeId: number | null
  nomSociete: string | null
  commercialId: number | null
  nomCommercial: string | null
  clientResponsableId: number | null
  nomResponsable: string | null
  serviceId: number | null
  nomService: string | null
  domaineId: number | null
  nomDomaine: string | null
  tarif: number | null
  commentaire: string | null
  statutActuel: StatutDemande | string
  montantPaye: number
  resteAPayer: number
  actif: boolean
  createdAt: string
  lignes: LigneService[]
  montantTotal: number | null
  /** Situation de règlement calculée par le backend (toutes lignes confondues). */
  situationPaiement: SituationPaiement | string | null
  /** Nombre total d'échéances, tous plans de paiement confondus. */
  nombreEcheances: number | null
}

export type SituationPaiement =
  | 'PAYE'
  | 'PARTIEL'
  | 'EN_RETARD'
  | 'EN_ATTENTE'
  | 'NON_PLANIFIE'

export type StatutLigneService =
  | 'VALIDE'
  | 'EN_ATTENTE'
  | 'EN_COURS'
  | 'TERMINE'

export interface LigneService {
  id: number
  demandeId: number
  referenceDemande: string
  serviceId: number | null
  nomService: string | null
  typeService: string | null
  tarif: number
  debutService: string
  finService: string
  statut: StatutLigneService | string | null
  factureId: number | null
  referenceFacture: string | null
  montantPaye: number
  resteAPayer: number
}

export interface CreateLignePayload {
  serviceId: number
  tarif: number
  debutService: string
  finService: string
}

export interface UpdateLignePayload {
  tarif?: number | null
  debutService?: string | null
  finService?: string | null
}

export interface HistoriqueLigne {
  id: number
  ligneServiceId: number
  statut: string
  ancienStatut: string | null
  commentaire: string | null
  utilisateurId: number | null
  nomUtilisateur: string | null
  origine: string
  createdAt: string
}

export type ModeFacturation = 'UNIQUE' | 'PAR_SERVICE'

export type StatutFacture = 'PROFORMA' | 'VALIDEE' | 'ANNULEE'

export interface FactureLigne {
  ligneServiceId: number
  serviceId: number | null
  nomService: string | null
  tarif: number
  debutService: string
  finService: string
  statutLigne: string | null
}

export interface Facture {
  id: number
  referenceFacture: string
  demandeId: number
  referenceDemande: string
  modeFacturation: ModeFacturation | string
  montant: number
  statut: StatutFacture | string
  valideParId: number | null
  valideParNom: string | null
  valideLe: string | null
  lignes: FactureLigne[]
}

export interface CreateDemandePayload {
  societeId?: number | null
  clientResponsableId?: number | null
  commercialId?: number | null
  dateDemande: string
  commentaire?: string | null
  lignes: CreateLignePayload[]
  modeFacturation?: ModeFacturation
}

export interface UpdateDemandePayload {
  commercialId?: number | null
  commentaire?: string | null
}

export interface ChangeStatutPayload {
  statut: StatutDemande
  commentaire?: string | null
}

/** Transitions manuelles autorisées (l'annulation n'est possible qu'en EN_ATTENTE). */
export const TRANSITIONS_MANUELLES: Record<string, StatutDemande[]> = {
  EN_ATTENTE: ['VALIDATION', 'ANNULEE'],
  VALIDATION: ['EN_COURS'],
  EN_COURS: ['TERMINEE'],
  TERMINEE: [],
  ANNULEE: [],
}

export interface HistoriqueDemande {
  id: number
  statutId: number
  statut: string
  ancienStatut: string | null
  commentaire: string | null
  utilisateurId: number | null
  nomUtilisateur: string
  createdAt: string
}

export interface Statut {
  id: number
  code: string
  libelle: string
  couleur: string | null
  ordre: number | null
  actif: boolean
}