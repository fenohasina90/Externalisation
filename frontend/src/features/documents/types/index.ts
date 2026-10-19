/** Pièce jointe telle que renvoyée par l'API documents (demande ou paiement). */
export interface PieceJointe {
  id: number
  demandeId: number | null
  paiementId: number | null
  referencePaiement: string | null
  nomFichier: string
  nomOriginal: string
  typeFichier: string | null
  tailleFichier: number | null
  description: string | null
  uploadedById: number | null
  uploadedByName: string | null
  createdAt: string
}

export type CiblePieceJointe = 'demande' | 'paiement'
