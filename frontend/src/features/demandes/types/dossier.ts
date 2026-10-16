import type { DossierEntreprise } from '../data/mockDossierEntreprise'

/** Dossier tel que renvoyé par GET /api/demandes/{id}/dossier. */
export interface Dossier {
  id: number
  demandeId: number
  referenceDemande: string
  dateDemande: string | null
  numeroIm: string | null
  formeJuridique: string
  formeJuridiquePrecision: string | null
  denomination: string
  siegeSocialActuel: string | null
  capitalSocial: number | null
  nombreAssociesDeclare: number | null
  nomGerant: string | null
  fonctionGerant: string | null
  locationGerance: boolean
  secteurActivite: string | null
  autorisationSpeciale: string | null
  adresseSiege: string | null
  ville: string | null
  commune: string | null
  district: string | null
  region: string | null
  typeHebergement: string | null
  hebergementPrecision: string | null
  contratBail: boolean | null
  contratBailDetail: string | null
  objetSocial: string | null
  dateSouhaiteeDebut: string | null
  regimeActivite: string | null
  activitesEtablissement: string | null
  observations: string | null
  assujettissementTva: string | null
  autreSociete: string | null
  situationMatrimoniale: string | null
  regimeImposition: string | null
  associes: DossierAssocie[]
}

export interface DossierAssocie {
  id: number | null
  ordre: number | null
  nom: string
  partSociale: string | null
  email: string | null
  contact: string | null
}

export interface DossierPayload {
  formeJuridique: string
  formeJuridiquePrecision?: string | null
  denomination: string
  siegeSocialActuel?: string | null
  capitalSocial?: number | null
  nombreAssociesDeclare?: number | null
  nomGerant?: string | null
  fonctionGerant?: string | null
  locationGerance?: boolean | null
  secteurActivite?: string | null
  autorisationSpeciale?: string | null
  adresseSiege?: string | null
  ville?: string | null
  commune?: string | null
  district?: string | null
  region?: string | null
  typeHebergement?: string | null
  hebergementPrecision?: string | null
  contratBail?: boolean | null
  contratBailDetail?: string | null
  objetSocial?: string | null
  dateSouhaiteeDebut?: string | null
  regimeActivite?: string | null
  activitesEtablissement?: string | null
  observations?: string | null
  assujettissementTva?: string | null
  autreSociete?: string | null
  situationMatrimoniale?: string | null
  regimeImposition?: string | null
  associes?: { nom: string; partSociale?: string | null; email?: string | null; contact?: string | null }[]
}

const vide = (v: string): string | null => (v.trim() === '' ? null : v.trim())

/** Formulaire → payload API (capital, nombre et associés nuls si individuelle). */
export function dossierVersPayload(d: DossierEntreprise): DossierPayload {
  const societe = d.formeJuridique !== 'INDIVIDUELLE'
  return {
    formeJuridique: d.formeJuridique,
    formeJuridiquePrecision: vide(d.formePrecision),
    denomination: d.denomination.trim(),
    siegeSocialActuel: vide(d.siegeSocialActuel),
    capitalSocial: societe ? d.capitalSocial : null,
    nombreAssociesDeclare: societe ? d.nombreAssocies : null,
    nomGerant: vide(d.nomGerant),
    fonctionGerant: vide(d.fonctionGerant),
    locationGerance: d.locationGerance,
    secteurActivite: vide(d.secteurActivite),
    autorisationSpeciale: vide(d.autorisationSpeciale),
    adresseSiege: vide(d.adresse),
    ville: vide(d.ville),
    commune: vide(d.commune),
    district: vide(d.district),
    region: vide(d.region),
    typeHebergement: d.hebergement === '' ? null : d.hebergement,
    hebergementPrecision: vide(d.hebergementPrecision),
    contratBail: d.contratBail === '' ? null : d.contratBail === 'oui',
    contratBailDetail: vide(d.contratBailDetail),
    objetSocial: vide(d.objetSocial),
    dateSouhaiteeDebut: d.dateSouhaitee === '' ? null : d.dateSouhaitee,
    regimeActivite: d.regimeActivite === '' ? null : d.regimeActivite,
    activitesEtablissement: vide(d.activitesEtablissement),
    observations: vide(d.observations),
    assujettissementTva: vide(d.tva),
    autreSociete: vide(d.autreSociete),
    situationMatrimoniale: vide(d.situationMatrimoniale),
    regimeImposition: vide(d.regimeImposition),
    associes: societe
      ? d.associes
          .filter((a) => a.nom.trim() !== '')
          .map((a) => ({
            nom: a.nom.trim(),
            partSociale: vide(a.partSociale),
            email: vide(a.email),
            contact: vide(a.contact),
          }))
      : [],
  }
}

const texte = (v: string | null | undefined): string => v ?? ''

/** API → formulaire (édition pré-remplie). */
export function dossierVersFormulaire(d: Dossier, referenceDemande: string): DossierEntreprise {
  return {
    referenceDemande,
    formeJuridique: (d.formeJuridique ?? 'SARL') as DossierEntreprise['formeJuridique'],
    formePrecision: texte(d.formeJuridiquePrecision),
    denomination: texte(d.denomination),
    siegeSocialActuel: texte(d.siegeSocialActuel),
    capitalSocial: d.capitalSocial,
    nombreAssocies: d.nombreAssociesDeclare ?? 0,
    nomGerant: texte(d.nomGerant),
    fonctionGerant: texte(d.fonctionGerant),
    locationGerance: d.locationGerance ?? false,
    secteurActivite: texte(d.secteurActivite),
    autorisationSpeciale: texte(d.autorisationSpeciale),
    adresse: texte(d.adresseSiege),
    ville: texte(d.ville),
    commune: texte(d.commune),
    district: texte(d.district),
    region: texte(d.region),
    hebergement: (d.typeHebergement ?? '') as DossierEntreprise['hebergement'],
    hebergementPrecision: texte(d.hebergementPrecision),
    contratBail: d.contratBail === null || d.contratBail === undefined ? '' : d.contratBail ? 'oui' : 'non',
    contratBailDetail: texte(d.contratBailDetail),
    objetSocial: texte(d.objetSocial),
    dateSouhaitee: d.dateSouhaiteeDebut ?? '',
    regimeActivite: (d.regimeActivite ?? '') as DossierEntreprise['regimeActivite'],
    activitesEtablissement: texte(d.activitesEtablissement),
    observations: texte(d.observations),
    tva: texte(d.assujettissementTva),
    autreSociete: texte(d.autreSociete),
    situationMatrimoniale: texte(d.situationMatrimoniale),
    regimeImposition: texte(d.regimeImposition),
    associes: d.associes.map((a) => ({
      nom: a.nom,
      partSociale: texte(a.partSociale),
      email: texte(a.email),
      contact: texte(a.contact),
    })),
  }
}
