import { useMemo, useState } from 'react'
import { dansPeriode, type ModePaiement, type PlanPaiement, type StatutPlan } from '../data/mockPaiements'

export interface PlansFiltres {
  client: string
  periode: string
  statut: StatutPlan | 'tous'
  mode: ModePaiement | 'tous'
  nbEcheances: string
  retard: 'tous' | 'oui' | 'non'
}

const DEFAUT: PlansFiltres = {
  client: 'tous',
  periode: 'tous',
  statut: 'tous',
  mode: 'tous',
  nbEcheances: 'toutes',
  retard: 'tous',
}

/** Liste des plans + filtres + pagination (8 par page). */
export function usePlansListStatic(source: PlanPaiement[], size = 8) {
  const [page, setPage] = useState(0)
  const [filtres, setFiltres] = useState<PlansFiltres>(DEFAUT)

  const filtered = useMemo(
    () =>
      source.filter((p) => {
        if (filtres.client !== 'tous' && p.client !== filtres.client) return false
        if (!dansPeriode(p.dateDebut, filtres.periode)) return false
        if (filtres.statut !== 'tous' && p.statut !== filtres.statut) return false
        if (filtres.mode !== 'tous' && p.dernierMode !== filtres.mode) return false
        if (filtres.nbEcheances !== 'toutes' && p.echeances.length !== Number(filtres.nbEcheances)) return false
        if (filtres.retard === 'oui' && p.statut !== 'En retard') return false
        if (filtres.retard === 'non' && p.statut === 'En retard') return false
        return true
      }),
    [source, filtres],
  )

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)

  return {
    items: filtered.slice(safePage * size, safePage * size + size),
    total,
    totalPages,
    page: safePage,
    size,
    setPage,
    filtres,
    setFiltre: <K extends keyof PlansFiltres>(cle: K, valeur: PlansFiltres[K]) => {
      setFiltres((f) => ({ ...f, [cle]: valeur }))
      setPage(0)
    },
    resetFiltres: () => {
      setFiltres(DEFAUT)
      setPage(0)
    },
    hasActiveFiltres: JSON.stringify(filtres) !== JSON.stringify(DEFAUT),
  }
}
