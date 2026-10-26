import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { statutsApi } from '../services/administration.service'
import type { StatutDemandeConfig } from '../types'

export type StatutConfigFilter = 'tous' | 'actifs' | 'inactifs'

/** Liste paginée des statuts de demande : filtre actif, tri par ordre. */
export function useStatutsList(size = 8) {
  const [page, setPage] = useState(0)
  const [statut, setStatut] = useState<StatutConfigFilter>('tous')

  const actif = statut === 'tous' ? undefined : statut === 'actifs'

  const { data, loading, error, reload } = useApi<ApiPage<StatutDemandeConfig>>(
    () => statutsApi.list({ page, size, recherche: null }, { actif }),
    [page, size, statut],
  )

  function handleStatut(value: StatutConfigFilter): void {
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
    statut,
    setStatut: handleStatut,
  }
}
