import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import type { Demande } from '@/features/demandes/types'
import { planPaiementApi } from '@/features/plan-paiement/services/plan-paiement.service'
import { paiementsApi } from '../services/paiements.service'
import type { EcheanceReglable, ModePaiement } from '../types'
import { comparerEcheances, echeancesOuvertes } from '../utils/situation'

export interface DemandeReglable {
  /** null tant qu'aucune demande n'est sélectionnée. */
  demande: Demande | null
  /** Échéances non soldées de toutes les lignes, à plat. */
  echeances: EcheanceReglable[]
}

/**
 * Charge une demande et aplatit ses échéances réglables (toutes lignes
 * confondues) pour alimenter le formulaire de règlement.
 */
export function useEcheancesReglables(demandeId: number | null) {
  return useApi<DemandeReglable>(
    async () => {
      if (!demandeId) return { demande: null, echeances: [] }
      const demande = await demandesApi.get(demandeId)
      const lignes = demande.lignes ?? []
      const plans = await planPaiementApi.getByLignes(lignes.map((l) => l.id))
      const echeances: EcheanceReglable[] = []
      lignes.forEach((ligne, i) => {
        const plan = plans.get(ligne.id)
        if (!plan) return
        const nom = ligne.nomService ?? `Service ${ligne.serviceId ?? i + 1}`
        echeancesOuvertes(plan.echeances ?? []).forEach((echeance) => {
          echeances.push({
            echeance,
            ligneId: ligne.id,
            ligneLabel: nom,
            demandeId: demande.id,
            referenceDemande: demande.referenceDemande,
            nomSociete: demande.nomSociete,
            debutService: ligne.debutService,
            finService: ligne.finService,
          })
        })
      })
      // Les plans sont chargés service par service : on recolle les échéances
      // dans un ordre chronologique global pour le sélecteur de règlement.
      echeances.sort((a, b) => comparerEcheances(a.echeance, b.echeance))
      return { demande, echeances }
    },
    [demandeId],
  )
}

/** Demandes candidates à un règlement (charge utile de l'écran de saisie). */
export function useDemandesReglables(size = 200) {
  return useApi<ApiPage<Demande>>(
    () => demandesApi.list({ page: 0, size, recherche: null }, {}),
    [size],
  )
}

/** Modes de paiement actifs (ESPECES / VIREMENT) pour le formulaire. */
export function useModesPaiement() {
  return useApi<ModePaiement[]>(() => paiementsApi.listModes(), [])
}