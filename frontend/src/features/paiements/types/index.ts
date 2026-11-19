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
  factureValideeId: number | null
  referenceFactureValidee: string | null
  echeanceId: number | null
  modePaiementId: number
  libelleMode: string
  datePaiement: string
  montantPaye: number
  referenceVirement: string | null
  /** Banque émettrice du chèque (mode CHÈQUE, sinon null). */
  banque: string | null
  /** Numéro de chèque (mode CHÈQUE, sinon null). */
  numeroCheque: string | null
  commentaire: string | null
  pieceJustificative: string | null
  createdById: number | null
  createdByName: string | null
}

/** Ligne de l'objet d'une facture (type + désignation, mois pour l'abonnement). */
export interface ObjetLigneFacture {
  /** PRESTATION | DEBOURS | ABONNEMENT_MENSUEL */
  type: string
  /** Désignation lisible du service / débours (mois inclus pour l'abonnement). */
  libelle: string
}

/** Détail d'un paiement : informations du reçu PDF (facture validée, montants, montant en lettres). */
export interface PaiementDetail extends Paiement {
  /** Toutes les lignes facturées (type + désignation). */
  objets?: ObjetLigneFacture[]
  numeroEcheance: number | null
  dateEcheance: string | null
  /** Type de facture validée liée (GENERALE | PRESTATION | DEBOURS | ABONNEMENT_MENSUEL). */
  typeFacture: string | null
  nomService: string | null
  /** Date d'émission de la facture validée liée. */
  dateFactureValidee: string | null
  /** Mois facturé (ABONNEMENT_MENSUEL uniquement). */
  moisFacture: string | null
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
  /** Banque émettrice du chèque (mode CHÈQUE). */
  banque?: string | null
  /** Numéro de chèque (mode CHÈQUE). */
  numeroCheque?: string | null
  commentaire?: string | null
  pieceJustificative?: string | null
}

export interface UpdatePaiementPayload {
  datePaiement: string
  montantPaye: number
  referenceVirement?: string | null
  banque?: string | null
  numeroCheque?: string | null
  commentaire?: string | null
}

