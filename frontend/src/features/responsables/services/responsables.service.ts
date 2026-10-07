import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { CreateResponsablePayload, Responsable } from '../types'

export const responsablesApi = {
  async list(page?: PageQuery): Promise<ApiPage<Responsable>> {
    const { data } = await http.get<ApiPage<Responsable>>('/responsables', {
      params: buildQuery({}, page),
    })
    return data
  },

  async get(id: number): Promise<Responsable> {
    const { data } = await http.get<Responsable>(`/responsables/${id}`)
    return data
  },

  async create(payload: CreateResponsablePayload): Promise<Responsable> {
    const { data } = await http.post<Responsable>('/responsables', payload)
    return data
  },

  async update(id: number, payload: CreateResponsablePayload): Promise<Responsable> {
    const { data } = await http.put<Responsable>(`/responsables/${id}`, payload)
    return data
  },

  async toggleActivation(id: number): Promise<Responsable> {
    const { data } = await http.patch<Responsable>(`/responsables/${id}/activation`)
    return data
  },
}