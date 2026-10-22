import { useMemo, useState } from 'react'
import { Can } from '@/components/shared/Can'
import { Alert, EmptyState, Pagination, Spinner } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { useApi } from '@/hooks/useApi'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { clientsApi } from '@/features/clients/services/clients.service'
import type { Societe } from '@/features/clients/types'
import { commerciauxApi } from '@/features/commerciaux/services/commerciaux.service'
import type { Commercial } from '@/features/commerciaux/types'
import { domainesApi, servicesApi } from '@/features/services/services/services.service'
import type { Domaine, Service } from '@/features/services/types'
import type { ApiPage } from '@/types'
import { formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import {
  CategoriesRapports,
  DemandesParStatut,
  TableRapportClients,
  TableRapportDemandes,
  TableRapportFinancier,
  TableRapportGeneral,
  TableRapportPaiements,
  TableRapportServices,
} from '../components'
import {
  CATEGORIES_RAPPORT,
  STATUTS_DEMANDE,
  permissionExport,
} from '../constants'
import { rapportsApi } from '../services/rapports.service'
import type {
  CategorieRapport,
  FormatExport,
  RapportClientLigne,
  RapportDemande,
  RapportFiltres,
  RapportPaiementLigne,
  RapportServiceLigne,
} from '../types'

const FILTRES_VIDES: RapportFiltres = {}

/** Rapports — données et exports servis par l'API. */
export function RapportsPage() {
  const { hasPermission } = useAuth()
  const peutDemandes = hasPermission('RAPPORT_DEMANDE_VIEW')
  const peutPaiements = hasPermission('RAPPORT_PAIEMENT_VIEW')

  const categories = useMemo(
    () => CATEGORIES_RAPPORT.filter((c) => hasPermission(c.permission)),
    [hasPermission],
  )

  const [selection, setSelection] = useState<CategorieRapport>('general')
  const categorie: CategorieRapport = categories.some((c) => c.id === selection)
    ? selection
    : categories[0]?.id ?? 'general'

  const [page, setPage] = useState(0)
  const taillePage = Math.max(1, useTaillePagination())
  const [showFilters, setShowFilters] = useState(false)
  const [filtres, setFiltres] = useState<RapportFiltres>(FILTRES_VIDES)
  const [appliques, setAppliques] = useState<RapportFiltres>(FILTRES_VIDES)
  const [exportEnCours, setExportEnCours] = useState<FormatExport | null>(null)
  const [erreurExport, setErreurExport] = useState<string | null>(null)

  const cleFiltres = JSON.stringify(appliques)
  const hasActiveFiltres = Object.values(appliques).some(
    (v) => v !== undefined && v !== null && v !== '',
  )

  const peutClients = hasPermission('CLIENT_VIEW')
  const peutServices = hasPermission('SERVICE_VIEW')
  const peutDomaines = hasPermission('DOMAINE_VIEW')
  const peutCommerciaux = hasPermission('COMMERCIAL_VIEW')

  const { data: general, loading: chargementGeneral, error: erreurGeneral } = useApi<
    Awaited<ReturnType<typeof rapportsApi.general>> | null
  >(
    () => (peutDemandes ? rapportsApi.general(appliques) : Promise.resolve(null)),
    [peutDemandes, cleFiltres],
  )
  const { data: financier, loading: chargementFinancier, error: erreurFinancier } = useApi<
    Awaited<ReturnType<typeof rapportsApi.financier>> | null
  >(
    () => (peutPaiements ? rapportsApi.financier(appliques) : Promise.resolve(null)),
    [peutPaiements, cleFiltres],
  )

  const { data: lignesClients, loading: chargementClients, error: erreurClients } = useApi<
    RapportClientLigne[] | null
  >(
    () =>
      categorie === 'clients'
        ? rapportsApi.clients(appliques)
        : Promise.resolve(null),
    [categorie, cleFiltres],
  )
  const { data: lignesServices, loading: chargementServices, error: erreurServices } = useApi<
    RapportServiceLigne[] | null
  >(
    () =>
      categorie === 'services'
        ? rapportsApi.services(appliques)
        : Promise.resolve(null),
    [categorie, cleFiltres],
  )
  const { data: lignesDemandes, loading: chargementDemandes, error: erreurDemandes } = useApi<
    RapportDemande[] | null
  >(
    () =>
      categorie === 'demandes'
        ? rapportsApi.demandes(appliques)
        : Promise.resolve(null),
    [categorie, cleFiltres],
  )
  const { data: lignesPaiements, loading: chargementPaiements, error: erreurPaiements } = useApi<
    RapportPaiementLigne[] | null
  >(
    () =>
      categorie === 'paiements'
        ? rapportsApi.paiements(appliques)
        : Promise.resolve(null),
    [categorie, cleFiltres],
  )

  const { data: societes } = useApi<ApiPage<Societe> | null>(
    () =>
      peutClients
        ? clientsApi.list({ page: 0, size: 200, recherche: null })
        : Promise.resolve(null),
    [peutClients],
  )
  const { data: services } = useApi<ApiPage<Service> | null>(
    () =>
      peutServices
        ? servicesApi.list({ page: 0, size: 200, recherche: null }, { actif: true })
        : Promise.resolve(null),
    [peutServices],
  )
  const { data: domaines } = useApi<ApiPage<Domaine> | null>(
    () =>
      peutDomaines
        ? domainesApi.list({ page: 0, size: 200, recherche: null }, { actif: true })
        : Promise.resolve(null),
    [peutDomaines],
  )
  const { data: commerciaux } = useApi<ApiPage<Commercial> | null>(
    () =>
      peutCommerciaux
        ? commerciauxApi.list({ page: 0, size: 200, recherche: null }, { actif: true })
        : Promise.resolve(null),
    [peutCommerciaux],
  )

  const labelCategorie =
    CATEGORIES_RAPPORT.find((c) => c.id === categorie)?.label ?? 'Rapport'

  const chargement =
    categorie === 'general'
      ? chargementGeneral
      : categorie === 'financier'
        ? chargementFinancier
        : categorie === 'clients'
          ? chargementClients
          : categorie === 'services'
            ? chargementServices
            : categorie === 'demandes'
              ? chargementDemandes
              : chargementPaiements

  const erreur =
    categorie === 'general'
      ? erreurGeneral
      : categorie === 'financier'
        ? erreurFinancier
        : categorie === 'clients'
          ? erreurClients
          : categorie === 'services'
            ? erreurServices
            : categorie === 'demandes'
              ? erreurDemandes
              : erreurPaiements

  const totalPrevu = general?.montantTotalPrevu ?? financier?.montantTotalPrevu
  const montantEncaisse = general?.montantEncaisse ?? financier?.montantEncaisse
  const resteAPayer = general?.resteAPayer ?? financier?.resteAPayer
  const montantEnRetard = general?.montantEnRetard ?? financier?.montantEnRetard
  const tauxPaye =
    totalPrevu && totalPrevu > 0 && montantEncaisse !== undefined
      ? Math.round((montantEncaisse / totalPrevu) * 100)
      : null

  const parStatut = useMemo(() => {
    if (!lignesDemandes) return []
    const compte = new Map<string, number>()
    for (const demande of lignesDemandes) {
      compte.set(demande.statutActuel, (compte.get(demande.statutActuel) ?? 0) + 1)
    }
    return STATUTS_DEMANDE.map((s) => ({
      statut: s.label,
      nombre: compte.get(s.code) ?? 0,
    }))
  }, [lignesDemandes])

  function setFiltre<K extends keyof RapportFiltres>(
    cle: K,
    valeur: RapportFiltres[K],
  ): void {
    setFiltres((f) => ({ ...f, [cle]: valeur }))
  }

  function appliquer(): void {
    setAppliques({ ...filtres })
    setPage(0)
  }

  function reinitialiser(): void {
    setFiltres(FILTRES_VIDES)
    setAppliques(FILTRES_VIDES)
    setPage(0)
  }

  async function exporter(format: FormatExport): Promise<void> {
    setExportEnCours(format)
    setErreurExport(null)
    try {
      const blob = await rapportsApi.exporter(categorie, format, appliques)
      telechargerBlob(blob, `rapport-${categorie}.${format}`)
    } catch (e) {
      setErreurExport(messageErreur(e))
    } finally {
      setExportEnCours(null)
    }
  }

  const nombreLignes =
    categorie === 'clients'
      ? lignesClients?.length ?? 0
      : categorie === 'services'
        ? lignesServices?.length ?? 0
        : categorie === 'demandes'
          ? lignesDemandes?.length ?? 0
          : categorie === 'paiements'
            ? lignesPaiements?.length ?? 0
            : 0
  const estListe =
    categorie === 'clients' ||
    categorie === 'services' ||
    categorie === 'demandes' ||
    categorie === 'paiements'
  const totalPages = Math.ceil(nombreLignes / taillePage)
  const pageCourante = Math.min(page, Math.max(totalPages - 1, 0))
  const debut = pageCourante * taillePage

  const tranche = <T,>(lignes: T[] | null): T[] =>
    lignes ? lignes.slice(debut, debut + taillePage) : []

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Rapports</h1>
          <p>Analysez l&apos;activité par catégorie et exportez les résultats.</p>
        </div>
        <div className="actions">
          <Can permission={permissionExport(categorie)}>
            <button
              type="button"
              className="btn-ghost"
              onClick={() => void exporter('pdf')}
              disabled={exportEnCours !== null}
            >
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
              </svg>
              {exportEnCours === 'pdf' ? 'Export…' : 'PDF'}
            </button>
            <button
              type="button"
              className="btn-ghost"
              onClick={() => void exporter('xlsx')}
              disabled={exportEnCours !== null}
            >
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
              </svg>
              {exportEnCours === 'xlsx' ? 'Export…' : 'Excel'}
            </button>
            <button
              type="button"
              className="btn-ghost"
              onClick={() => void exporter('csv')}
              disabled={exportEnCours !== null}
            >
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
              </svg>
              {exportEnCours === 'csv' ? 'Export…' : 'CSV'}
            </button>
          </Can>
          <button type="button" className="btn-ghost" onClick={() => setShowFilters((v) => !v)}>
            {showFilters ? 'Masquer les filtres' : 'Afficher les filtres'}
          </button>
        </div>
      </header>

      {erreurExport && <Alert tone="danger">{erreurExport}</Alert>}

      <CategoriesRapports
        selection={categorie}
        onSelect={(id) => {
          setSelection(id)
          setPage(0)
        }}
        categories={categories}
      />

      {showFilters && (
        <section className="card card-pad">
          <form
            onSubmit={(e) => {
              e.preventDefault()
              appliquer()
            }}
          >
            <div className="filters-line filters-line--4">
              <label className="field">
                Période (début)
                <input
                  type="date"
                  value={filtres.dateDebut ?? ''}
                  onChange={(e) => setFiltre('dateDebut', e.target.value || undefined)}
                />
              </label>
              <label className="field">
                Période (fin)
                <input
                  type="date"
                  value={filtres.dateFin ?? ''}
                  onChange={(e) => setFiltre('dateFin', e.target.value || undefined)}
                />
              </label>
              {peutClients && (
                <label className="field">
                  Client
                  <select
                    value={filtres.clientId ?? ''}
                    onChange={(e) =>
                      setFiltre('clientId', e.target.value ? Number(e.target.value) : undefined)
                    }
                  >
                    <option value="">Tous les clients</option>
                    {(societes?.content ?? []).map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.nomSociete}
                      </option>
                    ))}
                  </select>
                </label>
              )}
              {peutServices && (
                <label className="field">
                  Service
                  <select
                    value={filtres.serviceId ?? ''}
                    onChange={(e) =>
                      setFiltre('serviceId', e.target.value ? Number(e.target.value) : undefined)
                    }
                  >
                    <option value="">Tous les services</option>
                    {(services?.content ?? []).map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.nomService}
                      </option>
                    ))}
                  </select>
                </label>
              )}
            </div>
            <div className="filters-line filters-line--4">
              {peutDomaines && (
                <label className="field">
                  Domaine
                  <select
                    value={filtres.domaineId ?? ''}
                    onChange={(e) =>
                      setFiltre('domaineId', e.target.value ? Number(e.target.value) : undefined)
                    }
                  >
                    <option value="">Tous les domaines</option>
                    {(domaines?.content ?? []).map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.nomDomaine}
                      </option>
                    ))}
                  </select>
                </label>
              )}
              {peutCommerciaux && (
                <label className="field">
                  Commercial
                  <select
                    value={filtres.commercialId ?? ''}
                    onChange={(e) =>
                      setFiltre('commercialId', e.target.value ? Number(e.target.value) : undefined)
                    }
                  >
                    <option value="">Tous</option>
                    {(commerciaux?.content ?? []).map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nomCommercial}
                      </option>
                    ))}
                  </select>
                </label>
              )}
              <label className="field">
                Statut
                <select
                  value={filtres.statut ?? ''}
                  onChange={(e) => setFiltre('statut', e.target.value || undefined)}
                >
                  <option value="">Tous</option>
                  {STATUTS_DEMANDE.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="filter-actions filter-actions--bottom">
              <button
                type="button"
                className="btn-ghost"
                onClick={reinitialiser}
                disabled={!hasActiveFiltres && Object.keys(filtres).length === 0}
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

      <section className="kpi-grid">
        {general ? (
          <>
            <div className="card kpi-card">
              <span className="kpi-tag">Total</span>
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <circle cx="9" cy="8" r="3.5" />
                  <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{general.nombreClients}</div>
                <div className="kpi-label">Clients</div>
              </div>
            </div>
            <div className="card kpi-card">
              <span className="kpi-tag">Période</span>
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{general.nombreDemandes}</div>
                <div className="kpi-label">Demandes</div>
              </div>
            </div>
            <div className="card kpi-card">
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <path d="M3 10h18" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{formatMontantMGA(general.montantTotalPrevu)}</div>
                <div className="kpi-label">Montant total</div>
              </div>
            </div>
            <div className="card kpi-card hl">
              <span className="kpi-tag">{tauxPaye !== null ? `${tauxPaye} %` : '—'}</span>
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M8 12l3 3 5-6" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{formatMontantMGA(general.montantEncaisse)}</div>
                <div className="kpi-label">Montant payé</div>
              </div>
            </div>
          </>
        ) : (
          <>
            <div className="card kpi-card">
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <path d="M3 10h18" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{formatMontantMGA(totalPrevu)}</div>
                <div className="kpi-label">Montant total prévu</div>
              </div>
            </div>
            <div className="card kpi-card hl">
              <span className="kpi-tag">{tauxPaye !== null ? `${tauxPaye} %` : '—'}</span>
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M8 12l3 3 5-6" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{formatMontantMGA(montantEncaisse)}</div>
                <div className="kpi-label">Montant encaissé</div>
              </div>
            </div>
            <div className="card kpi-card">
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v5l3 3" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{formatMontantMGA(resteAPayer)}</div>
                <div className="kpi-label">Reste à payer</div>
              </div>
            </div>
            <div className="card kpi-card">
              <span className="kpi-tag">Retard</span>
              <div className="kpi-icon">
                <svg className="sv" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 8v5M12 16h.01" />
                </svg>
              </div>
              <div>
                <div className="kpi-num">{formatMontantMGA(montantEnRetard)}</div>
                <div className="kpi-label">Montant en retard</div>
              </div>
            </div>
          </>
        )}
      </section>

      {erreur && <Alert tone="danger">{erreur}</Alert>}

      <div className="rapport-grid">
        <section className="card">
          <div className="card-pad" style={{ paddingBottom: 0 }}>
            <h3 style={{ margin: 0 }}>{labelCategorie}</h3>
          </div>
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              {chargement ? (
                <Spinner label="Chargement du rapport…" />
              ) : categorie === 'general' && general ? (
                <TableRapportGeneral rapport={general} />
              ) : categorie === 'financier' && financier ? (
                <TableRapportFinancier rapport={financier} />
              ) : categorie === 'clients' && lignesClients ? (
                lignesClients.length > 0 ? (
                  <TableRapportClients lignes={tranche(lignesClients)} />
                ) : (
                  <EmptyState titre="Aucun client sur la période" />
                )
              ) : categorie === 'services' && lignesServices ? (
                lignesServices.length > 0 ? (
                  <TableRapportServices lignes={tranche(lignesServices)} />
                ) : (
                  <EmptyState titre="Aucun service sur la période" />
                )
              ) : categorie === 'demandes' && lignesDemandes ? (
                lignesDemandes.length > 0 ? (
                  <TableRapportDemandes lignes={tranche(lignesDemandes)} />
                ) : (
                  <EmptyState titre="Aucune demande sur la période" />
                )
              ) : categorie === 'paiements' && lignesPaiements ? (
                lignesPaiements.length > 0 ? (
                  <TableRapportPaiements lignes={tranche(lignesPaiements)} />
                ) : (
                  <EmptyState titre="Aucun paiement sur la période" />
                )
              ) : null}
            </div>
          </div>
          {!chargement && estListe && nombreLignes > 0 && (
            <div className="rapport-pagination">
              <span className="pagination-info">
                Affichage de {debut + 1} à {Math.min(debut + taillePage, nombreLignes)} sur{' '}
                {nombreLignes}
              </span>
              <Pagination
                page={pageCourante}
                totalPages={totalPages}
                onPageChange={setPage}
              />
            </div>
          )}
        </section>

        <section className="card card-pad">
          <h3>Synthèse financière</h3>
          <div className="rc">
            <span>Montant total prévu</span>
            <strong>{formatMontantMGA(totalPrevu)}</strong>
          </div>
          <div className="rc">
            <span>Montant encaissé</span>
            <strong>{formatMontantMGA(montantEncaisse)}</strong>
          </div>
          <div className="rc">
            <span>Reste à payer</span>
            <strong>{formatMontantMGA(resteAPayer)}</strong>
          </div>
          <div className="rc">
            <span>Montant en retard</span>
            <strong>{formatMontantMGA(montantEnRetard)}</strong>
          </div>
          {general && (
            <div className="rc">
              <span>Échéances en retard</span>
              <strong>{general.echeancesEnRetard}</strong>
            </div>
          )}
          {parStatut.length > 0 && (
            <>
              <h3 style={{ marginTop: '1.5rem' }}>Demandes par statut</h3>
              <DemandesParStatut items={parStatut} />
            </>
          )}
        </section>
      </div>
    </div>
  )
}
