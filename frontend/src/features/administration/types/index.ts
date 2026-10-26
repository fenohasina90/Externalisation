export interface AuditEntry {
  id: number
  utilisateurId: number | null
  nomUtilisateur: string | null
  action: string
  module: string
  entite: string | null
  entiteId: number | null
  ancienneValeur: string | null
  nouvelleValeur: string | null
  adresseIp: string | null
  userAgent: string | null
  createdAt: string
}

export interface ApplicationConfiguration {
  id: number
  cle: string
  valeur: string
  typeValeur: string | null
  description: string | null
  modifiable: boolean
}

export interface StatutDemandeConfig {
  id: number
  statut: string
  description: string | null
  ordre: number
  actif: boolean
}

export interface CreateStatutPayload {
  statut: string
  description?: string | null
  ordre?: number | null
}

export interface ModePaiementAdmin {
  id: number
  code: string
  libelle: string
  actif: boolean
}

export interface CreateModePaiementPayload {
  code: string
  libelle: string
}

export interface Permission {
  id: number
  codePermission: string
  nomPermission: string
  description: string | null
  module: string
  actif: boolean
}

export interface RoleDetail {
  id: number
  nomRole: string
  description: string | null
  actif: boolean
  permissions: Permission[]
}

export interface CreateRolePayload {
  nomRole: string
  description?: string | null
  permissionIds: number[]
}

export interface UpdateRolePayload {
  description?: string | null
  actif?: boolean
  permissionIds: number[]
}

export interface CreateUtilisateurPayload {
  nom: string
  prenom?: string | null
  username: string
  email: string
  password?: string | null
  roleId: number
  actif?: boolean
}

export type UpdateUtilisateurPayload = Partial<CreateUtilisateurPayload>

export type StatutDemandeReinitialisation =
  | 'EN_ATTENTE'
  | 'APPROUVEE'
  | 'REFUSEE'
  | 'EXPIREE'
  | 'TERMINEE'

export interface DemandeReinitialisation {
  id: number
  utilisateurId: number
  nomComplet: string
  username: string
  email: string
  statut: StatutDemandeReinitialisation
  motifRefus: string | null
  dateDemande: string
  dateTraitement: string | null
  traiteePar: string | null
  dateGenerationMdp: string | null
  dateExpirationMdp: string | null
  genereePar: string | null
}

export interface MotDePasseTemporaire {
  motDePasseTemporaire: string
  expiration: string
  message: string
  demande: DemandeReinitialisation
}