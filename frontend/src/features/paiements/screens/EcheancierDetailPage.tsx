import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Spinner } from '@/components/ui'
import { Can } from '@/components/shared/Can'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { EcheancePill, ModePill, SituationPlanPill, StatutDemandePill } from '../components'
import { useEcheancierDemande } from '../hooks'
import { paiementsApi } from '../services/paiements.service'
import { situationPlan } from '../utils/situation'

/** Échéancier d'une demande : résumé, échéances par service et paiements enregistrés. */
export function EcheancierDetailPage() {
  const { reference } = useParams<{ reference: string }>()
  const { data, loading, error } = useEcheancierDemande(reference)
  const [erreurRecu, setErreurRecu] = useState<string | null>(null)

  const telechargerRecu = async (id: number, referencePaiement: string) => {
    setErreurRecu(null)
    try {
      const recu = await paiementsApi.telechargerRecu(id)
      telechargerBlob(recu, `recu-${referencePaiement}.pdf`)
    } catch {
      setErreurRecu(`Téléchargement du reçu ${referencePaiement} impossible.`)
    }
  }

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  if (loading && !data) {
    return (
      <div className="app-page responsables-page">
        <Spinner label="Chargement de l'échéancier…" />
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="app-page responsables-page">
        <div className="alert alert-danger">{error ?? 'Échéancier introuvable.'}</div>
        <Link to="/paiements/echeanciers" className="btn-ghost" style={{ alignSelf: 'flex-start' }}>
          Retour à la liste
        </Link>
      </div>
    )
  }

  const { demande, plansParLigne, paiements } = data
  const reste = demande.resteAPayer ?? 0
  const montantTotal = demande.montantTotal ?? 0
  const progression = montantTotal === 0 ? 0 : Math.round(((demande.montantPaye / montantTotal) * 100) * 100) / 100

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Échéancier · {demande.referenceDemande}</h1>
          <p>{demande.nomSociete ?? 'Sans société'} — suivi des échéances de la demande.</p>
        </div>
        <div className="actions">
          <Link to="/paiements/echeanciers" className="btn-ghost">
            Retour à la liste
          </Link>
          <Can permission="PAIEMENT_CREATE">
            <Link
              to={`/paiements/nouveau?demande=${encodeURIComponent(demande.referenceDemande)}`}
              className="btn-primary"
            >
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 5v14M5 12h14" />
              </svg>
              Enregistrer un paiement
            </Link>
          </Can>
        </div>
      </header>

      {erreurRecu && (
        <div className="alert alert-danger" role="alert">
          {erreurRecu}
        </div>
      )}

      <section className="card card-pad">
        <div className="info-grid info-grid--5">
          <div className="info-item">
            <span>Client</span>
            <b>{demande.nomSociete ?? '—'}</b>
          </div>
          <div className="info-item">
            <span>Demande</span>
            <b>{demande.referenceDemande}</b>
          </div>
          <div className="info-item">
            <span>Statut</span>
            <b>
              <StatutDemandePill statut={demande.statutActuel ?? '—'} />
            </b>
          </div>
          <div className="info-item">
            <span>Montant total</span>
            <b>{formatMontantMGA(montantTotal)}</b>
          </div>
          <div className="info-item">
            <span>Montant payé</span>
            <b style={{ color: 'var(--success)' }}>{formatMontantMGA(demande.montantPaye)}</b>
          </div>
          <div className="info-item">
            <span>Reste à payer</span>
            <b style={{ color: 'var(--danger)' }}>{formatMontantMGA(reste)}</b>
          </div>
        </div>
        <div className="progression">
          <div
            className="trk"
            role="progressbar"
            aria-valuenow={progression}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <i style={{ width: `${Math.min(progression, 100)}%` }} />
          </div>
        </div>
      </section>

      {plansParLigne.map((ligne) => (
        <section className="card" key={ligne.ligneId}>
          <div
            className="card-pad"
            style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
          >
            <h3 style={{ margin: 0 }}>{ligne.ligneLabel}</h3>
            {ligne.plan ? (
              <span className="text-secondary">
                Plan du {formatDate(ligne.plan.dateDebut)} au {formatDate(ligne.plan.dateFin)}
              </span>
            ) : (
              <span className="text-muted">Aucun plan de paiement</span>
            )}
          </div>
          {ligne.plan ? (
            <div className="table-scroll-container no-global-scroll">
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>N°</th>
                      <th>Date d&apos;échéance</th>
                      <th>Montant attendu</th>
                      <th>Montant payé</th>
                      <th>Reste</th>
                      <th>Statut</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(ligne.plan.echeances ?? []).map((e) => (
                      <tr key={e.id}>
                        <td>{e.numeroEcheance}</td>
                        <td className="text-secondary">{formatDate(e.dateEcheance)}</td>
                        <td className="cell-number">{formatMontantMGA(e.montantAttendu)}</td>
                        <td className="cell-number">{formatMontantMGA(e.montantPaye)}</td>
                        <td className="cell-number">{formatMontantMGA(e.reste)}</td>
                        <td>
                          <EcheancePill statut={e.statut} />
                        </td>
                        <td>
                          {e.reste > 0 ? (
                            <Can permission="PAIEMENT_CREATE">
                              <Link
                                to={`/paiements/nouveau?demande=${encodeURIComponent(demande.referenceDemande)}`}
                                state={{ echeanceId: e.id }}
                                className="btn-link success"
                              >
                                Enregistrer un paiement
                              </Link>
                            </Can>
                          ) : (
                            <span className="text-muted">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="table-footer">
                <span>
                  Situation du service :{' '}
                  <SituationPlanPill situation={situationPlan(ligne.plan.echeances ?? [])} />
                </span>
              </div>
            </div>
          ) : (
            <div className="card-pad text-muted">
              Le plan de paiement de ce service n&apos;est pas encore établi.
            </div>
          )}
        </section>
      ))}

      <section className="card">
        <div className="card-pad" style={{ paddingBottom: 0 }}>
          <h3 style={{ margin: 0 }}>Paiements enregistrés</h3>
        </div>
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Référence</th>
                  <th>Montant</th>
                  <th>Mode</th>
                  <th>Réf. virement</th>
                  <th>Utilisateur</th>
                  <th>Reçu</th>
                </tr>
              </thead>
              <tbody>
                {paiements.map((p) => (
                  <tr key={p.id}>
                    <td className="text-secondary">{formatDate(p.datePaiement)}</td>
                    <td>{p.referencePaiement}</td>
                    <td className="cell-number">{formatMontantMGA(p.montantPaye)}</td>
                    <td>
                      <ModePill code={p.libelleMode} />
                    </td>
                    <td className="text-secondary">{p.referenceVirement ?? '—'}</td>
                    <td>{p.createdByName ?? '—'}</td>
                    <td>
                      <Can permission="DOCUMENT_PAIEMENT_VIEW">
                        <button
                          type="button"
                          className="btn-link success"
                          onClick={() => void telechargerRecu(p.id, p.referencePaiement)}
                        >
                          PDF ›
                        </button>
                      </Can>
                    </td>
                  </tr>
                ))}
                {paiements.length === 0 && (
                  <tr>
                    <td colSpan={7} className="text-muted">
                      Aucun paiement enregistré pour cette demande.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  )
}