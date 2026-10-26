import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { responsablesApi } from '../services/responsables.service'
import type { Responsable, StatutFilter } from '../types'

export function useResponsablesList(size = 8) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutFilter>('tous')
  const terme = useDebounce(search, 300)

  const actif = statut === 'tous' ? undefined : statut === 'actifs'

  const { data, loading, error, reload } = useApi<ApiPage<Responsable>>(
    () => responsablesApi.list({ page, size, recherche: terme.trim() || null }, { actif }),
    [page, size, terme, statut],
  )

  function handleSearch(value: string): void {
    setSearch(value)
    setPage(0)
  }

  function handleStatut(value: StatutFilter): void {
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