import type { Echeance } from '@/features/echeances/types'
import type { SituationPlan } from '../types'

/**
 * Situation de règlement d'un plan, déduite des états de ses échéances.
 * Le backend calcule l'état de chaque échéance (`EcheanceMapper`) ; on ne fait
 * ici qu'une agrégation pour l'affichage de la liste.
 */
export function situationPlan(echeances: Echeance[]): SituationPlan {
  if (echeances.length === 0) return 'EN_ATTENTE'
  const payes = echeances.filter((e) => e.reste <= 0).length
  if (payes === echeances.length) return 'PAYE'
  if (echeances.some((e) => e.statut === 'EN_RETARD')) return 'EN_RETARD'
  if (echeances.some((e) => e.montantPaye > 0)) return 'PARTIEL'
  return 'EN_ATTENTE'
}

/** Somme des restes des échéances d'un plan. */
export function resteDesEcheances(echeances: Echeance[]): number {
  return echeances.reduce((somme, e) => somme + e.reste, 0)
}

/** Ordre chronologique : date d'échéance puis numéro. */
export function comparerEcheances(a: Echeance, b: Echeance): number {
  return (
    a.dateEcheance.localeCompare(b.dateEcheance) ||
    a.numeroEcheance - b.numeroEcheance
  )
}

/** Échéances triées par date puis numéro, sans muter la liste reçue. */
export function trierEcheances(echeances: Echeance[]): Echeance[] {
  return [...echeances].sort(comparerEcheances)
}

/** Échéances non soldées, triées par date puis numéro. */
export function echeancesOuvertes(echeances: Echeance[]): Echeance[] {
  return trierEcheances(echeances.filter((e) => e.reste > 0))
}