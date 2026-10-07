import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { Spinner } from '@/components/ui'

export const CHANGE_PASSWORD_PATH = '/changer-mot-de-passe'

export function ProtectedRoute() {
  const { utilisateur, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <Spinner label="Chargement de la session…" />
  }

  if (!utilisateur) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (utilisateur.doitChangerMotDePasse && location.pathname !== CHANGE_PASSWORD_PATH) {
    return <Navigate to={CHANGE_PASSWORD_PATH} replace />
  }

  return <Outlet />
}