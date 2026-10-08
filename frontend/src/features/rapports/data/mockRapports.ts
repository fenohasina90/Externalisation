export type CategorieRapport =
  | 'general'
  | 'financier'
  | 'clients'
  | 'services'
  | 'demandes'
  | 'paiements'

export interface CategorieMeta {
  id: CategorieRapport
  label: string
}

export const CATEGORIES_RAPPORT: CategorieMeta[] = [
  { id: 'general', label: 'Rapport général' },
  { id: 'financier', label: 'Rapport financier' },
  { id: 'clients', label: 'Rapport clients' },
  { id: 'services', label: 'Rapport services' },
  { id: 'demandes', label: 'Rapport demandes' },
  { id: 'paiements', label: 'Rapport paiements' },
]

export interface LigneDomaine {
  domaine: string
  demandes: number
  montantTotal: number
  montantPaye: number
  resteAPayer: number
}

/** Chiffres statiques de démonstration (cf. maquette). */
export const MOCK_RAPPORT = {
  clients: 248,
  demandes: 126,
  montantTotal: 84500000,
  montantPaye: 52300000,
  parDomaine: [
    { domaine: 'Comptabilité', demandes: 34, montantTotal: 38200000, montantPaye: 22100000, resteAPayer: 16100000 },
    { domaine: 'Fiscalité', demandes: 26, montantTotal: 21500000, montantPaye: 14400000, resteAPayer: 7100000 },
    { domaine: 'Social', demandes: 22, montantTotal: 14800000, montantPaye: 9000000, resteAPayer: 5800000 },
    { domaine: 'Droit', demandes: 18, montantTotal: 6500000, montantPaye: 4200000, resteAPayer: 2300000 },
    { domaine: 'Conseil', demandes: 26, montantTotal: 3500000, montantPaye: 2600000, resteAPayer: 900000 },
  ] as LigneDomaine[],
  parStatut: [
    { statut: 'En attente', nombre: 24 },
    { statut: 'Validation', nombre: 18 },
    { statut: 'En cours', nombre: 52 },
    { statut: 'Terminées', nombre: 28 },
    { statut: 'Annulées', nombre: 4 },
  ],
}

export const FILTRE_OPTIONS = {
  clients: ['Atlas Conseil', 'Innovatech SARL', 'Cobalt Consulting', 'Matrizel SARL', 'Globex France'],
  services: ['Audit comptable', 'Conseil fiscal', 'Paie & RH', 'Bilan annuel', 'Tenue comptable'],
  domaines: ['Comptabilité', 'Fiscalité', 'Finance', 'Social', 'Droit', 'Conseil'],
  commerciaux: ['Jean Dupont', 'Marie Martin', 'Luc Moreau', 'Nirina Rakoto'],
}

export function formatMGA(montant: number): string {
  return `${montant.toLocaleString('fr-FR').replace(/,/g, ' ')} MGA`
}
