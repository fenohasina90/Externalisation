import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { Role, Utilisateur } from '@/features/auth/types'
import type {
  ApplicationConfiguration,
  AuditEntry,
  CreateUtilisateurPayload,
  DemandeReinitialisation,
  MotDePasseTemporaire,
  Permission,
  StatutDemandeReinitialisation,
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