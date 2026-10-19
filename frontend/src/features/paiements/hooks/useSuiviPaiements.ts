import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { dashboardApi } from '@/features/dashboard/services/dashboard.service'
import type { DashboardSummary } from '@/features/dashboard/types'
import type { Demande } from '@/features/demandes/types'
import { demandesApi } from '@/features/demandes/services/demandes.service'

export interface SuiviFiltresUI {
  recherche: string
  societeId: number | null
  statut: string
  dateDebut: string
  dateFin: string
}

export const FILTRES_SUIVI_DEFAUT: SuiviFiltresUI = {
  recherche: '',
  societeId: null,
  statut: 'tous',
  dateDebut: '',
  dateFin: '',
}

/**
 * Suivi financier par demande : `GET /demandes` expose déjà
 * `montantTotal` / `montantPaye` / `resteAPayer` par demande.
 */
export function useSuiviPaiements(size = 8) {
  const [page, setPage] = useState(0)
  const [filtres, setFiltres] = useState<SuiviFiltresUI>(FILTRES_SUIVI_DEFAUT)
  const terme = useDebounce(filtres.recherche, 300)

  const { data, loading, error, reload } = useApi<ApiPage<Demande>>(
    () =>
      demandesApi.list(
        { page, size, recherche: terme.trim() || null },
        {
          societeId: filtres.societeId ?? undefined,
          statut: filtres.statut === 'tous' ? undefined : filtres.statut,
          dateDebut: filtres.dateDebut || undefined,
          dateFin: filtres.dateFin || undefined,
        },
      ),
    [
      page,
      size,
      terme,
      filtres.societeId,
      filtres.statut,
      filtres.dateDebut,
      filtres.dateFin,
    ],
  )

  // Agrégats du module « paiements » (DASHBOARD_VIEW). Le backend masque les
  // montants pour les rôles qui n'y ont pas droit : la valeur peut donc
  // arriver à `null` sans que ce soit une erreur.
  const {
    data: synthese,
    loading: syntheseLoading,
    error: syntheseError,
  } = useApi<DashboardSummary>(() => dashboardApi.getSummary(), [])

  function setFiltre<K extends keyof SuiviFiltresUI>(cle: K, valeur: SuiviFiltresUI[K]): void {
    setFiltres((f) => ({ ...f, [cle]: valeur }))
    setPage(0)
  }

  function resetFiltres(): void {
    setFiltres(FILTRES_SUIVI_DEFAUT)
    setPage(0)
  }

  const hasActiveFiltres =
    filtres.recherche.trim() !== '' ||
    filtres.societeId !== null ||
    filtres.statut !== 'tous' ||
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
    synthese,
    syntheseLoading,
    syntheseError,
  }
}