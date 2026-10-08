import { useMemo } from 'react'
import { MOCK_NOTIFICATIONS } from '../data/mockNotifications'

/**
 * Compteur statique de notifications non lues pour les badges
 * (sidebar + topbar). Étape 1 — sans backend.
 * Étape 2 : remplacer la source par `notificationsApi.listNonLues()`.
 */
export function useNotificationsBadgeStatic() {
  const nonLues = useMemo(() => MOCK_NOTIFICATIONS.filter((n) => !n.lu).length, [])
  return { nonLues }
}
