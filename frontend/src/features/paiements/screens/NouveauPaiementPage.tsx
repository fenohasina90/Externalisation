import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Alert, Autocomplete, Spinner } from '@/components/ui'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { messageErreur } from '@/constants/messages'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { useApi } from '@/hooks/useApi'
import { SelectionFichiers, documentsApi } from '@/features/documents'
import { demandesApi, facturesValideesApi } from '@/features/demandes/services/demandes.service'
import type { Demande, FactureValidee } from '@/features/demandes/types'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { CHEQUE, VIREMENT } from '../constants/labels'
import { useDemandesReglables, useModesPaiement } from '../hooks'
import { paiementsApi } from '../services/paiements.service'

const LIMITE_COMMENTAIRE = 500

function aujourdhui(): string {
  return new Date().toISOString().slice(0, 10)
}

/** Libellé du mois facturé (format AAAA-MM ou ISO). */
function moisLabel(iso: string | null | undefined): string {
  if (!iso) return '—'
  const annee = iso.slice(0, 4)
  const mois = iso.slice(5, 7)
  const d = new Date(`${annee}-${mois}-01T00:00:00`)
  if (Number.isNaN(d.getTime())) return iso
  const s = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(d)
  return s.charAt(0).toUpperCase() + s.slice(1)
}

/** Type de facture tel qu'affiché à l'écran. */
function libelleTypeFacture(f: FactureValidee): string {
  if (f.type === 'ABONNEMENT_MENSUEL') return `Facture du mois${f.mois ? ` — ${moisLabel(f.mois)}` : ''}`
  if (f.type === 'PRESTATION') return 'Facture prestation'
  if (f.type === 'GENERALE') return 'Facture générale'
  if (f.type === 'DEBOURS') return 'Facture débours'
  return 'Facture'
}

/**
 * Enregistrer un paiement — POST /api/paiements.
 * Présélection via `?demande=DEM-01-01-2026-EXT-00017` depuis un onglet « Encaisser » ; la
 * sélection de la facture se fait sur la liste des factures validées du client.
 *
 * Le paiement est toujours rattaché à la facture (jamais au service) —
 * le backend refuse le surpaiement. Les montants sont revalidés côté serveur.
 */
