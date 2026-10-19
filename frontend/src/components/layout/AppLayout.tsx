import { useCallback, useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { useNotificationsBadge } from '@/features/notifications'

/** Structure des pages connectées : sidebar + topbar + contenu de la route. */
export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = useCallback(() => setMenuOpen(false), [])
  const { refresh } = useAuth()
  const { pathname } = useLocation()

  // Sidebar repliable (grand écran), préférence conservée entre les visites.
  const [repliee, setRepliee] = useState(
    () => localStorage.getItem('sidebar-repliee') === '1',
  )
  const basculerRepliee = useCallback(() => {
    setRepliee((v) => {
      localStorage.setItem('sidebar-repliee', v ? '0' : '1')
      return !v
    })
  }, [])
  const deplier = useCallback(() => {
    localStorage.setItem('sidebar-repliee', '0')
    setRepliee(false)
  }, [])

  // Recharge la session à chaque navigation : toute modification des rôles
  // et permissions s'applique à toutes les sessions connectées, sans
  // reconnexion (menus, gardes de route et boutons suivent aussitôt).
  useEffect(() => {
    void refresh().catch(() => undefined)
  }, [pathname, refresh])

  // Compteur de notifications non lues, rafraîchi périodiquement et après lecture.
  const { nonLues } = useNotificationsBadge()
  const badges = { notifications: nonLues, demandes: 8 }

  return (
    <div className={`app${repliee ? ' app--sidebar-repliee' : ''}`}>
      <a className="skip-link" href="#contenu">
        Aller au contenu
      </a>
      <Sidebar open={menuOpen} onClose={closeMenu} badges={badges} repliee={repliee} onDeplier={deplier} />
      {menuOpen && <div className="app__backdrop" onClick={closeMenu} aria-hidden="true" />}
      <div className="app__main">
        <Topbar
          onMenu={() => setMenuOpen(true)}
          onReplier={basculerRepliee}
          repliee={repliee}
          unreadNotifications={nonLues}
        />
        <main id="contenu" className="app__content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}