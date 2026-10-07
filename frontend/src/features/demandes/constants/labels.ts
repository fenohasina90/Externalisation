import type { StatutDemande } from '../types'

export const LIBELLES_STATUT_DEMANDE: Record<StatutDemande, string> = {
  EN_ATTENTE: 'EN ATTENTE',
  VALIDATION: 'VALIDATION',
  EN_COURS: 'EN COURS',
  TERMINEE: 'TERMINÉE',
  ANNULEE: 'ANNULÉE',
}

export const getLibelleStatutDemande = (statut: string): string =>
  LIBELLES_STATUT_DEMANDE[statut as StatutDemande] ?? statut