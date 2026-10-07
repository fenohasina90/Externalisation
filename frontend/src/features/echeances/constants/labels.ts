import type { EtatEcheance } from '../types'

export const LIBELLES_ETAT_ECHEANCE: Record<EtatEcheance, string> = {
  A_JOUR: 'À JOUR',
  PROCHE: 'PROCHE',
  EN_RETARD: 'EN RETARD',
}