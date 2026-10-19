import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { paiementsApi, type PaiementFiltres } from '../services/paiements.service'
import type { Paiement } from '../types'

export interface PaiementsFiltresUI {
  recherche: string
  modeId: number | null
  dateDebut: string
  dateFin: string
}

export const FILTRES_PAIEMENTS_DEFAUT: PaiementsFiltresUI = {
  recherche: '',
  modeId: null,
  dateDebut: '',
  dateFin: '',
}

/** Historique des règlements paginé et filtré côté serveur. */
export function usePaiementsList(size = 8, filtresFixes?: PaiementFiltres) {
  const [page, setPage] = useState(0)
  const [filtres, setFiltres] = useState<PaiementsFiltresUI>(FILTRES_PAIEMENTS_DEFAUT)
  const terme = useDebounce(filtres.recherche, 300)

  const apiFiltres: PaiementFiltres = {
    demandeId: filtresFixes?.demandeId,
    modeId: filtres.modeId ?? undefined,
    dateDebut: filtres.dateDebut || undefined,
    dateFin: filtres.dateFin || undefined,
  }

  const { data, loading, error, reload } = useApi<ApiPage<Paiement>>(
    () => paiementsApi.list({ page, size, recherche: terme.trim() || null }, apiFiltres),
    [
      page,
      size,
      terme,
      filtres.modeId,
      filtres.dateDebut,
      filtres.dateFin,
      filtresFixes?.demandeId,
    ],
  )

  function setFiltre<K extends keyof PaiementsFiltresUI>(
    cle: K,
    valeur: PaiementsFiltresUI[K],
  ): void {
    setFiltres((f) => ({ ...f, [cle]: valeur }))
    setPage(0)
  }

  function resetFiltres(): void {
    setFiltres(FILTRES_PAIEMENTS_DEFAUT)
    setPage(0)
  }

  const hasActiveFiltres =
    filtres.recherche.trim() !== '' ||
    filtres.modeId !== null ||
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