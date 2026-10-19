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
  /** Replie / déplie la sidebar (visible sur grand écran uniquement). */
  onReplier?: () => void
  /** Sidebar actuellement repliée. */
  repliee?: boolean
  /** Nombre de notifications non lues : affiche une pastille sur la cloche. */
  unreadNotifications?: number
}

export function Topbar({ onMenu, onReplier, repliee = false, unreadNotifications = 0 }: TopbarProps) {
  const today = new Date()
  const hasUnread = unreadNotifications > 0

  return (
    <header className="topbar">
      <button type="button" className="topbar__menu" onClick={onMenu} aria-label="Ouvrir le menu">
        <Icon name="menu" />
      </button>
      {onReplier && (
        <button
          type="button"
          className="topbar__replie"
          onClick={onReplier}
          aria-label={repliee ? 'Afficher le menu latéral' : 'Cacher le menu latéral'}
          title={repliee ? 'Afficher le menu latéral' : 'Cacher le menu latéral'}
        >
          <Icon name="panel" />
        </button>
      )}

      <Breadcrumbs />

      <div className="topbar__spacer" />

      <Link
        to="/notifications"
        className="topbar__icon"
        aria-label={hasUnread ? `Notifications (${unreadNotifications} non lues)` : 'Notifications'}
      >
        <Icon name="bell" />
        {hasUnread && (
          <span className="topbar__badge" aria-hidden="true">
            {unreadNotifications > 99 ? '99+' : unreadNotifications}
          </span>
        )}
      </Link>

      <time className="topbar__date" dateTime={today.toISOString().slice(0, 10)}>
        {dateFormat.format(today)}
      </time>
    </header>
  )
}