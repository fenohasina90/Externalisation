import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { Badge, Icon } from '@/components/ui'
import { LIBELLES_ROLE } from '@/constants/labels'
import { NAVIGATION, type BadgeKey, type NavItem } from '@/constants/navigation'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { initiales } from '@/utils/format'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'

interface SidebarProps {
  /** Ouverte (mobile uniquement : sur grand écran la sidebar est toujours visible). */
  open: boolean
  onClose: () => void
  /** Compteurs affichés dans les pastilles (ex. { demandes: 8, notifications: 3 }). */
  badges?: Partial<Record<BadgeKey, number>>
}

const isUnder = (pathname: string, to: string) => pathname === to || pathname.startsWith(`${to}/`)

function isGroupActive(item: NavItem, pathname: string): boolean {
  const paths = [item.to, ...(item.children?.map((child) => child.to) ?? [])]
  return paths.some((path) => isUnder(pathname, path))
}

interface NavEntryProps {
  item: NavItem
  pathname: string
  badges: Partial<Record<BadgeKey, number>>
  onNavigate: () => void
}

function NavEntry({ item, pathname, badges, onNavigate }: NavEntryProps) {
  const count = item.badge ? badges[item.badge] : undefined
  const content = (
    <>
      <Icon name={item.icon} />
      <span className="nav__label">{item.label}</span>
      {item.hint && <small className="nav__hint">{item.hint}</small>}
      {count ? <Badge>{count}</Badge> : null}
    </>
  )

  if (!item.children) {
    return (
      <NavLink
        to={item.to}
        end={item.end}
        className={({ isActive }) => `nav${isActive ? ' nav--active' : ''}`}
        onClick={onNavigate}
      >
        {content}
      </NavLink>
    )
  }

  const active = isGroupActive(item, pathname)
  return (
    <div className="nav-group">
      <Link
        to={item.to}
        className={`nav${active ? ' nav--active' : ''}`}
        aria-current={active ? 'true' : undefined}
        onClick={onNavigate}
      >
        {content}
      </Link>
      {active && (
        <ul className="nav-sub">
          {item.children.map((child) => (
            <li key={child.to}>
              <NavLink
                to={child.to}
                end={child.end}
                className={({ isActive }) =>
                  `nav-sub__link${isActive ? ' nav-sub__link--active' : ''}`
                }
                onClick={onNavigate}
              >
                {child.label}
              </NavLink>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function Sidebar({ open, onClose, badges = {} }: SidebarProps) {
  const { utilisateur, logout } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [confirmLogout, setConfirmLogout] = useState(false)

  useEffect(() => {
    if (!open) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, onClose])

  if (!utilisateur) return null

  const role = utilisateur.role.nomRole

  async function handleLogout(): Promise<void> {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <aside className={`sidebar${open ? ' sidebar--open' : ''}`}>
      <Link to="/" className="sidebar__brand" onClick={onClose}>
        <span className="sidebar__logo" aria-hidden="true">
          C
        </span>
        <span>
          <strong>Cabinet Excellent</strong>
          <small>ESPACE DE GESTION</small>
        </span>
      </Link>

      <nav className="sidebar__nav" aria-label="Menu principal">
        {NAVIGATION[role].map((section) => (
          <div key={section.label} className="sidebar__section">
            <p className="sidebar__section-label">{section.label}</p>
            {section.items.map((item) => (
              <NavEntry
                key={item.to + item.label}
                item={item}
                pathname={pathname}
                badges={badges}
                onNavigate={onClose}
              />
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar__user">
        <div className="sidebar__user-link">
          <span className="sidebar__avatar" aria-hidden="true">
            {initiales(utilisateur.nom, utilisateur.prenom)}
          </span>
          <span>
            <strong>
              {utilisateur.prenom} {utilisateur.nom}
            </strong>
            <small>{LIBELLES_ROLE[role] ?? role}</small>
          </span>
        </div>
        <button
          type="button"
          className="sidebar__logout"
          onClick={() => setConfirmLogout(true)}
          aria-label="Se déconnecter"
          title="Se déconnecter"
        >
          <Icon name="logout" />
        </button>
      </div>

      <ConfirmDialog
        open={confirmLogout}
        titre="Se déconnecter ?"
        message="Vous devrez vous reconnecter pour accéder à l’application."
        confirmer="Se déconnecter"
        ton="primary"
        onAnnuler={() => setConfirmLogout(false)}
        onConfirmer={() => {
          setConfirmLogout(false)
          void handleLogout()
        }}
      />
    </aside>
  )
}