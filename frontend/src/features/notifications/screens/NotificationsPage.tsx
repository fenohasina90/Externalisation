import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Spinner } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import { echeancesApi } from '@/features/echeances/services/echeances.service'
import { NotificationsList } from '../components'
import { TYPE_META, typesVisibles } from '../constants'
import { useNotificationsList } from '../hooks'
import { notificationsApi } from '../services/notifications.service'
import type { NotificationUser, TypeNotification } from '../types'

interface KpiNotification {
  tag: string
  label: string
  valeur: number
  highlight?: boolean
  icone: 'bell' | 'clock' | 'alert' | 'check' | 'doc' | 'refresh'
}

function IconeKpi({ icone }: { icone: KpiNotification['icone'] }) {
  const paths = {
    bell: <path d="M6 9a6 6 0 0 1 12 0c0 6 2 7 2 7H4s2-1 2-7M10 20a2 2 0 0 0 4 0" />,
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 3" />
      </>
    ),
    alert: <path d="M12 3l10 18H2L12 3zM12 10v5M12 18h.01" />,
    check: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M8 12l3 3 5-6" />
      </>
    ),
    doc: <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />,
    refresh: <path d="M20 11a8 8 0 1 0-2.3 5.7M20 4v7h-7" />,
  }
  return (
    <svg className="sv" viewBox="0 0 24 24">
      {paths[icone]}
    </svg>
  )
}

