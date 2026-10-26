export type ObjetTransaction = 'TERRAIN' | 'MAISON' | 'AUTRE'
export type TypeTerrain = 'COMMUNAL_DOMAINE' | 'PRIVE' | 'HERITAGE_DON'
export type SituationTerrain = 'TITRE_BORNE' | 'RECONNAISSANCE'
export type OuiNon = '' | 'oui' | 'non'

export const OBJETS: { valeur: ObjetTransaction; label: string }[] = [
  { valeur: 'TERRAIN', label: 'Terrain' },
  { valeur: 'MAISON', label: 'Maison' },
  { valeur: 'AUTRE', label: 'Autre à préciser' },
]

export const TYPES_TERRAIN: { valeur: TypeTerrain; label: string }[] = [
  { valeur: 'COMMUNAL_DOMAINE', label: 'Communal / domanial' },
  { valeur: 'PRIVE', label: 'Privé' },
  { valeur: 'HERITAGE_DON', label: 'Héritage ou don' },
]

export const SITUATIONS_TERRAIN: { valeur: SituationTerrain; label: string }[] = [
  { valeur: 'TITRE_BORNE', label: 'Titré borné' },
  { valeur: 'RECONNAISSANCE', label: 'Reconnaissance' },
]

export const POSITIONS_TERRAIN = [
  'Premier plan',
  'Deuxième plan',
  'Troisième plan',
  'Quatrième plan et plus',
  "Terrain d'angle",
  'Terrain intérieur',
  'Terrain en bord de route',
]

export interface DossierTransactionImmobiliere {
  id: number
  demandeId: number
  referenceDemande: string
  dateDemande: string | null
  nomCommercial: string | null
  numeroIm: string | null
  nomSociete: string | null
  contactClient: string | null
  emailClient: string | null
  fbClient: string | null
  adresseClient: string | null
  nomProprietairePrecedent: string | null
  actionAchat: boolean
  actionVente: boolean
  actionLocation: boolean
  actionRegularisationPapiers: boolean
  actionGestionImmobiliere: boolean
  actionAutre: boolean
  actionAutrePrecision: string | null
  objet: ObjetTransaction | null
  objetAutrePrecision: string | null
  achatMesureM2: number | null
  achatPosition: string | null
  achatAxeEmplacement: string | null
  achatMontant: number | null
  achatPermisConstruire: boolean | null
  venteMesureM2: number | null
  ventePosition: string | null
  venteAxeEmplacement: string | null
  ventePrisePhoto: boolean | null
  venteVisite: boolean | null
  venteMontant: number | null
  ventePermisConstruire: boolean | null
  regularisationTypeTerrain: TypeTerrain | null
  regularisationSituationTerrain: SituationTerrain | null
  documentPvReconnaissance: boolean
  documentNumeroDemandeAcquisition: boolean
  documentActeVentePriveeCommunale: boolean
  documentSituationJuridique: boolean
  documentActeNotoriete: boolean
  observations: string | null
  nombreCommerciauxTerrain: number | null
  commissionCn: number | null
  prestationAbonnementMensuel: boolean
}

export interface DossierTransactionPayload {
  nomProprietairePrecedent?: string | null
  actionAchat: boolean
  actionVente: boolean
  actionLocation: boolean
  actionRegularisationPapiers: boolean
  actionGestionImmobiliere: boolean
  actionAutre: boolean
  actionAutrePrecision?: string | null
  objet?: ObjetTransaction | null
  objetAutrePrecision?: string | null
  achatMesureM2?: number | null
  achatPosition?: string | null
  achatAxeEmplacement?: string | null
  achatMontant?: number | null
  achatPermisConstruire?: boolean | null
  venteMesureM2?: number | null
  ventePosition?: string | null
  venteAxeEmplacement?: string | null
  ventePrisePhoto?: boolean | null
  venteVisite?: boolean | null
  venteMontant?: number | null
  ventePermisConstruire?: boolean | null
  regularisationTypeTerrain?: TypeTerrain | null
  regularisationSituationTerrain?: SituationTerrain | null
  documentPvReconnaissance: boolean
  documentNumeroDemandeAcquisition: boolean
  documentActeVentePriveeCommunale: boolean
  documentSituationJuridique: boolean
  documentActeNotoriete: boolean
  observations?: string | null
  nombreCommerciauxTerrain?: number | null
  commissionCn?: number | null
  prestationAbonnementMensuel: boolean
}

