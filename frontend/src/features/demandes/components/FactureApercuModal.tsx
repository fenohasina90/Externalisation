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
  /** Facture générale validée en mode « unique » : aucun paiement ne lui est lié. */
  const estGeneraleUnique = !estProforma
    && apercu.facture.type === 'GENERALE'
    && apercu.facture.modeFacturation === 'UNIQUE'

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
    nomService?: string | null
    /** PRINCIPALE = payable sur cette facture ; RAPPEL = rappel uniquement. */
    type: 'PRINCIPALE' | 'RAPPEL'
  }
  const principales: LigneAffichee[] = estProforma
    ? apercu.facture.lignes.map((l) => ({
        id: `${apercu.facture.id}-${l.ligneServiceId}`,
        designation: l.nomService ?? l.libelleLigne ?? '—',
        referenceDemande: apercu.facture.referenceDemande,
        montant: l.tarif ?? l.tarifProforma ?? 0,
        secondary: l.libelleLigne && l.libelleLigne !== l.nomService ? l.libelleLigne : null,
        nomService: l.nomService,
        type: 'PRINCIPALE' as const,
      }))
    : apercu.facture.lignes.length > 0
      ? apercu.facture.lignes
          .filter((l) => l.type === 'PRINCIPALE')
          .map((l) => ({
            id: `${apercu.facture.id}-${l.id ?? l.ligneServiceId ?? l.designation}`,
            designation: l.designation,
            referenceDemande: l.referenceDemande,
            montant: l.montant,
            nomService: l.nomService,
            type: 'PRINCIPALE' as const,
          }))
      : [{
          id: `${apercu.facture.id}-unique`,
          designation: apercu.facture.nomService ?? libelleLigneFallback ?? 'Facture',
          referenceDemande: apercu.facture.referenceDemande,
          montant: montantTotal,
          secondary: libelleLigneFallback && apercu.facture.nomService ? libelleLigneFallback : null,
          nomService: apercu.facture.nomService,
          type: 'PRINCIPALE' as const,
        }]
  const rappels: LigneAffichee[] = !estProforma && apercu.facture.lignes.length > 0
    ? apercu.facture.lignes
        .filter((l) => l.type === 'RAPPEL')
        .map((l) => ({
          id: `${apercu.facture.id}-rappel-${l.id ?? l.ligneServiceId ?? l.designation}`,
          designation: l.designation,
          referenceDemande: l.referenceDemande,
          montant: l.montant,
          nomService: l.nomService,
          type: 'RAPPEL' as const,
        }))
    : []
  const totalPrincipales = principales.reduce((s, l) => s + l.montant, 0)
  const totalRappels = rappels.reduce((s, l) => s + l.montant, 0)

  // Lignes persistées de la proforma : affichage identique au PDF.
  const tarifValideParLigne = new Map(
    estProforma
      ? (apercu.facture.lignes ?? []).map((l) => [l.ligneServiceId, l.tarif])
      : [],
  )
  const lignesProforma = (estProforma ? apercu.facture.lignesProforma : undefined) ?? []
  const estValide = estProforma && apercu.facture.statut === 'VALIDEE'
  // Après validation : affichage du tarif validé (comme le PDF).
  // La proforma reste figée sur ses montants avant validation.
  const appliquerTarifValide = <T extends { pu: number; montantHtva: number; netCommercial: number; ligneServiceId: number | null }>(lignes: T[]) =>
    !estValide
      ? lignes
      : lignes.map((l) => {
          const tarifValide = l.ligneServiceId != null ? tarifValideParLigne.get(l.ligneServiceId) : undefined
          const puValide = tarifValide ?? l.pu
          return { ...l, pu: puValide, montantHtva: puValide, netCommercial: puValide }
        })
  const lignesAffichage = appliquerTarifValide(lignesProforma)
  const aboLignes = lignesAffichage.filter((l) => l.typePrestation === 'ABONNEMENT_MENSUEL')
  const prestaLignes = lignesAffichage.filter((l) =>
    l.categorie === 'EXTERNALISATION' && l.typePrestation !== 'ABONNEMENT_MENSUEL')
  const deboursLignes = lignesAffichage.filter((l) => l.categorie === 'DEBOURS')
  const totalHt = lignesAffichage.reduce((acc, l) => acc + l.netCommercial, 0)
  const sousTotalDe = (lignes: typeof lignesProforma) =>
    lignes.reduce((acc, l) => acc + l.netCommercial, 0)

  const rendLigneProforma = (l: (typeof lignesProforma)[number]) => (
    <tr key={`pl-${l.ordre ?? 0}-${l.designation}`}>
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
  const body = (
    <div
      className="modal-backdrop modal-backdrop--center"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="modal-card"
        style={{ maxWidth: 880 }}
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

          {/* Section A — objet de la présente facture */}
          {estProforma ? (
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
                        {aboLignes.map(rendLigneProforma)}
                        {rendSousTotal('Sous-total Abonnement mensuel', sousTotalDe(aboLignes))}
                      </>
                    )}
                    {prestaLignes.length > 0 && (
                      <>
                        {rendBande('Prestation:')}
                        {prestaLignes.map(rendLigneProforma)}
                        {rendSousTotal('Sous-total Prestation', sousTotalDe(prestaLignes))}
                      </>
                    )}
                    {deboursLignes.length > 0 && (
                      <>
                        {rendBande("Débours à payer à l'administration")}
                        {deboursLignes.map(rendLigneProforma)}
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
                  </tbody>
                </table>
              </div>
            </section>
          ) : (
            <section>
              <h4 style={{ margin: '0 0 6px', fontSize: 13, color: 'var(--texte-secondaire, #6b7280)', textTransform: 'uppercase' }}>
                Cette facture — objet du règlement
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
                    {principales.map((l) => (
                      <tr key={l.id}>
                        <td>
                          <div className="fw-600">
                            {l.nomService && l.nomService !== l.designation ? l.nomService : l.designation}
                          </div>
                          {l.nomService && l.nomService !== l.designation && (
                            <span className="text-secondary" style={{ fontSize: 12 }}>{l.designation}</span>
                          )}
                          {!l.nomService && l.secondary && (
                            <span className="text-secondary" style={{ fontSize: 12 }}>{l.secondary}</span>
                          )}
                        </td>
                        <td className="text-secondary">{l.referenceDemande ?? '—'}</td>
                        <td>{formatMontantMGA(l.montant)}</td>
                      </tr>
                    ))}
                    {principales.length === 0 && (
                      <tr><td colSpan={3} className="text-secondary">Aucune ligne principale.</td></tr>
                    )}
                    <tr>
                      <th colSpan={2} style={{ textAlign: 'right' }}>Total à payer (cette facture)</th>
                      <th>{formatMontantMGA(totalPrincipales)}</th>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {/* Section B — rappel des impayés, NON inclus */}
          {rappels.length > 0 && (
            <section>
              <h4 style={{ margin: '0 0 4px', fontSize: 13, color: 'var(--attention, #b45309)', textTransform: 'uppercase' }}>
                À titre de rappel de règlement
              </h4>
              <p className="section-note" style={{ margin: '0 0 6px', fontSize: 13 }}>
                Ces factures validées restent <b>impayées</b>. Elles ne sont <b>pas comptabilisées</b> dans
                le montant de la présente facture et doivent être réglées chacune sur sa propre facture validée.
              </p>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Facture concernée</th>
                      <th>Demande</th>
                      <th>Reste</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rappels.map((l) => (
                      <tr key={l.id}>
                        <td>
                          <div className="fw-600">
                            {l.nomService && l.nomService !== l.designation ? l.nomService : l.designation}
                          </div>
                          {l.nomService && l.nomService !== l.designation && (
                            <span className="text-secondary" style={{ fontSize: 12 }}>{l.designation}</span>
                          )}
                        </td>
                        <td className="text-secondary">{l.referenceDemande ?? '—'}</td>
                        <td>{formatMontantMGA(l.montant)}</td>
                      </tr>
                    ))}
                    <tr>
                      <th colSpan={2} style={{ textAlign: 'right' }}>Total des rappels impayés</th>
                      <th>{formatMontantMGA(totalRappels)}</th>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {/* Suivi des paiements (validée uniquement) — pas en mode unique */}
          {!estProforma && estGeneraleUnique && (
            <section>
              <h4 style={{ margin: '0 0 6px', fontSize: 13, color: 'var(--texte-secondaire, #6b7280)', textTransform: 'uppercase' }}>
                Suivi des paiements
              </h4>
              <p className="section-note" style={{ margin: '0 0 4px' }}>
                Cette facture générale couvre plusieurs services : <strong>aucun paiement ne lui est lié.</strong>
              </p>
              <p className="section-note" style={{ margin: 0 }}>
                Chaque service se règle sur sa propre facture validée (factures de service ci-dessous).
              </p>
            </section>
          )}
          {!estProforma && !estGeneraleUnique && (
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
