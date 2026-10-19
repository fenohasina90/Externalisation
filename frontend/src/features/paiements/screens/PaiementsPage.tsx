import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { Can } from '@/components/shared/Can'
import { Alert, Spinner } from '@/components/ui'
import type { Societe } from '@/features/clients/types'
import { clientsApi } from '@/features/clients/services/clients.service'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { KpiCard, SuiviTable } from '../components'
import { useSuiviPaiements } from '../hooks'

/** Liste des paiements — suivi financier par demande, branché sur l'API. */
export function PaiementsPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const { items, total, totalPages, page, setPage, filtres, setFiltre, resetFiltres,
    hasActiveFiltres, loading, error, synthese, syntheseLoading, syntheseError } =
    useSuiviPaiements(PAGE_SIZE)

  const { data: societes } = useApi<ApiPage<Societe>>(
    () =>
      clientsApi.list({ page: 0, size: 200, recherche: null }, {}, { champ: 'nomSociete', sens: 'asc' }),
    [],
  )

  // Agrégats du portefeuille. `null` = valeur masquée par le backend pour ce
  // rôle : on affiche « — » plutôt qu'un faux « 0 MGA ».
  const montantTotal = synthese?.montantTotal ?? null
  const montantPaye = synthese?.montantPaye ?? null
  const reste = synthese?.resteAPayer ?? null
  const enRetard = synthese?.echeancesEnRetard ?? null

  const pct = (part: number | null): string | undefined => {
    if (part === null || montantTotal === null || montantTotal <= 0) return undefined
    return `${Math.round((part / montantTotal) * 100)} %`
  }

  const exporter = () => {
    const header = 'Reference;Client;Services;Total;Paye;Reste;Statut'
    const rows = items.map((d) => {
      const noms = (d.lignes ?? [])
        .map((l) => l.nomService)
        .filter(Boolean)
      const services = noms.length > 1 ? `${noms[0]} + ${noms.length - 1} autre${noms.length > 2 ? 's' : ''}` : noms[0] ?? ''
      return [
        d.referenceDemande,
        d.nomSociete ?? '',
        services,
        d.montantTotal ?? 0,
        d.montantPaye,
        d.resteAPayer,
        d.statutActuel ?? '',
      ].join(';')
    })
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'paiements.csv'
    a.click()
    URL.revokeObjectURL(url)
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
          <h1>Liste des paiements</h1>
          <p>Suivez les montants payés et restants de chaque demande de service.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={exporter}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Exporter
          </button>
          <Can permission="PAIEMENT_CREATE">
            <Link to="/paiements/nouveau" className="btn-primary">
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 5v14M5 12h14" />
              </svg>
              Enregistrer un paiement
            </Link>
          </Can>
        </div>
      </header>

      {/* <p className="section-note" style={{ marginBottom: 10 }}>
        Indicateurs du portefeuille — toutes demandes confondues, indépendamment des filtres
        appliqués à la liste ci-dessous.
      </p> */}

      <section className="kpi-grid">
        <KpiCard
          loading={syntheseLoading}
          montant={montantTotal}
          libelle="Montant total à payer"
          icone={
            <svg className="sv" viewBox="0 0 24 24">
              <rect x="3" y="5" width="18" height="14" rx="2" />
              <path d="M3 10h18" />
            </svg>
          }
        />

        <KpiCard
          ton="hl"
          loading={syntheseLoading}
          montant={montantPaye}
          tag={pct(montantPaye)}
          libelle="Montant total payé"
          icone={
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          }
        />

        <KpiCard
          loading={syntheseLoading}
          montant={reste}
          tag={pct(reste)}
          libelle="Reste à payer"
          icone={
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          }
        />

        <KpiCard
          ton="danger"
          loading={syntheseLoading}
          nombre={enRetard}
          tag="À suivre"
          libelle="Échéances en retard"
          icone={
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l10 18H2L12 3zM12 10v5M12 18h.01" />
            </svg>
          }
        />
      </section>

      {syntheseError && (
        <div className="card card-pad">
          <Alert tone="danger">
            Statistiques indisponibles : {syntheseError} Les montants ci-dessus ne sont pas
            fiables.
          </Alert>
        </div>
      )}

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Recherche
            <input
              type="text"
              value={filtres.recherche}
              onChange={(e) => setFiltre('recherche', e.target.value)}
              placeholder="Référence, client…"
            />
          </label>
          <label className="field filter-field">
            Client
            <select
              value={filtres.societeId ?? ''}
              onChange={(e) => setFiltre('societeId', e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Tous les clients</option>
              {(societes?.content ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nomSociete}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Statut
            <select value={filtres.statut} onChange={(e) => setFiltre('statut', e.target.value)}>
              <option value="tous">Tous</option>
              <option value="EN_ATTENTE">En attente</option>
              <option value="VALIDATION">Validation</option>
              <option value="EN_COURS">En cours</option>
              <option value="TERMINEE">Terminée</option>
              <option value="ANNULEE">Annulée</option>
            </select>
          </label>
          <label className="field filter-field">
            Période (début)
            <input
              type="date"
              value={filtres.dateDebut}
              onChange={(e) => setFiltre('dateDebut', e.target.value)}
            />
          </label>
          <label className="field filter-field">
            Période (fin)
            <input
              type="date"
              value={filtres.dateFin}
              onChange={(e) => setFiltre('dateFin', e.target.value)}
            />
          </label>
          <div className="filter-actions filter-actions--bottom">
            <button
              type="button"
              className="btn-ghost"
              onClick={resetFiltres}
              disabled={!hasActiveFiltres}
            >
              Réinitialiser
            </button>
            <button type="submit" className="btn-primary">
              Appliquer
            </button>
          </div>
        </form>
      </section>

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des paiements…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <SuiviTable items={items} />
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                  {total} demandes
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
                  {Array.from({ length: totalPages }).map((_, i) => {
                    const show = i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1
                    if (!show) {
                      if (i === 1 || i === totalPages - 2) {
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
                        className={`pagination-item ${i === page ? 'active' : ''}`}
                        onClick={() => setPage(i)}
                      >
                        {i + 1}
                      </button>
                    )
                  })}
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