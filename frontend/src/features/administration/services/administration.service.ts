import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { Role, Utilisateur } from '@/features/auth/types'
import type {
  ApplicationConfiguration,
  AuditEntry,
  CreateRolePayload,
  CreateUtilisateurPayload,
  DemandeReinitialisation,
  MotDePasseTemporaire,
  Permission,
  RoleDetail,
  StatutDemandeReinitialisation,
  UpdateRolePayload,
} from '../types'

export interface UtilisateursFiltres {
  roleId?: number
  actif?: boolean
}

export const utilisateursApi = {
  async list(
    page: PageQuery,
    filters: UtilisateursFiltres = {},
    search?: string | null,
  ): Promise<ApiPage<Utilisateur>> {
    const { data } = await http.get<ApiPage<Utilisateur>>('/utilisateurs', {
      params: buildQuery(
        { search: search ?? undefined, roleId: filters.roleId, actif: filters.actif },
        page,
      ),
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

  async toggleActivation(id: number, actif: boolean): Promise<Utilisateur> {
    const { data } = await http.patch<Utilisateur>(`/utilisateurs/${id}/activation`, { actif })
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

  async get(id: number): Promise<RoleDetail> {
    const { data } = await http.get<RoleDetail>(`/roles/${id}`)
    return data
  },

  async create(payload: CreateRolePayload): Promise<RoleDetail> {
    const { data } = await http.post<RoleDetail>('/roles', payload)
    return data
  },

  async update(id: number, payload: UpdateRolePayload): Promise<RoleDetail> {
    const { data } = await http.put<RoleDetail>(`/roles/${id}`, payload)
    return data
  },
}

export const permissionsApi = {
  async list(): Promise<Permission[]> {
    const { data } = await http.get<Permission[]>('/permissions')
    return data
  },
}

export interface AuditFiltres {
  utilisateurId?: number | null
  action?: string | null
  module?: string | null
  entite?: string | null
  dateDebut?: string | null
  dateFin?: string | null
}

export const auditApi = {
  async list(page: PageQuery, filters: AuditFiltres = {}): Promise<ApiPage<AuditEntry>> {
    const { data } = await http.get<ApiPage<AuditEntry>>('/audit', {
      params: buildQuery(
        {
          utilisateurId: filters.utilisateurId,
          action: filters.action,
          module: filters.module,
          entite: filters.entite,
          dateDebut: filters.dateDebut,
          dateFin: filters.dateFin,
        },
        page,
      ),
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

export const reinitialisationsApi = {
  async list(
    statut: StatutDemandeReinitialisation | null,
    page?: PageQuery,
  ): Promise<ApiPage<DemandeReinitialisation>> {
    const { data } = await http.get<ApiPage<DemandeReinitialisation>>('/password-resets', {
      params: buildQuery({ statut }, page),
    })
    return data
  },

  async get(id: number): Promise<DemandeReinitialisation> {
    const { data } = await http.get<DemandeReinitialisation>(`/password-resets/${id}`)
    return data
  },

  async approuver(id: number): Promise<DemandeReinitialisation> {
    const { data } = await http.post<DemandeReinitialisation>(
      `/password-resets/${id}/approuver`,
    )
    return data
  },

  async refuser(id: number, motif: string): Promise<DemandeReinitialisation> {
    const { data } = await http.post<DemandeReinitialisation>(
      `/password-resets/${id}/refuser`,
      { motif },
    )
    return data
  },

  async reinitialiser(id: number): Promise<MotDePasseTemporaire> {
    const { data } = await http.post<MotDePasseTemporaire>(
      `/password-resets/${id}/reinitialiser`,
    )
    return data
  },
}