import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { CreateEcheancePayload, Echeance, UpdateEcheancePayload } from '../types'

export const echeancesApi = {
  async listByPlan(planId: number): Promise<Echeance[]> {
    const { data } = await http.get<Echeance[]>(`/plans-paiement/${planId}/echeances`)
    return data
  },

  async create(planId: number, payload: CreateEcheancePayload): Promise<Echeance> {
    const { data } = await http.post<Echeance>(`/plans-paiement/${planId}/echeances`, payload)
    return data
  },

  async update(echeanceId: number, payload: UpdateEcheancePayload): Promise<Echeance> {
    const { data } = await http.put<Echeance>(`/echeances/${echeanceId}`, payload)
    return data
  },

  /** Échéances dépassées avec un reste à payer (paginé côté serveur). */
  async enRetard(page?: PageQuery): Promise<ApiPage<Echeance>> {
    const { data } = await http.get<ApiPage<Echeance>>('/echeances/en-retard', {
      params: buildQuery({}, page),
    })
    return data
  },

  /** Échéances à venir non entièrement payées (paginé côté serveur). */
  async aVenir(page?: PageQuery): Promise<ApiPage<Echeance>> {
    const { data } = await http.get<ApiPage<Echeance>>('/echeances/a-venir', {
      params: buildQuery({}, page),
    })
    return data
  },
}