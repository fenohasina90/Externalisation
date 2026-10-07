export interface RechercheResultat {
  type: 'client' | 'demande' | 'paiement' | 'responsable' | 'commercial' | 'service'
  id: number
  libelle: string
  detail: string | null
  reference: string | null
}