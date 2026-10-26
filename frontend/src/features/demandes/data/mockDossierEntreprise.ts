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

/** Le dossier n'existe que pour la création d'entreprise ou de société. */
export function isCreationEntreprise(service: string): boolean {
  const s = service
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
  return s.includes('creation') && (s.includes('entreprise') || s.includes('societe'))
}

/** Dossier statique de démonstration (REF-2026-126, SARL, 2 associés). */
export const MOCK_DOSSIER_126: DossierEntreprise = {
  referenceDemande: 'REF-2026-126',
  formeJuridique: 'SARL',
  formePrecision: '',
  denomination: 'Sahan Agro SARL',
  siegeSocialActuel: 'Lot II M 45 Ankorondrano, Antananarivo',
  capitalSocial: 5000000,
  nombreAssocies: 2,
  nomGerant: 'Nirina Rasoanaivo',
  fonctionGerant: 'Gérante',
  locationGerance: false,
  secteurActivite: 'Agroalimentaire',
  autorisationSpeciale: '',
  adresse: 'Lot II M 45 Ankorondrano',
  ville: 'Antananarivo',
  commune: 'Antananarivo I',
  district: 'Antananarivo I',
  region: 'Analamanga',
  hebergement: 'LOCATION_CABINET',
  hebergementPrecision: '',
  contratBail: 'oui',
  contratBailDetail: 'Bail commercial 3/6/9 signé le 12/08/2026',
  objetSocial: 'Transformation et commercialisation de produits agricoles, import-export de denrées alimentaires.',
  dateSouhaitee: '2026-11-01',
  regimeActivite: 'PERMANENTE',
  activitesEtablissement: 'Siège et unité de transformation à Ankorondrano, point de vente à Analakely.',
  observations: 'Dossier complet, en attente de l’immatriculation.',
  tva: 'Assujettie à la TVA',
  autreSociete: 'Non',
  situationMatrimoniale: 'Mariée',
  regimeImposition: 'Régime réel',
  associes: [
    { nom: 'Nirina Rasoanaivo', partSociale: '60 %', email: 'nirina.r@sahanagro.mg', contact: '+261 34 90 111 22' },
    { nom: 'Tovo Randria', partSociale: '40 %', email: 'tovo.r@sahanagro.mg', contact: '+261 33 90 222 33' },
  ],
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

/** Dossier générique pré-rempli pour les autres demandes de création. */
export function dossierGenerique(reference: string, denomination: string): DossierEntreprise {
  return { ...DOSSIER_VIDE, referenceDemande: reference, denomination, nombreAssocies: 1 }
}
