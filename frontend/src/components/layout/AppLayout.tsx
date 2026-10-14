import { useCallback, useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { useNotificationsBadgeStatic } from '@/features/notifications'

/** Structure des pages connectées : sidebar + topbar + contenu de la route. */
export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = useCallback(() => setMenuOpen(false), [])
  const { refresh } = useAuth()
  const { pathname } = useLocation()

  // Recharge la session à chaque navigation : toute modification des rôles
  // et permissions s'applique à toutes les sessions connectées, sans
  // reconnexion (menus, gardes de route et boutons suivent aussitôt).
  useEffect(() => {
    void refresh().catch(() => undefined)
  }, [pathname, refresh])

  // Étape 1 : compteurs statiques (branchés sur les API à l'étape suivante).
  const { nonLues } = useNotificationsBadgeStatic()
  const badges = { notifications: nonLues, demandes: 8 }

  return (
    <div className="app">
      <a className="skip-link" href="#contenu">
        Aller au contenu
      </a>
      <Sidebar open={menuOpen} onClose={closeMenu} badges={badges} />
      {menuOpen && <div className="app__backdrop" onClick={closeMenu} aria-hidden="true" />}
      <div className="app__main">
        <Topbar onMenu={() => setMenuOpen(true)} unreadNotifications={nonLues} />
        <main id="contenu" className="app__content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}