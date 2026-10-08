import { useMemo, useState } from 'react'
import type { StatutServiceFilter } from '../data/mockServices'
import type { Service } from '../types'

interface Options {
  source: Service[]
  size?: number
}

/** Filtrage + pagination statiques (même signature que les hooks liste). */
export function useServicesListStatic({ source, size = 8 }: Options) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutServiceFilter>('tous')

  const filtered = useMemo(() => {
    const terme = search.trim().toLowerCase()
    return source.filter((s) => {
      if (statut === 'actifs' && !s.actif) return false
      if (statut === 'archives' && s.actif) return false
      if (!terme) return true
      return `${s.nomService} ${s.description ?? ''}`.toLowerCase().includes(terme)
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
    setStatut: (v: StatutServiceFilter) => {
      setStatut(v)
      setPage(0)
    },
  }
}
