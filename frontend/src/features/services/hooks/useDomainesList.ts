import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { domainesApi } from '../services/services.service'
import type { Domaine } from '../types'

export type StatutDomaineFilter = 'tous' | 'actifs' | 'inactifs'

/** Liste paginée via l'API : recherche (debounce), filtre statut, pagination. */
export function useDomainesList(size = 8) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutDomaineFilter>('tous')
  const terme = useDebounce(search, 300)

  const actif = statut === 'tous' ? undefined : statut === 'actifs'

  const { data, loading, error, reload } = useApi<ApiPage<Domaine>>(
    () => domainesApi.list({ page, size, recherche: terme.trim() || null }, { actif }),
    [page, size, terme, statut],
  )

  function handleSearch(value: string): void {
    setSearch(value)
    setPage(0)
  }

  function handleStatut(value: StatutDomaineFilter): void {
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
