import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import type { Demande } from '@/features/demandes/types'
import type { ModePaiement } from '../types'
import { paiementsApi } from '../services/paiements.service'

/** Demandes candidates à un règlement (terminées et annulées exclues). */
export function useDemandesReglables(size = 200) {
  return useApi<ApiPage<Demande>>(
    () =>
      demandesApi.list({ page: 0, size, recherche: null }, {}).then((page) => ({
        ...page,
        content: page.content.filter(
          (d) => d.statutActuel !== 'TERMINEE' && d.statutActuel !== 'ANNULEE',
        ),
      })),
    [size],
  )
}

/** Modes de paiement actifs (ESPECES / VIREMENT / CHEQUE). */
export function useModesPaiement() {
  return useApi<ModePaiement[]>(() => paiementsApi.listModes(), [])
}
