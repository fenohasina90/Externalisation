import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`card ${className}`}>{children}</div>
}

export function CardTitle({ children }: { children: ReactNode }) {
  return <h3 className="card-title">{children}</h3>
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="spinner-container" role="status">
      <span className="spinner" aria-hidden="true" />
      {label ? <p className="spinner-label">{label}</p> : null}
    </div>
  )
}

type AlertTone = 'info' | 'success' | 'warning' | 'danger'

export function Alert({
  tone = 'info',
  children,
}: {
  tone?: AlertTone
  children: ReactNode
}) {
  return <div className={`alert alert-${tone}`}>{children}</div>
}

export function EmptyState({ titre, hint }: { titre: string; hint?: string }) {
  return (
    <div className="empty-state">
      <p className="empty-state-title">{titre}</p>
      {hint ? <p className="empty-state-hint">{hint}</p> : null}
    </div>
  )
}

export function PageHeader({
  titre,
  sousTitre,
  actions,
}: {
  titre: string
  sousTitre?: string
  actions?: ReactNode
}) {
  return (
    <header className="page-header">
      <div>
        <h1 className="page-title">{titre}</h1>
        {sousTitre ? <p className="page-subtitle">{sousTitre}</p> : null}
      </div>
      {actions ? <div className="page-actions">{actions}</div> : null}
    </header>
  )
}

export function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="btn btn-ghost"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        Précédent
      </button>
      <span className="pagination-info">
        Page {page + 1} / {totalPages}
      </span>
      <button
        type="button"
        className="btn btn-ghost"
        disabled={page + 1 >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Suivant
      </button>
    </nav>
  )
}