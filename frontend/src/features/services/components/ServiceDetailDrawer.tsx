import type { Service } from '../types'

interface Props {
  service: Service | null
  onClose: () => void
  onEdit: (s: Service) => void
  onArchive: (id: number) => void
}

/** Panneau latéral de détail d'un service (cf. maquette). */
export function ServiceDetailDrawer({ service, onClose, onEdit, onArchive }: Props) {
  if (!service) return null

  return (
    <>
      <div className="drawer-backdrop" onClick={onClose} aria-hidden="true" />
      <aside className="drawer" role="dialog" aria-label={`Détail : ${service.nomService}`}>
        <div className="drawer__head">
          <span className="drawer__eyebrow">Service</span>
          <button type="button" className="btn-ghost drawer__close" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </div>

        <h2 className="drawer__title">{service.nomService}</h2>
        {service.actif ? (
          <span className="pill ok">Actif</span>
        ) : (
          <span className="pill wt">Archivé</span>
        )}

        <div>
          <p className="drawer__label">Description</p>
          <p className="drawer__text">{service.description ?? '—'}</p>
        </div>

        <div className="drawer__row">
          <span className="drawer__label">Code</span>
          <strong>{service.codeService ?? '—'}</strong>
        </div>

        <div className="drawer__row">
          <span className="drawer__label">Statut</span>
          <strong>{service.actif ? 'Actif' : 'Archivé'}</strong>
        </div>

        <footer className="drawer__footer">
          <button
            type="button"
            className="btn-ghost"
            onClick={() => onArchive(service.id)}
            disabled={!service.actif}
          >
            Archiver
          </button>
          <button type="button" className="btn-primary" onClick={() => onEdit(service)}>
            Modifier
          </button>
        </footer>
      </aside>
    </>
  )
}
