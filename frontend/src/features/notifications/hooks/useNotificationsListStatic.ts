import { useMemo, useState } from 'react'
import type { NotificationUser } from '../types'
import type { OngletNotifications, TypeNotification } from '../data/mockNotifications'

interface Options {
  source: NotificationUser[]
  size?: number
}

/** Onglets Toutes / Non lues + filtre par type + pagination (statique). */
export function useNotificationsListStatic({ source, size = 8 }: Options) {
  const [page, setPage] = useState(0)
  const [onglet, setOnglet] = useState<OngletNotifications>('non-lues')
  const [type, setType] = useState<TypeNotification | 'tous'>('tous')

  const filtered = useMemo(
    () =>
      source.filter((n) => {
        if (onglet === 'non-lues' && n.lu) return false
        if (type !== 'tous' && n.typeNotification !== type) return false
        return true
      }),
    [source, onglet, type],
  )

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)

  return {
    items: filtered.slice(safePage * size, safePage * size + size),
    total,
    totalPages,
    page: safePage,
    size,
    setPage,
    onglet,
    setOnglet: (v: OngletNotifications) => {
      setOnglet(v)
      setPage(0)
    },
    type,
    setType: (v: TypeNotification | 'tous') => {
      setType(v)
      setPage(0)
    },
  }
}
