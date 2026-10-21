import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { registerUnauthorizedHandler } from '@/api/http'
import type { LoginCredentials, Utilisateur } from '../types'
import { AuthContext, type AuthContextValue } from './authContext'
import { authApi } from '../services/auth.service'
import {
  clearSession,
  getSessionUtilisateur,
  getToken,
  saveSession,
  saveUtilisateur,
} from '@/utils/storage'

function memeUtilisateur(a: Utilisateur | null, b: Utilisateur): boolean {
  if (a === null) return false
  return (
    a.id === b.id &&
    a.nom === b.nom &&
    a.prenom === b.prenom &&
    a.username === b.username &&
    a.email === b.email &&
    a.actif === b.actif &&
    a.doitChangerMotDePasse === b.doitChangerMotDePasse &&
    a.derniereConnexion === b.derniereConnexion &&
    a.createdAt === b.createdAt &&
    a.role.id === b.role.id &&
    a.role.nomRole === b.role.nomRole &&
    a.role.actif === b.role.actif &&
    a.role.description === b.role.description &&
    a.permissions.length === b.permissions.length &&
    a.permissions.every((permission, index) => permission === b.permissions[index])
  )
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [utilisateur, setUtilisateur] = useState<Utilisateur | null>(() =>
    getSessionUtilisateur<Utilisateur>(),
  )
  const [loading, setLoading] = useState(true)
  const refreshEnCours = useRef<Promise<void> | null>(null)

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

  const refresh = useCallback((): Promise<void> => {
    if (!getToken()) return Promise.resolve()
    if (refreshEnCours.current) return refreshEnCours.current

    const requete = authApi
      .me()
      .then((response) => {
        if (!getToken()) return
        saveUtilisateur(response.utilisateur)
        setUtilisateur((precedent) =>
          memeUtilisateur(precedent, response.utilisateur) ? precedent : response.utilisateur,
        )
      })
      .finally(() => {
        refreshEnCours.current = null
      })

    refreshEnCours.current = requete
    return requete
  }, [])

  // Les permissions sont relues périodiquement (et au retour sur l'onglet)
  // pour appliquer immédiatement un changement de rôle à toutes les pages,
  // menus et gardes de route, y compris pour les autres sessions connectées.
  useEffect(() => {
    if (!utilisateur) return

    const rafraichir = () => {
      if (document.visibilityState === 'hidden') return
      void refresh().catch(() => undefined)
    }
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') rafraichir()
    }

    const intervalle = window.setInterval(rafraichir, 10_000)
    window.addEventListener('focus', rafraichir)
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      window.clearInterval(intervalle)
      window.removeEventListener('focus', rafraichir)
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }, [utilisateur, refresh])

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
    () => ({ utilisateur, loading, login, logout, refresh, hasPermission, hasAnyRole }),
    [utilisateur, loading, login, logout, refresh, hasPermission, hasAnyRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}