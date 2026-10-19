import type { CategorieRapport } from './types'

export interface CategorieMeta {
  id: CategorieRapport
  label: string
  permission: string
}

export const CATEGORIES_RAPPORT: CategorieMeta[] = [
  { id: 'general', label: 'Rapport général', permission: 'RAPPORT_DEMANDE_VIEW' },
  { id: 'financier', label: 'Rapport financier', permission: 'RAPPORT_PAIEMENT_VIEW' },
  { id: 'clients', label: 'Rapport clients', permission: 'RAPPORT_DEMANDE_VIEW' },
  { id: 'services', label: 'Rapport services', permission: 'RAPPORT_PAIEMENT_VIEW' },
  { id: 'demandes', label: 'Rapport demandes', permission: 'RAPPORT_DEMANDE_VIEW' },
  { id: 'paiements', label: 'Rapport paiements', permission: 'RAPPORT_PAIEMENT_VIEW' },
]

export const STATUTS_DEMANDE: { code: string; label: string }[] = [
  { code: 'EN_ATTENTE', label: 'En attente' },
  { code: 'VALIDATION', label: 'Validation' },
  { code: 'EN_COURS', label: 'En cours' },
  { code: 'TERMINEE', label: 'Terminées' },
  { code: 'ANNULEE', label: 'Annulées' },
]

/** Types de rapport filtrés par les permissions « demandes ». */
export function estRapportDemande(categorie: CategorieRapport): boolean {
  return categorie === 'general' || categorie === 'clients' || categorie === 'demandes'
}

export function permissionExport(categorie: CategorieRapport): string {
  return estRapportDemande(categorie) ? 'RAPPORT_DEMANDE_EXPORT' : 'RAPPORT_PAIEMENT_EXPORT'
}
