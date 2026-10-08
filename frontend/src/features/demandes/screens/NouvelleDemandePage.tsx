import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DEMANDE_FORM_VIDE, DemandeForm, versDateFR, type DemandeFormValues } from '../components'

/**
 * Nouvelle demande de service — étape 1 : interface seule.
 * Le statut initial est « En attente », le tarif est figé à la création.
 */
export function NouvelleDemandePage() {
  const navigate = useNavigate()
  const [apercu, setApercu] = useState<DemandeFormValues>(DEMANDE_FORM_VIDE)

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
            initial={DEMANDE_FORM_VIDE}
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
