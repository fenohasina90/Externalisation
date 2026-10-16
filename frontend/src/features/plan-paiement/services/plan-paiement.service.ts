import { http } from '@/api/http'
import type { CreatePlanPaiementPayload, PlanPaiement } from '../types'

export const planPaiementApi = {
  async getByDemande(demandeId: number): Promise<PlanPaiement | null> {
    const { data } = await http.get<PlanPaiement>(`/demandes/${demandeId}/plan-paiement`)
    return data
  },

  async create(
    demandeId: number,
    payload: CreatePlanPaiementPayload,
  ): Promise<PlanPaiement> {
    const { data } = await http.post<PlanPaiement>(
      `/demandes/${demandeId}/plan-paiement`,
      payload,
    )
    return data
  },

  async getByLigne(ligneId: number): Promise<PlanPaiement | null> {
    const { data } = await http.get<PlanPaiement>(`/lignes/${ligneId}/plan-paiement`)
    return data
  },

  async createForLigne(
    ligneId: number,
    payload: CreatePlanPaiementPayload,
  ): Promise<PlanPaiement> {
    const { data } = await http.post<PlanPaiement>(
      `/lignes/${ligneId}/plan-paiement`,
      payload,
    )
    return data
  },
}