/** Modèle du formulaire : nombres nullables et OUI/NON laissés vides si inconnus. */
export interface DossierTransactionForm {
  nomProprietairePrecedent: string
  actionAchat: boolean
  actionVente: boolean
  actionLocation: boolean
  actionRegularisationPapiers: boolean
  actionGestionImmobiliere: boolean
  actionAutre: boolean
  actionAutrePrecision: string
  objet: ObjetTransaction | ''
  objetAutrePrecision: string
  achatMesureM2: number | null
  achatPosition: string
  achatAxeEmplacement: string
  achatMontant: number | null
  achatPermisConstruire: OuiNon
  venteMesureM2: number | null
  ventePosition: string
  venteAxeEmplacement: string
  ventePrisePhoto: OuiNon
  venteVisite: OuiNon
  venteMontant: number | null
  ventePermisConstruire: OuiNon
  regularisationTypeTerrain: TypeTerrain | ''
  regularisationSituationTerrain: SituationTerrain | ''
  documentPvReconnaissance: boolean
  documentNumeroDemandeAcquisition: boolean
  documentActeVentePriveeCommunale: boolean
  documentSituationJuridique: boolean
  documentActeNotoriete: boolean
  observations: string
  nombreCommerciauxTerrain: number | null
  commissionCn: number | null
  prestationAbonnementMensuel: boolean
}

export const DOSSIER_TRANSACTION_VIDE: DossierTransactionForm = {
  nomProprietairePrecedent: '',
  actionAchat: false,
  actionVente: false,
  actionLocation: false,
  actionRegularisationPapiers: false,
  actionGestionImmobiliere: false,
  actionAutre: false,
  actionAutrePrecision: '',
  objet: '',
  objetAutrePrecision: '',
  achatMesureM2: null,
  achatPosition: '',
  achatAxeEmplacement: '',
  achatMontant: null,
  achatPermisConstruire: '',
  venteMesureM2: null,
  ventePosition: '',
  venteAxeEmplacement: '',
  ventePrisePhoto: '',
  venteVisite: '',
  venteMontant: null,
  ventePermisConstruire: '',
  regularisationTypeTerrain: '',
  regularisationSituationTerrain: '',
  documentPvReconnaissance: false,
  documentNumeroDemandeAcquisition: false,
  documentActeVentePriveeCommunale: false,
  documentSituationJuridique: false,
  documentActeNotoriete: false,
  observations: '',
  nombreCommerciauxTerrain: null,
  commissionCn: null,
  prestationAbonnementMensuel: false,
}

const texte = (v: string | null | undefined): string => v ?? ''
const ouiNon = (v: boolean | null): OuiNon =>
  v === null || v === undefined ? '' : v ? 'oui' : 'non'
const bool = (v: OuiNon): boolean | null => (v === '' ? null : v === 'oui')
const vide = (v: string): string | null => (v.trim() === '' ? null : v.trim())

