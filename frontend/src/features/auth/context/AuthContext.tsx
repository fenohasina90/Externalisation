import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { registerUnauthorizedHandler } from '@/api/http'
import type { LoginCredentials, Utilisateur } from '../types'
import { AuthContext, type AuthContextValue } from './authContext'
import { authApi } from '../services/auth.service'
import { clearSession, getSessionUtilisateur, getToken, saveSession } from '@/utils/storage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [utilisateur, setUtilisateur] = useState<Utilisateur | null>(() =>
    getSessionUtilisateur<Utilisateur>(),
  )
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let stale = false

    async function restoreSession(): Promise<void> {
      if (!getToken()) {
        setLoading(false)
        return
      }
      try {
        const response = await authApi.me()
        if (stale) return
        setUtilisateur(response.utilisateur)
      } catch {
        if (!stale) {
          clearSession()
          setUtilisateur(null)
        }
      } finally {
        if (!stale) setLoading(false)
      }
    }

    void restoreSession()
    return () => {
      stale = true
    }
  }, [])

  useEffect(() => {
    const effacerSession = () => {
      clearSession()
      setUtilisateur(null)
    }
    const unsubscribe = registerUnauthorizedHandler(effacerSession)
    return unsubscribe
  }, [])

  const login = useCallback(async (credentials: LoginCredentials): Promise<Utilisateur> => {
    const response = await authApi.login(credentials)
    saveSession(response.token, response.utilisateur)
    setUtilisateur(response.utilisateur)
    return response.utilisateur
  }, [])

  const logout = useCallback(async (): Promise<void> => {
    await authApi.logout()
    clearSession()
    setUtilisateur(null)
  }, [])

  const hasPermission = useCallback(
    (permission: string): boolean => utilisateur?.permissions.includes(permission) ?? false,
    [utilisateur],
  )

  const hasAnyRole = useCallback(
    (...roles: string[]): boolean =>
      utilisateur !== null && roles.includes(utilisateur.role.nomRole),
    [utilisateur],
  )

  const value = useMemo<AuthContextValue>(
    () => ({ utilisateur, loading, login, logout, hasPermission, hasAnyRole }),
    [utilisateur, loading, login, logout, hasPermission, hasAnyRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}