/** Centre de notifications branché sur l'API, filtré selon le rôle connecté. */
export function NotificationsPage() {
  const PAGE_SIZE = useTaillePagination()
  const navigate = useNavigate()
  const { utilisateur } = useAuth()
  const role = utilisateur?.role.nomRole
  const typesDisponibles = useMemo(() => typesVisibles(role), [role])

  const {
    items,
    total,
    totalPages,
    page,
    setPage,
    onglet,
    setOnglet,
    type,
    setType,
    loading,
    error,
    nonLues,
    parType,
    rafraichir,
  } = useNotificationsList(PAGE_SIZE)

  const [actionEnCours, setActionEnCours] = useState(false)
  const [erreurAction, setErreurAction] = useState<string | null>(null)

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  const cartes: KpiNotification[] = useMemo(() => {
    const nonLuesCarte: KpiNotification = {
      tag: 'À consulter',
      label: 'Non lues',
      valeur: nonLues,
      highlight: true,
      icone: 'bell',
    }
    if (role === 'ACCUEIL') {
      return [
        nonLuesCarte,
        {
          tag: 'Demandes',
          label: 'Nouvelles demandes',
          valeur: parType.NOUVELLE_DEMANDE ?? 0,
          icone: 'doc',
        },
        {
          tag: 'Suivi',
          label: 'Changements de statut',
          valeur: parType.CHANGEMENT_STATUT ?? 0,
          icone: 'refresh',
        },
      ]
    }
    if (role === 'ECONOMAT') {
      return [
        nonLuesCarte,
        {
          tag: 'À venir',
          label: 'Échéances proches',
          valeur: parType.ECHEANCE_PROCHE ?? 0,
          icone: 'clock',
        },
        {
          tag: 'À suivre',
          label: 'Paiements en retard',
          valeur: parType.PAIEMENT_RETARD ?? 0,
          icone: 'alert',
        },
        {
          tag: 'Encaissements',
          label: 'Paiements enregistrés',
          valeur: parType.PAIEMENT_ENREGISTRE ?? 0,
          icone: 'check',
        },
      ]
    }
    return [
      nonLuesCarte,
      {
        tag: 'À venir',
        label: 'Échéances proches',
        valeur: parType.ECHEANCE_PROCHE ?? 0,
        icone: 'clock',
      },
      {
        tag: 'À suivre',
        label: 'Paiements en retard',
        valeur: parType.PAIEMENT_RETARD ?? 0,
        icone: 'alert',
      },
      {
        tag: 'Système',
        label: 'Alertes système',
        valeur: parType.ALERTE_SYSTEME ?? 0,
        icone: 'check',
      },
    ]
  }, [nonLues, parType, role])

  async function marquerLue(id: number): Promise<void> {
    setErreurAction(null)
    try {
      await notificationsApi.marquerLue(id)
      rafraichir()
    } catch (e) {
      setErreurAction(messageErreur(e))
    }
  }

  async function marquerTout(): Promise<void> {
    setActionEnCours(true)
    setErreurAction(null)
    try {
      await notificationsApi.toutLire()
      setPage(0)
      rafraichir()
    } catch (e) {
      setErreurAction(messageErreur(e))
    } finally {
      setActionEnCours(false)
    }
  }

  async function consulter(notification: NotificationUser): Promise<void> {
    setErreurAction(null)
    try {
      if (!notification.lu) {
        await notificationsApi.marquerLue(notification.id)
        rafraichir()
      }
      switch (notification.referenceType) {
        case 'DEMANDE':
          if (notification.referenceId) {
            const demande = await demandesApi.get(notification.referenceId)
            navigate(`/demandes/${encodeURIComponent(demande.referenceDemande)}`)
          } else {
            navigate('/demandes')
          }
          break
        case 'ECHEANCE':
          if (notification.referenceId) {
            const echeance = await echeancesApi.get(notification.referenceId)
            navigate(`/paiements/echeanciers/${encodeURIComponent(echeance.referenceDemande)}`)
          } else {
            navigate('/paiements/echeanciers')
          }
          break
        case 'PAIEMENT':
          navigate('/paiements/historique')
          break
        case 'PASSWORD_RESET':
          navigate('/administration/reinitialisations')
          break
        case 'BACKUP':
          navigate('/administration/sauvegardes')
          break
        default:
          break
      }
    } catch (e) {
      setErreurAction(messageErreur(e))
    }
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Notifications</h1>
          <p>
            {role === 'ACCUEIL'
              ? 'Suivez les nouvelles demandes et leurs changements de statut.'
              : role === 'ECONOMAT'
                ? 'Suivez les échéances et les paiements enregistrés.'
                : 'Restez informé des demandes, paiements, échéances et alertes système.'}
          </p>
        </div>
        <div className="actions">
          <button
            type="button"
            className="btn-ghost"
            onClick={() => void marquerTout()}
            disabled={nonLues === 0 || actionEnCours}
          >
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
            {actionEnCours ? 'Mise à jour…' : 'Marquer tout comme lu'}
          </button>
        </div>
      </header>

      <section className="kpi-grid">
        {cartes.map((carte) => (
          <div key={carte.label} className={`card kpi-card${carte.highlight ? ' hl' : ''}`}>
            <span className="kpi-tag">{carte.tag}</span>
            <div className="kpi-icon">
              <IconeKpi icone={carte.icone} />
            </div>
            <div>
              <div className="kpi-num">{carte.valeur}</div>
              <div className="kpi-label">{carte.label}</div>
            </div>
          </div>
        ))}
      </section>

      {erreurAction && <Alert tone="danger">{erreurAction}</Alert>}
      {error && <Alert tone="danger">{error}</Alert>}

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
              Toutes
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
          <label className="field filter-field" style={{ maxWidth: 280 }}>
            Type
            <select
              value={type}
              onChange={(e) => setType(e.target.value as TypeNotification | 'tous')}
            >
              <option value="tous">Tous les types</option>
              {typesDisponibles.map((t) => (
                <option key={t} value={t}>
                  {TYPE_META[t].label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>

      <section className="card">
        {loading ? (
          <Spinner label="Chargement des notifications…" />
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <NotificationsList
                items={items}
                onConsulter={(n) => void consulter(n)}
                onMarquerLue={(id) => void marquerLue(id)}
              />
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à{' '}
                  {Math.min((page + 1) * PAGE_SIZE, total)} sur {total} notification
                  {total > 1 ? 's' : ''}
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
        )}
      </section>
    </div>
  )
}
