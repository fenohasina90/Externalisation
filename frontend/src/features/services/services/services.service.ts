import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type {
  CreateServicePayload,
  Domaine,
  Service,
  TypeService,
  UpdateServicePayload,
} from '../types'

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

  async create(payload: { nomDomaine: string; codeDomaine: string }): Promise<Domaine> {
    const { data } = await http.post<Domaine>('/domaines', payload)
    return data
  },

  async update(id: number, payload: { nomDomaine: string; codeDomaine: string }): Promise<Domaine> {
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
  async list(
    page: PageQuery,
    filters: { actif?: boolean; typeService?: TypeService; domaineId?: number } = {},
  ): Promise<ApiPage<Service>> {
    const { data } = await http.get<ApiPage<Service>>('/services', {
      params: buildQuery({ actif: filters.actif, typeService: filters.typeService, domaineId: filters.domaineId }, page),
    })
    return data
  },

  async get(id: number): Promise<Service> {
    const { data } = await http.get<Service>(`/services/${id}`)
    return data
  },

  /** Lecture par code (ex. SRV-0001). */
  async getByCode(code: string): Promise<Service> {
    const { data } = await http.get<Service>(`/services/code/${encodeURIComponent(code.trim())}`)
    return data
  },

  async create(payload: CreateServicePayload): Promise<Service> {
    const { data } = await http.post<Service>('/services', payload)
    return data
  },

  async update(id: number, payload: UpdateServicePayload): Promise<Service> {
    const { data } = await http.put<Service>(`/services/${id}`, payload)
    return data
  },

  /** Archive (ou désarchive) un service. */
  async setArchive(id: number, archive: boolean): Promise<Service> {
    const { data } = await http.patch<Service>(`/services/${id}/archive`, { archive })
    return data
  },
}