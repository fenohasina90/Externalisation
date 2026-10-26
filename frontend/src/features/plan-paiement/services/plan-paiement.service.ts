import { http } from '@/api/http'
import type { ApiError } from '@/types'
import type {
  CreatePlanPaiementPayload,
  PlanPaiement,
  UpdatePlanPaiementPayload,
} from '../types'

/** 404 = aucun plan pour cette demande / cette ligne : l'appelant affiche « non planifié ». */
function estIntrouvable(error: unknown): boolean {
  return typeof error === 'object' && error !== null && (error as ApiError).status === 404
}

export const planPaiementApi = {
  /** Plan actif d'une demande (le premier trouvé) ; null si aucun plan. */
  async getByDemande(demandeId: number): Promise<PlanPaiement | null> {
    try {
      const { data } = await http.get<PlanPaiement>(`/demandes/${demandeId}/plan-paiement`)
      return data
    } catch (error) {
      if (estIntrouvable(error)) return null
      throw error
    }
  },

  /** Plan d'une ligne de service ; null si la ligne n'est pas encore planifiée. */
  async getByLigne(ligneId: number): Promise<PlanPaiement | null> {
    try {
      const { data } = await http.get<PlanPaiement>(`/lignes/${ligneId}/plan-paiement`)
      return data
    } catch (error) {
      if (estIntrouvable(error)) return null
      throw error
    }
  },

  /** Plans de toutes les lignes d'une demande, indexés par identifiant de ligne. */
  async getByLignes(ligneIds: number[]): Promise<Map<number, PlanPaiement>> {
    const plans = await Promise.all(ligneIds.map((id) => this.getByLigne(id)))
    const index = new Map<number, PlanPaiement>()
    plans.forEach((plan, i) => {
      if (plan) index.set(ligneIds[i], plan)
    })
    return index
  },

  async create(demandeId: number, payload: CreatePlanPaiementPayload): Promise<PlanPaiement> {
    const { data } = await http.post<PlanPaiement>(`/demandes/${demandeId}/plan-paiement`, payload)
    return data
  },

  async createForLigne(
    ligneId: number,
    payload: CreatePlanPaiementPayload,
  ): Promise<PlanPaiement> {
    const { data } = await http.post<PlanPaiement>(`/lignes/${ligneId}/plan-paiement`, payload)
    return data
  },

  async update(planId: number, payload: UpdatePlanPaiementPayload): Promise<PlanPaiement> {
    const { data } = await http.put<PlanPaiement>(`/plans-paiement/${planId}`, payload)
    return data
  },
}