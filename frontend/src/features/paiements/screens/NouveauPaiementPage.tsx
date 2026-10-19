import { useMemo, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Alert, Autocomplete, Spinner } from '@/components/ui'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { messageErreur } from '@/constants/messages'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { SelectionFichiers, documentsApi } from '@/features/documents'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { VIREMENT } from '../constants/labels'
import { EcheancePill } from '../components'
import { useDemandesReglables, useEcheancesReglables, useModesPaiement } from '../hooks'
import { paiementsApi } from '../services/paiements.service'
import type { EcheanceReglable } from '../types'

const LIMITE_COMMENTAIRE = 500

function aujourdhui(): string {
  return new Date().toISOString().slice(0, 10)
}

interface GroupeService {
  label: string
  lignes: EcheanceReglable[]
}

/**
 * Enregistrer un paiement — POST /api/paiements.
 * Présélection via `?demande=DEM-2026-00017` depuis l'échéancier ; l'échéance
 * concernée est transmise en état de navigation, jamais exposée en identifiant.
 *
 * Les règles contrôlées par le backend (montant ≤ reste, échéance obligatoire,
 * date dans la période de service, référence de virement) sont aussi vérifiées
 * ici pour un retour immédiat, sans jamais les remplacer : un 409 reste affiché
 * tel quel via `messageErreur`.
 */
