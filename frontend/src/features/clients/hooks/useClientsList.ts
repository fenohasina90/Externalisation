import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { clientsApi, type TriClients } from '../services/clients.service'
import type { Societe, StatutClient } from '../types'

interface Options {
  size?: number
}

export type StatutClientFilter = StatutClient | 'tous'

/** Liste paginée via l'API : recherche, domaine, statut, tri serveur. */
export function useClientsList({ size = 8 }: Options = {}) {
  const [page, setPage] = useState(0)
  const [recherche, setRecherche] = useState('')
  const [domaineId, setDomaineId] = useState<number | null>(null)
  const [statut, setStatut] = useState<StatutClientFilter>('tous')
  const [tri, setTri] = useState<TriClients>({ champ: 'nomSociete', sens: 'asc' })
  const terme = useDebounce(recherche, 300)

  const actif = statut === 'Actif' ? true : statut === 'Inactif' ? false : undefined
  const archive = statut === 'Archivé' ? true : statut === 'tous' ? undefined : false

  const { data, loading, error, reload } = useApi<ApiPage<Societe>>(
    () =>
      clientsApi.list(
        { page, size, recherche: terme.trim() || null },
        { actif, archive, domaineId: domaineId ?? undefined },
        tri,
      ),
    [page, size, terme, domaineId, statut, tri],
  )

  function resetPage(): void {
    setPage(0)
  }

  const hasActiveFiltres =
    recherche.trim() !== '' || domaineId !== null || statut !== 'tous'

  function resetFiltres(): void {
    setRecherche('')
    setDomaineId(null)
    setStatut('tous')
    setTri({ champ: 'nomSociete', sens: 'asc' })
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
    recherche,
    setRecherche: (v: string) => {
      setRecherche(v)
      resetPage()
    },
    domaineId,
    setDomaineId: (v: number | null) => {
      setDomaineId(v)
      resetPage()
    },
    statut,
    setStatut: (v: StatutClientFilter) => {
      setStatut(v)
      resetPage()
    },
    tri,
    setTri: (v: TriClients) => {
      setTri(v)
      resetPage()
    },
    hasActiveFiltres,
    resetFiltres,
  }
}
