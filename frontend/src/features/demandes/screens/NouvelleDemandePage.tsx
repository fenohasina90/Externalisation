import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { DemandeForm } from '../components'
import type { DemandeFormValues } from '../data/mockDemandes'
import { DEMANDE_FORM_VIDE, versDateFR } from '../data/mockDemandes'
import { DEMANDE_OPTIONS, RESPONSABLES_PAR_CLIENT } from '../data/mockDemandes'

/**
 * Nouvelle demande de service — étape 1 : interface seule.
 * Le statut initial est « En attente », le tarif est figé à la création.
 * Présélection du client via ?client=Nom (depuis la fiche client).
 */
export function NouvelleDemandePage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()

  const initial = useMemo<DemandeFormValues>(() => {
    const clientParam = params.get('client')
    if (clientParam && (DEMANDE_OPTIONS.clients as string[]).includes(clientParam)) {
      const premiers = RESPONSABLES_PAR_CLIENT[clientParam] ?? []
      return { ...DEMANDE_FORM_VIDE, client: clientParam, responsable: premiers[0] ?? '' }
    }
    return DEMANDE_FORM_VIDE
  }, [params])

  const [apercu, setApercu] = useState<DemandeFormValues>(initial)

  const handleSubmit = (v: DemandeFormValues) => {
    // Étape 2 : POST /api/demandes puis redirection vers le détail.
    alert(
      `Demande créée (démonstration) : ${v.client} — ${v.service}, ${v.montantTotal.toLocaleString('fr-FR')} MGA, statut En attente.`,
    )
    navigate('/demandes')
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Nouvelle demande de service</h1>
          <p>Le statut initial de la demande est « En attente ».</p>
        </div>
      </header>

      <div className="split-layout">
        <section className="card card-pad">
          <DemandeForm
            key={initial.client}
            initial={initial}
            submitLabel="Créer la demande"
            onCancel={() => navigate('/demandes')}
            onSubmit={handleSubmit}
            avecDocuments
            onChange={setApercu}
          />
        </section>

        <aside className="card card-pad recap-card">
          <h3>Récapitulatif</h3>
          <div className="rc">
            <span>Service</span>
            <strong>{apercu.service || '—'}</strong>
          </div>
          <div className="rc">
            <span>Montant total</span>
            <strong>{apercu.montantTotal.toLocaleString('fr-FR')} MGA</strong>
          </div>
          <div className="nt" style={{ marginTop: 12 }}>
            <strong>Montant figé</strong> : les échéances, paiements et rapports
            s&apos;appuient sur ce montant, même si le tarif du service change ensuite.
          </div>
          <p className="form-help" style={{ marginTop: 8 }}>
            Période : {apercu.debutService ? versDateFR(apercu.debutService) : '—'} →{' '}
            {apercu.finService ? versDateFR(apercu.finService) : '—'}
          </p>
        </aside>
      </div>
    </div>
  )
}
