import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { STATUT_LABEL } from '@/features/demandes/data/mockDemandes'
import { RESPONSABLES_PAR_CLIENT } from '@/features/demandes/data/mockDemandes'
import { MOCK_DEMANDES } from '@/features/demandes/data/mockDemandes'
import { MOCK_HISTORIQUE } from '@/features/paiements/data/mockPaiements'
import { ModePill } from '@/features/paiements/components'
import { MOCK_PLANS } from '@/features/paiements/data/mockPaiements'
import { ClientFormModal } from '../components'
import {
  ATLAS_DEMANDES,
  ATLAS_DETAIL,
  ATLAS_DOCUMENTS,
  ATLAS_HISTORIQUE,
  ATLAS_RESPONSABLES,
  MOCK_CLIENTS,
  initialesClient,
  type ClientResume,
  type EvenementClient,
  type ResponsableClient,
} from '../data/mockClients'

type Onglet = 'informations' | 'demandes' | 'paiements' | 'documents' | 'historique'

function formatMGA(n: number): string {
  return `${n.toLocaleString('fr-FR')} MGA`
}

function toneStatut(label: string): string {
  if (label === 'En cours' || label === 'EN_COURS') return 'pill info'
  if (label === 'Terminée' || label === 'TERMINEE' || label === 'Validation' || label === 'VALIDATION') return 'pill ok'
  if (label === 'Annulée' || label === 'ANNULEE') return 'pill danger'
  return 'pill wt'
}

function tonePaiement(label: string): string {
  if (label === 'Payé') return 'pill ok'
  if (label === 'Partiel') return 'pill warning'
  if (label === 'En retard') return 'pill danger'
  return 'pill wt'
}

function toneEvenement(e: EvenementClient): string {
  if (e.categorie === 'Paiement') return 'pill info'
  if (e.texte.startsWith('Création')) return 'pill ok'
  return 'pill wt'
}

function paginer<T>(items: T[], page: number, size: number): T[] {
  return items.slice(page * size, page * size + size)
}

