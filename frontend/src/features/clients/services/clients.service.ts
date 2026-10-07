import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { CreateSocietePayload, Societe, UpdateSocietePayload } from '../types'

export interface SocieteFiltres {
  actif?: boolean
  domaineId?: number
}

export const clientsApi = {
  async list(page?: PageQuery, filtres?: SocieteFiltres): Promise<ApiPage<Societe>> {
    const { data } = await http.get<ApiPage<Societe>>('/clients', {
      params: buildQuery({ ...filtres }, page),
    })
    return data
  },

  async get(id: number): Promise<Societe> {
    const { data } = await http.get<Societe>(`/clients/${id}`)
    return data
  },

  async create(payload: CreateSocietePayload): Promise<Societe> {
    const { data } = await http.post<Societe>('/clients', payload)
    return data
  },

  async update(id: number, payload: UpdateSocietePayload): Promise<Societe> {
    const { data } = await http.put<Societe>(`/clients/${id}`, payload)
    return data
  },

  async archive(id: number): Promise<Societe> {
    const { data } = await http.patch<Societe>(`/clients/${id}/archive`)
    return data
  },
}