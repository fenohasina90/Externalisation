import { useCallback, useEffect, useState } from 'react'
import { EVENEMENT_NOTIFICATIONS } from '../constants'
import { notificationsApi } from '../services/notifications.service'

/** Compteur de notifications non lues pour la sidebar et la cloche du topbar. */
export function useNotificationsBadge() {
  const [nonLues, setNonLues] = useState(0)

  const rafraichir = useCallback(async () => {
    try {
      const donnees = await notificationsApi.listNonLues()
      setNonLues(donnees.totalNonLues)
    } catch {
      // Badge silencieux : une erreur réseau ne doit pas perturber la navigation.
    }
  }, [])

  useEffect(() => {
    let actif = true

    async function charger(): Promise<void> {
      try {
        const donnees = await notificationsApi.listNonLues()
        if (actif) setNonLues(donnees.totalNonLues)
      } catch {
        // Badge silencieux : une erreur réseau ne doit pas perturber la navigation.
      }
    }

    void charger()
    const intervalle = window.setInterval(() => void charger(), 60_000)
    const ecoute = () => void charger()
    window.addEventListener(EVENEMENT_NOTIFICATIONS, ecoute)
    return () => {
      actif = false
      window.clearInterval(intervalle)
      window.removeEventListener(EVENEMENT_NOTIFICATIONS, ecoute)
    }
  }, [])

  return { nonLues, rafraichir }
}
