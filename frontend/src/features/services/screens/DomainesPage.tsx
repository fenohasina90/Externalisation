import { useEffect, useState } from 'react'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Can } from '@/components/shared/Can'
import { Alert, Spinner } from '@/components/ui'
import { DomaineFormModal, DomainesTable } from '../components'
import { useDomainesList, type StatutDomaineFilter } from '../hooks/useDomainesList'
import { domainesApi } from '../services/services.service'
import type { Domaine } from '../types'


/**
 * Domaines de société branchés sur l'API :
 * recherche, filtre statut, pagination serveur, activation.
 */
export function DomainesPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Domaine | null>(null)
  const [confirmToggle, setConfirmToggle] = useState<{ id: number; actif: boolean } | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { items, total, totalPages, page, loading, error, setPage, search, setSearch, statut, setStatut, reload } =
    useDomainesList(PAGE_SIZE)

  const actifs = items.filter((d) => d.actif).length
  const pctActifs = total === 0 ? 0 : Math.round((actifs / total) * 100)
  const societes = items.reduce((sum, d) => sum + (d.nombreSocietes ?? 0), 0)

  const handleSave = async (payload: { nomDomaine: string }, id?: number) => {
    if (id != null) {
      await domainesApi.update(id, payload)
    } else {
      await domainesApi.create(payload)
    }
    setModalOpen(false)
    setEditing(null)
    reload()
  }

  const handleToggleActif = async (id: number, actif: boolean) => {
    setActionError(null)
    try {
      await domainesApi.setActif(id, actif)
      reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Échec du changement de statut')
    }
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
          <h1>Domaines de société</h1>
          <p>Organisez les sociétés clientes par domaine d&apos;activité.</p>
        </div>
        <div className="actions">
          <Can permission="DOMAINE_CREATE">
            <button
              type="button"
              className="btn-primary"
              onClick={() => {
                setEditing(null)
                setModalOpen(true)
              }}
            >
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 5v14M5 12h14" />
              </svg>
              Ajouter un domaine
            </button>
          </Can>
        </div>
      </header>

      {actionError && (
        <div className="alert alert-danger" role="alert">
          {actionError}
        </div>
      )}

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{total}</div>
            <div className="kpi-label">Domaines</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{pctActifs} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{actifs}</div>
            <div className="kpi-label">Actifs</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">Au total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="9" cy="8" r="3.5" />
              <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{societes}</div>
            <div className="kpi-label">Sociétés associées</div>
          </div>
        </div>
      </section>

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Recherche
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Nom du domaine…"
            />
          </label>
          <label className="field filter-field">
            Statut
            <select value={statut} onChange={(e) => setStatut(e.target.value as StatutDomaineFilter)}>
              <option value="tous">Tous</option>
              <option value="actifs">Actifs</option>
              <option value="inactifs">Inactifs</option>
            </select>
          </label>
          {/* <button type="submit" className="btn-primary">
            Rechercher
          </button> */}
        </form>
      </section>

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des domaines…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <DomainesTable
                items={items}
                onEdit={(d) => {
                  setEditing(d)
                  setModalOpen(true)
                }}
                onToggleActif={(id, actif) => setConfirmToggle({ id, actif })}
              />
            </div>
            {total > 0 && (
              <div className="table-footer">
              <span>
                Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                {total} domaines
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

      <DomaineFormModal
        open={modalOpen}
        domaine={editing}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={confirmToggle !== null}
        titre={confirmToggle?.actif ? 'Activer ce domaine ?' : 'Désactiver ce domaine ?'}
        message={
          confirmToggle?.actif
            ? 'Ce domaine sera de nouveau proposé pour les sociétés clientes.'
            : 'Ce domaine ne sera plus proposé. Les sociétés associées sont conservées.'
        }
        confirmer={confirmToggle?.actif ? 'Activer' : 'Désactiver'}
        ton={confirmToggle?.actif ? 'success' : 'danger'}
        onAnnuler={() => setConfirmToggle(null)}
        onConfirmer={() => {
          if (!confirmToggle) return
          const { id, actif } = confirmToggle
          setConfirmToggle(null)
          void handleToggleActif(id, actif)
        }}
      />
    </div>
  )
}
