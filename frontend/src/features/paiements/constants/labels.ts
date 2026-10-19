import type { SituationPlan } from '../types'

export const LIBELLES_SITUATION: Record<SituationPlan, string> = {
  PAYE: 'Payé',
  PARTIEL: 'Partiel',
  EN_RETARD: 'En retard',
  EN_ATTENTE: 'En attente',
  NON_PLANIFIE: 'Non planifié',
}

export const classeSituation = (situation: SituationPlan): string => {
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

/** Le virement impose une référence (règle métier R3). */
export const VIREMENT = 'VIREMENT'

export const classeMode = (code: string | null | undefined): string =>
  code === VIREMENT ? 'pill info' : 'pill wt'