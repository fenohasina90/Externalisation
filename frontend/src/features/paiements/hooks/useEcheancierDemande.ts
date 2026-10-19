import { useApi } from '@/hooks/useApi'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import type { Demande, LigneService } from '@/features/demandes/types'
import { planPaiementApi } from '@/features/plan-paiement/services/plan-paiement.service'
import { paiementsApi } from '../services/paiements.service'
import type { Paiement, PlanParLigne } from '../types'

export interface EcheancierDemande {
  demande: Demande
  plansParLigne: PlanParLigne[]
  paiements: Paiement[]
}

function labelLigne(ligne: LigneService, index: number): string {
  const nom = ligne.nomService ?? `Service ${ligne.serviceId ?? index + 1}`
  return `${index + 1}. ${nom}`
}

/**
 * Échéancier complet d'une demande : en-tête, plan de chaque ligne de service
 * et paiements enregistrés. `?reference=` résout la demande via
 * `GET /demandes/reference/{ref}`.
 */
export function useEcheancierDemande(reference: string | undefined) {
  return useApi<EcheancierDemande>(
    async () => {
      if (!reference) throw new Error('Référence de demande manquante.')
      const demande = await demandesApi.getByReference(reference)
      const lignes = demande.lignes ?? []
      const plans = await planPaiementApi.getByLignes(lignes.map((l) => l.id))
      const pagePaiements = await paiementsApi.list(
        { page: 0, size: 200, recherche: null },
        { demandeId: demande.id },
      )
      const plansParLigne: PlanParLigne[] = lignes.map((ligne, i) => ({
        ligneId: ligne.id,
        ligneLabel: labelLigne(ligne, i),
        debutService: ligne.debutService,
        finService: ligne.finService,
        plan: plans.get(ligne.id) ?? null,
      }))
      return {
        demande,
        plansParLigne,
        paiements: pagePaiements.content,
      }
    },
    [reference],
  )
}