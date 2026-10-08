import { Link } from 'react-router-dom'
import { Icon } from '@/components/ui'
import { Breadcrumbs } from './Breadcrumbs'

const dateFormat = new Intl.DateTimeFormat('fr-FR', {
  weekday: 'short',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

interface TopbarProps {
  /** Ouvre le menu latéral (visible sur mobile uniquement). */
  onMenu: () => void
  /** Nombre de notifications non lues : affiche un point sur la cloche. */
  unreadNotifications?: number
}

export function Topbar({ onMenu, unreadNotifications = 0 }: TopbarProps) {
  const today = new Date()
  const hasUnread = unreadNotifications > 0

  return (
    <header className="topbar">
      <button type="button" className="topbar__menu" onClick={onMenu} aria-label="Ouvrir le menu">
        <Icon name="menu" />
      </button>

      <Breadcrumbs />

      <div className="topbar__spacer" />

      <Link
        to="/notifications"
        className="topbar__icon"
        aria-label={hasUnread ? `Notifications (${unreadNotifications} non lues)` : 'Notifications'}
      >
        <Icon name="bell" />
        {hasUnread && <span className="topbar__dot" aria-hidden="true" />}
      </Link>

      <time className="topbar__date" dateTime={today.toISOString().slice(0, 10)}>
        {dateFormat.format(today)}
      </time>
    </header>
  )
}