import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { http } from '@/api/http'
import { telechargerBlob } from '@/utils/telechargement'
import { formatDate, formatMontantMGA } from '@/utils/format'
import type { Facture, FactureValidee } from '../types'

export type FactureApercu =
  | { kind: 'proforma'; facture: Facture }
  | { kind: 'validee'; facture: FactureValidee }

/**
 * Aperçu complet d'une facture (proforma ou validée) dans une modale :
 * références, lignes avec désignation séparée, suivi des paiements (validée),
 * bouton « Télécharger le PDF ».
 */
export function FactureApercuModal({
  apercu,
  onClose,
}: {
  apercu: FactureApercu | null
  onClose: () => void
}) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  if (!apercu) {
    return null
  }

  const estProforma = apercu.kind === 'proforma'

  const referenceTitre = estProforma ? apercu.facture.referenceFacture : apercu.facture.referenceFactureValidee
  const telechargerPdf = async () => {
    const url = estProforma
      ? `/factures/${apercu.facture.id}/document/proforma`
      : `/factures-validees/${apercu.facture.id}/document`
    const { data } = await http.get<Blob>(url, { responseType: 'blob' })
    telechargerBlob(data, `${referenceTitre.replace('/', '-')}.pdf`)
  }

  const montantTotal = estProforma
    ? (apercu.facture.montantValidee ?? apercu.facture.montantProforma ?? apercu.facture.montant)
    : apercu.facture.montant
  const montantPaye = estProforma ? 0 : apercu.facture.montantPaye
  const resteAPayer = estProforma ? montantTotal : apercu.facture.resteAPayer
  const taux = montantTotal > 0 ? Math.min(100, Math.round((montantPaye / montantTotal) * 100)) : 0

  const pill = estProforma
    ? apercu.facture.statut === 'VALIDEE'
      ? { label: 'Validée', cls: 'pill ok' }
      : { label: 'Proforma', cls: 'pill wt' }
    : apercu.facture.type === 'DEBOURS'
      ? { label: 'Débours (validée)', cls: 'pill info' }
      : apercu.facture.type === 'ABONNEMENT_MENSUEL'
        ? { label: 'Abonnement (validée)', cls: 'pill ok' }
        : { label: 'Externalisation (validée)', cls: 'pill ok' }

  const libelleLigneFallback = !estProforma ? apercu.facture.libelleLigne : null
  type LigneAffichee = {
    id: string | number
    designation: string
    referenceDemande: string | null
    montant: number
    secondary?: string | null
  }
  const lignes: LigneAffichee[] = estProforma
    ? apercu.facture.lignes.map((l) => ({
        id: `${apercu.facture.id}-${l.ligneServiceId}`,
        designation: l.nomService ?? l.libelleLigne ?? '—',
        referenceDemande: apercu.facture.referenceDemande,
        montant: l.tarif,
        secondary: l.libelleLigne && l.libelleLigne !== l.nomService ? l.libelleLigne : null,
      }))
    : apercu.facture.lignes.length > 0
      ? apercu.facture.lignes.map((l) => ({
          id: `${apercu.facture.id}-${l.id}`,
          designation: l.designation,
          referenceDemande: l.referenceDemande,
          montant: l.montant,
        }))
      : [{
          id: `${apercu.facture.id}-unique`,
          designation: apercu.facture.nomService ?? libelleLigneFallback ?? 'Facture',
          referenceDemande: apercu.facture.referenceDemande,
          montant: montantTotal,
          secondary: libelleLigneFallback && apercu.facture.nomService ? libelleLigneFallback : null,
        }]

  const body = (
    <div
      className="modal-backdrop modal-backdrop--center"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="modal-card"
        style={{ maxWidth: 760 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Aperçu de la facture"
      >
        <header className="modal-header">
          <h3 className="modal-title">
            {referenceTitre}{' '}
            <span className={pill.cls} style={{ marginLeft: 8 }}>● {pill.label}</span>
          </h3>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Références */}
          <section>
            <h4 style={{ margin: '0 0 6px', fontSize: 13, color: 'var(--texte-secondaire, #6b7280)', textTransform: 'uppercase' }}>
              Références
            </h4>
            <div className="info-grid">
              {estProforma ? (
                <>
                  <div className="info-item">
                    <span>Réf. proforma</span>
                    <b>{apercu.facture.referenceFacture}</b>
                  </div>
                  <div className="info-item">
                    <span>Réf. validée</span>
                    <b>{apercu.facture.referenceFactureValidee ?? '— (non validée)'}</b>
                  </div>
                  <div className="info-item">
                    <span>Mode de facturation</span>
                    <b>{apercu.facture.modeFacturation === 'UNIQUE' ? 'Unique' : 'Par service'}</b>
                  </div>
                  <div className="info-item">
                    <span>Montant proforma</span>
                    <b>{formatMontantMGA(apercu.facture.montantProforma ?? apercu.facture.montant)}</b>
                  </div>
                  {apercu.facture.statut === 'VALIDEE' && apercu.facture.montantValidee != null && (
                    <div className="info-item">
                      <span>Montant validée</span>
                      <b>{formatMontantMGA(apercu.facture.montantValidee)}</b>
                    </div>
                  )}
                  {apercu.facture.valideLe && (
                    <div className="info-item">
                      <span>Validée par</span>
                      <b>{apercu.facture.valideParNom ?? '—'} le {formatDate(apercu.facture.valideLe.slice(0, 10))}</b>
                    </div>
                  )}
                </>
              ) : (
                <>
                  <div className="info-item">
                    <span>Réf. proforma</span>
                    <b>{apercu.facture.referenceFactureProforma ?? '— (sans proforma)'}</b>
                  </div>
                  <div className="info-item">
                    <span>Réf. validée</span>
                    <b>{apercu.facture.referenceFactureValidee}</b>
                  </div>
                  {apercu.facture.type === 'ABONNEMENT_MENSUEL' && (
                    <>
                      <div className="info-item">
                        <span>Facture générale</span>
                        <b>{apercu.facture.referenceFactureGenerale ?? '—'}</b>
                      </div>
                      <div className="info-item">
                        <span>Mois facturé</span>
                        <b>
                          {apercu.facture.mois
                            ? new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' })
                                .format(new Date(`${apercu.facture.mois}-01T00:00:00`))
                            : '—'}
                        </b>
                      </div>
                    </>
                  )}
                  <div className="info-item">
                    <span>Validée le</span>
                    <b>{apercu.facture.valideLe ? formatDate(apercu.facture.valideLe.slice(0, 10)) : '—'}</b>
                  </div>
                  <div className="info-item">
                    <span>Validée par</span>
                    <b>{apercu.facture.valideParNom ?? '—'}</b>
                  </div>
                </>
              )}
              <div className="info-item">
                <span>Demande</span>
                <b>{estProforma ? apercu.facture.referenceDemande : apercu.facture.referenceDemande}</b>
              </div>
            </div>
          </section>

          {/* Lignes */}
          <section>
            <h4 style={{ margin: '0 0 6px', fontSize: 13, color: 'var(--texte-secondaire, #6b7280)', textTransform: 'uppercase' }}>
              Détail de la facture
            </h4>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Désignation</th>
                    <th>Demande</th>
                    <th>Montant</th>
                  </tr>
                </thead>
                <tbody>
                  {lignes.map((l) => (
                    <tr key={l.id}>
                      <td>
                        <div className="fw-600">{l.designation}</div>
                        {l.secondary && (
                          <span className="text-secondary" style={{ fontSize: 12 }}>{l.secondary}</span>
                        )}
                      </td>
                      <td className="text-secondary">{l.referenceDemande ?? '—'}</td>
                      <td>{formatMontantMGA(l.montant)}</td>
                    </tr>
                  ))}
                  <tr>
                    <th colSpan={2} style={{ textAlign: 'right' }}>Total à payer</th>
                    <th>{formatMontantMGA(montantTotal)}</th>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          {/* Suivi des paiements (validée uniquement) */}
          {!estProforma && (
            <section>
              <h4 style={{ margin: '0 0 6px', fontSize: 13, color: 'var(--texte-secondaire, #6b7280)', textTransform: 'uppercase' }}>
                Suivi des paiements
              </h4>
              <div className="info-grid">
                <div className="info-item">
                  <span>Montant validée</span>
                  <b>{formatMontantMGA(montantTotal)}</b>
                </div>
                <div className="info-item">
                  <span>Payé</span>
                  <b style={{ color: 'var(--success)' }}>{formatMontantMGA(montantPaye)}</b>
                </div>
                <div className="info-item">
                  <span>Reste à payer</span>
                  <b style={{ color: resteAPayer > 0 ? 'var(--danger)' : 'var(--success)' }}>
                    {formatMontantMGA(resteAPayer)}
                  </b>
                </div>
              </div>
              <div className="trk" role="progressbar" aria-valuenow={taux} aria-valuemin={0} aria-valuemax={100} style={{ marginTop: 8 }}>
                <i style={{ width: `${taux}%` }} />
              </div>
            </section>
          )}
        </div>

        <footer className="modal-footer" style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', padding: '12px 20px' }}>
          <button type="button" className="btn-ghost" onClick={onClose}>
            Fermer
          </button>
          <button type="button" className="btn-primary" onClick={() => void telechargerPdf()}>
            <svg className="sv" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Télécharger le PDF
          </button>
        </footer>
      </div>
    </div>
  )

  return createPortal(body, document.body)
}
