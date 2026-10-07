import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type {
  ChangeStatutPayload,
  CreateDemandePayload,
  Demande,
  HistoriqueDemande,
  Statut,
} from '../types'

export interface DemandeFiltres {
  statut?: string
  societeId?: number
  serviceId?: number
  commercialId?: number
  dateDebut?: string
  dateFin?: string
}

export const demandesApi = {
  async list(page?: PageQuery, filtres?: DemandeFiltres): Promise<ApiPage<Demande>> {
    const { data } = await http.get<ApiPage<Demande>>('/demandes', {
      params: buildQuery({ ...filtres }, page),
    })
    return data
  },

  async get(id: number): Promise<Demande> {
    const { data } = await http.get<Demande>(`/demandes/${id}`)
    return data
  },

  async create(payload: CreateDemandePayload): Promise<Demande> {
    const { data } = await http.post<Demande>('/demandes', payload)
    return data
  },

  async update(id: number, payload: CreateDemandePayload): Promise<Demande> {
    const { data } = await http.put<Demande>(`/demandes/${id}`, payload)
    return data
  },

  async changeStatut(id: number, payload: ChangeStatutPayload): Promise<Demande> {
    const { data } = await http.patch<Demande>(`/demandes/${id}/statut`, payload)
    return data
  },

  async archive(id: number): Promise<Demande> {
    const { data } = await http.patch<Demande>(`/demandes/${id}/archive`)
    return data
  },

  async historique(id: number): Promise<HistoriqueDemande[]> {
    const { data } = await http.get<HistoriqueDemande[]>(`/demandes/${id}/historique`)
    return data
  },

  async listStatuts(): Promise<Statut[]> {
    const { data } = await http.get<Statut[]>('/demandes/statuts')
    return data
  },
}