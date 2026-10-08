import { useMemo, useState } from 'react'
import type { StatutDomaineFilter } from '../data/mockDomaines'
import type { Domaine } from '../types'

interface Options {
  source: Domaine[]
  size?: number
}

/** Filtrage + pagination statiques (même signature que les hooks liste). */
export function useDomainesListStatic({ source, size = 8 }: Options) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutDomaineFilter>('tous')

  const filtered = useMemo(() => {
    const terme = search.trim().toLowerCase()
    return source.filter((d) => {
      if (statut === 'actifs' && !d.actif) return false
      if (statut === 'inactifs' && d.actif) return false
      if (!terme) return true
      return d.nomDomaine.toLowerCase().includes(terme)
    })
  }, [source, search, statut])

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
    search,
    setSearch: (v: string) => {
      setSearch(v)
      setPage(0)
    },
    statut,
    setStatut: (v: StatutDomaineFilter) => {
      setStatut(v)
      setPage(0)
    },
  }
}
