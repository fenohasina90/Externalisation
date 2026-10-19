export type StatutClient = 'Actif' | 'Inactif' | 'Archivé'

/** Société telle que renvoyée par GET /api/clients. */
export interface Societe {
  id: number
  nomSociete: string
  activite: string | null
  contact: string | null
  email: string | null
  fb: string | null
  adresse: string | null
  dateCreation: string | null
  nombrePersonnel: number | null
  chiffreAffaireMensuel: number | null
  actif: boolean
  archive: boolean
  responsablePrincipal: string | null
  nombreDemandes: number
  createdAt: string | null
  domaineId: number | null
  nomDomaine: string | null
}

export interface CreateSocietePayload {
  nomSociete: string
  activite?: string | null
  contact?: string | null
  email?: string | null
  fb?: string | null
  adresse?: string | null
  /** Date de création de la société au format AAAA-MM-JJ. */
  dateCreation?: string | null
  nombrePersonnel?: number | null
  chiffreAffaireMensuel?: number | null
  domaineId: number
}

export type UpdateSocietePayload = CreateSocietePayload

/** Code affiché (CLI-0248) dérivé de l'identifiant. */
export function codeSociete(id: number): string {
  return `CLI-${String(id).padStart(4, '0')}`
}

/** Identifiant numérique déduit du code client (CLI-0248 → 248), sinon null. */
export function idDepuisCodeSociete(code: string): number | null {
  const correspondance = /^CLI-(\d+)$/i.exec(code.trim())
  if (!correspondance) return null
  const id = Number(correspondance[1])
  return Number.isInteger(id) && id > 0 ? id : null
}

/** Statut d'affichage : archivé prime, sinon actif/inactif. */
export function statutSociete(societe: Pick<Societe, 'actif' | 'archive'>): StatutClient {
  if (societe.archive) return 'Archivé'
  return societe.actif ? 'Actif' : 'Inactif'
}

export interface ResponsableFiche {
  id: number
  nomClient: string
  contact: string | null
  email: string | null
  fonction: string | null
  actif: boolean
}

export interface DemandeFiche {
  id: number
  referenceDemande: string
  dateDemande: string | null
  debutService: string | null
  finService: string | null
  serviceNom: string | null
  statutActuel: string | null
  montantPaye: number | null
  resteAPayer: number | null
}

export interface PaiementFiche {
  id: number
  referencePaiement: string | null
  referenceDemande: string | null
  demandeId: number | null
  libelleMode: string | null
  datePaiement: string | null
  montantPaye: number | null
  referenceVirement: string | null
}

export interface PieceJointeFiche {
  id: number
  nomOriginal: string | null
  typeFichier: string | null
  tailleFichier: number | null
  description: string | null
  demandeId: number | null
  referenceDemande: string | null
  createdAt: string | null
}

export interface SyntheseFiche {
  montantTotal: number | null
  montantPaye: number | null
  resteAPayer: number | null
  nombreEcheancesEnRetard: number | null
}

/** Fiche 360° : GET /api/clients/{id} (synthèse nulle = masquée, ex. ACCUEIL). */
export interface SocieteDetail {
  societe: Societe
  responsables: ResponsableFiche[]
  demandes: DemandeFiche[]
  paiements: PaiementFiche[]
  piecesJointes: PieceJointeFiche[]
  synthese: SyntheseFiche | null
}
