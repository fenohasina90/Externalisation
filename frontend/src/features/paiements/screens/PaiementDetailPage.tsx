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

const TYPE_LABEL: Record<string, string> = {
  GENERALE: 'Facture générale validée',
  PRESTATION: 'Prestation (facture validée)',
  DEBOURS: 'Débours (facture validée)',
  ABONNEMENT_MENSUEL: 'Abonnement mensuel',
}

function formatMois(mois: string | null | undefined): string {
  if (!mois) return '—'
  const parsed = new Date(mois)
  if (Number.isNaN(parsed.getTime())) return mois
  const texte = parsed.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })
  return texte.charAt(0).toUpperCase() + texte.slice(1)
}

/** Détail d'un paiement — reprend complètement et précisément le reçu PDF. */
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

  const estDebours = paiement.typeFacture === 'DEBOURS'
  const estAbonnement = paiement.typeFacture === 'ABONNEMENT_MENSUEL'
  const objetLabel = estDebours ? 'Désignation du débours' : 'Nom du service'
  const typeFactureLabel = paiement.typeFacture
    ? (TYPE_LABEL[paiement.typeFacture] ?? paiement.typeFacture)
    : '—'
  const estPlan = paiement.numeroEcheance !== null && paiement.dateEcheance !== null
  const libelleEcheance = estPlan
    ? `N° ${paiement.numeroEcheance} — échéance du ${formatDate(paiement.dateEcheance)}`
    : null
  const resteAPayer = paiement.resteAPayerFacture ?? 0

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
            <span>Référence paiement</span>
            <b>{paiement.referencePaiement}</b>
          </div>
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
          <h3>Service et facture</h3>
          <div className="rc">
            <span>{objetLabel}</span>
            <b>{paiement.nomService ?? '—'}</b>
          </div>
          <div className="rc">
            <span>Type de facture</span>
            <b>{typeFactureLabel}</b>
          </div>
          <div className="rc">
            <span>Référence facture</span>
            <b>{paiement.referenceFactureValidee ?? '—'}</b>
          </div>
          {estAbonnement && (
            <>
              <div className="rc">
                <span>Mois facturé</span>
                <b>{formatMois(paiement.moisFacture)}</b>
              </div>
              <div className="rc">
                <span>Date d&apos;émission</span>
                <b>{formatDate(paiement.dateFactureValidee)}</b>
              </div>
            </>
          )}
          {!estAbonnement && paiement.dateFactureValidee && (
            <div className="rc">
              <span>Date d&apos;émission</span>
              <b>{formatDate(paiement.dateFactureValidee)}</b>
            </div>
          )}
          {libelleEcheance && (
            <div className="rc">
              <span>Échéance réglée</span>
              <b>{libelleEcheance}</b>
            </div>
          )}
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
          {paiement.referenceVirement && (
            <div className="rc">
              <span>Référence virement</span>
              <b>{paiement.referenceVirement}</b>
            </div>
          )}
          {paiement.banque && (
            <div className="rc">
              <span>Banque</span>
              <b>{paiement.banque}</b>
            </div>
          )}
          {paiement.numeroCheque && (
            <div className="rc">
              <span>N° de chèque</span>
              <b>{paiement.numeroCheque}</b>
            </div>
          )}
          {paiement.pieceJustificative && (
            <div className="rc">
              <span>Pièce justificative</span>
              <b>{paiement.pieceJustificative}</b>
            </div>
          )}
          <div className="rc">
            <span>Commentaire</span>
            <b>{paiement.commentaire?.trim() ? paiement.commentaire : '—'}</b>
          </div>
        </div>

        <div className="card card-pad">
          <h3>Montants de la facture</h3>
          <div className="rc">
            <span>Montant de la facture validée</span>
            <b>{formatMontantMGA(paiement.montantFactureValidee)}</b>
          </div>
          <div className="rc">
            <span>Montant déjà payé</span>
            <b>{formatMontantMGA(paiement.montantPayeSurFacture)}</b>
          </div>
          <div className="rc">
            <span>Montant reçu (ce paiement)</span>
            <b>{formatMontantMGA(paiement.montantPaye)}</b>
          </div>
          <div className="rc">
            <span>Reste à payer</span>
            <b style={resteAPayer > 0 ? { color: 'var(--danger)' } : undefined}>
              {formatMontantMGA(paiement.resteAPayerFacture)}
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
