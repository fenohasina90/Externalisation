import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { Commercial, CreateCommercialPayload } from '../types'

export const commerciauxApi = {
  async list(page?: PageQuery): Promise<ApiPage<Commercial>> {
    const { data } = await http.get<ApiPage<Commercial>>('/commerciaux', {
      params: buildQuery({}, page),
    })
    return data
  },

  async get(id: number): Promise<Commercial> {
    const { data } = await http.get<Commercial>(`/commerciaux/${id}`)
    return data
  },

  async create(payload: CreateCommercialPayload): Promise<Commercial> {
    const { data } = await http.post<Commercial>('/commerciaux', payload)
    return data
  },

  async update(id: number, payload: CreateCommercialPayload): Promise<Commercial> {
    const { data } = await http.put<Commercial>(`/commerciaux/${id}`, payload)
    return data
  },

  async toggleActivation(id: number): Promise<Commercial> {
    const { data } = await http.patch<Commercial>(`/commerciaux/${id}/activation`)
    return data
  },
}