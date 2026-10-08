import { Link, useParams } from 'react-router-dom'
import { ModePill, StatutEcheancePill } from '../components'
import { MOCK_HISTORIQUE, MOCK_PLANS, formatMGA } from '../data/mockPaiements'

/** Échéancier d'une demande : résumé, échéances et paiements enregistrés. */
export function EcheancierDetailPage() {
  const { reference } = useParams<{ reference: string }>()
  const plan = MOCK_PLANS.find((p) => p.demandeRef === reference)
  const paiements = MOCK_HISTORIQUE.filter((h) => h.demandeRef === reference)

  if (!plan) {
    return (
      <div className="app-page responsables-page">
        <div className="alert alert-danger">Échéancier introuvable.</div>
        <Link to="/paiements" className="btn-ghost" style={{ alignSelf: 'flex-start' }}>
          Retour à la liste
        </Link>
      </div>
    )
  }

  const reste = plan.montantTotal - plan.montantPaye
  const progression = plan.montantTotal === 0 ? 0 : Math.round((plan.montantPaye / plan.montantTotal) * 100)

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Échéancier · {plan.demandeRef}</h1>
          <p>
            {plan.client} — suivi des échéances de la demande.
          </p>
        </div>
        <div className="actions">
          <Link to="/paiements" className="btn-ghost">
            Retour à la liste
          </Link>
          <Link to={`/paiements/nouveau?demande=${plan.demandeRef}`} className="btn-primary">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Enregistrer un paiement
          </Link>
        </div>
      </header>

      <section className="card card-pad">
        <div className="info-grid info-grid--5">
          <div className="info-item">
            <span>Client</span>
            <b>{plan.client}</b>
          </div>
          <div className="info-item">
            <span>Demande</span>
            <b>{plan.demandeRef}</b>
          </div>
          <div className="info-item">
            <span>Montant total</span>
            <b>{formatMGA(plan.montantTotal)}</b>
          </div>
          <div className="info-item">
            <span>Montant payé</span>
            <b style={{ color: 'var(--success)' }}>{formatMGA(plan.montantPaye)}</b>
          </div>
          <div className="info-item">
            <span>Reste à payer</span>
            <b style={{ color: 'var(--danger)' }}>{formatMGA(reste)}</b>
          </div>
        </div>
        <div className="progression">
          <div className="trk" role="progressbar" aria-valuenow={progression} aria-valuemin={0} aria-valuemax={100}>
            <i style={{ width: `${progression}%` }} />
          </div>
        </div>
      </section>

      <section className="card">
        <div className="card-pad" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>Échéances</h3>
        </div>
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
                {plan.echeances.map((e) => {
                  const resteEch = e.montantAttendu - e.montantPaye
                  return (
                    <tr key={e.numero}>
                      <td>{e.numero}</td>
                      <td className="text-secondary">{e.dateEcheance}</td>
                      <td className="cell-number">{formatMGA(e.montantAttendu)}</td>
                      <td className="cell-number">{formatMGA(e.montantPaye)}</td>
                      <td className="cell-number">{formatMGA(resteEch)}</td>
                      <td>
                        <StatutEcheancePill attendu={e.montantAttendu} paye={e.montantPaye} />
                      </td>
                      <td>
                        {resteEch > 0 ? (
                          <Link
                            to={`/paiements/nouveau?demande=${plan.demandeRef}&echeance=${e.numero}`}
                            className="btn-link success"
                          >
                            Enregistrer un paiement
                          </Link>
                        ) : (
                          <span className="text-muted">—</span>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="table-footer">
            <span>Une échéance peut être réglée en plusieurs paiements.</span>
          </div>
        </div>
      </section>

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
                  <th>Échéance</th>
                  <th>Montant</th>
                  <th>Mode</th>
                  <th>Réf. virement</th>
                  <th>Utilisateur</th>
                </tr>
              </thead>
              <tbody>
                {paiements.map((p) => (
                  <tr key={p.reference}>
                    <td className="text-secondary">{p.date}</td>
                    <td>N° {p.echeance}</td>
                    <td className="cell-number">{formatMGA(p.montant)}</td>
                    <td>
                      <ModePill mode={p.mode} />
                    </td>
                    <td className="text-secondary">{p.refVirement ?? '—'}</td>
                    <td>{p.utilisateur}</td>
                  </tr>
                ))}
                {paiements.length === 0 && (
                  <tr>
                    <td colSpan={6} className="text-muted">
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
