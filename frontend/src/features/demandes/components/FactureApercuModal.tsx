import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { http } from '@/api/http'
import { telechargerBlob } from '@/utils/telechargement'
import { formatDate, formatMontantMGA } from '@/utils/format'
import type { Facture, FactureProformaLigne, FactureValidee, FactureValideeLigne } from '../types'

/** Classification de repli pour une ligne persistée sans catégorie/typePrestation. */
function categorieDepuisDesignation(designation: string): string {
  const d = designation.trim().toLowerCase()
  return d.startsWith('débours') || d.startsWith('debours') ? 'DEBOURS' : 'EXTERNALISATION'
}

function typePrestationDepuisDesignation(designation: string): string | null {
  const d = designation.trim().toLowerCase()
  if (d.startsWith('débours') || d.startsWith('debours')) {
    return null
  }
  return d.startsWith('abonnement') ? 'ABONNEMENT_MENSUEL' : 'PRESTATION'
}

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
  /** Facture générale validée en mode « unique » : aucun paiement ne lui est lié. */
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

  // Pour les factures liées au paiement (kindest enfant ou mensuelle), seule la
  // section « Suivi des paiements » est affichée — pas les lignes de désignation
  // ni les rappels.

  // Lignes persistées de la proforma : affichage identique au PDF.
  // La proforma est figée à la génération : consultation affiche toujours
  // les montants de création (avant validation ou après — c'est le document
  // PROFORMA). La facture validée (kind === 'validee') affiche le tarif
  // validé (ligne_service.tarif).
  const lignesProforma = apercu.facture.lignesProforma ?? []
  // Lignes persistées de la facture validée elle-même (facture mensuelle /
  // combinée) : désignation figée à la création — source de vérité quand
  // la facture n'a pas de lignes de proforma (facture du mois).
  const lignesValidee: FactureValideeLigne[] = !estProforma && 'referenceFactureGenerale' in apercu.facture
    ? (apercu.facture.lignes ?? [])
    : []
  const normaliserValidee = (l: (typeof lignesValidee)[number]): FactureProformaLigne => ({
    ordre: null,
    ligneServiceId: l.ligneServiceId,
    designation: l.designation,
    categorie: l.categorie ?? categorieDepuisDesignation(l.designation),
    typePrestation: l.typePrestation ?? typePrestationDepuisDesignation(l.designation),
    pu: l.montant,
    quantite: 1,
    montantHtva: l.montant,
    remise: 0,
    netCommercial: l.montant,
  })
  const principalesValidee = lignesValidee
    .filter((l) => (l.type ?? 'PRINCIPALE').toUpperCase() !== 'RAPPEL')
    .map(normaliserValidee)
  const rappels = lignesValidee
    .filter((l) => (l.type ?? '').toUpperCase() === 'RAPPEL')
    .map(normaliserValidee)
  const lignesAffichage = lignesProforma.length > 0 ? lignesProforma : principalesValidee
  const aboLignes = lignesAffichage.filter((l) => l.typePrestation === 'ABONNEMENT_MENSUEL')
  const prestaLignes = lignesAffichage.filter((l) =>
    l.categorie === 'EXTERNALISATION' && l.typePrestation !== 'ABONNEMENT_MENSUEL')
  const deboursLignes = lignesAffichage.filter((l) => l.categorie === 'DEBOURS')
  const totalHt = lignesAffichage.reduce((acc, l) => acc + l.netCommercial, 0)
  const sousTotalDe = (lignes: typeof lignesAffichage) =>
    lignes.reduce((acc, l) => acc + l.netCommercial, 0)

  const rendLigneProforma = (l: (typeof lignesAffichage)[number], cle = 'pl') => (
    <tr key={`${cle}-${l.ordre ?? 0}-${l.designation}`}>
      <td>{l.designation}</td>
      <td className="cell-number">{formatMontantMGA(l.pu)}</td>
      <td className="cell-number">{String(l.quantite).replace(/\.0+$/, '')}</td>
      <td className="cell-number">{formatMontantMGA(l.montantHtva)}</td>
      <td className="cell-number">{l.remise > 0 ? formatMontantMGA(l.remise) : '-'}</td>
      <td className="cell-number fw-600">{formatMontantMGA(l.netCommercial)}</td>
    </tr>
  )
  const rendBande = (titre: string) => (
    <tr>
      <td colSpan={6} style={{
        background: 'var(--vert-pale, #f0fdf4)',
        borderLeft: '2.5px solid var(--vert-marque, #10b981)',
        fontWeight: 700,
        padding: '4px 10px',
      }}>{titre}</td>
    </tr>
  )
  const rendSousTotal = (titre: string, total: number) => (
    <tr>
      <th colSpan={3} style={{ textAlign: 'right' }}>{titre}</th>
      <th>{formatMontantMGA(total)}</th>
      <th>-</th>
      <th>{formatMontantMGA(total)}</th>
    </tr>
  )
  const rendBandeRappel = (titre: string) => (
    <tr>
      <td colSpan={6} style={{
        background: '#FEF3C7',
        borderLeft: '2.5px solid #F59E0B',
        color: '#B45309',
        fontWeight: 700,
        padding: '4px 10px',
      }}>{titre}</td>
    </tr>
  )
  const body = (
    <div
      className="modal-backdrop modal-backdrop--center"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="modal-card"
        style={{ maxWidth: 980 }}
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

          {/* Section A — objet de la présente facture */}
          {
            <section>
              <h4 style={{ margin: '0 0 6px', fontSize: 13, color: 'var(--texte-secondaire, #6b7280)', textTransform: 'uppercase' }}>
                Cette facture — lignes persistées
              </h4>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Désignation</th>
                      <th>PU (Ariary)</th>
                      <th>Qté</th>
                      <th>Montant HTVA (Ariary)</th>
                      <th>Remise (Ariary)</th>
                      <th>Net commercial (Ariary)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {aboLignes.length > 0 && (
                      <>
                        {rendBande('Abonnement mensuel :')}
                        {aboLignes.map((l) => rendLigneProforma(l))}
                        {rendSousTotal('Sous-total Abonnement mensuel', sousTotalDe(aboLignes))}
                      </>
                    )}
                    {prestaLignes.length > 0 && (
                      <>
                        {rendBande('Prestation:')}
                        {prestaLignes.map((l) => rendLigneProforma(l))}
                        {rendSousTotal('Sous-total Prestation', sousTotalDe(prestaLignes))}
                      </>
                    )}
                    {deboursLignes.length > 0 && (
                      <>
                        {rendBande("Débours à payer à l'administration")}
                        {deboursLignes.map((l) => rendLigneProforma(l))}
                        {rendSousTotal('Sous-total Débours', sousTotalDe(deboursLignes))}
                      </>
                    )}
                    {lignesAffichage.length === 0 && (
                      <tr><td colSpan={6} className="text-secondary">Aucune ligne persistée.</td></tr>
                    )}
                    {lignesAffichage.length > 0 && (
                      <tr>
                        <th colSpan={3} style={{ textAlign: 'right' }}>TOTAL HT</th>
                        <th>{formatMontantMGA(totalHt)}</th>
                        <th>-</th>
                        <th>{formatMontantMGA(totalHt)}</th>
                      </tr>
                    )}
                    {rappels.length > 0 && (
                      <>
                        {rendBandeRappel('À titre de rappel de règlement — non inclus dans le total')}
                        {rappels.map((l) => rendLigneProforma(l, 'rp'))}
                        <tr>
                          <th colSpan={3} style={{ textAlign: 'right' }}>Total des rappels (non inclus)</th>
                          <th>{formatMontantMGA(rappels.reduce((acc, l) => acc + l.netCommercial, 0))}</th>
                          <th>-</th>
                          <th>{formatMontantMGA(rappels.reduce((acc, l) => acc + l.netCommercial, 0))}</th>
                        </tr>
                      </>
                    )}
                  </tbody>
                </table>
              </div>
            </section>
          }


          {/* Suivi des paiements — règle générale : chaque facture est liée au paiement */}
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