export function NouveauPaiementPage() {
  const navigate = useNavigate()
  const { hasPermission } = useAuth()
  const [params] = useSearchParams()
  const location = useLocation()
  const referenceParam = params.get('demande')
  const echeanceIdParam =
    (location.state as { echeanceId?: number } | null)?.echeanceId ?? NaN

  const { data: demandes, loading: loadingDemandes, error: errorDemandes } = useDemandesReglables()
  const [demandeId, setDemandeId] = useState<number | null>(null)
  const [demandeTexte, setDemandeTexte] = useState('')

  const {
    data: reglatable,
    loading: loadingEcheances,
    error: errorEcheances,
  } = useEcheancesReglables(demandeId)

  const { data: modes } = useModesPaiement()

  const echeances = useMemo(() => reglatable?.echeances ?? [], [reglatable])
  const [echeanceChoisie, setEcheanceChoisie] = useState<number | null>(
    Number.isFinite(echeanceIdParam) ? echeanceIdParam : null,
  )
  const [datePaiement, setDatePaiement] = useState(aujourdhui)
  const [modeId, setModeId] = useState<number | null>(null)
  const [refVirement, setRefVirement] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [confirmation, setConfirmation] = useState(false)
  const [erreurEnregistrement, setErreurEnregistrement] = useState<string | null>(null)
  const [fichiers, setFichiers] = useState<File[]>([])
  const [paiementCree, setPaiementCree] = useState<{ id: number; reference: string } | null>(null)
  // Montant saisi, rattaché à l'échéance concernée : le pré-remplissage suit
  // l'échéance choisie sans effet de bord.
  const [montantSaisi, setMontantSaisi] = useState<{ echeanceId: number; montant: number } | null>(
    null,
  )

  const listeDemandes = useMemo(() => demandes?.content ?? [], [demandes])
  const optionsDemandes = useMemo(
    () =>
      listeDemandes.map((d) => ({
        id: d.id,
        label: `${d.referenceDemande} — ${d.nomSociete ?? 'Sans société'}`,
      })),
    [listeDemandes],
  )

  // Résout la référence de l'URL en demande dès que la liste est chargée.
  const [referenceTraitee, setReferenceTraitee] = useState(false)
  if (!referenceTraitee && referenceParam && listeDemandes.length > 0) {
    setReferenceTraitee(true)
    const trouve = listeDemandes.find((d) => d.referenceDemande === referenceParam)
    if (trouve) {
      setDemandeId(trouve.id)
      setDemandeTexte(`${trouve.referenceDemande} — ${trouve.nomSociete ?? 'Sans société'}`)
    }
  }

  // Échéance effective : présélection depuis l'URL, sinon première à régler.
  const echeanceId = useMemo(() => {
    if (echeanceChoisie && echeances.some((e) => e.echeance.id === echeanceChoisie)) {
      return echeanceChoisie
    }
    return echeances[0]?.echeance.id ?? 0
  }, [echeanceChoisie, echeances])

  const choisie = useMemo(
    () => echeances.find((e) => e.echeance.id === echeanceId) ?? null,
    [echeances, echeanceId],
  )

  // Échéances groupées par service : l'utilisateur règle service par service.
  const groupes = useMemo(() => {
    const map = new Map<string, EcheanceReglable[]>()
    echeances.forEach((e) => {
      const liste = map.get(e.ligneLabel) ?? []
      liste.push(e)
      map.set(e.ligneLabel, liste)
    })
    return Array.from(map, ([label, lignes]): GroupeService => ({ label, lignes }))
  }, [echeances])

  const montant =
    montantSaisi && montantSaisi.echeanceId === echeanceId
      ? montantSaisi.montant
      : (choisie?.echeance.reste ?? 0)

  // Mode effectif : le premier disponible tant que l'utilisateur n'a pas choisi.
  const modeChoisi = modes?.find((m) => m.id === modeId) ?? modes?.[0] ?? null
  const estVirement = modeChoisi?.code?.toUpperCase() === VIREMENT

  const attendu = choisie?.echeance.montantAttendu ?? 0
  const dejaPaye = choisie?.echeance.montantPaye ?? 0
  const reste = choisie?.echeance.reste ?? 0
  const apresPaiement = reste - montant
  const tauxEcheance = attendu > 0 ? Math.min(100, Math.round((dejaPaye / attendu) * 100)) : 0

  const montantDepasse = montant > reste
  const dateHorsPeriode =
    Boolean(choisie) &&
    Boolean(datePaiement) &&
    ((choisie!.debutService !== null && datePaiement < choisie!.debutService) ||
      (choisie!.finService !== null && datePaiement > choisie!.finService))
  const champValide =
    Boolean(choisie) &&
    Boolean(modeChoisi) &&
    Boolean(datePaiement) &&
    montant > 0 &&
    !montantDepasse &&
    !dateHorsPeriode &&
    (!estVirement || refVirement.trim().length > 0)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setErreurEnregistrement(null)

    if (!demandeId) {
      setError('Sélectionnez une demande.')
      return
    }
    if (!choisie) {
      setError('Sélectionnez une échéance à régler.')
      return
    }
    if (!modeChoisi) {
      setError('Sélectionnez un mode de paiement.')
      return
    }
    if (!datePaiement) {
      setError('La date du paiement est obligatoire.')
      return
    }
    if (!Number.isFinite(montant) || montant <= 0) {
      setError('Le montant doit être supérieur à 0.')
      return
    }
    if (montantDepasse) {
      setError(`Le montant ne doit pas dépasser le montant restant (${formatMontantMGA(reste)}).`)
      return
    }
    if (dateHorsPeriode) {
      setError(
        `Le paiement doit être effectué entre le début et la fin du service (${formatDate(choisie!.debutService)} – ${formatDate(choisie!.finService)}).`,
      )
      return
    }
    if (estVirement && !refVirement.trim()) {
      setError('La référence du virement est obligatoire pour un virement.')
      return
    }

    setConfirmation(true)
  }

  async function confirmerEnregistrement() {
    if (!choisie || !modeChoisi || !demandeId) return

    setSubmitting(true)
    try {
      let paiement = paiementCree
      if (!paiement) {
        const cree = await paiementsApi.create({
          demandeId,
          echeanceId: choisie.echeance.id,
          modePaiementId: modeChoisi.id,
          datePaiement,
          montantPaye: montant,
          referenceVirement: estVirement ? refVirement.trim() : null,
          commentaire: commentaire.trim() || null,
        })
        paiement = { id: cree.id, reference: cree.referencePaiement }
        setPaiementCree(paiement)

        if (hasPermission('DOCUMENT_PAIEMENT_VIEW')) {
          const recu = await paiementsApi.telechargerRecu(cree.id).catch(() => null)
          if (recu) telechargerBlob(recu, `recu-${cree.referencePaiement}.pdf`)
        }
      }

      for (const fichier of fichiers) {
        await documentsApi.uploadPaiement(paiement.id, fichier, null)
      }

      navigate(`/paiements/echeanciers/${choisie.referenceDemande}`)
    } catch (err) {
      setErreurEnregistrement(messageErreur(err))
      setSubmitting(false)
    }
  }

  const chargement = loadingDemandes || loadingEcheances

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Enregistrer un paiement</h1>
          <p>Saisissez un règlement pour une échéance d&apos;une demande de service.</p>
        </div>
      </header>

      <div className="split-layout">
        <section className="card card-pad">
          {chargement ? (
            <Spinner label="Chargement des échéances…" />
          ) : (
            <form onSubmit={handleSubmit} noValidate>
              {error && (
                <div className="alert alert-danger" role="alert" style={{ marginBottom: 16 }}>
                  {error}
                </div>
              )}
              {errorDemandes && (
                <div className="alert alert-danger" role="alert" style={{ marginBottom: 16 }}>
                  {errorDemandes}
                </div>
              )}
              {errorEcheances && (
                <div className="alert alert-danger" role="alert" style={{ marginBottom: 16 }}>
                  {errorEcheances}
                </div>
              )}

              <h3>1 · Demande et échéance</h3>
              <div className="form-grid">
                <label className="field form-grid__full">
                  Demande <span className="required">*</span>
                  <Autocomplete
                    value={demandeTexte}
                    onTextChange={(texte) => {
                      setDemandeTexte(texte)
                      // Toute frappe remet la sélection à zéro : la demande
                      // affichée doit toujours correspondre à la saisie.
                      setDemandeId(null)
                      setEcheanceChoisie(null)
                      setError(null)
                    }}
                    onPick={(option) => {
                      if (!option) return
                      setDemandeId(Number(option.id))
                      setEcheanceChoisie(null)
                      setError(null)
                    }}
                    options={optionsDemandes}
                    placeholder="Rechercher une demande (référence ou client)…"
                    loading={loadingDemandes}
                    name="demande"
                  />
                  <span className="form-help">
                    Saisissez la référence (ex. DEM-2026-00017) ou le nom du client.
                  </span>
                </label>

                <label className="field form-grid__full">
                  Échéance <span className="required">*</span>
                  <select
                    value={echeanceId || ''}
                    onChange={(e) => setEcheanceChoisie(Number(e.target.value))}
                    required
                    disabled={echeances.length === 0}
                  >
                    {echeances.length === 0 && (
                      <option value="">Aucune échéance à régler</option>
                    )}
                    {groupes.map((groupe) => (
                      <optgroup key={groupe.label} label={groupe.label}>
                        {groupe.lignes.map((reglable) => (
                          <option key={reglable.echeance.id} value={reglable.echeance.id}>
                            N° {reglable.echeance.numeroEcheance} ·{' '}
                            {formatDate(reglable.echeance.dateEcheance)} — reste{' '}
                            {formatMontantMGA(reglable.echeance.reste)}
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </select>
                  <span className="form-help">
                    Une échéance peut être réglée en plusieurs paiements ; seules celles qui
                    restent dues sont listées.
                  </span>
                </label>
              </div>

              {demandeId && !chargement && echeances.length === 0 && (
                <Alert tone="warning">
                  Cette demande n&apos;a aucune échéance non soldée. Un plan de paiement doit être
                  établi — et sa facture validée — avant tout règlement.
                </Alert>
              )}

              {choisie && (
                <>
                  <h3 style={{ marginTop: 24 }}>2 · Règlement</h3>
                  <div className="form-grid">
                    <label className="field">
                      Date du paiement 
                      {/* <span className="required">*</span> */}
                      <input
                        type="date"
                        value={datePaiement}
                        min={choisie.debutService ?? undefined}
                        max={choisie.finService ?? undefined}
                        onChange={(e) => setDatePaiement(e.target.value)}
                        required
                      />
                      <span className="form-help">
                        {choisie.debutService || choisie.finService
                          ? `Période du service : ${formatDate(choisie.debutService)} – ${formatDate(choisie.finService)}.`
                          : 'La date doit être comprise dans la période du service.'}
                      </span>
                    </label>

                    <div className="field">
                      <span className="field-label">
                        Montant (MGA) <span className="required">*</span>
                      </span>
                      <div style={{ display: 'flex', gap: 8, alignItems: 'stretch' }}>
                        <input
                          type="number"
                          min={1}
                          step="1"
                          value={Number.isFinite(montant) ? montant : 0}
                          onChange={(e) =>
                            setMontantSaisi({ echeanceId, montant: Number(e.target.value) })
                          }
                          style={{ flex: 1, minWidth: 0 }}
                          required
                        />
                        <button
                          type="button"
                          className="btn-ghost"
                          onClick={() => setMontantSaisi({ echeanceId, montant: reste })}
                          disabled={reste <= 0}
                        >
                          Solder
                        </button>
                      </div>
                      <span className="form-help">
                        Reste à payer sur l&apos;échéance : {formatMontantMGA(reste)}.
                      </span>
                    </div>

                    <div className="field form-grid__full">
                      <span className="field-label">
                        Mode de paiement <span className="required">*</span>
                      </span>
                      <div className="rd-group" role="radiogroup" aria-label="Mode de paiement">
                        {(modes ?? []).map((m) => (
                          <label
                            key={m.id}
                            className={`rd${modeChoisi?.id === m.id ? ' on' : ''}`}
                          >
                            <input
                              type="radio"
                              name="mode"
                              value={m.id}
                              checked={modeChoisi?.id === m.id}
                              onChange={() => {
                                setModeId(m.id)
                                if (m.code?.toUpperCase() !== VIREMENT) setRefVirement('')
                              }}
                            />
                            {m.libelle}
                            <span style={{ marginLeft: 6, opacity: 0.65, fontWeight: 500 }}>
                              {m.code}
                            </span>
                          </label>
                        ))}
                      </div>
                    </div>

                    {estVirement && (
                      <label className="field form-grid__full">
                        Référence du virement <span className="required">*</span>
                        <input
                          value={refVirement}
                          onChange={(e) => setRefVirement(e.target.value)}
                          placeholder="Ex. VIR-2026-1004"
                          maxLength={150}
                          required
                        />
                        <span className="form-help">
                          Obligatoire pour un règlement par virement (150 caractères maximum).
                        </span>
                      </label>
                    )}
                  </div>
                </>
              )}

              <h3 style={{ marginTop: 24 }}>3 · Informations complémentaires</h3>
              <div className="form-grid">
                <label className="field form-grid__full">
                  Commentaire
                  <textarea
                    value={commentaire}
                    onChange={(e) => setCommentaire(e.target.value)}
                    placeholder="Information complémentaire (facultatif)"
                    maxLength={LIMITE_COMMENTAIRE}
                    rows={3}
                    style={{ height: 92 }}
                  />
                  <span className="form-help">
                    {commentaire.length} / {LIMITE_COMMENTAIRE} caractères
                  </span>
                </label>

                <SelectionFichiers
                  fichiers={fichiers}
                  onChange={setFichiers}
                  desactive={submitting}
                />
              </div>

              <footer className="modal-footer" style={{ marginTop: 20 }}>
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={() => navigate('/paiements')}
                  disabled={submitting}
                >
                  Annuler
                </button>
                <button type="submit" className="btn-primary" disabled={submitting || !champValide}>
                  {submitting ? 'Enregistrement…' : 'Enregistrer le paiement'}
                </button>
              </footer>
            </form>
          )}
        </section>

        <aside className="card card-pad recap-card">
          <h3>Récapitulatif</h3>
          <div className="rc">
            <span>Client</span>
            <strong>{choisie?.nomSociete ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Demande</span>
            <strong>{choisie?.referenceDemande ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Service</span>
            <strong>{choisie?.ligneLabel ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Échéance</span>
            <strong>
              {choisie
                ? `N° ${choisie.echeance.numeroEcheance} - ${formatDate(choisie.echeance.dateEcheance)}`
                : '—'}
            </strong>
          </div>
          {choisie && (
            <div style={{ margin: '4px 0 8px' }}>
              <EcheancePill statut={choisie.echeance.statut} />
            </div>
          )}

          <div className="rc">
            <span>Montant attendu</span>
            <strong>{formatMontantMGA(attendu)}</strong>
          </div>
          <div className="rc">
            <span>Déjà payé</span>
            <strong style={{ color: 'var(--success)' }}>{formatMontantMGA(dejaPaye)}</strong>
          </div>
          <div className="rc">
            <span>Reste à payer</span>
            <strong style={{ color: 'var(--danger)' }}>{formatMontantMGA(reste)}</strong>
          </div>

          {choisie && (
            <div className="progression" style={{ marginTop: 4 }}>
              <div className="progression__label" style={{ marginBottom: 0 }}>
                <span>Échéance réglée</span>
                <strong>{tauxEcheance} %</strong>
              </div>
              <div
                className="trk"
                role="progressbar"
                aria-valuenow={tauxEcheance}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <i style={{ width: `${tauxEcheance}%` }} />
              </div>
            </div>
          )}

          <div className="nt" style={{ marginTop: 12 }}>
            <strong>Après ce paiement</strong> :{' '}
            {montant <= 0
              ? 'saisissez un montant supérieur à 0.'
              : apresPaiement <= 0
                ? `échéance n° ${choisie?.echeance.numeroEcheance} soldée (0 MGA).`
                : `reste ${formatMontantMGA(apresPaiement)} sur l'échéance n° ${choisie?.echeance.numeroEcheance}.`}
          </div>

          {(montantDepasse || dateHorsPeriode) && (
            <div style={{ marginTop: 12 }}>
              <Alert tone="danger">
                {montantDepasse
                  ? `Le montant dépasse le reste à payer de ${formatMontantMGA(montant - reste)} MGA.`
                  : 'La date du paiement est hors de la période du service.'}
              </Alert>
            </div>
          )}

          {choisie && (
            <div style={{ marginTop: 14 }}>
              <div className="rc">
                <span>Mode</span>
                <strong>{modeChoisi?.libelle ?? '—'}</strong>
              </div>
              {estVirement && (
                <div className="rc">
                  <span>Réf. virement</span>
                  <strong>{refVirement.trim() || '—'}</strong>
                </div>
              )}
              <div className="rc">
                <span>Date</span>
                <strong>{datePaiement ? formatDate(datePaiement) : '—'}</strong>
              </div>
              <Link
                to={`/paiements/echeanciers/${choisie.referenceDemande}`}
                className="btn-link success"
              >
                Voir l&apos;échéancier ›
              </Link>
            </div>
          )}
        </aside>
      </div>

      <ConfirmDialog
        open={confirmation}
        titre="Confirmer l'enregistrement du paiement"
        message={
          choisie
            ? `Ce règlement sera imputé sur l'échéance n° ${choisie.echeance.numeroEcheance} du service « ${choisie.ligneLabel} » de la demande ${choisie.referenceDemande}. L'opération est définitive : le reçu PDF pourra être téléchargé depuis l'historique.`
            : ''
        }
        confirmer="Valider le paiement"
        ton="success"
        enCours={submitting}
        erreur={erreurEnregistrement}
        onAnnuler={() => {
          if (submitting) return
          setConfirmation(false)
          setErreurEnregistrement(null)
          setPaiementCree(null)
        }}
        onConfirmer={() => void confirmerEnregistrement()}
        details={
          choisie ? (
            <div style={{ marginTop: 14 }}>
              <div className="rc">
                <span>Demande</span>
                <strong>{choisie.referenceDemande}</strong>
              </div>
              <div className="rc">
                <span>Client</span>
                <strong>{choisie.nomSociete ?? '—'}</strong>
              </div>
              <div className="rc">
                <span>Service</span>
                <strong>{choisie.ligneLabel}</strong>
              </div>
              <div className="rc">
                <span>Échéance</span>
                <strong>
                  N° {choisie.echeance.numeroEcheance} —{' '}
                  {formatDate(choisie.echeance.dateEcheance)}
                </strong>
              </div>
              <div className="rc">
                <span>Montant</span>
                <strong style={{ color: 'var(--success)' }}>
                  {formatMontantMGA(montant)}
                </strong>
              </div>
              <div className="rc">
                <span>Mode</span>
                <strong>{modeChoisi?.libelle ?? '—'}</strong>
              </div>
              {estVirement && (
                <div className="rc">
                  <span>Réf. virement</span>
                  <strong>{refVirement.trim() || '—'}</strong>
                </div>
              )}
              <div className="rc">
                <span>Date</span>
                <strong>{datePaiement ? formatDate(datePaiement) : '—'}</strong>
              </div>
              <div className="rc">
                <span>Reste après validation</span>
                <strong>{formatMontantMGA(Math.max(apresPaiement, 0))}</strong>
              </div>
            </div>
          ) : null
        }
      />
    </div>
  )
}