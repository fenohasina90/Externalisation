import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { auditApi, type AuditFiltres } from '@/features/administration/services/administration.service'
import type { AuditEntry } from '@/features/administration/types'
import { formatDate, formatDateHeure, formatMontantMGA, formatTailleOctets } from '@/utils/format'
import { getLibelleActionAudit } from '@/features/administration/constants/labels'
import { clientsApi } from '../services/clients.service'
import type { Societe, SocieteDetail } from '../types'
import { codeSociete, idDepuisCodeSociete, statutSociete } from '../types'

type Onglet = 'informations' | 'demandes' | 'paiements' | 'documents' | 'historique'

function initiales(nomComplet: string): string {
  const parts = nomComplet.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '—'
  if (parts.length === 1) return (parts[0][0] ?? '—').toUpperCase()
  return `${parts[0][0] ?? ''}${parts[parts.length - 1][0] ?? ''}`.toUpperCase()
}

function statutPill(statut: string, fallback = 'pill wt'): string {
  const code = statut.toUpperCase().replace(/ /g, '_')
  if (code === 'EN_COURS' || statut === 'En cours') return 'pill info'
  if (['TERMINEE', 'VALIDATION'].includes(code) || ['Terminée', 'Validation'].includes(statut))
    return 'pill ok'
  if (code === 'ANNULEE' || statut === 'Annulée') return 'pill danger'
  if (statut === 'Payé' || statut === 'Payée') return 'pill ok'
  if (statut === 'Partiel' || statut === 'Partielle') return 'pill warning'
  if (statut === 'En retard') return 'pill danger'
  if (statut === 'En attente' || code === 'EN_ATTENTE') return 'pill wt'
  return fallback
}

/** Identifiant client accepté dans l'URL : code CLI-0248 ou ancien identifiant numérique. */
function resoudreIdClient(reference: string | undefined): number | null {
  if (!reference) return null
  const parCode = idDepuisCodeSociete(reference)
  if (parCode !== null) return parCode
  const numerique = Number(reference)
  return Number.isInteger(numerique) && numerique > 0 ? numerique : null
}

