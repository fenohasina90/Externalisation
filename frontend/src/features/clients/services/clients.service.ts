import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { CreateSocietePayload, Societe, SocieteDetail, UpdateSocietePayload } from '../types'

export interface ClientsFiltres {
  actif?: boolean
  archive?: boolean
  domaineId?: number
}

export interface TriClients {
  champ: 'nomSociete' | 'activite'
  sens: 'asc' | 'desc'
}

export const clientsApi = {
  async list(
    page: PageQuery,
    filtres: ClientsFiltres = {},
    tri: TriClients = { champ: 'nomSociete', sens: 'asc' },
  ): Promise<ApiPage<Societe>> {
    const params = buildQuery(
      { actif: filtres.actif, archive: filtres.archive, domaineId: filtres.domaineId },
      page,
    )
    params.set('sort', `${tri.champ},${tri.sens}`)
    const { data } = await http.get<ApiPage<Societe>>('/clients', { params })
    return data
  },

  async getDetail(id: number): Promise<SocieteDetail> {
    const { data } = await http.get<SocieteDetail>(`/clients/${id}`)
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

  /** Archive (définitif, sans restauration) ou restaure via l'API. */
  async archive(id: number, archive: boolean): Promise<Societe> {
    const { data } = await http.patch<Societe>(`/clients/${id}/archive`, { archive })
    return data
  },

  /** Télécharge une pièce jointe (blob). */
  async telechargerPiece(id: number): Promise<Blob> {
    const { data } = await http.get<Blob>(`/documents/${id}/download`, {
      responseType: 'blob',
    })
    return data
  },
}
