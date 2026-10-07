import { Link } from 'react-router-dom'
import { Icon } from '@/components/ui'

interface TopbarProps {
  /** Ouvre la sidebar (mobile). */
  onMenu: () => void
}

export function Topbar({ onMenu }: TopbarProps) {
  return (
    <header className="topbar">
      <button type="button" className="topbar__menu" onClick={onMenu} aria-label="Ouvrir le menu">
        <Icon name="menu" />
      </button>
      <span className="topbar__brand">Gestion Externalisation</span>

      <div className="topbar__spacer" />

      <Link className="topbar__action" to="/recherche">
        <Icon name="search" />
        <span>Recherche</span>
      </Link>
    </header>
  )
}