export function NouveauPaiementPage() {
  const navigate = useNavigate()
  const { hasPermission } = useAuth()
  const [params] = useSearchParams()
  const referenceParam = params.get('demande')
  const ligneIdParam = params.get('ligne')

  const { data: demandes, loading: loadingDemandes, error: errorDemandes } = useDemandesReglables()
  const [demandeId, setDemandeId] = useState<number | null>(null)
  const [demandeTexte, setDemandeTexte] = useState('')

  const { data: reglatable, loading: loadingReglable } = useApi<Demande | null>(
    () => (demandeId ? demandesApi.get(demandeId).catch(() => null) : Promise.resolve(null)),
    [demandeId],
  )

  const { data: fv, loading: loadingFactures, error: errorFactures } =
    useApi<FactureValidee[]>(
      () => (demandeId ? facturesValideesApi.list(demandeId).catch(() => []) : Promise.resolve([])),
      [demandeId],
    )

  const { data: demandeDepuisReference } = useApi<Demande | null>(
    () => (referenceParam
      ? demandesApi.getByReference(referenceParam).catch(() => null)
      : Promise.resolve(null)),
    [referenceParam],
  )

  const { data: modes } = useModesPaiement()

  const factures = useMemo(() => fv ?? [], [fv])

  const [factureId, setFactureId] = useState<number | null>(null)
  const [datePaiement, setDatePaiement] = useState(aujourdhui)
  const [modeId, setModeId] = useState<number | null>(null)
  const [refVirement, setRefVirement] = useState('')
  const [banque, setBanque] = useState('')
  const [numeroCheque, setNumeroCheque] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [confirmation, setConfirmation] = useState(false)
  const [erreurEnregistrement, setErreurEnregistrement] = useState<string | null>(null)
  const [fichiers, setFichiers] = useState<File[]>([])
  const [paiementCree, setPaiementCree] = useState<{ id: number; reference: string } | null>(null)
  const [montantSaisi, setMontantSaisi] = useState<{ factureId: number | null; montant: number } | null>(null)

  const listeDemandes = useMemo(() => demandes?.content ?? [], [demandes])
  const optionsDemandes = useMemo(
    () =>
      listeDemandes.map((d) => ({
        id: d.id,
        label: `${d.referenceDemande} — ${d.nomSociete ?? 'Sans société'}`,
      })),
    [listeDemandes],
  )

  const [referenceTraitee, setReferenceTraitee] = useState(false)
  if (!referenceTraitee && referenceParam) {
    const incluse = demandeDepuisReference
      ?? listeDemandes.find((d) => d.referenceDemande === referenceParam)
      ?? null
    if (incluse) {
      setReferenceTraitee(true)
      setDemandeId(incluse.id)
      setDemandeTexte(`${incluse.referenceDemande} — ${incluse.nomSociete ?? 'Sans société'}`)
    }
  }

  const demandeAvecAbonnement = useMemo(
    () => (reglatable?.lignes ?? []).some((l) => l.typePrestation === 'ABONNEMENT_MENSUEL'),
    [reglatable],
  )
  const mensuelleExistante = useMemo(
    () => factures.some((f) => f.type === 'ABONNEMENT_MENSUEL'),
    [factures],
  )
  const demandeBloquee = demandeAvecAbonnement && !mensuelleExistante

  const cible = useMemo(
    () => factures.find((f) => f.id === factureId) ?? null,
    [factures, factureId],
  )
  const cibleId = cible?.id ?? null

  // Présélection via paramètre ligne=<ligneId> : première mensuelle contenant
  // cette ligne, sinon la première facture payable.
  const [ligneTraitee, setLigneTraitee] = useState(false)
  if (!ligneTraitee && ligneIdParam && factures.length > 0) {
    const ligne = Number(ligneIdParam)
    const avecLigne = factures.find((f) =>
      f.lignes?.some((l) => l.ligneServiceId === ligne))
    const cibleInitiale = avecLigne
      ?? factures.find((f) => f.type === 'ABONNEMENT_MENSUEL')
      ?? factures[0]
      ?? null
    if (cibleInitiale) {
      setLigneTraitee(true)
      setFactureId(cibleInitiale.id)
    }
  }

  const montant = montantSaisi && montantSaisi.factureId === cibleId
    ? montantSaisi.montant
    : (cible?.resteAPayer ?? 0)

  const modeChoisi = modes?.find((m) => m.id === modeId) ?? modes?.[0] ?? null
  const estVirement = modeChoisi?.code?.toUpperCase() === VIREMENT
  const estCheque = modeChoisi?.code?.toUpperCase() === CHEQUE

  const attendu = cible?.montant ?? 0
  const dejaPaye = cible?.montantPaye ?? 0
  const reste = cible?.resteAPayer ?? 0
  const apresPaiement = reste - montant
  const tauxPaiement = attendu > 0 ? Math.min(100, Math.round((dejaPaye / attendu) * 100)) : 0

  const montantDepasse = montant > reste
  const cibleValide = cible !== null && !demandeBloquee
  const champValide =
    cibleValide &&
    Boolean(modeChoisi) &&
    Boolean(datePaiement) &&
    montant > 0 &&
    !montantDepasse &&
    (!estVirement || refVirement.trim().length > 0) &&
    (!estCheque || (banque.trim().length > 0 && numeroCheque.trim().length > 0))

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setErreurEnregistrement(null)

    if (!demandeId) {
      setError('Sélectionnez une demande.')
      return
    }
    if (demandeBloquee) {
      setError('Créez d’abord la facture du mois (onglet Factures mensuelles) avant d’enregistrer un paiement.')
      return
    }
    if (!cible || cibleId === null) {
      setError('Sélectionnez une facture à régler.')
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
      setError(`Le montant ne peut pas dépasser le reste à payer (${reste}).`)
      return
    }
    if (estVirement && refVirement.trim().length === 0) {
      setError('La référence du virement est obligatoire pour un paiement par virement.')
      return
    }
    if (estCheque && (banque.trim().length === 0 || numeroCheque.trim().length === 0)) {
      setError('La banque et le numéro de chèque sont obligatoires pour un paiement par chèque.')
      return
    }
    setConfirmation(true)
  }

  async function handleConfirmer() {
    if (!cible || cibleId === null || !modeChoisi || !datePaiement || !demandeId) {
      return
    }
    setSubmitting(true)
    setErreurEnregistrement(null)
    try {
      let paiement = paiementCree
      if (!paiement) {
        const cree = await paiementsApi.create({
          demandeId,
          factureValideeId: cible.id,
          modePaiementId: modeChoisi.id,
          datePaiement,
          montantPaye: montant,
          referenceVirement: estVirement ? refVirement.trim() : null,
          banque: estCheque ? banque.trim() : null,
          numeroCheque: estCheque ? numeroCheque.trim() : null,
          commentaire: commentaire.trim() ? commentaire.trim() : null,
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

      navigate(`/demandes/${encodeURIComponent((reglatable?.referenceDemande ?? demandeTexte)
        .split(' — ')[0])}`)
    } catch (err) {
      setErreurEnregistrement(messageErreur(err))
      setConfirmation(false)
    } finally {
      setSubmitting(false)
    }
  }

  const chargement = loadingDemandes || loadingReglable || loadingFactures

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Enregistrer un paiement</h1>
          <p>Saisissez un règlement pour une facture validée d&apos;une demande de service.</p>
        </div>
      </header>

      <div className="split-layout">
        <section className="card card-pad">
          {chargement ? (
            <Spinner label="Chargement des factures…" />
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
              {errorFactures && (
                <div className="alert alert-danger" role="alert" style={{ marginBottom: 16 }}>
                  {errorFactures}
                </div>
              )}

              <h3>1 · Demande et facture à régler</h3>
              <div className="form-grid">
                <label className="field form-grid__full">
                  Demande <span className="required">*</span>
                  <Autocomplete
                    value={demandeTexte}
                    onTextChange={(texte) => {
                      setDemandeTexte(texte)
                      setDemandeId(null)
                      setFactureId(null)
                      setError(null)
                    }}
                    onPick={(option) => {
                      if (!option) return
                      setDemandeId(Number(option.id))
                      setFactureId(null)
                      setLigneTraitee(false)
                      setError(null)
                    }}
                    options={optionsDemandes}
                    placeholder="Rechercher une demande (référence ou client)…"
                    loading={loadingDemandes}
                    name="demande"
                  />
                  <span className="form-help">
                    Saisissez la référence ou le nom du client.
                  </span>
                </label>

                <label className="field form-grid__full">
                  Facture à régler <span className="required">*</span>
                  <select
                    value={factureId ?? ''}
                    onChange={(e) => setFactureId(e.target.value ? Number(e.target.value) : null)}
                    required
                  >
                    <option value="">— Choisir —</option>
                    {factures
                      .filter((f) => f.resteAPayer > 0)
                      .map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.referenceFactureValidee} · {libelleTypeFacture(f)} — reste{' '}
                          {formatMontantMGA(f.resteAPayer)}
                        </option>
                      ))}
                  </select>
                  {demandeId && factures.filter((f) => f.resteAPayer > 0).length === 0 && !demandeBloquee && (
                    <span className="form-help">
                      Toutes les factures de cette demande sont déjà soldées.
                    </span>
                  )}
                  {demandeId && demandeBloquee && (
                    <div style={{ marginTop: 10 }}>
                      <Alert tone="warning">
                      Cette demande contient un ou plusieurs abonnements : aucune facture du mois
                      n&apos;est encore créée. Créez d’abord la facture du mois (onglet Factures
                      mensuelles), puis revenez ici pour enregistrer le paiement.{' '}
                      <Link
                        className="btn-link"
                        to={reglatable
                          ? `/demandes/${encodeURIComponent(reglatable.referenceDemande)}?onglet=factures-mensuelles`
                          : '/demandes'}
                      >
                        Ouvrir la demande →
                      </Link>
                    </Alert>
                    </div>
                  )}
                  {cible && (
                    <span className="form-help">
                      Montant validé {formatMontantMGA(attendu)} · Payé {formatMontantMGA(dejaPaye)} ·
                      reste <b>{formatMontantMGA(reste)}</b>.
                    </span>
                  )}
                </label>
              </div>

              {cibleValide && (
                <>
                  <h3 style={{ marginTop: 24 }}>2 · Règlement</h3>
                  <div className="form-grid">
                    <label className="field">
                      Date du paiement
                      <input
                        type="date"
                        value={datePaiement}
                        onChange={(e) => setDatePaiement(e.target.value)}
                        required
                      />
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
                            setMontantSaisi({ factureId: cibleId, montant: Number(e.target.value) })
                          }
                          style={{ flex: 1, minWidth: 0 }}
                          required
                        />
                        <button
                          type="button"
                          className="btn-ghost"
                          onClick={() => setMontantSaisi({ factureId: cibleId, montant: reste })}
                          disabled={reste <= 0}
                        >
                          Solder
                        </button>
                      </div>
                      <span className="form-help">
                        Reste à payer : {formatMontantMGA(reste)}.
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
                                if (m.code?.toUpperCase() !== CHEQUE) {
                                  setBanque('')
                                  setNumeroCheque('')
                                }
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
                    {estCheque && (
                      <>
                        <label className="field">
                          Banque <span className="required">*</span>
                          <input
                            value={banque}
                            onChange={(e) => setBanque(e.target.value)}
                            placeholder="Ex. BNI Madagascar"
                            maxLength={150}
                            required
                          />
                        </label>
                        <label className="field">
                          N° de chèque <span className="required">*</span>
                          <input
                            value={numeroCheque}
                            onChange={(e) => setNumeroCheque(e.target.value)}
                            placeholder="Ex. 00004521"
                            maxLength={100}
                            inputMode="numeric"
                            required
                          />
                        </label>
                        <p className="form-help form-grid__full" style={{ margin: 0 }}>
                          Obligatoires pour un règlement par chèque : la banque émettrice et le numéro du chèque.
                        </p>
                      </>
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
            <strong>{reglatable?.nomSociete ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Demande</span>
            <strong>{reglatable?.referenceDemande ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Facture</span>
            <strong>{cible?.referenceFactureValidee ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Nature</span>
            <strong>
              {cible ? libelleTypeFacture(cible) : '—'}
            </strong>
          </div>

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

          {cibleValide && (
            <div className="progression" style={{ marginTop: 4 }}>
              <div className="progression__label" style={{ marginBottom: 0 }}>
                <span>Facture réglée</span>
                <strong>{tauxPaiement} %</strong>
              </div>
              <div
                className="trk"
                role="progressbar"
                aria-valuenow={tauxPaiement}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <i style={{ width: `${tauxPaiement}%` }} />
              </div>
            </div>
          )}

          <div className="nt" style={{ marginTop: 12 }}>
            <strong>Après ce paiement</strong> :{' '}
            {montant <= 0
              ? 'saisissez un montant supérieur à 0.'
              : apresPaiement <= 0
                ? `facture ${cible?.referenceFactureValidee ?? ''} soldée (0 MGA).`
                : `reste ${formatMontantMGA(apresPaiement)} sur la facture ${cible?.referenceFactureValidee ?? ''}.`}
          </div>

          {montantDepasse && (
            <div style={{ marginTop: 12 }}>
              <Alert tone="danger">
                Le montant dépasse le reste à payer de {formatMontantMGA(montant - reste)} MGA.
              </Alert>
            </div>
          )}

          {cibleValide && (
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
              {estCheque && (
                <>
                  <div className="rc">
                    <span>Banque</span>
                    <strong>{banque.trim() || '—'}</strong>
                  </div>
                  <div className="rc">
                    <span>N° de chèque</span>
                    <strong>{numeroCheque.trim() || '—'}</strong>
                  </div>
                </>
              )}
              <div className="rc">
                <span>Date</span>
                <strong>{datePaiement ? formatDate(datePaiement) : '—'}</strong>
              </div>
            </div>
          )}
        </aside>
      </div>

      <ConfirmDialog
        open={confirmation}
        titre="Confirmer l'enregistrement du paiement"
        message={
          cible
            ? `Ce règlement sera imputé sur la facture ${cible.referenceFactureValidee} « ${libelleTypeFacture(cible)} » de la demande ${reglatable?.referenceDemande}. L'opération est définitive : le reçu PDF pourra être téléchargé depuis l'historique.`
            : ''
        }
        confirmer="Enregistrer le paiement"
        ton="primary"
        enCours={submitting}
        erreur={erreurEnregistrement}
        onAnnuler={() => {
          setConfirmation(false)
          setErreurEnregistrement(null)
        }}
        onConfirmer={() => void handleConfirmer()}
      />
    </div>
  )
}
