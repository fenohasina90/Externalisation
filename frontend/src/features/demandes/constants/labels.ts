import type { SituationPaiement, StatutDemande } from '../types'

export const LIBELLES_STATUT_DEMANDE: Record<StatutDemande, string> = {
  EN_ATTENTE: 'EN ATTENTE',
  VALIDATION: 'VALIDATION',
  EN_COURS: 'EN COURS',
  TERMINEE: 'TERMINÉE',
  ANNULEE: 'ANNULÉE',
}

export const getLibelleStatutDemande = (statut: string): string =>
  LIBELLES_STATUT_DEMANDE[statut as StatutDemande] ?? statut

/**
 * Situation de règlement, calculée par le backend sur l'ensemble des lignes
 * d'une demande (`DemandeResponse.situationPaiement`). Source unique, également
 * utilisée par le filtre `situation` de `GET /demandes` : la pastille affichée
 * et le filtre appliqué ne peuvent pas diverger.
 */
export const LIBELLES_SITUATION_PAIEMENT: Record<SituationPaiement, string> = {
  PAYE: 'Payé',
  PARTIEL: 'Partiel',
  EN_RETARD: 'En retard',
  EN_ATTENTE: 'En attente',
  NON_PLANIFIE: 'Non planifié',
}

export const getLibelleSituationPaiement = (situation: string): string =>
  LIBELLES_SITUATION_PAIEMENT[situation as SituationPaiement] ?? situation

export const classeSituationPaiement = (situation: SituationPaiement): string => {
  switch (situation) {
    case 'PAYE':
      return 'pill ok'
    case 'PARTIEL':
      return 'pill warning'
    case 'EN_RETARD':
      return 'pill danger'
    default:
      return 'pill wt'
  }
}