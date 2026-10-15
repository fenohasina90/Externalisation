import { useEffect, useState } from 'react'
import { auditApi } from '../services/administration.service'
import type { AuditEntry } from '../types'
import { formatDateHeure, formatValeurAudit } from '@/utils/format'
import { getLibelleActionAudit } from '../constants/labels'

interface Props {
  id: number | null
  onClose: () => void
}

/** Détail d'une trace d'audit : valeurs avant/après, IP, navigateur. */
export function AuditDetailModal({ id, onClose }: Props) {
  const [entry, setEntry] = useState<AuditEntry | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (id == null) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setEntry(null)
      setError(null)
      return
    }
    let mounted = true
    setLoading(true)
    setError(null)
    auditApi
      .get(id)
      .then((data) => {
        if (mounted) setEntry(data)
      })
      .catch((err: unknown) => {
        if (mounted) setError(err instanceof Error ? err.message : 'Échec du chargement')
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => {
      mounted = false
    }
  }, [id])

  if (id == null) return null

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 620 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Détail de la trace d'audit"
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">Trace n° {id}</h3>
            <p className="modal-subtitle">
              {entry ? `${getLibelleActionAudit(entry.action)} · ${formatDateHeure(entry.createdAt)}` : 'Chargement…'}
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <div className="modal-body">
          {loading && <p className="text-muted">Chargement…</p>}
          {error && <div style={{ color: 'var(--danger)', fontSize: 14 }}>{error}</div>}
          {entry && (
            <div className="info-grid">
              <div className="info-item">
                <span>Utilisateur</span>
                <b>{entry.nomUtilisateur ?? '—'}</b>
              </div>
              <div className="info-item">
                <span>Module</span>
                <b>{entry.module}</b>
              </div>
              <div className="info-item">
                <span>Entité</span>
                <b>
                  {entry.entite ?? '—'}
                  {entry.entiteId != null ? ` (n° ${entry.entiteId})` : ''}
                </b>
              </div>
              <div className="info-item">
                <span>Adresse IP</span>
                <b>{entry.adresseIp ?? '—'}</b>
              </div>
              <div className="info-item info-item--full">
                <span>Navigateur</span>
                <b style={{ fontWeight: 500, fontSize: 13 }}>{entry.userAgent ?? '—'}</b>
              </div>
              <div className="info-item info-item--full">
                <span>Ancienne valeur</span>
                <code className="code-inline" style={{ whiteSpace: 'pre-wrap' }}>
                  {formatValeurAudit(entry.ancienneValeur)}
                </code>
              </div>
              <div className="info-item info-item--full">
                <span>Nouvelle valeur</span>
                <code className="code-inline" style={{ whiteSpace: 'pre-wrap' }}>
                  {formatValeurAudit(entry.nouvelleValeur)}
                </code>
              </div>
            </div>
          )}
        </div>

        <footer className="modal-footer">
          <button type="button" className="btn-ghost" onClick={onClose}>
            Fermer
          </button>
        </footer>
      </div>
    </div>
  )
}
