import type { JSX } from 'react'
import { TYPE_META, type TypeNotification } from '../data/mockNotifications'
import type { NotificationUser } from '../types'

function Icone({ type }: { type: string }) {
  const tone = (TYPE_META[type as TypeNotification]?.tone ?? 'gray') as string
  const paths: Record<string, JSX.Element> = {
    red: (
      <svg className="sv" viewBox="0 0 24 24">
        <path d="M12 3l10 18H2L12 3zM12 10v5M12 18h.01" />
      </svg>
    ),
    orange: (
      <svg className="sv" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 3" />
      </svg>
    ),
    blue: (
      <svg className="sv" viewBox="0 0 24 24">
        <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
      </svg>
    ),
    green: (
      <svg className="sv" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="9" />
        <path d="M8 12l3 3 5-6" />
      </svg>
    ),
    gray: (
      <svg className="sv" viewBox="0 0 24 24">
        <path d="M6 9a6 6 0 0 1 12 0c0 6 2 7 2 7H4s2-1 2-7M10 20a2 2 0 0 0 4 0" />
      </svg>
    ),
  }
  return <span className={`notif-ic notif-ic--${tone}`}>{paths[tone]}</span>
}

interface Props {
  items: NotificationUser[]
  onConsulter: (id: number) => void
  onMarquerLue: (id: number) => void
}

/** Liste des notifications (non lues surlignées, cf. maquette). */
export function NotificationsList({ items, onConsulter, onMarquerLue }: Props) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucune notification.</div>

  return (
    <ul className="notif-list">
      {items.map((n) => (
        <li key={n.id} className={`notif-row${n.lu ? '' : ' notif-row--unread'}`}>
          <span className="notif-dot" aria-hidden="true">
            {!n.lu && <i />}
          </span>
          <Icone type={n.typeNotification} />
          <div className="notif-main">
            <p className="notif-title">{n.titre}</p>
            <p className="notif-text">{n.message}</p>
          </div>
          <span className="notif-time">{n.dateCreation}</span>
          <div className="notif-actions">
            <button type="button" className="btn-link" onClick={() => onConsulter(n.id)}>
              Consulter
            </button>
            {!n.lu && (
              <button type="button" className="btn-link" onClick={() => onMarquerLue(n.id)}>
                Marquer comme lue
              </button>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
