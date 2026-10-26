import type { ReactNode } from 'react'
import { useAuth } from '@/features/auth/hooks/useAuth'

interface Props {
  /** Une permission suffit (chaîne unique ou liste = l'une suffit). */
  permission: string | string[]
  children: ReactNode
}

/** N'affiche les enfants que si l'utilisateur connecté a la permission effective. */
export function Can({ permission, children }: Props) {
  const { hasPermission } = useAuth()
  const codes = Array.isArray(permission) ? permission : [permission]
  if (!codes.some((code) => hasPermission(code))) return null
  return <>{children}</>
}
