import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import type { Demande } from '@/features/demandes/types'
import { planPaiementApi } from '@/features/plan-paiement/services/plan-paiement.service'
import { paiementsApi } from '../services/paiements.service'
import type { EcheanceReglable, ModePaiement } from '../types'
import { comparerEcheances, echeancesOuvertes } from '../utils/situation'

/** Ligne DEBOURS réglable (montant validé, reste dû). */
export interface DeboursReglable {
  ligneId: number
  designation: string
  montantDebours: number
  montantPaye: number
  reste: number
  statut: string | null
}

export interface DemandeReglable {
  /** null tant qu'aucune demande n'est sélectionnée. */
  demande: Demande | null
  /** Échéances non soldées de toutes les lignes, à plat. */
  echeances: EcheanceReglable[]
  /** Lignes DEBOURS réglables (NON_PAYEE / PARTIEL avec reste). */
  debours: DeboursReglable[]
}

/**
 * Charge une demande et aplatit ses échéances réglables (toutes lignes
 * confondues) pour alimenter le formulaire de règlement.
 */
export function useEcheancesReglables(demandeId: number | null) {
  return useApi<DemandeReglable>(
    async () => {
      if (!demandeId) return { demande: null, echeances: [], debours: [] }
      const demande = await demandesApi.get(demandeId)
      const lignes = demande.lignes ?? []
      const plans = await planPaiementApi.getByLignes(lignes.map((l) => l.id))
      const echeances: EcheanceReglable[] = []
      const debours: DeboursReglable[] = []
      lignes.forEach((ligne, i) => {
        if (ligne.categorie === 'DEBOURS') {
          if (
            (ligne.statut === 'NON_PAYEE' || ligne.statut === 'PARTIEL') &&
            ligne.montantDebours != null && ligne.resteAPayer > 0
          ) {
            debours.push({
              ligneId: ligne.id,
              designation: ligne.designationDebours ?? `Débours ${i + 1}`,
              montantDebours: ligne.montantDebours,
              montantPaye: ligne.montantPaye,
              reste: ligne.resteAPayer,
              statut: ligne.statut,
            })
          }
          return
        }
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
      return { demande, echeances, debours }
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