import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Spinner } from '@/components/ui'
import { Can } from '@/components/shared/Can'
import { useApi } from '@/hooks/useApi'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { ModePill } from '../components'
import { paiementsApi } from '../services/paiements.service'
import type { PaiementDetail } from '../types'

/** Détail d'un paiement — reprend les informations du reçu PDF. */
export function PaiementDetailPage() {
  const { reference } = useParams<{ reference: string }>()
  const { data: paiement, loading, error } = useApi<PaiementDetail | null>(
    () => (!reference ? Promise.resolve(null) : paiementsApi.getByReference(reference)),
    [reference],
  )
  const [erreurRecu, setErreurRecu] = useState<string | null>(null)

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  if (loading) {
    return (
      <div className="app-page responsables-page">
        <Spinner label="Chargement du paiement…" />
      </div>
    )
  }

  if (error || !paiement) {
    return (
      <div className="app-page responsables-page">
        <div className="alert alert-danger">{error ?? 'Paiement introuvable.'}</div>
        <Link to="/paiements/historique" className="btn-ghost" style={{ alignSelf: 'flex-start' }}>
          Retour à l&apos;historique
        </Link>
      </div>
    )
  }

  const telechargerRecu = async (): Promise<void> => {
    setErreurRecu(null)
    try {
      const recu = await paiementsApi.telechargerRecu(paiement.id)
      telechargerBlob(recu, `recu-${paiement.referencePaiement}.pdf`)
    } catch {
      setErreurRecu(`Téléchargement du reçu ${paiement.referencePaiement} impossible.`)
    }
  }

  const libelleEcheance =
    paiement.numeroEcheance === null
      ? '—'
      : `N° ${paiement.numeroEcheance} — ${formatDate(paiement.dateEcheance)}`

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Reçu de paiement · {paiement.referencePaiement}</h1>
          <p>
            {paiement.nomSociete ?? 'Sans société'} — demande {paiement.referenceDemande}.
          </p>
        </div>
        <div className="actions">
          <Link to="/paiements/historique" className="btn-ghost">
            Retour à l&apos;historique
          </Link>
          <Can permission="DOCUMENT_PAIEMENT_VIEW">
            <button type="button" className="btn-primary" onClick={() => void telechargerRecu()}>
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
              </svg>
              Télécharger le reçu
            </button>
          </Can>
        </div>
      </header>

      {erreurRecu && (
        <div className="alert alert-danger" role="alert">
          {erreurRecu}
        </div>
      )}

      <section className="recu-hero">
        <div className="recu-hero__label">Montant reçu</div>
        <div className="recu-hero__montant">{formatMontantMGA(paiement.montantPaye)}</div>
      </section>

      <section className="detail-cards">
        <div className="card card-pad">
          <h3>Client et demande</h3>
          <div className="rc">
            <span>Client</span>
            <b>{paiement.nomSociete ?? '—'}</b>
          </div>
          <div className="rc">
            <span>Demande</span>
            <Link
              to={`/demandes/${encodeURIComponent(paiement.referenceDemande)}`}
              className="btn-link"
            >
              {paiement.referenceDemande}
            </Link>
          </div>
        </div>

        <div className="card card-pad">
          <h3>Service et échéance</h3>
          <div className="rc">
            <span>Service</span>
            <b>{paiement.nomService ?? '—'}</b>
          </div>
          <div className="rc">
            <span>Échéance</span>
            <b>{libelleEcheance}</b>
          </div>
        </div>

        <div className="card card-pad">
          <h3>Règlement</h3>
          <div className="rc">
            <span>Date de paiement</span>
            <b>{formatDate(paiement.datePaiement)}</b>
          </div>
          <div className="rc">
            <span>Mode de paiement</span>
            <b>
              <ModePill code={paiement.libelleMode} />
            </b>
          </div>
          <div className="rc">
            <span>Référence virement</span>
            <b>{paiement.referenceVirement ?? '—'}</b>
          </div>
        </div>

        <div className="card card-pad">
          <h3>Montants de l&apos;échéance</h3>
          <div className="rc">
            <span>Montant attendu</span>
            <b>{formatMontantMGA(paiement.montantAttendu)}</b>
          </div>
          <div className="rc">
            <span>Montant payé</span>
            <b>{formatMontantMGA(paiement.montantPayeEcheance)}</b>
          </div>
          <div className="rc">
            <span>Reste à payer</span>
            <b style={paiement.resteAPayer ? { color: 'var(--danger)' } : undefined}>
              {formatMontantMGA(paiement.resteAPayer)}
            </b>
          </div>
        </div>
      </section>

      {paiement.montantEnLettres && (
        <section className="recu-lettres">
          <span>Arrêté le présent reçu à la somme de</span>
          <b>{paiement.montantEnLettres.toUpperCase()} ARIARY</b>
        </section>
      )}

      <section className="card card-pad recu-pied">
        <div className="info-item">
          <span>Enregistré par</span>
          <b>{paiement.createdByName ?? '—'}</b>
        </div>
        <Link
          to={`/paiements/echeanciers/${encodeURIComponent(paiement.referenceDemande)}`}
          className="btn-ghost"
        >
          Voir l&apos;échéancier
        </Link>
      </section>
    </div>
  )
}
