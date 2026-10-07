import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { NAV_ITEMS } from '@/constants/navigation'
import { LIBELLES_ROLE } from '@/constants/labels'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { Button } from '@/components/ui'
import { initiales } from '@/utils/format'

export function AppLayout() {
  const { utilisateur, logout, hasAnyRole } = useAuth()
  const navigate = useNavigate()

  if (!utilisateur) return null

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.roles || hasAnyRole(...item.roles),
  )

  async function handleLogout(): Promise<void> {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="sidebar-logo">C</span>
          <div>
            <strong>CABEX</strong>
            <span>Externalisation</span>
          </div>
        </div>
        <nav className="sidebar-nav" aria-label="Navigation principale">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <div className="topbar-user">
            <span className="avatar">{initiales(utilisateur.nom, utilisateur.prenom)}</span>
            <div>
              <strong>
                {utilisateur.nom} {utilisateur.prenom}
              </strong>
              <span>{LIBELLES_ROLE[utilisateur.role.nomRole] ?? utilisateur.role.nomRole}</span>
            </div>
          </div>
          <Button variant="ghost" onClick={() => void handleLogout()}>
            Déconnexion
          </Button>
        </header>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}