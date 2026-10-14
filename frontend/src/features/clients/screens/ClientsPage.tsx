import { useEffect, useState } from 'react'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { responsablesApi } from '@/features/responsables/services/responsables.service'
import { domainesApi } from '@/features/services/services/services.service'
import type { Domaine } from '@/features/services/types'
import { ClientFormModal, ClientsTable } from '../components'
import { clientsApi, type TriClients } from '../services/clients.service'
import { useClientsList, type StatutClientFilter } from '../hooks/useClientsList'
import type { CreateSocietePayload, Societe } from '../types'

const PAGE_SIZE = 8

/** Liste des clients branchée sur l'API : recherche, domaine, statut, tri serveur. */
export function ClientsPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Societe | null>(null)
  const [editingResponsableId, setEditingResponsableId] = useState<number | null>(null)
  const [showFilters, setShowFilters] = useState(false)
  const [confirmArchive, setConfirmArchive] = useState<Societe | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const liste = useClientsList({ size: PAGE_SIZE })

  const { data: domaines, reload: reloadDomaines } = useApi<ApiPage<Domaine>>(
    () => domainesApi.list({ page: 0, size: 100, recherche: null }, {}),
    [],
  )
  const optionsDomaines = (domaines?.content ?? []).map((d) => ({ id: d.id, nom: d.nomDomaine }))

  /** Domaine existant sélectionné, sinon créé à la volée lors de la création du client. */
  const resolveDomaine = async (nom: string): Promise<number> => {
    const exact = optionsDomaines.find((d) => d.nom.toLowerCase() === nom.trim().toLowerCase())
    if (exact) return exact.id
    const cree = await domainesApi.create({ nomDomaine: nom.trim() })
    reloadDomaines()
    return cree.id
  }

  const { data: totalPage } = useApi<ApiPage<Societe>>(
    () => clientsApi.list({ page: 0, size: 1, recherche: null }),
    [],
  )
  const { data: actifsPage } = useApi<ApiPage<Societe>>(
    () => clientsApi.list({ page: 0, size: 1, recherche: null }, { actif: true }),
    [],
  )
  const { data: archivesPage } = useApi<ApiPage<Societe>>(
    () => clientsApi.list({ page: 0, size: 1, recherche: null }, { archive: true }),
    [],
  )
  const nbTotal = totalPage?.totalElements ?? 0
  const nbActifs = actifsPage?.totalElements ?? 0
  const nbArchives = archivesPage?.totalElements ?? 0
  const nbInactifs = Math.max(0, nbTotal - nbActifs - nbArchives)
  const pctActifs = nbTotal === 0 ? 0 : Math.round((nbActifs / nbTotal) * 100)

  const exporter = () => {
    const header = 'Code;Client;Activite;Email;Contact;Responsable;Demandes;Statut'
    const rows = liste.items.map((c) =>
      [
        `CLI-${String(c.id).padStart(4, '0')}`,
        c.nomSociete,
        c.activite,
        c.email,
        c.contact,
        c.responsablePrincipal,
        c.nombreDemandes,
        c.archive ? 'Archivé' : c.actif ? 'Actif' : 'Inactif',
      ]
        .map((v) => `"${(v ?? '').toString().replace(/"/g, '""')}"`)
        .join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'clients.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  const openEdit = async (c: Societe) => {
    setEditing(c)
    setEditingResponsableId(null)
    setModalOpen(true)
    try {
      const res = await responsablesApi.list({ page: 0, size: 5, recherche: null }, { societeId: c.id })
      setEditingResponsableId(res.content[0]?.id ?? null)
    } catch {
      setEditingResponsableId(null)
    }
  }

  const handleSave = async (
    payload: CreateSocietePayload & { responsable: string },
    id?: number,
  ) => {
    const { responsable, ...societe } = payload
    if (id != null) {
      await clientsApi.update(id, societe)
      if (editingResponsableId != null) {
        const actuel = editing?.responsablePrincipal ?? ''
        if (responsable !== actuel) {
          await responsablesApi.update(editingResponsableId, { nomClient: responsable })
        }
      } else {
        await responsablesApi.create({ societeId: id, nomClient: responsable })
      }
    } else {
      const cree = await clientsApi.create(societe)
      await responsablesApi.create({ societeId: cree.id, nomClient: responsable })
    }
    setModalOpen(false)
    setEditing(null)
    setEditingResponsableId(null)
    liste.reload()
  }

  const handleArchive = async (id: number) => {
    setActionError(null)
    try {
      await clientsApi.archive(id, true)
      liste.reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Échec de l'archivage")
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
              setEditingResponsableId(null)
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
              <circle cx="9" cy="8" r="3.5" />
              <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{nbTotal}</div>
            <div className="kpi-label">Clients enregistrés</div>
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
            <div className="kpi-num">{nbActifs}</div>
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
            <div className="kpi-num">{nbInactifs}</div>
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
            <div className="kpi-num">{nbArchives}</div>
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
              Domaine
              <select
                value={liste.domaineId ?? ''}
                onChange={(e) =>
                  liste.setDomaineId(e.target.value === '' ? null : Number(e.target.value))
                }
              >
                <option value="">Tous</option>
                {optionsDomaines.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nom}
                  </option>
                ))}
              </select>
            </label>
            <label className="field filter-field">
              Statut
              <select
                value={liste.statut}
                onChange={(e) => liste.setStatut(e.target.value as StatutClientFilter)}
              >
                <option value="tous">Tous</option>
                <option value="Actif">Actif</option>
                <option value="Inactif">Inactif</option>
                <option value="Archivé">Archivé</option>
              </select>
            </label>
            <label className="field filter-field">
              Trier par
              <select
                value={`${liste.tri.champ}:${liste.tri.sens}`}
                onChange={(e) => {
                  const [champ, sens] = e.target.value.split(':') as [
                    TriClients['champ'],
                    TriClients['sens'],
                  ]
                  liste.setTri({ champ, sens })
                }}
              >
                <option value="nomSociete:asc">Client (A → Z)</option>
                <option value="nomSociete:desc">Client (Z → A)</option>
                <option value="activite:asc">Activité (A → Z)</option>
                <option value="activite:desc">Activité (Z → A)</option>
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
                Filtrer
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="card">
        {liste.loading && liste.items.length === 0 ? (
          <Spinner label="Chargement des clients…" />
        ) : liste.error ? (
          <div className="card-pad">
            <Alert tone="danger">{liste.error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <ClientsTable items={liste.items} onEdit={openEdit} onArchiver={(id) => {
                const cible = liste.items.find((c) => c.id === id) ?? null
                setConfirmArchive(cible)
              }} />
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
                    const show =
                      i === 0 || i === liste.totalPages - 1 || Math.abs(i - liste.page) <= 1
                    if (!show) {
                      if (i === 1 || i === liste.totalPages - 2) {
                        return (
                          <span
                            key={i}
                            className="pagination-item"
                            style={{ border: 'none', background: 'transparent' }}
                          >
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
        )}
      </section>

      <ClientFormModal
        open={modalOpen}
        client={editing}
        domaines={optionsDomaines}
        resolveDomaine={resolveDomaine}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
          setEditingResponsableId(null)
        }}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={confirmArchive !== null}
        titre="Archiver ce client ?"
        message={`« ${confirmArchive?.nomSociete ?? ''} » sera masqué des listes courantes. Ses demandes et son historique sont conservés.`}
        confirmer="Archiver"
        ton="danger"
        onAnnuler={() => setConfirmArchive(null)}
        onConfirmer={() => {
          if (!confirmArchive) return
          const id = confirmArchive.id
          setConfirmArchive(null)
          void handleArchive(id)
        }}
      />
    </div>
  )
}
