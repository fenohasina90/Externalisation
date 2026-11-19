import { useApi } from '@/hooks/useApi'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import type { Demande, LigneService } from '@/features/demandes/types'
import { paiementsApi } from '../services/paiements.service'
import type { Paiement } from '../types'

export interface LigneSuiviDemande {
  ligneId: number
  ligneLabel: string
  categorie: string | null
  typePrestation: string | null
  typeService: string | null
  nomService: string | null
  designationDebours: string | null
}

export interface SuiviDemande {
  demande: Demande
  lignes: LigneSuiviDemande[]
  paiements: Paiement[]
}

function labelLigne(ligne: LigneService, index: number): string {
  const estDebours = ligne.categorie === 'DEBOURS'
  const nom = estDebours
    ? (ligne.designationDebours ?? `Débours ${index + 1}`)
    : (ligne.nomService ?? `Service ${ligne.serviceId ?? index + 1}`)
  return `${index + 1}. ${nom}`
}

/**
 * Suivi des paiements d'une demande : en-tête de demande, lignes de service et
 * paiements enregistrés. `?reference=` résout la demande via
 * `GET /demandes/reference/{ref}`.
 */
export function useSuiviDemande(reference: string | undefined) {
  return useApi<SuiviDemande>(
    async () => {
      if (!reference) throw new Error('Référence de demande manquante.')
      const demande = await demandesApi.getByReference(reference)
      const lignes = demande.lignes ?? []
      const pagePaiements = await paiementsApi.list(
        { page: 0, size: 200, recherche: null },
        { demandeId: demande.id },
      )
      const suiviLignes: LigneSuiviDemande[] = lignes.map((ligne, i) => ({
        ligneId: ligne.id,
        ligneLabel: labelLigne(ligne, i),
        categorie: ligne.categorie ?? null,
        typePrestation: ligne.typePrestation ?? null,
        typeService: ligne.typeService ?? null,
        nomService: ligne.nomService ?? null,
        designationDebours: ligne.designationDebours ?? null,
      }))
      return {
        demande,
        lignes: suiviLignes,
        paiements: pagePaiements.content,
      }
    },
    [reference],
  )
}