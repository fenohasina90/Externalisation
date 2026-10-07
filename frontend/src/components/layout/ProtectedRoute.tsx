import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { Spinner } from '@/components/ui'

export function ProtectedRoute() {
  const { utilisateur, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <Spinner label="Chargement de la session…" />
  }

  if (!utilisateur) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}