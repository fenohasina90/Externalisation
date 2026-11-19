export type FormeJuridique = 'INDIVIDUELLE' | 'SARL' | 'SARLU' | 'SA' | 'AUTRE'
export type TypeHebergement = 'DOMICILE_GERANT' | 'LOCATION_CABINET' | 'AUTRE'
export type RegimeActivite = 'PERMANENTE' | 'SAISONNIERE' | 'AMBULANT'

export const FORMES_JURIDIQUES: { valeur: FormeJuridique; label: string }[] = [
  { valeur: 'INDIVIDUELLE', label: 'Individuelle' },
  { valeur: 'SARL', label: 'SARL' },
  { valeur: 'SARLU', label: 'SARLU' },
  { valeur: 'SA', label: 'SA' },
  { valeur: 'AUTRE', label: 'Autre à préciser' },
]

export const HEBERGEMENTS: { valeur: TypeHebergement; label: string }[] = [
  { valeur: 'DOMICILE_GERANT', label: 'Au domicile du gérant ou du propriétaire' },
  { valeur: 'LOCATION_CABINET', label: 'Location de siège au Cabinet' },
  { valeur: 'AUTRE', label: 'Autres' },
]

export const REGIMES_ACTIVITE: { valeur: RegimeActivite; label: string }[] = [
  { valeur: 'PERMANENTE', label: 'Permanente' },
  { valeur: 'SAISONNIERE', label: 'Saisonnière' },
  { valeur: 'AMBULANT', label: 'Ambulant' },
]

export interface AssocieDossier {
  nom: string
  partSociale: string
  email: string
  contact: string
}

export interface DossierEntreprise {
  referenceDemande: string
  formeJuridique: FormeJuridique
  formePrecision: string
  denomination: string
  siegeSocialActuel: string
  capitalSocial: number | null
  nombreAssocies: number
  nomGerant: string
  fonctionGerant: string
  locationGerance: boolean
  secteurActivite: string
  autorisationSpeciale: string
  adresse: string
  ville: string
  commune: string
  district: string
  region: string
  hebergement: TypeHebergement | ''
  hebergementPrecision: string
  contratBail: '' | 'oui' | 'non'
  contratBailDetail: string
  objetSocial: string
  dateSouhaitee: string
  regimeActivite: RegimeActivite | ''
  activitesEtablissement: string
  observations: string
  tva: string
  autreSociete: string
  situationMatrimoniale: string
  regimeImposition: string
  associes: AssocieDossier[]
}

export const DOSSIER_VIDE: DossierEntreprise = {
  referenceDemande: '',
  formeJuridique: 'SARL',
  formePrecision: '',
  denomination: '',
  siegeSocialActuel: '',
  capitalSocial: null,
  nombreAssocies: 1,
  nomGerant: '',
  fonctionGerant: '',
  locationGerance: false,
  secteurActivite: '',
  autorisationSpeciale: '',
  adresse: '',
  ville: '',
  commune: '',
  district: '',
  region: '',
  hebergement: '',
  hebergementPrecision: '',
  contratBail: '',
  contratBailDetail: '',
  objetSocial: '',
  dateSouhaitee: '',
  regimeActivite: '',
  activitesEtablissement: '',
  observations: '',
  tva: '',
  autreSociete: '',
  situationMatrimoniale: '',
  regimeImposition: '',
  associes: [{ nom: '', partSociale: '', email: '', contact: '' }],
}

