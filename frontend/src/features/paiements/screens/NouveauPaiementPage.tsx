import { useMemo, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Alert, Autocomplete, Spinner } from '@/components/ui'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { messageErreur } from '@/constants/messages'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { useApi } from '@/hooks/useApi'
import { SelectionFichiers, documentsApi } from '@/features/documents'
import { demandesApi } from '@/features/demandes/services/demandes.service'
import type { Demande } from '@/features/demandes/types'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { CHEQUE, VIREMENT } from '../constants/labels'
import { EcheancePill } from '../components'
import { useDemandesReglables, useEcheancesReglables, useModesPaiement } from '../hooks'
import { paiementsApi } from '../services/paiements.service'

const LIMITE_COMMENTAIRE = 500

function aujourdhui(): string {
  return new Date().toISOString().slice(0, 10)
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
  const ligneIdParam = params.get('ligne')
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

  // Résolution directe de la référence d'URL : l'appel ne passe pas par la
  // liste des demandes (jusqu'à 200 éléments). Le formulaire s'initialise
  // donc immédiatement, au lieu d'attendre la liste complète.
  const { data: demandeDepuisReference, loading: loadingReference } = useApi<Demande | null>(
    () => (referenceParam
      ? demandesApi.getByReference(referenceParam).catch(() => null)
      : Promise.resolve(null)),
    [referenceParam],
  )

  const { data: modes } = useModesPaiement()

  const echeances = useMemo(() => reglatable?.echeances ?? [], [reglatable])
  const deboursList = useMemo(() => reglatable?.debours ?? [], [reglatable])
  const lignesDirectesList = useMemo(() => reglatable?.lignesDirectes ?? [], [reglatable])
  const [typeReglement, setTypeReglement] = useState<'ECHEANCE' | 'DEBOURS' | 'LIGNE_DIRECTE'>('ECHEANCE')
  const [ligneDeboursChoisie, setLigneDeboursChoisie] = useState<number | null>(
    ligneIdParam ? Number(ligneIdParam) : null,
  )
  const [echeanceChoisie, setEcheanceChoisie] = useState<number | null>(
    Number.isFinite(echeanceIdParam) ? echeanceIdParam : null,
  )
  const [ligneDirecteChoisie, setLigneDirecteChoisie] = useState<number | null>(
    ligneIdParam ? Number(ligneIdParam) : null,
  )
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

  // Résolution de la référence d'URL → demande : tentative directe (une seule
  // ligne) puis repli sur la liste si celle-ci arrive la première.
  // Patron « au-render-time, une seule fois » (même style que le reste).
  const [referenceTraitee, setReferenceTraitee] = useState(false)
  if (!referenceTraitee && referenceParam) {
    const directe = demandeDepuisReference
    const incluse = directe
      ?? listeDemandes.find((d) => d.referenceDemande === referenceParam)
      ?? null
    if (incluse) {
      setReferenceTraitee(true)
      setDemandeId(incluse.id)
      setDemandeTexte(`${incluse.referenceDemande} — ${incluse.nomSociete ?? 'Sans société'}`)
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

  const deboursChoisi = useMemo(
    () => deboursList.find((d) => d.ligneId === ligneDeboursChoisie) ?? null,
    [deboursList, ligneDeboursChoisie],
  )
  const ligneDirecteSelectionnee = lignesDirectesList.find((d) => d.ligneId === ligneDirecteChoisie) ?? null
  const estDeboursSelectionne = typeReglement === 'DEBOURS' && deboursChoisi !== null
  const estLigneDirecteSelectionne = typeReglement === 'LIGNE_DIRECTE' && ligneDirecteSelectionnee !== null
  // Libellé complet de la cible du règlement (nom du service inclus).
  const libelleCible = estDeboursSelectionne
    ? deboursChoisi?.designation ?? null
    : estLigneDirecteSelectionne
      ? ligneDirecteSelectionnee
        ? `${ligneDirecteSelectionnee.nomService ?? ligneDirecteSelectionnee.ligneLabel} (${ligneDirecteSelectionnee.typePrestation === 'PRESTATION' ? 'Prestation' : 'Abonnement mensuel'})`
        : null
      : choisie?.ligneLabel ?? null
  const referenceFactureCible = estDeboursSelectionne
    ? deboursChoisi?.referenceFactureValidee ?? null
    : estLigneDirecteSelectionne
      ? ligneDirecteSelectionnee?.referenceFactureValidee ?? null
      : choisie?.referenceFactureValidee ?? null

  const montant =
    montantSaisi && montantSaisi.echeanceId === echeanceId
      ? montantSaisi.montant
      : estDeboursSelectionne
        ? (deboursChoisi?.reste ?? 0)
        : estLigneDirecteSelectionne
          ? (ligneDirecteSelectionnee?.resteAPayer ?? 0)
          : (choisie?.echeance.reste ?? 0)

  // Mode effectif : le premier disponible tant que l'utilisateur n'a pas choisi.
  const modeChoisi = modes?.find((m) => m.id === modeId) ?? modes?.[0] ?? null
  const estVirement = modeChoisi?.code?.toUpperCase() === VIREMENT
  const estCheque = modeChoisi?.code?.toUpperCase() === CHEQUE

  const attendu = estDeboursSelectionne
    ? (deboursChoisi?.montantDebours ?? 0)
    : estLigneDirecteSelectionne
      ? (ligneDirecteSelectionnee
          ? ligneDirecteSelectionnee.montantPaye + ligneDirecteSelectionnee.resteAPayer
          : 0)
      : (choisie?.echeance.montantAttendu ?? 0)
  const dejaPaye = estDeboursSelectionne
    ? (deboursChoisi?.montantPaye ?? 0)
    : estLigneDirecteSelectionne
      ? (ligneDirecteSelectionnee?.montantPaye ?? 0)
      : (choisie?.echeance.montantPaye ?? 0)
  const reste = estDeboursSelectionne
    ? (deboursChoisi?.reste ?? 0)
    : estLigneDirecteSelectionne
      ? (ligneDirecteSelectionnee?.resteAPayer ?? 0)
      : (choisie?.echeance.reste ?? 0)
  const cible = estDeboursSelectionne || estLigneDirecteSelectionne ? null : choisie
  const apresPaiement = reste - montant
  const tauxEcheance = attendu > 0 ? Math.min(100, Math.round((dejaPaye / attendu) * 100)) : 0

  const montantDepasse = montant > reste
  const dateHorsPeriode =
    !estDeboursSelectionne &&
    !estLigneDirecteSelectionne &&
    Boolean(cible) &&
    Boolean(datePaiement) &&
    ((cible!.debutService !== null && datePaiement < cible!.debutService) ||
      (cible!.finService !== null && datePaiement > cible!.finService))
  const cibleValide =
    estDeboursSelectionne
      ? Boolean(deboursChoisi && deboursChoisi.factureValideeId != null)
      : estLigneDirecteSelectionne
        ? Boolean(ligneDirecteSelectionnee && ligneDirecteSelectionnee.factureValideeId != null)
        : Boolean(cible && cible.factureValideeId != null)
  const champValide =
    Boolean(cibleValide) &&
    Boolean(modeChoisi) &&
    Boolean(datePaiement) &&
    montant > 0 &&
    !montantDepasse &&
    !dateHorsPeriode &&
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
    if (typeReglement === 'DEBOURS' && !deboursChoisi) {
      setError('Sélectionnez un débours à régler.')
      return
    }
    if (typeReglement === 'ECHEANCE' && !choisie) {
      setError('Sélectionnez une échéance à régler.')
      return
    }
    if (typeReglement === 'LIGNE_DIRECTE' && !ligneDirecteSelectionnee) {
      setError('Sélectionnez une ligne à régler.')
      return
    }
    if (typeReglement === 'DEBOURS' && deboursChoisi?.factureValideeId == null) {
      setError('Ce débours n’a pas de facture validée : validez d’abord son montant.')
      return
    }
    if (typeReglement === 'LIGNE_DIRECTE' && ligneDirecteSelectionnee?.factureValideeId == null) {
      setError(ligneDirecteSelectionnee!.factureMensuelleRequise
        ? 'Créez d’abord la facture du mois dans le détail de la demande.'
        : 'Cette ligne n’a pas de facture validée à régler.')
      return
    }
    if (typeReglement === 'ECHEANCE' && choisie?.factureValideeId == null) {
      setError('L’échéance n’a pas de facture validée associée.')
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
        `Le paiement doit être effectué entre le début et la fin du service (${formatDate(cible!.debutService)} – ${formatDate(cible!.finService)}).`,
      )
      return
    }
    if (estVirement && !refVirement.trim()) {
      setError('La référence du virement est obligatoire pour un virement.')
      return
    }
    if (estCheque && !banque.trim()) {
      setError('La banque est obligatoire pour un chèque.')
      return
    }
    if (estCheque && !numeroCheque.trim()) {
      setError('Le numéro de chèque est obligatoire pour un chèque.')
      return
    }

    setConfirmation(true)
  }

  async function confirmerEnregistrement() {
    if (
      (!estDeboursSelectionne && !choisie && !estLigneDirecteSelectionne)
      || !modeChoisi
      || !demandeId
    ) return

    setSubmitting(true)
    try {
      let paiement = paiementCree
      if (!paiement) {
        const cree = await paiementsApi.create({
          demandeId,
          factureValideeId: estLigneDirecteSelectionne
            ? ligneDirecteSelectionnee!.factureValideeId!
            : estDeboursSelectionne
              ? deboursChoisi!.factureValideeId!
              : choisie!.factureValideeId!,
          echeanceId: estDeboursSelectionne || estLigneDirecteSelectionne ? null : choisie!.echeance.id,
          modePaiementId: modeChoisi.id,
          datePaiement,
          montantPaye: montant,
          referenceVirement: estVirement ? refVirement.trim() : null,
          banque: estCheque ? banque.trim() : null,
          numeroCheque: estCheque ? numeroCheque.trim() : null,
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

      navigate(estDeboursSelectionne || estLigneDirecteSelectionne
        ? `/demandes/${reglatable?.demande?.referenceDemande ?? referenceParam ?? ''}`
        : `/paiements/echeanciers/${choisie!.referenceDemande}`)
    } catch (err) {
      setErreurEnregistrement(messageErreur(err))
      setSubmitting(false)
    }
  }

  const chargement = loadingDemandes || loadingEcheances || loadingReference

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

              <h3>1 · Demande et règlement</h3>
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
                      setLigneDirecteChoisie(null)
                      setError(null)
                    }}
                    onPick={(option) => {
                      if (!option) return
                      setDemandeId(Number(option.id))
                      setEcheanceChoisie(null)
                      setLigneDirecteChoisie(null)
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

                <div className="field form-grid__full">
                  <span className="field-label">Type de règlement</span>
                  <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                    <label style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                      <input type="radio" name="typeReglement" checked={typeReglement === 'ECHEANCE'} onChange={() => setTypeReglement('ECHEANCE')} />
                      Échéance (Externalisation)
                    </label>
                    <label style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                      <input type="radio" name="typeReglement" checked={typeReglement === 'DEBOURS'} onChange={() => setTypeReglement('DEBOURS')} disabled={deboursList.length === 0} />
                      Débours (ligne)
                    </label>
                    <label style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                      <input type="radio" name="typeReglement" checked={typeReglement === 'LIGNE_DIRECTE'} onChange={() => setTypeReglement('LIGNE_DIRECTE')} disabled={lignesDirectesList.length === 0} />
                      Ligne directe (Prestation / Abonnement)
                    </label>
                  </div>
                </div>

                {typeReglement === 'ECHEANCE' ? (
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
                    {echeances.map((reglable) => (
                      <option key={reglable.echeance.id} value={reglable.echeance.id}>
                        N° {reglable.echeance.numeroEcheance} ·{' '}
                        {formatDate(reglable.echeance.dateEcheance)} — {reglable.ligneLabel} · reste{' '}
                        {formatMontantMGA(reglable.echeance.reste)}
                      </option>
                    ))}
                  </select>
                  <span className="form-help">
                    Une échéance peut être réglée en plusieurs paiements ; seules celles qui
                    restent dues sont listées.
                  </span>
                </label>
                ) : typeReglement === 'DEBOURS' ? (
                <label className="field form-grid__full">
                  Débours <span className="required">*</span>
                  <select
                    value={ligneDeboursChoisie ?? ''}
                    onChange={(e) => setLigneDeboursChoisie(e.target.value ? Number(e.target.value) : null)}
                    required
                  >
                    <option value="">— Choisir —</option>
                    {deboursList.map((d) => (
                      <option key={d.ligneId} value={d.ligneId}>
                        {d.designation}{d.referenceFactureValidee ? ` (facture ${d.referenceFactureValidee})` : ''} — reste {formatMontantMGA(d.reste)}
                      </option>
                    ))}
                  </select>
                  <span className="form-help">Lignes DEBOURS non soldées (Non payé / Partiel).</span>
                </label>
                ) : (
                <label className="field form-grid__full">
                  Ligne à régler <span className="required">*</span>
                  <select
                    value={ligneDirecteChoisie ?? ''}
                    onChange={(e) => setLigneDirecteChoisie(e.target.value ? Number(e.target.value) : null)}
                    required
                  >
                    <option value="">— Choisir —</option>
                    {lignesDirectesList.map((d) => (
                      <option key={d.ligneId} value={d.ligneId} disabled={d.factureMensuelleRequise}>
                        {d.nomService ?? d.ligneLabel} ({d.typePrestation === 'PRESTATION' ? 'Prestation' : 'Abonnement mensuel'}) —{' '}
                        {d.factureMensuelleRequise
                          ? 'facture du mois à créer'
                          : `reste ${formatMontantMGA(d.resteAPayer)}`}
                      </option>
                    ))}
                  </select>
                  {ligneDirecteSelectionnee?.factureMensuelleRequise && (
                    <Alert tone="warning">
                      Aucune facture du mois n&apos;est ouverte pour cet abonnement.{' '}
                      <Link
                        to={`/demandes/${encodeURIComponent(ligneDirecteSelectionnee.referenceDemande)}`}
                      >
                        Créez la facture du mois dans le détail de la demande
                      </Link>{' '}
                      puis revenez ici pour l&apos;encaisser.
                    </Alert>
                  )}
                  {ligneDirecteSelectionnee && !ligneDirecteSelectionnee.factureMensuelleRequise && (
                    <p className="form-help">
                      {ligneDirecteSelectionnee.typePrestation === 'ABONNEMENT_MENSUEL'
                        ? `Facture du mois ${ligneDirecteSelectionnee.referenceFactureValidee ?? ''}`
                          + ` (facture générale ${ligneDirecteSelectionnee.referenceFactureGenerale ?? '—'}`
                          + `${ligneDirecteSelectionnee.referenceFactureProforma ? ` · proforma ${ligneDirecteSelectionnee.referenceFactureProforma}` : ''}).`
                        : `Facture validée ${ligneDirecteSelectionnee.referenceFactureValidee ?? ''}.`}
                    </p>
                  )}
                  <span className="form-help">
                    Lignes PRESTATION / ABONNEMENT_MENSUEL réglables directement sur la facture validée (pas d’échéance ni de plan de paiement).
                  </span>
                </label>
                )}
              </div>

              {demandeId && !chargement && typeReglement === 'ECHEANCE' && echeances.length === 0 && deboursList.length === 0 && lignesDirectesList.length === 0 && (
                <Alert tone="warning">
                  Cette demande n&apos;a aucune échéance non soldée ni ligne réglable.
                  {(reglatable?.deboursEnAttente?.length ?? 0) > 0 ? (
                    <>
                      {' '}Des débours sont en attente de validation du montant ({reglatable?.deboursEnAttente?.map((d) => d.designation).join(', ')}) :
                      {' '}
                      <Link
                        className="btn-link"
                        to={`/demandes/${encodeURIComponent(reglatable?.demande?.referenceDemande ?? '')}?onglet=services`}
                      >
                        ouvrez la fiche demande → Services
                      </Link>
                    </>
                  ) : (
                    <>
                      {' '}Un plan de paiement doit être établi — et sa facture validée — avant tout règlement.
                    </>
                  )}
                </Alert>
              )}
              {demandeId && !chargement && (reglatable?.deboursEnAttente?.length ?? 0) > 0 && deboursList.length === 0 && (
                <Alert tone="warning">
                  Demande&nbsp;: certaines lignes débours sont <b>en attente de validation du montant</b>
                  {' '}({reglatable?.deboursEnAttente?.map((d) => d.designation).join(', ')}) et ne sont
                  donc pas réglables ici. Validez d'abord leur montant depuis la fiche de la demande,
                  onglet Services (bouton « Valider le montant ») :
                  {' '}
                  <Link
                    className="btn-link"
                    to={`/demandes/${encodeURIComponent(reglatable?.demande?.referenceDemande ?? '')}?onglet=services`}
                  >
                    Ouvrir la fiche demande →
                  </Link>
                </Alert>
              )}
              {demandeId && !chargement && typeReglement === 'DEBOURS' && deboursList.length === 0 && (reglatable?.deboursEnAttente?.length ?? 0) === 0 && (
                <Alert tone="warning">
                  Cette demande n&apos;a aucun débours à régler (montant validé avec reste dû).
                </Alert>
              )}
              {demandeId && !chargement && typeReglement === 'LIGNE_DIRECTE' && lignesDirectesList.length === 0 && (
                <Alert tone="warning">
                  Cette demande n&apos;a aucune ligne Prestation / Abonnement mensuel réglable directement.
                </Alert>
              )}

              {cibleValide && (
                <>
                  <h3 style={{ marginTop: 24 }}>2 · Règlement</h3>
                  <div className="form-grid">
                    <label className="field">
                      Date du paiement 
                      {/* <span className="required">*</span> */}
                      <input
                        type="date"
                        value={datePaiement}
                        min={cible?.debutService ?? undefined}
                        max={cible?.finService ?? undefined}
                        onChange={(e) => setDatePaiement(e.target.value)}
                        required
                      />
                      <span className="form-help">
                        {cible?.debutService || cible?.finService
                          ? `Période du service : ${formatDate(cible?.debutService)} – ${formatDate(cible?.finService)}.`
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
            <strong>{choisie?.nomSociete ?? reglatable?.demande?.nomSociete ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>Demande</span>
            <strong>{choisie?.referenceDemande ?? reglatable?.demande?.referenceDemande ?? '—'}</strong>
          </div>
          <div className="rc">
            <span>{estDeboursSelectionne ? 'Débours' : 'Service'}</span>
            <strong>{libelleCible ?? '—'}</strong>
          </div>
          {referenceFactureCible && (
            <div className="rc">
              <span>Facture validée</span>
              <strong>{referenceFactureCible}</strong>
            </div>
          )}
          <div className="rc">
            <span>Échéance</span>
            <strong>
              {estDeboursSelectionne || estLigneDirecteSelectionne
                ? '— (sans échéance)'
                : choisie
                ? `N° ${choisie.echeance.numeroEcheance} - ${formatDate(choisie.echeance.dateEcheance)}`
                : '—'}
            </strong>
          </div>
          {choisie && !estDeboursSelectionne && (
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

          {cibleValide && (
            <div className="progression" style={{ marginTop: 4 }}>
              <div className="progression__label" style={{ marginBottom: 0 }}>
                <span>{estDeboursSelectionne ? 'Débours réglé' : estLigneDirecteSelectionne ? 'Ligne réglée' : 'Échéance réglée'}</span>
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
                ? estDeboursSelectionne
                  ? `débours « ${deboursChoisi?.designation ?? ''} » soldé (0 MGA).`
                  : estLigneDirecteSelectionne
                    ? `ligne « ${libelleCible ?? ''} » soldée (0 MGA).`
                    : `échéance n° ${choisie?.echeance.numeroEcheance} soldée (0 MGA).`
                : estDeboursSelectionne
                  ? `reste ${formatMontantMGA(apresPaiement)} sur le débours « ${deboursChoisi?.designation ?? ''} ».`
                  : estLigneDirecteSelectionne
                    ? `reste ${formatMontantMGA(apresPaiement)} sur la ligne « ${libelleCible ?? ''} ».`
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
              {!estDeboursSelectionne && choisie && (
              <Link
                to={`/paiements/echeanciers/${choisie.referenceDemande}`}
                className="btn-link success"
              >
                Voir l&apos;échéancier ›
              </Link>
              )}
            </div>
          )}
        </aside>
      </div>

      <ConfirmDialog
        open={confirmation}
        titre="Confirmer l'enregistrement du paiement"
        message={
          estDeboursSelectionne
            ? `Ce règlement sera imputé sur le débours « ${deboursChoisi?.designation ?? ''} » de la demande ${reglatable?.demande?.referenceDemande ?? ''}. L'opération est définitive : le reçu PDF pourra être téléchargé depuis l'historique.`
          : estLigneDirecteSelectionne
            ? `Ce règlement sera imputé sur la facture validée ${referenceFactureCible ?? ''} du service « ${libelleCible ?? ''} » de la demande ${reglatable?.demande?.referenceDemande ?? ''}. L'opération est définitive : le reçu PDF pourra être téléchargé depuis l'historique.`
          : choisie
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
          cibleValide ? (
            <div style={{ marginTop: 14 }}>
              <div className="rc">
                <span>Demande</span>
                <strong>{choisie?.referenceDemande ?? reglatable?.demande?.referenceDemande ?? ''}</strong>
              </div>
              <div className="rc">
                <span>Client</span>
                <strong>{choisie?.nomSociete ?? reglatable?.demande?.nomSociete ?? '—'}</strong>
              </div>
              <div className="rc">
                <span>{estDeboursSelectionne ? 'Débours' : 'Service'}</span>
                <strong>{libelleCible ?? ''}</strong>
              </div>
              {referenceFactureCible && (
                <div className="rc">
                  <span>Facture validée</span>
                  <strong>{referenceFactureCible}</strong>
                </div>
              )}
              <div className="rc">
                <span>Échéance</span>
                <strong>
                  {estDeboursSelectionne || estLigneDirecteSelectionne ? '—' : choisie ? `N° ${choisie.echeance.numeroEcheance} — ${formatDate(choisie.echeance.dateEcheance)}` : '—'}
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