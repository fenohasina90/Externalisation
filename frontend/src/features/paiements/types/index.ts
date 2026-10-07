export interface ModePaiement {
  id: number
  code: string
  libelle: string
  actif: boolean
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
  echeanceId?: number | null
  modePaiementId: number
  datePaiement: string
  montantPaye: number
  referenceVirement?: string | null
  commentaire?: string | null
}