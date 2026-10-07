import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { CreatePaiementPayload, ModePaiement, Paiement } from '../types'

export interface PaiementFiltres {
  societeId?: number
  modePaiementId?: number
  dateDebut?: string
  dateFin?: string
}

export const paiementsApi = {
  async list(page?: PageQuery, filtres?: PaiementFiltres): Promise<ApiPage<Paiement>> {
    const { data } = await http.get<ApiPage<Paiement>>('/paiements', {
      params: buildQuery({ ...filtres }, page),
    })
    return data
  },

  async get(id: number): Promise<Paiement> {
    const { data } = await http.get<Paiement>(`/paiements/${id}`)
    return data
  },

  async create(payload: CreatePaiementPayload): Promise<Paiement> {
    const { data } = await http.post<Paiement>('/paiements', payload)
    return data
  },

  async listModes(): Promise<ModePaiement[]> {
    const { data } = await http.get<ModePaiement[]>('/modes-paiement')
    return data
  },
}