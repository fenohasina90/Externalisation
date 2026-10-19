import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { ApiPage } from '@/types'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import type { Demande } from '@/features/demandes/types'
import type { SituationPlan } from '../types'

export interface EcheanciersFiltresUI {
  recherche: string
  societeId: number | null
  /** Situation de règlement — filtrée par le serveur sur l'ensemble des résultats. */
  situation: SituationPlan | 'tous'
}

export const FILTRES_ECHEANCIERS_DEFAUT: EcheanciersFiltresUI = {
  recherche: '',
  societeId: null,
  situation: 'tous',
}

/**
 * Liste des échéanciers : une ligne par demande.
 *
 * La situation et le nombre d'échéances sont fournis par le backend
 * (`DemandeResponse`), calculés sur l'ensemble des lignes de la demande. Le
 * filtre `situation` est appliqué par le serveur : il porte donc sur tous les
 * résultats, pas sur la seule page affichée, et la pastille affichée ne peut
 * pas diverger de la situation qui a servi à filtrer.
 */
export function useEcheanciers(size = 8) {
  const [page, setPage] = useState(0)
  const [filtres, setFiltres] = useState<EcheanciersFiltresUI>(FILTRES_ECHEANCIERS_DEFAUT)
  const terme = useDebounce(filtres.recherche, 300)

  const { data, loading, error, reload } = useApi<ApiPage<Demande>>(
    () =>
      demandesApi.list(
        { page, size, recherche: terme.trim() || null },
        {
          societeId: filtres.societeId ?? undefined,
          situation: filtres.situation === 'tous' ? undefined : filtres.situation,
        },
      ),
    [page, size, terme, filtres.societeId, filtres.situation],
  )

  const items = (data?.content ?? []).map((d) => ({
    demandeId: d.id,
    referenceDemande: d.referenceDemande,
    nomSociete: d.nomSociete,
    nbServices: d.lignes?.length ?? 0,
    nbEcheances: d.nombreEcheances ?? 0,
    montantTotal: d.montantTotal ?? 0,
    reste: d.resteAPayer ?? 0,
    situation: (d.situationPaiement ?? 'NON_PLANIFIE') as SituationPlan,
  }))

  function setFiltre<K extends keyof EcheanciersFiltresUI>(
    cle: K,
    valeur: EcheanciersFiltresUI[K],
  ): void {
    setFiltres((f) => ({ ...f, [cle]: valeur }))
    setPage(0)
  }

  function resetFiltres(): void {
    setFiltres(FILTRES_ECHEANCIERS_DEFAUT)
    setPage(0)
  }

  return {
    items,
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
    hasActiveFiltres:
      filtres.recherche.trim() !== '' ||
      filtres.societeId !== null ||
      filtres.situation !== 'tous',
  }
}