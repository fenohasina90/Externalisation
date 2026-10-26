import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { modesPaiementApi } from '../services/administration.service'
import type { ModePaiementAdmin } from '../types'

export type ModePaiementFilter = 'tous' | 'actifs' | 'inactifs'

/** Liste paginée des modes de paiement : filtre actif, tri par code. */
export function useModesPaiementList(size = 8) {
  const [page, setPage] = useState(0)
  const [statut, setStatut] = useState<ModePaiementFilter>('tous')

  const actif = statut === 'tous' ? undefined : statut === 'actifs'

  const { data, loading, error, reload } = useApi<ApiPage<ModePaiementAdmin>>(
    () => modesPaiementApi.list({ page, size, recherche: null }, { actif }),
    [page, size, statut],
  )

  function handleStatut(value: ModePaiementFilter): void {
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
