import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import type { Demande, SituationPaiement } from '../types'
import { demandesApi, type DemandeFiltres } from '../services/demandes.service'

export interface DemandesFiltresUI {
  recherche: string
  statut: string
  /** Situation de règlement, filtrée par le serveur. */
  situation: SituationPaiement | 'tous'
  societeId: number | null
  serviceId: number | null
  commercialId: number | null
  dateDebut: string
  dateFin: string
}

export const FILTRES_DEMANDES_DEFAUT: DemandesFiltresUI = {
  recherche: '',
  statut: 'tous',
  situation: 'tous',
  societeId: null,
  serviceId: null,
  commercialId: null,
  dateDebut: '',
  dateFin: '',
}

/** Liste paginée des demandes via l'API, avec filtres serveur. */
export function useDemandesList(size = 8) {
  const [page, setPage] = useState(0)
  const [filtres, setFiltres] = useState<DemandesFiltresUI>(FILTRES_DEMANDES_DEFAUT)
  const terme = useDebounce(filtres.recherche, 300)

  const apiFiltres: DemandeFiltres = {
    statut: filtres.statut === 'tous' ? undefined : filtres.statut,
    situation: filtres.situation === 'tous' ? undefined : filtres.situation,
    societeId: filtres.societeId ?? undefined,
    serviceId: filtres.serviceId ?? undefined,
    commercialId: filtres.commercialId ?? undefined,
    dateDebut: filtres.dateDebut || undefined,
    dateFin: filtres.dateFin || undefined,
  }

  const { data, loading, error, reload } = useApi<ApiPage<Demande>>(
    () =>
      demandesApi.list(
        { page, size, recherche: terme.trim() || null },
        apiFiltres,
      ),
    [page, size, terme, filtres.statut, filtres.situation, filtres.societeId,
      filtres.serviceId, filtres.commercialId, filtres.dateDebut, filtres.dateFin],
  )

  function setFiltre<K extends keyof DemandesFiltresUI>(cle: K, valeur: DemandesFiltresUI[K]): void {
    setFiltres((f) => ({ ...f, [cle]: valeur }))
    setPage(0)
  }

  function resetFiltres(): void {
    setFiltres(FILTRES_DEMANDES_DEFAUT)
    setPage(0)
  }

  const hasActiveFiltres =
    filtres.recherche.trim() !== '' ||
    filtres.statut !== 'tous' ||
    filtres.situation !== 'tous' ||
    filtres.societeId !== null ||
    filtres.serviceId !== null ||
    filtres.commercialId !== null ||
    filtres.dateDebut !== '' ||
    filtres.dateFin !== ''

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
    filtres,
    setFiltre,
    resetFiltres,
    hasActiveFiltres,
  }
}
