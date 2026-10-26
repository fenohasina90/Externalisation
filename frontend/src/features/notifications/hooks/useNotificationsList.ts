import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { signalerChangementNotifications } from '../constants'
import { notificationsApi } from '../services/notifications.service'
import type {
  NotificationUser,
  NotificationsNonLues,
  OngletNotifications,
  TypeNotification,
} from '../types'

/** Liste API des notifications : onglets, filtre par type et pagination serveur. */
export function useNotificationsList(size = 8) {
  const [page, setPage] = useState(0)
  const [onglet, setOngletState] = useState<OngletNotifications>('non-lues')
  const [type, setTypeState] = useState<TypeNotification | 'tous'>('tous')

  const { data, loading, error, reload } = useApi<ApiPage<NotificationUser>>(
    () =>
      notificationsApi.list(
        { page, size, recherche: null },
        {
          type: type === 'tous' ? undefined : type,
          nonLues: onglet === 'non-lues' ? true : undefined,
        },
      ),
    [page, size, onglet, type],
  )

  const {
    data: nonLues,
    loading: chargementNonLues,
    reload: reloadNonLues,
  } = useApi<NotificationsNonLues>(() => notificationsApi.listNonLues(), [])

  function setOnglet(valeur: OngletNotifications): void {
    setOngletState(valeur)
    setPage(0)
  }

  function setType(valeur: TypeNotification | 'tous'): void {
    setTypeState(valeur)
    setPage(0)
  }

  function rafraichir(): void {
    setPage(0)
    reload()
    reloadNonLues()
    signalerChangementNotifications()
  }

  return {
    items: data?.content ?? [],
    total: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page,
    size,
    loading,
    error,
    onglet,
    type,
    setPage,
    setOnglet,
    setType,
    nonLues: nonLues?.totalNonLues ?? 0,
    parType: nonLues?.parType ?? {},
    chargementNonLues,
    rafraichir,
  }
}
