import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { commerciauxApi } from '../services/commerciaux.service'
import type { Commercial } from '../types'

export type StatutCommercialFilter = 'tous' | 'actifs' | 'inactifs'

/** Liste paginée via l'API : recherche (debounce), filtre statut, pagination. */
export function useCommerciauxList(size = 8) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutCommercialFilter>('tous')
  const terme = useDebounce(search, 300)

  const actif = statut === 'tous' ? undefined : statut === 'actifs'

  const { data, loading, error, reload } = useApi<ApiPage<Commercial>>(
    () => commerciauxApi.list({ page, size, recherche: terme.trim() || null }, { actif }),
    [page, size, terme, statut],
  )

  function handleSearch(value: string): void {
    setSearch(value)
    setPage(0)
  }

  function handleStatut(value: StatutCommercialFilter): void {
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
