import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { Domaine, HistoriqueTarif, Service } from '../types'

export const domainesApi = {
  async list(
    page: PageQuery,
    filters: { actif?: boolean } = {},
  ): Promise<ApiPage<Domaine>> {
    const { data } = await http.get<ApiPage<Domaine>>('/domaines', {
      params: buildQuery({ actif: filters.actif }, page),
    })
    return data
  },

  async get(id: number): Promise<Domaine> {
    const { data } = await http.get<Domaine>(`/domaines/${id}`)
    return data
  },

  async create(payload: { nomDomaine: string }): Promise<Domaine> {
    const { data } = await http.post<Domaine>('/domaines', payload)
    return data
  },

  async update(id: number, payload: { nomDomaine: string }): Promise<Domaine> {
    const { data } = await http.put<Domaine>(`/domaines/${id}`, payload)
    return data
  },

  /** Active ou désactive un domaine (il reste dans la liste avec le statut Inactif). */
  async setActif(id: number, actif: boolean): Promise<Domaine> {
    const { data } = await http.patch<Domaine>(`/domaines/${id}/activation`, { actif })
    return data
  },
}

export const servicesApi = {
  async list(page?: PageQuery): Promise<ApiPage<Service>> {
    const { data } = await http.get<ApiPage<Service>>('/services', {
      params: buildQuery({}, page),
    })
    return data
  },

  async historiqueTarifs(serviceId: number): Promise<HistoriqueTarif[]> {
    const { data } = await http.get<HistoriqueTarif[]>(`/services/${serviceId}/tarifs`)
    return data
  },
}