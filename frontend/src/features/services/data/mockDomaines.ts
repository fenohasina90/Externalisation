import type { Domaine } from '../types'

/**
 * Données statiques d'interface (étape 1 — sans backend).
 * 8 domaines : 6 actifs (75 %), 60 sociétés associées au total.
 */
export const MOCK_DOMAINES: Domaine[] = [
  { id: 1, nomDomaine: 'Comptabilité', description: null, actif: true, nombreSocietes: 14 },
  { id: 2, nomDomaine: 'Fiscalité', description: null, actif: true, nombreSocietes: 11 },
  { id: 3, nomDomaine: 'Finance', description: null, actif: true, nombreSocietes: 8 },
  { id: 4, nomDomaine: 'Social', description: null, actif: true, nombreSocietes: 9 },
  { id: 5, nomDomaine: 'Droit', description: null, actif: true, nombreSocietes: 6 },
  { id: 6, nomDomaine: 'Conseil', description: null, actif: true, nombreSocietes: 5 },
  { id: 7, nomDomaine: 'Technologie', description: null, actif: false, nombreSocietes: 4 },
  { id: 8, nomDomaine: 'Industrie', description: null, actif: false, nombreSocietes: 3 },
]

export type StatutDomaineFilter = 'tous' | 'actifs' | 'inactifs'
