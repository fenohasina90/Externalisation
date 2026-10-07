import { createContext } from 'react'
import type { LoginCredentials, Utilisateur } from '../types'

export interface AuthContextValue {
  utilisateur: Utilisateur | null
  loading: boolean
  login: (credentials: LoginCredentials) => Promise<Utilisateur>
  logout: () => Promise<void>
  hasPermission: (permission: string) => boolean
  hasAnyRole: (...roles: string[]) => boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)