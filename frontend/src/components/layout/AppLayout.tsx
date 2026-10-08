import { useCallback, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { useNotificationsBadgeStatic } from '@/features/notifications'

/** Structure des pages connectées : sidebar + topbar + contenu de la route. */
export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = useCallback(() => setMenuOpen(false), [])

  // Étape 1 : compteurs statiques (demandes branchées à l'étape demandes).
  const { nonLues } = useNotificationsBadgeStatic()
  const badges = { notifications: nonLues }

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