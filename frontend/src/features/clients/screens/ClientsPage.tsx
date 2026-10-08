import { useEffect, useMemo, useState } from 'react'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { ClientFormModal, ClientsTable } from '../components'
import { CLIENT_OPTIONS, MOCK_CLIENTS, type ClientResume, type StatutClient } from '../data/mockClients'
import { useClientsListStatic } from '../hooks'

const PAGE_SIZE = 8

/** Liste des clients — étape 1 : interface seule, données statiques. */
export function ClientsPage() {
  const [clients, setClients] = useState<ClientResume[]>(MOCK_CLIENTS)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ClientResume | null>(null)
  const [showFilters, setShowFilters] = useState(false)
  const [confirmArchive, setConfirmArchive] = useState<ClientResume | null>(null)

  const liste = useClientsListStatic({ source: clients, size: PAGE_SIZE })

  const kpis = useMemo(() => {
    const actifs = clients.filter((c) => c.statut === 'Actif').length
    const inactifs = clients.filter((c) => c.statut === 'Inactif').length
    const archives = clients.filter((c) => c.statut === 'Archivé').length
    return {
      total: clients.length,
      actifs,
      pctActifs: clients.length === 0 ? 0 : Math.round((actifs / clients.length) * 100),
      inactifs,
      archives,
    }
  }, [clients])

  const exporter = () => {
    const header = 'Code;Client;Activite;Email;Telephone;Responsable;Demandes;Statut'
    const rows = liste.items.map((c) =>
      [c.code, c.nom, c.activite, c.email, c.telephone, c.responsable, c.nbDemandes, c.statut].join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'clients.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleSave = (payload: { nom: string; activite: string; responsable: string; telephone: string; email: string }) => {
    if (editing) {
      setClients((prev) => prev.map((c) => (c.code === editing.code ? { ...c, ...payload } : c)))
    } else {
      const maxCode = Math.max(0, ...clients.map((c) => Number(c.code.split('-')[1] ?? 0)))
      const code = `CLI-${String(maxCode + 1).padStart(4, '0')}`
      setClients((prev) => [
        { code, nbDemandes: 0, statut: 'Actif' as StatutClient, ...payload },
        ...prev,
      ])
    }
    setModalOpen(false)
    setEditing(null)
  }

  const handleStatut = (code: string, statut: StatutClient) => {
    setClients((prev) => prev.map((c) => (c.code === code ? { ...c, statut } : c)))
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
          <h1>Liste des clients</h1>
          <p>Centralisez les informations, demandes et activités de votre portefeuille.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={() => setShowFilters((v) => !v)}>
            {showFilters ? 'Masquer les filtres' : 'Afficher les filtres'}
          </button>
          <button type="button" className="btn-ghost" onClick={exporter}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Exporter
          </button>
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
            Ajouter un client
          </button>
        </div>
      </header>

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="9" cy="8" r="3.5" />
              <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.total}</div>
            <div className="kpi-label">Clients enregistrés</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{kpis.pctActifs} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.actifs}</div>
            <div className="kpi-label">Clients actifs</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">À relancer</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.inactifs}</div>
            <div className="kpi-label">Clients inactifs</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">Masqués</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.archives}</div>
            <div className="kpi-label">Clients archivés</div>
          </div>
        </div>
      </section>

      {showFilters && (
      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Recherche
            <input
              type="text"
              value={liste.recherche}
              onChange={(e) => liste.setRecherche(e.target.value)}
              placeholder="Client, contact, responsable…"
            />
          </label>
          <label className="field filter-field">
            Activité
            <select value={liste.activite} onChange={(e) => liste.setActivite(e.target.value)}>
              <option value="toutes">Toutes</option>
              {CLIENT_OPTIONS.activites.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Responsable
            <select value={liste.responsable} onChange={(e) => liste.setResponsable(e.target.value)}>
              <option value="tous">Tous</option>
              {CLIENT_OPTIONS.responsables.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Statut
            <select
              value={liste.statut}
              onChange={(e) => liste.setStatut(e.target.value as StatutClient | 'tous')}
            >
              <option value="tous">Tous</option>
              <option value="Actif">Actif</option>
              <option value="Inactif">Inactif</option>
              <option value="Archivé">Archivé</option>
            </select>
          </label>
          <label className="field filter-field">
            Trier par
            <select value={liste.tri} onChange={(e) => liste.setTri(e.target.value as typeof liste.tri)}>
              <option value="nom-az">Client (A → Z)</option>
              <option value="nom-za">Client (Z → A)</option>
              <option value="demandes-desc">Demandes (décroissant)</option>
              <option value="demandes-asc">Demandes (croissant)</option>
            </select>
          </label>
          <div className="filter-actions filter-actions--bottom">
            <button
              type="button"
              className="btn-ghost"
              onClick={liste.resetFiltres}
              disabled={!liste.hasActiveFiltres}
            >
              Réinitialiser
            </button>
            <button type="submit" className="btn-primary">
              Appliquer
            </button>
          </div>
        </form>
      </section>
      )}

      <section className="card">
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <ClientsTable
              items={liste.items}
              onEdit={(c) => {
                setEditing(c)
                setModalOpen(true)
              }}
              onArchiver={(code) => {
                const cible = clients.find((c) => c.code === code) ?? null
                setConfirmArchive(cible)
              }}
            />
          </div>
          {liste.total > 0 && (
            <div className="table-footer">
              <span>
                Affichage de {liste.page * PAGE_SIZE + 1} à{' '}
                {Math.min((liste.page + 1) * PAGE_SIZE, liste.total)} sur {liste.total} clients
              </span>
              <div className="pagination">
                <button
                  type="button"
                  className="pagination-item"
                  onClick={() => liste.setPage(Math.max(0, liste.page - 1))}
                  disabled={liste.page <= 0}
                >
                  ‹
                </button>
                {Array.from({ length: liste.totalPages }).map((_, i) => {
                  const show = i === 0 || i === liste.totalPages - 1 || Math.abs(i - liste.page) <= 1
                  if (!show) {
                    if (i === 1 || i === liste.totalPages - 2) {
                      return (
                        <span key={i} className="pagination-item" style={{ border: 'none', background: 'transparent' }}>
                          …
                        </span>
                      )
                    }
                    return null
                  }
                  return (
                    <button
                      key={i}
                      type="button"
                      className={`pagination-item ${i === liste.page ? 'active' : ''}`}
                      onClick={() => liste.setPage(i)}
                    >
                      {i + 1}
                    </button>
                  )
                })}
                <button
                  type="button"
                  className="pagination-item"
                  onClick={() => liste.setPage(Math.min(liste.totalPages - 1, liste.page + 1))}
                  disabled={liste.page >= liste.totalPages - 1}
                >
                  ›
                </button>
              </div>
            </div>
          )}
        </div>
      </section>

      <ClientFormModal
        open={modalOpen}
        client={editing}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={confirmArchive !== null}
        titre="Archiver ce client ?"
        message={`« ${confirmArchive?.nom ?? ''} » sera masqué des listes courantes. Ses demandes et son historique sont conservés.`}
        confirmer="Archiver"
        ton="danger"
        onAnnuler={() => setConfirmArchive(null)}
        onConfirmer={() => {
          if (!confirmArchive) return
          const code = confirmArchive.code
          setConfirmArchive(null)
          handleStatut(code, 'Archivé')
        }}
      />
    </div>
  )
}
