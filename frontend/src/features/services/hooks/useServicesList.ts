import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { servicesApi } from '../services/services.service'
import type { Service, TypeService } from '../types'

export type StatutServiceFilter = 'tous' | 'actifs' | 'archives'
export type TypeServiceFilter = 'tous' | TypeService

/** Liste paginée via l'API : recherche (nom ou code, debounce), filtres statut, type et domaine. */
export function useServicesList(size = 8) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statut, setStatut] = useState<StatutServiceFilter>('tous')
  const [typeService, setTypeService] = useState<TypeServiceFilter>('tous')
  const [domaineId, setDomaineId] = useState<string>('')
  const terme = useDebounce(search, 300)

  const actif = statut === 'tous' ? undefined : statut === 'actifs'
  const type = typeService === 'tous' ? undefined : typeService
  const domaine = domaineId.trim() ? Number(domaineId) : undefined

  const { data, loading, error, reload } = useApi<ApiPage<Service>>(
    () => servicesApi.list(
      { page, size, recherche: terme.trim() || null },
      { actif, typeService: type, domaineId: domaine },
    ),
    [page, size, terme, statut, typeService, domaineId],
  )

  function handleSearch(value: string): void {
    setSearch(value)
    setPage(0)
  }

  function handleStatut(value: StatutServiceFilter): void {
    setStatut(value)
    setPage(0)
  }

  function handleTypeService(value: TypeServiceFilter): void {
    setTypeService(value)
    setPage(0)
  }

  function handleDomaineId(value: string): void {
    setDomaineId(value)
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
    typeService,
    setTypeService: handleTypeService,
    domaineId,
    setDomaineId: handleDomaineId,
  }
}
