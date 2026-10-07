import { http } from '@/api/http'
import type { CreateEcheancePayload, Echeance } from '../types'

export const echeancesApi = {
  async listByPlan(planId: number): Promise<Echeance[]> {
    const { data } = await http.get<Echeance[]>(`/plans-paiement/${planId}/echeances`)
    return data
  },

  async create(planId: number, payload: CreateEcheancePayload): Promise<Echeance> {
    const { data } = await http.post<Echeance>(`/plans-paiement/${planId}/echeances`, payload)
    return data
  },

  async enRetard(): Promise<Echeance[]> {
    const { data } = await http.get<Echeance[]>('/echeances/en-retard')
    return data
  },

  async aVenir(): Promise<Echeance[]> {
    const { data } = await http.get<Echeance[]>('/echeances/a-venir')
    return data
  },
}