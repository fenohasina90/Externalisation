import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { servicesApi } from '../services/services.service'
import type { Service } from '../types'

export type StatutServiceFilter = 'tous' | 'actifs' | 'archives'

/** Liste paginée via l'API : recherche (nom ou code, debounce), filtre statut. */
export function useServicesList(size = 8) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutServiceFilter>('tous')
  const terme = useDebounce(search, 300)

  const actif = statut === 'tous' ? undefined : statut === 'actifs'

  const { data, loading, error, reload } = useApi<ApiPage<Service>>(
    () => servicesApi.list({ page, size, recherche: terme.trim() || null }, { actif }),
    [page, size, terme, statut],
  )

  function handleSearch(value: string): void {
    setSearch(value)
    setPage(0)
  }

  function handleStatut(value: StatutServiceFilter): void {
    setStatut(value)
    setPage(0)
  }

  return {
    items: data?.content ?? [],
    total: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page,
    size,
    loading,
    error,
    reload,
    setPage,
    search,
    setSearch: handleSearch,
    statut,
    setStatut: handleStatut,
  }
}
