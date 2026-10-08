import { useMemo, useState } from 'react'
import type { Commercial } from '../types'
import type { StatutCommercialFilter } from '../data/mockCommerciaux'

interface Options {
  /** Source statique (état local de la page). */
  source: Commercial[]
  size?: number
}

/**
 * Version statique du hook de liste (étape 1 — sans backend).
 * Même signature que useResponsablesList pour basculer
 * facilement vers l'API ensuite : recherche, filtre statut, pagination.
 */
export function useCommerciauxListStatic({ source, size = 8 }: Options) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutCommercialFilter>('tous')

  const filtered = useMemo(() => {
    const terme = search.trim().toLowerCase()
    return source.filter((c) => {
      if (statut === 'actifs' && !c.actif) return false
      if (statut === 'inactifs' && c.actif) return false
      if (!terme) return true
      const haystack = [c.nomCommercial, c.immatricule, c.email, c.contact]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return haystack.includes(terme)
    })
  }, [source, search, statut])

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)
  const items = filtered.slice(safePage * size, safePage * size + size)

  const handleSearch = (value: string) => {
    setSearch(value)
    setPage(0)
  }

  const handleStatut = (value: StatutCommercialFilter) => {
    setStatut(value)
    setPage(0)
  }

  return {
    items,
    total,
    totalPages,
    page: safePage,
    size,
    setPage,
    search,
    setSearch: handleSearch,
    statut,
    setStatut: handleStatut,
  }
}
