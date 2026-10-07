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
}

export interface CreateDemandePayload {
  societeId: number
  serviceId: number
  clientResponsableId?: number | null
  commercialId?: number | null
  dateDemande: string
  debutService: string
  finService: string
  tarif?: number | null
  commentaire?: string | null
}

export type UpdateDemandePayload = Partial<CreateDemandePayload>

export interface ChangeStatutPayload {
  nouveauStatut: StatutDemande
  commentaire?: string | null
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