export function dossierTransactionVersFormulaire(
  d: DossierTransactionImmobiliere,
): DossierTransactionForm {
  return {
    nomProprietairePrecedent: texte(d.nomProprietairePrecedent),
    actionAchat: d.actionAchat,
    actionVente: d.actionVente,
    actionLocation: d.actionLocation,
    actionRegularisationPapiers: d.actionRegularisationPapiers,
    actionGestionImmobiliere: d.actionGestionImmobiliere,
    actionAutre: d.actionAutre,
    actionAutrePrecision: texte(d.actionAutrePrecision),
    objet: d.objet ?? '',
    objetAutrePrecision: texte(d.objetAutrePrecision),
    achatMesureM2: d.achatMesureM2,
    achatPosition: texte(d.achatPosition),
    achatAxeEmplacement: texte(d.achatAxeEmplacement),
    achatMontant: d.achatMontant,
    achatPermisConstruire: ouiNon(d.achatPermisConstruire),
    venteMesureM2: d.venteMesureM2,
    ventePosition: texte(d.ventePosition),
    venteAxeEmplacement: texte(d.venteAxeEmplacement),
    ventePrisePhoto: ouiNon(d.ventePrisePhoto),
    venteVisite: ouiNon(d.venteVisite),
    venteMontant: d.venteMontant,
    ventePermisConstruire: ouiNon(d.ventePermisConstruire),
    regularisationTypeTerrain: d.regularisationTypeTerrain ?? '',
    regularisationSituationTerrain: d.regularisationSituationTerrain ?? '',
    documentPvReconnaissance: d.documentPvReconnaissance,
    documentNumeroDemandeAcquisition: d.documentNumeroDemandeAcquisition,
    documentActeVentePriveeCommunale: d.documentActeVentePriveeCommunale,
    documentSituationJuridique: d.documentSituationJuridique,
    documentActeNotoriete: d.documentActeNotoriete,
    observations: texte(d.observations),
    nombreCommerciauxTerrain: d.nombreCommerciauxTerrain,
    commissionCn: d.commissionCn,
    prestationAbonnementMensuel: d.prestationAbonnementMensuel,
  }
}

export function dossierTransactionVersPayload(
  f: DossierTransactionForm,
): DossierTransactionPayload {
  return {
    nomProprietairePrecedent: vide(f.nomProprietairePrecedent),
    actionAchat: f.actionAchat,
    actionVente: f.actionVente,
    actionLocation: f.actionLocation,
    actionRegularisationPapiers: f.actionRegularisationPapiers,
    actionGestionImmobiliere: f.actionGestionImmobiliere,
    actionAutre: f.actionAutre,
    actionAutrePrecision: vide(f.actionAutrePrecision),
    objet: f.objet === '' ? null : f.objet,
    objetAutrePrecision: vide(f.objetAutrePrecision),
    achatMesureM2: f.achatMesureM2,
    achatPosition: vide(f.achatPosition),
    achatAxeEmplacement: vide(f.achatAxeEmplacement),
    achatMontant: f.achatMontant,
    achatPermisConstruire: bool(f.achatPermisConstruire),
    venteMesureM2: f.venteMesureM2,
    ventePosition: vide(f.ventePosition),
    venteAxeEmplacement: vide(f.venteAxeEmplacement),
    ventePrisePhoto: bool(f.ventePrisePhoto),
    venteVisite: bool(f.venteVisite),
    venteMontant: f.venteMontant,
    ventePermisConstruire: bool(f.ventePermisConstruire),
    regularisationTypeTerrain:
      f.regularisationTypeTerrain === '' ? null : f.regularisationTypeTerrain,
    regularisationSituationTerrain:
      f.regularisationSituationTerrain === '' ? null : f.regularisationSituationTerrain,
    documentPvReconnaissance: f.documentPvReconnaissance,
    documentNumeroDemandeAcquisition: f.documentNumeroDemandeAcquisition,
    documentActeVentePriveeCommunale: f.documentActeVentePriveeCommunale,
    documentSituationJuridique: f.documentSituationJuridique,
    documentActeNotoriete: f.documentActeNotoriete,
    observations: vide(f.observations),
    nombreCommerciauxTerrain: f.nombreCommerciauxTerrain,
    commissionCn: f.commissionCn,
    prestationAbonnementMensuel: f.prestationAbonnementMensuel,
  }
}
