import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/hooks/useAuth'

interface Props {
  /** Une permission suffit (chaîne unique ou liste = l'une suffit). */
  permission: string | string[]
  children: ReactNode
}

/**
 * Garde de route par permission effective : sans le droit requis,
 * redirection vers la page 403. Toute modification des permissions
 * d'un rôle s'applique donc à la navigation sans reconnexion.
 */
export function RequirePermission({ permission, children }: Props) {
  const { utilisateur, hasPermission } = useAuth()
  const location = useLocation()

  if (!utilisateur) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  const codes = Array.isArray(permission) ? permission : [permission]
  if (!codes.some((code) => hasPermission(code))) {
    return <Navigate to="/403" replace />
  }

  return <>{children}</>
}
