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
  | 'NON_PAYEE'
  | 'PARTIEL'
  | 'PAYEE'
  | 'ANNULEE'

export type CategorieLigne = 'EXTERNALISATION' | 'DEBOURS'

export interface LigneService {
  id: number
  demandeId: number
  referenceDemande: string
  serviceId: number | null
  nomService: string | null
  typeService: string | null
  /** Tarif EXTERNALISATION (null pour DEBOURS). */
  tarif: number | null
  debutService: string | null
  finService: string | null
  statut: StatutLigneService | string | null
  factureId: number | null
  referenceFacture: string | null
  montantPaye: number
  resteAPayer: number
  /** Catégorie par ligne : EXTERNALISATION ou DEBOURS. */
  categorie: CategorieLigne | string
  /** Désignation du débours (DEBOURS uniquement). */
  designationDebours: string | null
  /** Montant du débours validé (DEBOURS uniquement, null tant que non validé). */
  montantDebours: number | null
  /** Sous-type STANDARD : PRESTATION ou ABONNEMENT_MENSUEL (EXTERNALISATION uniquement). */
  typePrestation: TypePrestation | string | null
  /** Libellé affiché : "Prestation", "Abonnement mensuel" ou nom du service. */
  libelleLigne: string | null
}

export type TypePrestation = 'PRESTATION' | 'ABONNEMENT_MENSUEL'

export interface CreateLignePayload {
  /** EXTERNALISATION : requis. DEBOURS : null. */
  serviceId?: number | null
  /** EXTERNALISATION : requis. DEBOURS : null. */
  tarif?: number | null
  debutService?: string | null
  finService?: string | null
  /** EXTERNALISATION (défaut) ou DEBOURS. */
  categorie?: CategorieLigne | string | null
  /** DEBOURS : requis. EXTERNALISATION : null. */
  designationDebours?: string | null
  /** DEBOURS : montant saisi à la création (optionnel ; s'il est renseigné, le débours est directement validé). */
  montantDebours?: number | null
  /** STANDARD : PRESTATION (défaut) ou ABONNEMENT_MENSUEL. */
  typePrestation?: TypePrestation | string | null
}

export interface ValiderMontantDeboursPayload {
  montantDebours: number
}

export interface UpdateLignePayload {
  tarif?: number | null
  debutService?: string | null
  finService?: string | null
  /** DEBOURS en EN_ATTENTE uniquement. */
  designationDebours?: string | null
  /** STANDARD : PRESTATION ou ABONNEMENT_MENSUEL. */
  typePrestation?: TypePrestation | string | null
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

export interface ValiderFactureLignePayload {
  ligneServiceId: number
  montant: number
}

export interface FactureLigne {
  ligneServiceId: number
  serviceId: number | null
  nomService: string | null
  libelleLigne: string | null
  tarif: number
  /** Tarif figé au moment de la génération de la proforma. */
  tarifProforma?: number | null
  debutService: string
  finService: string
  statutLigne: string | null
}

/** Ligne d'une facture validée (désignation figée à la création). */
export interface FactureValideeLigne {
  id: number | null
  ligneServiceId: number | null
  /** Référence de la demande d'où provient la ligne. */
  referenceDemande: string | null
  designation: string
  montant: number
  /** PRINCIPALE : l'abonnement (seul élément payable). RAPPEL : mention informative. */
  type: 'PRINCIPALE' | 'RAPPEL' | string
  /** Nom du service catalogue (null pour un débours). */
  nomService: string | null
}

/** Élément impayé pouvant être repris dans la facture du mois. */
export interface InclusionDisponible {
  ligneId: number
  demandeId: number
  referenceDemande: string
  designation: string
  /** PRESTATION ou DEBOURS. */
  type: string
  reste: number
}

/** Facture validée telle qu'utilisée pour les paiements. */
export interface FactureValidee {
  id: number
  referenceFactureValidee: string
  /** Référence proforma (null pour débours / factures du mois). */
  referenceFactureProforma: string | null
  /** Référence de la facture générale validée (factures du mois d'abonnement). */
  referenceFactureGenerale: string | null
  factureGeneraleId: number | null
  demandeId: number
  referenceDemande: string
  ligneServiceId: number | null
  libelleLigne: string | null
  /** Nom du service catalogue (ligne EXTERNALISATION), null pour un débours. */
  nomService: string | null
  montant: number
  montantPaye: number
  resteAPayer: number
  /** GENERALE (proforma validée) | ABONNEMENT_MENSUEL (facture du mois) | DEBOURS. */
  type: 'GENERALE' | 'ABONNEMENT_MENSUEL' | 'DEBOURS' | string
  valideParNom: string | null
  valideLe: string | null
  /** Lignes de la facture (facture combinée du mois) ; tableau vide sinon. */
  lignes: FactureValideeLigne[]
  /** Mois facturé (format AAAA-MM), uniquement pour les factures du mois d'abonnement. */
  mois: string | null
}

export interface Facture {
  id: number
  /** Référence PROFORMA : PRO-DDMMYYYY-XXXXX (toujours visible). */
  referenceFacture: string
  /** Référence VALIDÉE : XXXXXEXT/TNR/YYYY (null tant que proforma). */
  referenceFactureValidee: string | null
  /** ID de la facture validée liée — envoyée à l'API pour chaque paiement. */
  factureValideeId: number | null
  demandeId: number
  referenceDemande: string
  modeFacturation: ModeFacturation | string
  /** Montant proforma. */
  montantProforma: number
  /** Montant validé (peut différer du proforma). */
  montantValidee: number | null
  /** Compat : montant proforma. */
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