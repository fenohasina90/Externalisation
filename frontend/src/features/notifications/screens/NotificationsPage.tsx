import { useEffect, useMemo, useState } from 'react'
import { NotificationsList } from '../components'
import { MOCK_NOTIFICATIONS, TYPE_META, type TypeNotification } from '../data/mockNotifications'
import { useNotificationsListStatic } from '../hooks'
import type { NotificationUser } from '../types'

const PAGE_SIZE = 8

/**
 * Notifications — étape 1 : interface seule, données statiques.
 * Onglets Toutes / Non lues, filtre par type, marquage local comme lue.
 */
export function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationUser[]>(MOCK_NOTIFICATIONS)

  const { items, total, totalPages, page, setPage, onglet, setOnglet, type, setType } =
    useNotificationsListStatic({ source: notifications, size: PAGE_SIZE })

  const nonLues = useMemo(() => notifications.filter((n) => !n.lu).length, [notifications])
  const echeancesProches = useMemo(
    () => notifications.filter((n) => n.typeNotification === 'ECHEANCE_PROCHE').length,
    [notifications],
  )
  const paiementsRetard = useMemo(
    () => notifications.filter((n) => n.typeNotification === 'PAIEMENT_RETARD').length,
    [notifications],
  )
  const alertesSysteme = useMemo(
    () => notifications.filter((n) => n.typeNotification === 'ALERTE_SYSTEME').length,
    [notifications],
  )

  const marquerLue = (id: number) => {
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, lu: true } : n)))
  }

  const marquerTout = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, lu: true })))
  }

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Notifications</h1>
          <p>Restez informé des échéances, paiements, demandes et alertes système.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={marquerTout} disabled={nonLues === 0}>
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
            Marquer tout comme lu
          </button>
        </div>
      </header>

      <section className="kpi-grid">
        <div className="card kpi-card hl">
          <span className="kpi-tag">À consulter</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M6 9a6 6 0 0 1 12 0c0 6 2 7 2 7H4s2-1 2-7M10 20a2 2 0 0 0 4 0" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{nonLues}</div>
            <div className="kpi-label">Non lues</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">7 jours</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{echeancesProches}</div>
            <div className="kpi-label">Échéances proches</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">À suivre</span>
          <div
            className="kpi-icon"
            style={{ background: 'var(--danger-bg)', color: 'var(--danger)' }}
          >
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l10 18H2L12 3zM12 10v5M12 18h.01" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{paiementsRetard}</div>
            <div className="kpi-label">Paiements en retard</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">Ce mois</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{alertesSysteme}</div>
            <div className="kpi-label">Alertes système</div>
          </div>
        </div>
      </section>

      <section className="card card-pad">
        <div className="notif-filter">
          <div className="seg-tabs" role="tablist" aria-label="Filtrer les notifications">
            <button
              type="button"
              role="tab"
              aria-selected={onglet === 'toutes'}
              className={`seg-tab${onglet === 'toutes' ? ' seg-tab--active' : ''}`}
              onClick={() => setOnglet('toutes')}
            >
              Toutes ({notifications.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={onglet === 'non-lues'}
              className={`seg-tab${onglet === 'non-lues' ? ' seg-tab--active' : ''}`}
              onClick={() => setOnglet('non-lues')}
            >
              Non lues ({nonLues})
            </button>
          </div>
          <label className="field filter-field" style={{ maxWidth: 260 }}>
            Type
            <select
              value={type}
              onChange={(e) => setType(e.target.value as TypeNotification | 'tous')}
            >
              <option value="tous">Tous les types</option>
              {(Object.keys(TYPE_META) as TypeNotification[]).map((t) => (
                <option key={t} value={t}>
                  {TYPE_META[t].label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>

      <section className="card">
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <NotificationsList items={items} onConsulter={marquerLue} onMarquerLue={marquerLue} />
          </div>
          {total > 0 && (
            <div className="table-footer">
              <span>
                Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                {total} notification{total > 1 ? 's' : ''}
              </span>
              <div className="pagination">
                <button
                  type="button"
                  className="pagination-item"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page <= 0}
                >
                  ‹
                </button>
                {Array.from({ length: totalPages }).map((_, i) => (
                  <button
                    key={i}
                    type="button"
                    className={`pagination-item ${i === page ? 'active' : ''}`}
                    onClick={() => setPage(i)}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  type="button"
                  className="pagination-item"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                >
                  ›
                </button>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
