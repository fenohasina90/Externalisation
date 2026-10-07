import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { Domaine, HistoriqueTarif, Service } from '../types'

export const domainesApi = {
  async list(page?: PageQuery): Promise<ApiPage<Domaine>> {
    const { data } = await http.get<ApiPage<Domaine>>('/domaines', {
      params: buildQuery({}, page),
    })
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