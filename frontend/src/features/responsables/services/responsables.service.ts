import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type {
  CreateResponsablePayload,
  Responsable,
  ResponsablesFilters,
  SocieteOption,
  UpdateResponsablePayload,
} from '../types'

interface SocieteOptionResponse {
  id: number
  nomSociete: string
}

export const responsablesApi = {
  async list(
    page: PageQuery,
    filters: ResponsablesFilters = {},
  ): Promise<ApiPage<Responsable>> {
    const { data } = await http.get<ApiPage<Responsable>>('/responsables', {
      params: buildQuery({ actif: filters.actif }, page),
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

  async update(id: number, payload: UpdateResponsablePayload): Promise<Responsable> {
    const { data } = await http.put<Responsable>(`/responsables/${id}`, payload)
    return data
  },

  /** Active ou désactive un responsable (il reste dans la liste avec le statut Inactif). */
  async setActif(id: number, actif: boolean): Promise<Responsable> {
    const { data } = await http.patch<Responsable>(`/responsables/${id}/activation`, { actif })
    return data
  },

  /** Sociétés clientes proposées dans le formulaire (dérivées de la liste des clients). */
  async societes(): Promise<SocieteOption[]> {
    const { data } = await http.get<ApiPage<SocieteOptionResponse>>('/clients', {
      params: buildQuery({}, { page: 0, size: 500, recherche: null }),
    })
    return data.content.map((societe) => ({ id: societe.id, nom: societe.nomSociete }))
  },
}