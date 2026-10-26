export interface RechercheClientItem {
  id: number
  nomSociete: string
  contact: string | null
  email: string | null
}

export interface RechercheServiceItem {
  id: number
  nomService: string
  codeService: string | null
}

export interface RechercheDemandeItem {
  id: number
  referenceDemande: string
  nomSociete: string | null
  nomService: string | null
  statutActuel: string | null
  dateDemande: string | null
  tarif: number | null
}

export interface RecherchePaiementItem {
  id: number
  referencePaiement: string
  referenceDemande: string | null
  nomSociete: string | null
  modePaiement: string | null
  datePaiement: string | null
  montant: number | null
}

/** Réponse de `GET /api/recherche?q=…`, limitée à 8 éléments par catégorie. */
export interface RechercheGlobale {
  clients: RechercheClientItem[]
  services: RechercheServiceItem[]
  demandes: RechercheDemandeItem[]
  paiements: RecherchePaiementItem[]
}
