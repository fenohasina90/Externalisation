import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { Role, Utilisateur } from '@/features/auth/types'
import type {
  ApplicationConfiguration,
  AuditEntry,
  CreateUtilisateurPayload,
  Permission,
} from '../types'

export const utilisateursApi = {
  async list(page?: PageQuery): Promise<ApiPage<Utilisateur>> {
    const { data } = await http.get<ApiPage<Utilisateur>>('/utilisateurs', {
      params: buildQuery({}, page),
    })
    return data
  },

  async get(id: number): Promise<Utilisateur> {
    const { data } = await http.get<Utilisateur>(`/utilisateurs/${id}`)
    return data
  },

  async create(payload: CreateUtilisateurPayload): Promise<Utilisateur> {
    const { data } = await http.post<Utilisateur>('/utilisateurs', payload)
    return data
  },

  async update(id: number, payload: CreateUtilisateurPayload): Promise<Utilisateur> {
    const { data } = await http.put<Utilisateur>(`/utilisateurs/${id}`, payload)
    return data
  },

  async toggleActivation(id: number): Promise<Utilisateur> {
    const { data } = await http.patch<Utilisateur>(`/utilisateurs/${id}/activation`)
    return data
  },

  async remove(id: number): Promise<void> {
    await http.delete(`/utilisateurs/${id}`)
  },
}

export const rolesApi = {
  async list(): Promise<Role[]> {
    const { data } = await http.get<Role[]>('/roles')
    return data
  },
}

export const permissionsApi = {
  async list(): Promise<Permission[]> {
    const { data } = await http.get<Permission[]>('/permissions')
    return data
  },
}

export const auditApi = {
  async list(page?: PageQuery): Promise<ApiPage<AuditEntry>> {
    const { data } = await http.get<ApiPage<AuditEntry>>('/audit', {
      params: buildQuery({}, page),
    })
    return data
  },

  async get(id: number): Promise<AuditEntry> {
    const { data } = await http.get<AuditEntry>(`/audit/${id}`)
    return data
  },
}

export const configurationApi = {
  async list(): Promise<ApplicationConfiguration[]> {
    const { data } = await http.get<ApplicationConfiguration[]>('/configuration')
    return data
  },

  async update(cle: string, valeur: string): Promise<ApplicationConfiguration> {
    const { data } = await http.put<ApplicationConfiguration>(`/configuration/${cle}`, {
      valeur,
    })
    return data
  },
}