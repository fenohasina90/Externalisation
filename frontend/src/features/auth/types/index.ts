import type { NomRole } from '@/types'

export interface Role {
  id: number
  nomRole: NomRole
  description: string | null
  actif: boolean
}

export interface Utilisateur {
  id: number
  nom: string
  prenom: string
  username: string
  email: string
  actif: boolean
  derniereConnexion: string | null
  createdAt: string
  role: Role
  permissions: string[]
}

export interface AuthResponse {
  token: string
  type: string
  utilisateur: Utilisateur
}

export interface LoginCredentials {
  username: string
  password: string
}

export interface ForgotPasswordPayload {
  email: string
}

export interface ResetPasswordPayload {
  token: string
  newPassword: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export interface MessageResponse {
  message: string
}