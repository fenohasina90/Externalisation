import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { MOCK_PLANS, formatMGA, type ModePaiement } from '../data/mockPaiements'

/**
 * Enregistrer un paiement — étape 1 : interface seule.
 * Présélection via ?demande=REF&echeance=N depuis l'échéancier.
 */
export function NouveauPaiementPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const plansOuverts = useMemo(() => MOCK_PLANS.filter((p) => p.montantTotal - p.montantPaye > 0), [])

  const [demandeRef, setDemandeRef] = useState(
    () => params.get('demande') ?? plansOuverts[0]?.demandeRef ?? '',
  )
  const plan = plansOuverts.find((p) => p.demandeRef === demandeRef) ?? plansOuverts[0]
  const echeancesOuvertes = useMemo(
    () => (plan ? plan.echeances.filter((e) => e.montantAttendu - e.montantPaye > 0) : []),
    [plan],
  )

  const [echeance, setEcheance] = useState(() => {
    const n = Number(params.get('echeance'))
    return Number.isFinite(n) && n > 0 ? n : 0
  })
  const [datePaiement, setDatePaiement] = useState('2026-10-04')
  const [montant, setMontant] = useState(100000)
  const [mode, setMode] = useState<ModePaiement>('Virement')
  const [refVirement, setRefVirement] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [error, setError] = useState<string | null>(null)

  const echeanceChoisie =
    echeancesOuvertes.find((e) => e.numero === echeance) ?? echeancesOuvertes[0] ?? null
  const attendu = echeanceChoisie?.montantAttendu ?? 0
  const dejaPaye = echeanceChoisie?.montantPaye ?? 0
  const reste = attendu - dejaPaye
  const apresPaiement = reste - montant

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!plan || !echeanceChoisie) {
      setError('Sélectionnez une demande et une échéance à régler')
      return
    }
    if (montant <= 0) {
      setError('Le montant doit être positif')
      return
    }
    if (montant > reste) {
      setError(`Le montant ne doit pas dépasser le montant restant (${formatMGA(reste)})`)
      return
    }
    if (mode === 'Virement' && !refVirement.trim()) {
      setError('La référence du virement est obligatoire pour un virement')
      return
    }
    alert(
      `Paiement enregistré (démonstration) : ${formatMGA(montant)} pour ${plan.demandeRef}, échéance n° ${echeanceChoisie.numero} (${mode}).`,
    )
    navigate('/paiements')
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Enregistrer un paiement</h1>
          <p>Saisissez un règlement pour une échéance d&apos;une demande.</p>
        </div>
      </header>

      <div className="split-layout">
        <section className="card card-pad">
          <form onSubmit={handleSubmit}>
            {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
            )}

            <div className="form-grid">
              <label className="field">
                Demande <span className="required">*</span>
                <select
                  value={plan?.demandeRef ?? ''}
                  onChange={(e) => {
                    setDemandeRef(e.target.value)
                    setEcheance(0)
                  }}
                  required
                >
                  {plansOuverts.map((p) => (
                    <option key={p.demandeRef} value={p.demandeRef}>
                      {p.demandeRef} — {p.client}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                Échéance <span className="required">*</span>
                <select
                  value={echeanceChoisie?.numero ?? 0}
                  onChange={(e) => setEcheance(Number(e.target.value))}
                  required
                >
                  {echeancesOuvertes.map((ech) => (
                    <option key={ech.numero} value={ech.numero}>
                      N° {ech.numero} – {ech.dateEcheance}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                Date du paiement <span className="required">*</span>
                <input
                  type="date"
                  value={datePaiement}
                  onChange={(e) => setDatePaiement(e.target.value)}
                  required
                />
              </label>

              <label className="field">
                Montant (MGA) <span className="required">*</span>
                <input
                  type="number"
                  min={1}
                  value={montant}
                  onChange={(e) => setMontant(Number(e.target.value))}
                  required
                />
                <span className="form-help">
                  Le montant ne doit pas dépasser le montant restant ({formatMGA(reste)}).
                </span>
              </label>

              <div className="field">
                <span className="field-label">
                  Mode de paiement <span className="required">*</span>
                </span>
                <div className="rd-group" role="radiogroup" aria-label="Mode de paiement">
                  {(['Espèces', 'Virement'] as ModePaiement[]).map((m) => (
                    <label key={m} className={`rd${mode === m ? ' on' : ''}`}>
                      <input
                        type="radio"
                        name="mode"
                        value={m}
                        checked={mode === m}
                        onChange={() => setMode(m)}
                      />
                      {m}
                    </label>
                  ))}
                </div>
              </div>

              <label className="field">
                Référence du virement {mode === 'Virement' && <span className="required">*</span>}
                <input
                  value={refVirement}
                  onChange={(e) => setRefVirement(e.target.value)}
                  placeholder="Ex. VIR-2026-1004"
                  required={mode === 'Virement'}
                />
                <span className="form-help">Obligatoire pour un virement.</span>
              </label>

              <div className="field form-grid__full">
                <span className="field-label">Justificatif</span>
                <div className="dz" role="button" tabIndex={0} aria-label="Ajouter un justificatif">
                  <div>↑</div>
                  <div>
                    <strong className="lk">Parcourir</strong> ou glisser un fichier ici
                  </div>
                  <small>PDF, JPG ou PNG</small>
                </div>
              </div>

              <label className="field form-grid__full">
                Commentaire
                <textarea
                  value={commentaire}
                  onChange={(e) => setCommentaire(e.target.value)}
                  placeholder="Information complémentaire (facultatif)"
                  rows={3}
                  style={{ height: 92 }}
                />
              </label>
            </div>

            <footer className="modal-footer" style={{ borderTop: 'none', padding: '16px 0 0' }}>
              <button type="button" className="btn-ghost" onClick={() => navigate('/paiements')}>
                Annuler
              </button>
              <button type="submit" className="btn-primary">
                Enregistrer le paiement
              </button>
            </footer>
          </form>
        </section>

        <aside className="card card-pad recap-card">
          <h3>Récapitulatif</h3>
          <div className="rc">
            <span>Client</span>
            <strong>{plan?.client ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Demande</span>
            <strong>{plan?.demandeRef ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Échéance</span>
            <strong>
              {echeanceChoisie ? `N° ${echeanceChoisie.numero} - ${echeanceChoisie.dateEcheance}` : '—'}
            </strong>
          </div>
          <div className="rc">
            <span>Montant attendu</span>
            <strong>{formatMGA(attendu)}</strong>
          </div>
          <div className="rc">
            <span>Déjà payé</span>
            <strong style={{ color: 'var(--success)' }}>{formatMGA(dejaPaye)}</strong>
          </div>
          <div className="rc">
            <span>Reste à payer</span>
            <strong style={{ color: 'var(--danger)' }}>{formatMGA(reste)}</strong>
          </div>
          {echeanceChoisie && (
            <div className="nt" style={{ marginTop: 12 }}>
              <strong>Après ce paiement</strong> :{' '}
              {apresPaiement <= 0
                ? `échéance n° ${echeanceChoisie.numero} soldée (0 MGA).`
                : `reste ${formatMGA(apresPaiement)} sur l'échéance n° ${echeanceChoisie.numero}.`}
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}