/** Détail client 360° : informations, demandes, paiements, documents, historique. */
export function ClientDetailPage() {
  const { code } = useParams<{ code: string }>()
  const [client, setClient] = useState<ClientResume | null>(
    () => MOCK_CLIENTS.find((c) => c.code === code) ?? null,
  )
  const [onglet, setOnglet] = useState<Onglet>('informations')
  const [editOpen, setEditOpen] = useState(false)
  const [confirmArchive, setConfirmArchive] = useState(false)
  const [pageDemandes, setPageDemandes] = useState(0)
  const [pagePaiements, setPagePaiements] = useState(0)

  const estAtlas = code === 'CLI-0248'

  const demandes = useMemo(() => {
    if (!client) return []
    if (estAtlas) {
      return ATLAS_DEMANDES.map((d) => ({
        reference: d.reference,
        service: d.service,
        dateDebut: d.dateDebut,
        dateFin: d.dateFin,
        statut: d.statut,
        paiement: d.paiement,
      }))
    }
    return MOCK_DEMANDES.filter((d) => d.client === client.nom).map((d) => ({
      reference: d.reference,
      service: d.service,
      dateDebut: d.dateDebut,
      dateFin: d.dateFin,
      statut: STATUT_LABEL[d.statut],
      paiement: d.situationPaiement,
    }))
  }, [client, estAtlas])

  const paiements = useMemo(
    () => (client ? MOCK_HISTORIQUE.filter((h) => h.client === client.nom) : []),
    [client],
  )

  const kpis = useMemo(() => {
    if (!client) return null
    if (estAtlas) {
      const total = ATLAS_DETAIL.montantTotal
      const paye = ATLAS_DETAIL.montantPaye
      return { demandes: 15, total, paye, pctPaye: 68, reste: total - paye, pctReste: 32 }
    }
    const dems = MOCK_DEMANDES.filter((d) => d.client === client.nom)
    const total = dems.reduce((s, d) => s + d.montantTotal, 0)
    const paye = MOCK_PLANS.filter((p) => p.client === client.nom).reduce((s, p) => s + p.montantPaye, 0)
    const pctPaye = total === 0 ? 0 : Math.round((paye / total) * 100)
    return { demandes: dems.length, total, paye, pctPaye, reste: total - paye, pctReste: 100 - pctPaye }
  }, [client, estAtlas])

  const responsables: ResponsableClient[] = useMemo(() => {
    if (!client) return []
    if (estAtlas) return ATLAS_RESPONSABLES
    const noms = RESPONSABLES_PAR_CLIENT[client.nom] ?? [client.responsable]
    return noms.map((nom, i) => ({
      nom,
      principal: i === 0,
      fonction: '—',
      telephone: '—',
      email: '—',
      actif: true,
    }))
  }, [client, estAtlas])

  const documents = useMemo(() => (estAtlas ? ATLAS_DOCUMENTS : []), [estAtlas])

  const historique: EvenementClient[] = useMemo(() => {
    if (!client) return []
    if (estAtlas) return ATLAS_HISTORIQUE
    return [
      {
        dateHeure: '14/01/2026 11:00',
        categorie: 'Client' as const,
        texte: 'Création du client',
        utilisateur: 'Sarah Cohen',
        commentaire: '—',
      },
    ]
  }, [client, estAtlas])

  if (!client || !kpis) {
    return (
      <div className="app-page responsables-page">
        <div className="alert alert-danger">Client introuvable.</div>
        <Link to="/clients" className="btn-ghost" style={{ alignSelf: 'flex-start' }}>
          Retour à la liste
        </Link>
      </div>
    )
  }

  const telechargerDoc = (nom: string) => {
    const url = URL.createObjectURL(
      new Blob([`Document de démonstration : ${nom}`], { type: 'text/plain;charset=utf-8' }),
    )
    const a = document.createElement('a')
    a.href = url
    a.download = nom
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>
            {client.nom}{' '}
            <span className={client.statut === 'Actif' ? 'pill ok' : 'pill wt'}>
              ● {client.statut}
            </span>
          </h1>
          <p>
            {client.code} · {client.activite || '—'}
          </p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={() => setEditOpen(true)}>
            Modifier
          </button>
          {client.statut !== 'Archivé' && (
            <button
              type="button"
              className="btn-ghost"
              onClick={() => setConfirmArchive(true)}
            >
              Archiver
            </button>
          )}
          <Link to={`/demandes/nouveau?client=${encodeURIComponent(client.nom)}`} className="btn-primary">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Nouvelle demande
          </Link>
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
            <div className="kpi-num">{kpis.demandes}</div>
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
            <div className="kpi-num">{formatMGA(kpis.total)}</div>
            <div className="kpi-label">Montant total</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{kpis.pctPaye} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{formatMGA(kpis.paye)}</div>
            <div className="kpi-label">Montant payé</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">{kpis.pctReste} %</span>
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
            <div className="kpi-num">{formatMGA(kpis.reste)}</div>
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
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={onglet === id}
            className={`tab${onglet === id ? ' tab--active' : ''}`}
            onClick={() => setOnglet(id)}
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
                <b>{client.nom}</b>
              </div>
              <div className="info-item">
                <span>Code</span>
                <b>{client.code}</b>
              </div>
              <div className="info-item">
                <span>Activité</span>
                <b>{client.activite || '—'}</b>
              </div>
              <div className="info-item">
                <span>Contact</span>
                <b>{client.telephone || '—'}</b>
              </div>
              <div className="info-item">
                <span>Email</span>
                <b>{client.email || '—'}</b>
              </div>
              <div className="info-item">
                <span>Statut</span>
                <b>
                  <span className={client.statut === 'Actif' ? 'pill ok' : 'pill wt'}>
                    ● {client.statut}
                  </span>
                </b>
              </div>
              <div className="info-item">
                <span>Nombre de demandes</span>
                <b>{client.nbDemandes}</b>
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
                    {responsables.map((r) => (
                      <tr key={r.nom}>
                        <td>
                          <span className="avatar">{initialesClient(r.nom)}</span>
                          <span className="fw-600">{r.nom}</span>{' '}
                          {r.principal && <span className="pill info">● Principal</span>}
                        </td>
                        <td className="text-secondary">{r.fonction}</td>
                        <td className="text-secondary">{r.telephone}</td>
                        <td className="text-secondary">{r.email}</td>
                        <td>
                          {r.actif ? (
                            <span className="pill ok">● Actif</span>
                          ) : (
                            <span className="pill wt">● Inactif</span>
                          )}
                        </td>
                      </tr>
                    ))}
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
                  {paginer(demandes, pageDemandes, 5).map((d) => (
                    <tr key={d.reference}>
                      <td className="fw-700">{d.reference}</td>
                      <td>{d.service}</td>
                      <td className="text-secondary">{d.dateDebut}</td>
                      <td className="text-secondary">{d.dateFin}</td>
                      <td>
                        <span className={toneStatut(d.statut)}>● {d.statut}</span>
                      </td>
                      <td>
                        <span className={tonePaiement(d.paiement)}>● {d.paiement}</span>
                      </td>
                      <td>
                        <Link to={`/demandes/${d.reference}`} className="lk">
                          Voir ›
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="table-footer">
              <span>
                Affichage de {pageDemandes * 5 + 1} à {Math.min((pageDemandes + 1) * 5, demandes.length)} sur{' '}
                {demandes.length} demandes
              </span>
              <div className="pagination">
                {Array.from({ length: Math.max(1, Math.ceil(demandes.length / 5)) }).map((_, i) => (
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
                  {paginer(paiements, pagePaiements, 5).map((p) => (
                    <tr key={p.reference}>
                      <td className="fw-700">{p.reference}</td>
                      <td className="text-secondary">{p.demandeRef}</td>
                      <td className="text-secondary">{p.date}</td>
                      <td className="cell-number fw-700">{formatMGA(p.montant)}</td>
                      <td>
                        <ModePill mode={p.mode} />
                      </td>
                      <td className="text-secondary">{p.refVirement ?? '—'}</td>
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
                  Affichage de {pagePaiements * 5 + 1} à{' '}
                  {Math.min((pagePaiements + 1) * 5, paiements.length)} sur {paiements.length} paiements
                </span>
                <div className="pagination">
                  {Array.from({ length: Math.max(1, Math.ceil(paiements.length / 5)) }).map((_, i) => (
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
                    <th>Utilisateur</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {documents.map((doc) => (
                    <tr key={doc.nom}>
                      <td className="fw-600">{doc.nom}</td>
                      <td className="text-secondary">{doc.type}</td>
                      <td className="text-secondary">{doc.demande ?? '—'}</td>
                      <td className="text-secondary">{doc.taille}</td>
                      <td className="text-secondary">{doc.dateAjout}</td>
                      <td>{doc.utilisateur}</td>
                      <td>
                        <button type="button" className="btn-link success" onClick={() => telechargerDoc(doc.nom)}>
                          Télécharger
                        </button>
                      </td>
                    </tr>
                  ))}
                  {documents.length === 0 && (
                    <tr>
                      <td colSpan={7} className="text-muted">
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
                  {historique.map((h, i) => (
                    <tr key={i}>
                      <td className="text-secondary">{h.dateHeure}</td>
                      <td>
                        <span className={toneEvenement(h)}>● {h.categorie}</span> {h.texte}
                      </td>
                      <td>{h.utilisateur}</td>
                      <td className="text-secondary">{h.commentaire}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

      <ClientFormModal
        open={editOpen}
        client={client}
        onClose={() => setEditOpen(false)}
        onSave={(payload) => {
          setClient({ ...client, ...payload })
          setEditOpen(false)
        }}
      />

      <ConfirmDialog
        open={confirmArchive}
        titre="Archiver ce client ?"
        message={`« ${client.nom} » sera masqué des listes courantes. Ses demandes et son historique sont conservés.`}
        confirmer="Archiver"
        ton="danger"
        onAnnuler={() => setConfirmArchive(false)}
        onConfirmer={() => {
          setConfirmArchive(false)
          setClient({ ...client, statut: 'Archivé' })
        }}
      />
    </div>
  )
}
