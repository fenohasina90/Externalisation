import type { EtatEcheance } from '../types'

export const LIBELLES_ETAT_ECHEANCE: Record<EtatEcheance, string> = {
  PAYEE: 'Payée',
  EN_RETARD: 'En retard',
  PROCHE: 'Proche',
  PARTIELLE: 'Partielle',
  A_VENIR: 'À venir',
}

export const getLibelleEtatEcheance = (statut: string): string =>
  LIBELLES_ETAT_ECHEANCE[statut as EtatEcheance] ?? statut

/** Classe CSS du badge d'état d'échéance. */
export const classeEtatEcheance = (statut: string): string => {
  switch (statut) {
    case 'PAYEE':
      return 'pill ok'
    case 'EN_RETARD':
      return 'pill danger'
    case 'PROCHE':
      return 'pill warning'
    case 'PARTIELLE':
      return 'pill info'
    default:
      return 'pill wt'
  }
}