/** Fiche client 360° branchée sur GET /api/clients/{id}, URL canonique /clients/CLI-0248. */
export function ClientDetailPage() {
  const { reference } = useParams<{ reference: string }>()
  const navigate = useNavigate()
  const societeId = resoudreIdClient(reference)
  const [onglet, setOnglet] = useState<Onglet>('informations')
  const [pageDemandes, setPageDemandes] = useState(0)
  const [pagePaiements, setPagePaiements] = useState(0)

  const {
    data: detail,
    loading,
    error,
  } = useApi<SocieteDetail>(() => {
    if (societeId === null) return Promise.reject(new Error('Référence client invalide.'))
    return clientsApi.getDetail(societeId)
  }, [societeId])

  const client: Societe | null = detail?.societe ?? null

  // URL canonique : la fiche est toujours consultée par sa référence (CLI-0248).
  useEffect(() => {
    if (client && reference !== codeSociete(client.id)) {
      navigate(`/clients/${codeSociete(client.id)}`, { replace: true })
    }
  }, [client, reference, navigate])

  const statut = client ? statutSociete(client) : 'Actif'
  const synthese = detail?.synthese ?? null
  const demandes = detail?.demandes ?? []
  const paiements = detail?.paiements ?? []
  const documents = detail?.piecesJointes ?? []

  const filtresHistorique: AuditFiltres = { entite: 'Societe' }
  const { data: audit } = useApi(
    () => auditApi.list({ page: 0, size: 100, recherche: null }, filtresHistorique),
    [societeId],
  )
  const historique: AuditEntry[] = (audit?.content ?? []).filter((e) => e.entiteId === societeId)

  const pctPaye =
    synthese?.montantTotal != null && synthese.montantTotal > 0 && synthese.montantPaye != null
      ? Math.round((synthese.montantPaye / synthese.montantTotal) * 100)
      : 0
  const pctReste = synthese ? 100 - pctPaye : 0

  const telecharger = async (pieceId: number, nom: string | null) => {
    const blob = await clientsApi.telechargerPiece(pieceId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = nom ?? `document-${pieceId}`
    a.click()
    URL.revokeObjectURL(url)
  }

  if (loading) {
    return (
      <div className="app-page responsables-page">
        <Spinner label="Chargement du client…" />
      </div>
    )
  }

  if (error || !client) {
    return (
      <div className="app-page responsables-page">
        <div className="card-pad">
          <Alert tone="danger">{error ?? 'Client introuvable.'}</Alert>
        </div>
        <Link to="/clients" className="btn-ghost" style={{ alignSelf: 'flex-start' }}>
          Retour à la liste
        </Link>
      </div>
    )
  }

  const pageSizeDemandes = 5
  const totalPagesDemandes = Math.max(1, Math.ceil(demandes.length / pageSizeDemandes))
  const demandesPage = demandes.slice(pageDemandes * pageSizeDemandes, pageDemandes * pageSizeDemandes + pageSizeDemandes)
  const pageSizePaiements = 5
  const totalPagesPaiements = Math.max(1, Math.ceil(paiements.length / pageSizePaiements))
  const paiementsPage = paiements.slice(pagePaiements * pageSizePaiements, pagePaiements * pageSizePaiements + pageSizePaiements)

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>
            {client.nomSociete}{' '}
            <span className={statut === 'Actif' ? 'pill ok' : 'pill wt'}>● {statut}</span>
          </h1>
          <p>
            {codeSociete(client.id)} · {client.activite || '—'}
          </p>
        </div>
      </header>

      <section className="kpi-grid">
        <div className="card kpi-card">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{client.nombreDemandes}</div>
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
            <div className="kpi-num">{synthese ? formatMontantMGA(synthese.montantTotal) : '—'}</div>
            <div className="kpi-label">Montant total</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          {synthese && <span className="kpi-tag">{pctPaye} %</span>}
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{synthese ? formatMontantMGA(synthese.montantPaye) : '—'}</div>
            <div className="kpi-label">Montant payé</div>
          </div>
        </div>

        <div className="card kpi-card">
          {synthese && <span className="kpi-tag">{pctReste} %</span>}
          <div
            className="kpi-icon"
            style={{ background: 'var(--danger-bg)', color: 'var(--danger)' }}
          >
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{synthese ? formatMontantMGA(synthese.resteAPayer) : '—'}</div>
            <div className="kpi-label">Reste à payer</div>
          </div>
        </div>
      </section>

      <nav className="tabs" aria-label="Sections du client">
        {(
          [
            ['informations', 'Informations'],
            ['demandes', 'Demandes'],
            ['paiements', 'Paiements'],
            ['documents', 'Documents'],
            ['historique', 'Historique'],
          ] as Array<[Onglet, string]>
        ).map(([tabId, label]) => (
          <button
            key={tabId}
            type="button"
            role="tab"
            aria-selected={onglet === tabId}
            className={`tab${onglet === tabId ? ' tab--active' : ''}`}
            onClick={() => setOnglet(tabId)}
          >
            {label}
          </button>
        ))}
      </nav>

      {onglet === 'informations' && (
        <>
          <section className="card card-pad">
            <h3 style={{ margin: '0 0 14px', fontSize: 17, fontWeight: 700 }}>
              Informations générales
            </h3>
            <div className="info-grid">
              <div className="info-item">
                <span>Client</span>
                <b>{client.nomSociete}</b>
              </div>
              <div className="info-item">
                <span>Code</span>
                <b>{codeSociete(client.id)}</b>
              </div>
              <div className="info-item">
                <span>Activité</span>
                <b>{client.activite || '—'}</b>
              </div>
              <div className="info-item">
                <span>Contact</span>
                <b>{client.contact || '—'}</b>
              </div>
              <div className="info-item">
                <span>Email</span>
                <b>{client.email || '—'}</b>
              </div>
              <div className="info-item">
                <span>Facebook</span>
                <b>
                  {client.fb ? (
                    client.fb.startsWith('http') ? (
                      <a href={client.fb} target="_blank" rel="noreferrer" className="lk">
                        {client.fb}
                      </a>
                    ) : (
                      client.fb
                    )
                  ) : (
                    '—'
                  )}
                </b>
              </div>
              <div className="info-item">
                <span>Adresse</span>
                <b>{client.adresse || '—'}</b>
              </div>
              <div className="info-item">
                <span>Date de création</span>
                <b>{formatDate(client.dateCreation)}</b>
              </div>
              <div className="info-item">
                <span>Nombre de personnel</span>
                <b>{client.nombrePersonnel ?? '—'}</b>
              </div>
              <div className="info-item">
                <span>Chiffre d&apos;affaires mensuel</span>
                <b>
                  {client.chiffreAffaireMensuel === null || client.chiffreAffaireMensuel === undefined
                    ? '—'
                    : formatMontantMGA(client.chiffreAffaireMensuel)}
                </b>
              </div>
              <div className="info-item">
                <span>Domaine</span>
                <b>{client.nomDomaine || '—'}</b>
              </div>
              <div className="info-item">
                <span>Statut</span>
                <b>
                  <span className={statut === 'Actif' ? 'pill ok' : 'pill wt'}>● {statut}</span>
                </b>
              </div>
              <div className="info-item">
                <span>Nombre de demandes</span>
                <b>{client.nombreDemandes}</b>
              </div>
            </div>
          </section>

          <section className="card">
            <div className="card-pad" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0 }}>Responsables</h3>
              <Link to="/responsables" className="lk">
                Gérer les responsables ›
              </Link>
            </div>
            <div className="table-scroll-container no-global-scroll">
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Nom</th>
                      <th>Fonction</th>
                      <th>Téléphone</th>
                      <th>Email</th>
                      <th>Statut</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(detail?.responsables ?? []).map((r, i) => (
                      <tr key={r.id}>
                        <td>
                          <span className="avatar">{initiales(r.nomClient)}</span>
                          <span className="fw-600">{r.nomClient}</span>{' '}
                          {i === 0 && <span className="pill info">● Principal</span>}
                        </td>
                        <td className="text-secondary">{r.fonction ?? '—'}</td>
                        <td className="text-secondary">{r.contact ?? '—'}</td>
                        <td className="text-secondary">{r.email ?? '—'}</td>
                        <td>
                          {r.actif ? (
                            <span className="pill ok">● Actif</span>
                          ) : (
                            <span className="pill wt">● Inactif</span>
                          )}
                        </td>
                      </tr>
                    ))}
                    {(detail?.responsables ?? []).length === 0 && (
                      <tr>
                        <td colSpan={5} className="text-muted">
                          Aucun responsable pour ce client.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </>
      )}

      {onglet === 'demandes' && (
        <section className="card">
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Référence</th>
                    <th>Service</th>
                    <th>Date de début</th>
                    <th>Date de fin</th>
                    <th>Statut</th>
                    <th>Situation du paiement</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {demandesPage.map((d) => {
                    const reste = d.resteAPayer ?? 0
                    const paye = d.montantPaye ?? 0
                    const situation = reste <= 0 && paye > 0 ? 'Payé' : paye > 0 ? 'Partiel' : 'En attente'
                    return (
                      <tr key={d.id}>
                        <td className="fw-700">{d.referenceDemande}</td>
                        <td>{d.serviceNom ?? '—'}</td>
                        <td className="text-secondary">{formatDate(d.debutService)}</td>
                        <td className="text-secondary">{formatDate(d.finService)}</td>
                      <td>
                        <span className={statutPill(d.statutActuel ?? '')}>
                          ● {d.statutActuel?.replace(/_/g, ' ') ?? '—'}
                        </span>
                      </td>
                        <td>
                          <span className={statutPill(situation)}>● {situation}</span>
                        </td>
                        <td>
                          <Link to={`/demandes/${d.referenceDemande}`} className="lk">
                            Voir ›
                          </Link>
                        </td>
                      </tr>
                    )
                  })}
                  {demandes.length === 0 && (
                    <tr>
                      <td colSpan={7} className="text-muted">
                        Aucune demande pour ce client.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            {demandes.length > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {pageDemandes * pageSizeDemandes + 1} à{' '}
                  {Math.min((pageDemandes + 1) * pageSizeDemandes, demandes.length)} sur{' '}
                  {demandes.length} demandes
                </span>
                <div className="pagination">
                  {Array.from({ length: totalPagesDemandes }).map((_, i) => (
                    <button
                      key={i}
                      type="button"
                      className={`pagination-item ${i === pageDemandes ? 'active' : ''}`}
                      onClick={() => setPageDemandes(i)}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </section>
      )}

      {onglet === 'paiements' && (
        <section className="card">
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Référence</th>
                    <th>Demande</th>
                    <th>Date</th>
                    <th>Montant</th>
                    <th>Mode de paiement</th>
                    <th>Réf. virement</th>
                  </tr>
                </thead>
                <tbody>
                  {paiementsPage.map((p) => (
                    <tr key={p.id}>
                      <td className="fw-700">{p.referencePaiement}</td>
                      <td className="text-secondary">{p.referenceDemande}</td>
                      <td className="text-secondary">{formatDate(p.datePaiement)}</td>
                      <td className="cell-number fw-700">{formatMontantMGA(p.montantPaye)}</td>
                      <td>
                        <span className={p.libelleMode === 'Virement' ? 'pill info' : 'pill wt'}>
                          ● {p.libelleMode ?? '—'}
                        </span>
                      </td>
                      <td className="text-secondary">{p.referenceVirement ?? '—'}</td>
                    </tr>
                  ))}
                  {paiements.length === 0 && (
                    <tr>
                      <td colSpan={6} className="text-muted">
                        Aucun paiement pour ce client.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            {paiements.length > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {pagePaiements * pageSizePaiements + 1} à{' '}
                  {Math.min((pagePaiements + 1) * pageSizePaiements, paiements.length)} sur{' '}
                  {paiements.length} paiements
                </span>
                <div className="pagination">
                  {Array.from({ length: totalPagesPaiements }).map((_, i) => (
                    <button
                      key={i}
                      type="button"
                      className={`pagination-item ${i === pagePaiements ? 'active' : ''}`}
                      onClick={() => setPagePaiements(i)}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </section>
      )}

      {onglet === 'documents' && (
        <section className="card">
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Type</th>
                    <th>Demande</th>
                    <th>Taille</th>
                    <th>Date d&apos;ajout</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {documents.map((doc) => (
                    <tr key={doc.id}>
                      <td className="fw-600">{doc.nomOriginal}</td>
                      <td className="text-secondary">{(doc.typeFichier ?? '').toUpperCase()}</td>
                      <td className="text-secondary">{doc.referenceDemande ?? '—'}</td>
                      <td className="text-secondary">{formatTailleOctets(doc.tailleFichier)}</td>
                      <td className="text-secondary">{formatDateHeure(doc.createdAt)}</td>
                      <td>
                        <button
                          type="button"
                          className="btn-link success"
                          onClick={() => void telecharger(doc.id, doc.nomOriginal)}
                        >
                          Télécharger
                        </button>
                      </td>
                    </tr>
                  ))}
                  {documents.length === 0 && (
                    <tr>
                      <td colSpan={6} className="text-muted">
                        Aucun document pour ce client.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

      {onglet === 'historique' && (
        <section className="card">
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Date et heure</th>
                    <th>Événement</th>
                    <th>Utilisateur</th>
                    <th>Commentaire</th>
                  </tr>
                </thead>
                <tbody>
                  {historique.map((h) => (
                    <tr key={h.id}>
                      <td className="text-secondary">{formatDateHeure(h.createdAt)}</td>
                      <td>
                        <span className="pill info">● {getLibelleActionAudit(h.action)}</span>{' '}
                        {h.nouvelleValeur ?? h.ancienneValeur ?? ''}
                      </td>
                      <td>{h.nomUtilisateur ?? '—'}</td>
                      <td className="text-secondary">—</td>
                    </tr>
                  ))}
                  {historique.length === 0 && (
                    <tr>
                      <td colSpan={4} className="text-muted">
                        Aucun événement pour ce client.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

    </div>
  )
}
