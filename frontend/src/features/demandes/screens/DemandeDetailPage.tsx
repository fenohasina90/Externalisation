import { Fragment, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Can } from '@/components/shared/Can'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { FormEtapes } from '@/components/shared/FormEtapes'
import { Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { http } from '@/api/http'
import { messageErreur } from '@/constants/messages'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { formatDate, formatMontantMGA } from '@/utils/format'
import type { Commercial } from '@/features/commerciaux/types'
import { estTypeServiceSolo, type Service } from '@/features/services/types'
import { commerciauxApi } from '@/features/commerciaux/services/commerciaux.service'
import { servicesApi } from '@/features/services/services/services.service'
import type { PlanPaiement } from '@/features/plan-paiement/types'
import { planPaiementApi } from '@/features/plan-paiement/services/plan-paiement.service'
import type { Echeance } from '@/features/echeances/types'
import { echeancesApi } from '@/features/echeances/services/echeances.service'
import { ServiceFormModal } from '@/features/services/components/ServiceFormModal'
import { PiecesJointes } from '@/features/documents'
import { getLibelleTypePrestation } from '../constants/labels'
import { ChangerStatutModal, DemandeStepper, DossierEntrepriseDetail,
  DossierTransactionImmobiliereDetail, FactureApercuModal, StatutDemandePill } from '../components'
import type { FactureApercu } from '../components'
import type { Demande, Facture, FactureValidee, InclusionDisponible, HistoriqueDemande, HistoriqueLigne,
  LigneService, StatutDemande } from '../types'
import { dossierVersFormulaire, type Dossier } from '../types/dossier'
import type { DossierTransactionImmobiliere } from '../types/dossierTransaction'
import {
  demandesApi,
  dossierApi,
  dossierTransactionApi,
  facturesApi,
  facturesValideesApi,
  lignesApi,
} from '../services/demandes.service'

type Onglet = 'informations' | 'services' | 'factures' | 'factures-mensuelles' | 'documents' | 'paiements'
  | 'historique' | 'dossier' | 'dossier-transaction'

const ONGLETS_VALIDES: Onglet[] = ['informations', 'services', 'factures', 'factures-mensuelles', 'documents',
  'paiements', 'historique', 'dossier', 'dossier-transaction']

/** « 2026-01 » → « Janvier 2026 ». */
const moisLabel = (isoMois: string | null | undefined): string => {
  if (!isoMois) return '—'
  const d = new Date(`${isoMois}-01T00:00:00`)
  if (Number.isNaN(d.getTime())) return isoMois
  const s = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(d)
  return s.charAt(0).toUpperCase() + s.slice(1)
}

const LIBELLES_LIGNE: Record<string, string> = {
  VALIDE: 'Validé',
  EN_ATTENTE: 'En attente',
  EN_COURS: 'En cours',
  TERMINE: 'Terminée',
  NON_PAYEE: 'Non payé',
  PARTIEL: 'Partiel',
  PAYEE: 'Payée',
  ANNULEE: 'Annulée',
}

/** Libellé d'affichage d'une ligne (service ou désignation du débours). */
function libelleLigne(ligne: LigneService): string {
  if (ligne.categorie === 'DEBOURS') return ligne.designationDebours ?? 'Débours'
  return ligne.libelleLigne ?? ligne.nomService ?? '—'
}

function LignePill({ statut }: { statut: string | null }) {
  if (!statut) return <span className="pill wt">● En préparation</span>
  const classe =
    statut === 'EN_COURS' ? 'pill info'
    : statut === 'TERMINE' || statut === 'VALIDE' || statut === 'PAYEE' ? 'pill ok'
    : statut === 'ANNULEE' ? 'pill danger'
    : statut === 'PARTIEL' || statut === 'NON_PAYEE' ? 'pill warning'
    : 'pill wt'
  return <span className={classe}>● {LIBELLES_LIGNE[statut] ?? statut}</span>
}

function EcheancePill({ statut }: { statut: string }) {
  const classe =
    statut === 'PAYEE' ? 'pill ok'
    : statut === 'PARTIELLE' ? 'pill warning'
    : statut === 'EN_RETARD' ? 'pill danger'
    : statut === 'PROCHE' ? 'pill warning'
    : 'pill wt'
  const libelles: Record<string, string> = {
    PAYEE: 'Payée',
    PARTIELLE: 'Partielle',
    EN_RETARD: 'En retard',
    PROCHE: 'Proche',
    A_VENIR: 'À venir',
  }
  return <span className={classe}>● {libelles[statut] ?? statut}</span>
}

/** Détail d'une demande multi-services branché sur l'API (route /demandes/:reference). */
export function DemandeDetailPage() {
  const { reference } = useParams<{ reference: string }>()
  const { hasPermission } = useAuth()
  const [paramsRecherche] = useSearchParams()
  const ongletInitial = paramsRecherche.get('onglet')
  const [onglet, setOnglet] = useState<Onglet>(
    ongletInitial !== null && (ONGLETS_VALIDES as string[]).includes(ongletInitial)
      ? (ongletInitial as Onglet)
      : 'informations',
  )
  const [editOpen, setEditOpen] = useState(false)
  const [statutOpen, setStatutOpen] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [traitement, setTraitement] = useState(false)

  const { data: demande, loading, error, reload } = useApi<Demande | null>(
    () => (!reference ? Promise.resolve(null) : demandesApi.getByReference(reference)),
    [reference],
  )
  const demandeId = demande?.id
  const { data: historique, reload: reloadHistorique } = useApi<HistoriqueDemande[]>(
    () => (demandeId === undefined ? Promise.resolve([]) : demandesApi.historique(demandeId)),
    [demandeId, onglet],
  )
  const { data: factures, reload: reloadFactures } = useApi<Facture[]>(
    () => (demandeId === undefined ? Promise.resolve([]) : facturesApi.list(demandeId)),
    [demandeId, onglet],
  )

  if (loading) return <Spinner label="Chargement de la demande…" />
  if (error || !demande) {
    return (
      <div className="app-page responsables-page">
        <div className="alert alert-danger">{error ?? 'Demande introuvable.'}</div>
        <Link to="/demandes" className="btn-ghost" style={{ alignSelf: 'flex-start' }}>
          Retour à la liste
        </Link>
      </div>
    )
  }

  const modifiable = demande.statutActuel === 'EN_ATTENTE'
  const progression =
    (demande.montantTotal ?? 0) === 0
      ? 0
      : Math.round((demande.montantPaye / (demande.montantTotal ?? 1)) * 100)

  const toutRecharger = () => {
    reload()
    reloadHistorique()
    reloadFactures()
  }

  const handleStatut = async (nouveau: StatutDemande, commentaire: string) => {
    setTraitement(true)
    setErreur(null)
    try {
      await demandesApi.changeStatut(demande.id, { statut: nouveau, commentaire: commentaire || null })
      setStatutOpen(false)
      setMessage(`Statut passé à ${nouveau}.`)
      toutRecharger()
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Échec du changement de statut')
    } finally {
      setTraitement(false)
    }
  }

  const telechargerRecapitulatif = async () => {
    setErreur(null)
    try {
      const { data } = await http.get<Blob>(`/demandes/${demande.id}/documents/recapitulatif`, {
        responseType: 'blob',
      })
      const url = URL.createObjectURL(data)
      const a = document.createElement('a')
      a.href = url
      a.download = `recapitulatif-${demande.referenceDemande}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setErreur('Téléchargement du récapitulatif impossible')
    }
  }

  const telechargerProforma = async (facture: Facture) => {    setErreur(null)
    try {
      const { data } = await http.get<Blob>(`/factures/${facture.id}/document/proforma`, {
        responseType: 'blob',
      })
      const url = URL.createObjectURL(data)
      const a = document.createElement('a')
      a.href = url
      a.download = `${facture.referenceFacture}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setErreur('Téléchargement du PDF impossible')
    }
  }

  const onglets: Array<[Onglet, string]> = [
    ['informations', 'Informations'],
    ['services', `Services (${demande.lignes?.length ?? 0})`],
    ['factures', `Factures (${factures?.length ?? 0})`],
    ...((demande.lignes ?? []).some((l) => l.typePrestation === 'ABONNEMENT_MENSUEL')
      ? [['factures-mensuelles', 'Factures mensuelles'] as [Onglet, string]]
      : []),
    ...(hasPermission('DOCUMENT_DEMANDE_VIEW')
      ? [['documents', 'Documents'] as [Onglet, string]]
      : []),
    ['paiements', 'Paiements'],
    ['historique', 'Historique'],
    ...((demande.lignes ?? []).some((l) => l.typeService === 'CREATION')
      ? [['dossier', 'Dossier entreprise'] as [Onglet, string]]
      : []),
    ...((demande.lignes ?? []).some((l) => l.typeService === 'TRANSACTION_IMMOBILIERE')
      ? [['dossier-transaction', 'Dossier transaction immobilière'] as [Onglet, string]]
      : []),
  ]

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>{demande.referenceDemande}</h1>
          <p>
            <StatutDemandePill statut={demande.statutActuel} /> — {demande.nomSociete ?? 'Création d’entreprise'}
          </p>
        </div>
        <div className="actions">
          <Link to="/demandes" className="btn-ghost">
            Retour
          </Link>
          <button type="button" className="btn-ghost" onClick={() => void telechargerRecapitulatif()}>
            Récapitulatif PDF
          </button>
          {modifiable && (
            <Can permission="DEMANDE_UPDATE">
              <button type="button" className="btn-ghost" onClick={() => setEditOpen(true)}>
                Modifier
              </button>
            </Can>
          )}
          <Can permission="DEMANDE_STATUS_UPDATE">
            <button type="button" className="btn-primary" onClick={() => setStatutOpen(true)}>
              Changer le statut
            </button>
          </Can>
        </div>
      </header>

      {message && (
        <div className="alert alert-success" role="status">
          {message}
        </div>
      )}
      {erreur && (
        <div className="alert alert-danger" role="alert">
          {erreur}
        </div>
      )}

      <DemandeStepper statut={demande.statutActuel} />

      <nav className="tabs" aria-label="Sections de la demande">
        {onglets.map(([tabId, label]) => (
          <button
            key={tabId}
            type="button"
            role="tab"
            aria-selected={onglet === tabId}
            className={`tab${onglet === tabId ? ' tab--active' : ''}`}
            onClick={() => setOnglet(tabId)}
          >
            {label}
          </button>
        ))}
      </nav>

      {onglet === 'informations' && (
        <section className="card card-pad">
          <h3 style={{ margin: '0 0 14px', fontSize: 17, fontWeight: 700 }}>
            Informations générales
          </h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Référence</span>
              <b>{demande.referenceDemande}</b>
            </div>
            <div className="info-item">
              <span>Client</span>
              <b>{demande.nomSociete ?? '—'}</b>
            </div>
            <div className="info-item">
              <span>Responsable</span>
              <b>{demande.nomResponsable ?? '—'}</b>
            </div>
            <div className="info-item">
              <span>Commercial</span>
              <b>{demande.nomCommercial ?? '—'}</b>
            </div>
            <div className="info-item">
              <span>Date de demande</span>
              <b>{formatDate(demande.dateDemande)}</b>
            </div>
            <div className="info-item">
              <span>Montant total</span>
              <b>{formatMontantMGA(demande.montantTotal)}</b>
            </div>
            <div className="info-item">
              <span>Payé</span>
              <b>{formatMontantMGA(demande.montantPaye)} ({progression} %)</b>
            </div>
            <div className="info-item">
              <span>Reste à payer</span>
              <b>{formatMontantMGA(demande.resteAPayer)}</b>
            </div>
            {demande.commentaire && (
              <div className="info-item info-item--full">
                <span>Commentaire</span>
                <b>{demande.commentaire}</b>
              </div>
            )}
          </div>
        </section>
      )}

      {onglet === 'services' && (
        <ServicesTab
          demande={demande}
          factures={factures ?? []}
          onChange={() => {
            toutRecharger()
          }}
          onErreur={setErreur}
          onMessage={setMessage}
        />
      )}

      {onglet === 'factures' && (
        <FacturesTab
          factures={factures ?? []}
          demandeId={demande.id}
          demandeReference={demande.referenceDemande}
          traitement={traitement}
          onTelecharger={(f) => void telechargerProforma(f)}
          onErreur={setErreur}
          onMessage={setMessage}
          onChange={toutRecharger}
        />
      )}
      {onglet === 'factures-mensuelles' && (
        <FacturesAbonnementSection
          demande={demande}
          onErreur={setErreur}
          onMessage={setMessage}
        />
      )}

      {onglet === 'paiements' && <PaiementsTab demande={demande} />}

      {onglet === 'documents' && (
        <PiecesJointes cible="demande" id={demande.id} titre="Pièces jointes de la demande" />
      )}

      {onglet === 'dossier' && <DossierTab demande={demande} />}

      {onglet === 'dossier-transaction' && (
        <DossierTransactionTab demande={demande} />
      )}

      {onglet === 'historique' && (
        <section className="card">
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Date et heure</th>
                    <th>Statut</th>
                    <th>Commentaire</th>
                    <th>Utilisateur</th>
                  </tr>
                </thead>
                <tbody>
                  {(historique ?? []).map((h) => (
                    <tr key={h.id}>
                      <td className="text-secondary">{formatDate(h.createdAt)}</td>
                      <td>
                        <StatutDemandePill statut={h.statut} />
                      </td>
                      <td>{h.commentaire ?? '—'}</td>
                      <td>{h.nomUtilisateur ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

      <ChangerStatutModal
        open={statutOpen}
        statutActuel={demande.statutActuel}
        onClose={() => setStatutOpen(false)}
        onConfirm={(nouveau, commentaire) => void handleStatut(nouveau, commentaire)}
        enregistrement={traitement}
      />

      <ModifierDemandeModal
        open={editOpen}
        demande={demande}
        onClose={() => setEditOpen(false)}
        onSaved={() => {
          setEditOpen(false)
          setMessage('Demande modifiée.')
          toutRecharger()
        }}
        onErreur={setErreur}
      />
    </div>
  )
}

/** Onglet Factures : aperçu d'ensemble, un seul bouton de validation (avec confirmation). */
/** Libellé affiché pour le type de facture validée. */
function pillTypeValidee(type: string): string {
  switch (type) {
    case 'DEBOURS':
      return 'Débours'
    case 'PRESTATION':
      return 'Prestation'
    case 'ABONNEMENT_MENSUEL':
      return 'Abonnement'
    case 'CREATION':
      return 'Création'
    case 'TRANSACTION_IMMOBILIERE':
      return 'Transaction'
    default:
      return 'Externalisation'
  }
}

function FacturesTab({
  factures,
  demandeId,
  demandeReference,
  traitement,
  onTelecharger,
  onErreur,
  onMessage,
  onChange,
}: {
  factures: Facture[]
  demandeId: number
  demandeReference: string
  traitement: boolean
  onTelecharger: (facture: Facture) => void
  onErreur: (message: string | null) => void
  onMessage: (message: string | null) => void
  onChange: () => void
}) {
  const [confirmValidation, setConfirmValidation] = useState(false)
  const [validationEnCours, setValidationEnCours] = useState(false)
  /** Tarif saisi par ligne service (défaut = tarif proforma). */
  const [tarifsModifies, setTarifsModifies] = useState<Record<number, number | ''>>({})
  const [apercu, setApercu] = useState<FactureApercu | null>(null)
  const navigate = useNavigate()
  const { data: facturesValidees, reload: reloadValidees } = useApi<FactureValidee[]>(
    () => facturesValideesApi.list(demandeId).catch(() => []),
    [demandeId],
  )
  useEffect(() => {
    // Recharger les factures validées après chaque génération / validation / paiement.
    reloadValidees()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [factures])
  // Les factures du mois d'abonnement restent dans l'onglet « Factures mensuelles ».
  const validees = (facturesValidees ?? []).filter((f) => f.type !== 'ABONNEMENT_MENSUEL')
  const nbSoldees = validees.filter((f) => f.resteAPayer <= 0).length
  const proformas = factures.filter((f) => f.statut === 'PROFORMA')
  const toutesLignes = factures.flatMap((f) =>
    f.lignes.map((l) => ({ ...l, referenceFacture: f.referenceFacture })),
  )
  const montantLigneFacture = (l: { tarif: number | null; tarifProforma?: number | null }): number =>
    l.tarif ?? l.tarifProforma ?? 0
  const totalApercu = toutesLignes.reduce((s, l) => s + montantLigneFacture(l), 0)
  const tarifEffectif = (l: {
    ligneServiceId: number
    tarif: number | null
    tarifProforma?: number | null
  }): number => {
    const v = tarifsModifies[l.ligneServiceId]
    return v === '' || v === undefined ? montantLigneFacture(l) : Number(v)
  }
  const totalProformas = proformas
    .flatMap((f) => f.lignes)
    .reduce((s, l) => s + tarifEffectif(l), 0)

  const handleValiderTout = async () => {
    setConfirmValidation(false)
    setValidationEnCours(true)
    try {
      for (const f of proformas) {
        const lignes = f.lignes.map((l) => ({
          ligneServiceId: l.ligneServiceId,
          montant: tarifEffectif(l),
        }))
        await facturesApi.valider(f.id, null, null, lignes)
      }
      onMessage(
        proformas.length === 1
          ? `Facture ${proformas[0].referenceFacture} validée.`
          : `${proformas.length} factures validées.`,
      )
      setTarifsModifies({})
      onChange()
    } catch (err) {
      onErreur(err instanceof Error ? err.message : 'Échec de la validation')
    } finally {
      setValidationEnCours(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <section className="card card-pad">
        <h3 style={{ margin: '0 0 14px', fontSize: 17, fontWeight: 700 }}>
          Aperçu d&apos;ensemble des factures
        </h3>
        {factures.length === 0 && (
          <p className="section-note">Aucune facture générée pour cette demande.</p>
        )}
        {toutesLignes.length > 0 && (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Service</th>
                    <th>Période</th>
                    <th>Tarif</th>
                    <th>Facture</th>
                  </tr>
                </thead>
                <tbody>
                  {toutesLignes.map((l) => (
                    <tr key={`${l.referenceFacture}-${l.ligneServiceId}`}>
                      <td>
                        <div className="fw-600">{l.nomService ?? l.libelleLigne ?? '—'}</div>
                        {l.nomService && l.libelleLigne && l.libelleLigne !== l.nomService && (
                          <span className="text-secondary" style={{ fontSize: 12 }}>{l.libelleLigne}</span>
                        )}
                      </td>
                      <td className="text-secondary">
                        {formatDate(l.debutService)} → {formatDate(l.finService)}
                      </td>
                      <td>{formatMontantMGA(montantLigneFacture(l))}</td>
                      <td>
                        <button
                          type="button"
                          className="btn-link"
                          onClick={() => {
                            const f = factures.find((x) => x.referenceFacture === l.referenceFacture)
                            if (f) setApercu({ kind: 'proforma', facture: f })
                          }}
                        >
                          {l.referenceFacture}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
        <div className="info-grid" style={{ marginTop: 8 }}>
          <div className="info-item">
            <span>Nombre de factures</span>
            <b>{factures.length}</b>
          </div>
          <div className="info-item">
            <span>Total facturé</span>
            <b>{formatMontantMGA(totalApercu)}</b>
          </div>
          <div className="info-item">
            <span>En attente de validation</span>
            <b>{proformas.length}</b>
          </div>
          <div className="info-item">
            <span>Factures validées</span>
            <b>{validees.length}</b>
          </div>
          <div className="info-item">
            <span>Factures soldées</span>
            <b style={{ color: 'var(--success)' }}>{nbSoldees}</b>
          </div>
        </div>
        {proformas.length > 0 && (
          <Can permission="FACTURE_VALIDATE">
            <div style={{ marginTop: 8 }}>
              <button
                type="button"
                className="btn-primary"
                disabled={traitement || validationEnCours}
                onClick={() => setConfirmValidation(true)}
              >
                {validationEnCours
                  ? 'Validation…'
                  : proformas.length === 1
                    ? 'Valider la facture'
                    : `Valider les ${proformas.length} factures`}
              </button>
            </div>
          </Can>
        )}
      </section>

      {/* Factures validées — externalisation (GENERALE) et débours */}
      <section className="card card-pad">
        <h3 style={{ margin: '0 0 8px', fontSize: 17, fontWeight: 700 }}>
          Factures validées
        </h3>
        {validees.length === 0 && (
          <p className="section-note" style={{ margin: 0 }}>
            Aucune facture validée sur cette demande pour l&apos;instant. La proforma
            se valide ci-dessus ; une facture débours est créée automatiquement à la
            validation de son montant (onglet Services).
          </p>
        )}
        {validees.length > 0 && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Réf. validée</th>
                  <th>Type</th>
                  <th>Ligne</th>
                  <th>Proforma</th>
                  <th>Montant</th>
                  <th>Payé / reste</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {validees.map((f) => {
                  const montantPaye = f.montantPaye ?? 0
                  const reste = f.resteAPayer ?? Math.max(f.montant - montantPaye, 0)
                  const taux = f.montant > 0 ? Math.min(100, Math.round((montantPaye / f.montant) * 100)) : 0
                  const estGeneraleUnique = f.type === 'GENERALE' && f.modeFacturation === 'UNIQUE'
                  return (
                    <tr key={f.id}>
                      <td className="fw-600">
                        <button
                          type="button"
                          className="btn-link"
                          onClick={() => setApercu({ kind: 'validee', facture: f })}
                        >
                          {f.referenceFactureValidee}
                        </button>
                      </td>
                      <td>
                        <span className={f.type === 'DEBOURS' ? 'pill wt' : 'pill ok'}>
                          {pillTypeValidee(f.type)}
                        </span>
                      </td>
                      <td>
                        {f.lignes && f.lignes.length > 0 ? (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                            {f.lignes.map((l) => (
                              <div key={l.id ?? l.ligneServiceId ?? l.designation}>
                                <div className="fw-600">
                                  {l.nomService && l.nomService !== l.designation ? l.nomService : l.designation}
                                </div>
                                {l.nomService && l.nomService !== l.designation && (
                                  <span className="text-secondary" style={{ fontSize: 11 }}>{l.designation}</span>
                                )}
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="fw-600">{f.nomService ?? f.libelleLigne ?? '—'}</div>
                        )}
                      </td>
                      <td className="text-secondary">
                        {f.referenceFactureProforma ?? '— (facture sans proforma)'}
                      </td>
                      <td>{formatMontantMGA(f.montant)}</td>
                      <td>
                        {estGeneraleUnique ? (
                          <span className="text-secondary" style={{ fontSize: 12 }}>
                            Règlement par service (chaque facture de service porte sa référence)
                          </span>
                        ) : (
                          <div className="progression">
                            <div className="progression__label">
                              <span>{formatMontantMGA(montantPaye)} payé</span>
                              <strong style={{ color: reste > 0 ? 'var(--danger)' : 'var(--success)' }}>
                                {reste > 0 ? `${formatMontantMGA(reste)} restants` : 'Soldée'}
                              </strong>
                            </div>
                            <div
                              className="trk"
                              role="progressbar"
                              aria-valuenow={taux}
                              aria-valuemin={0}
                              aria-valuemax={100}
                            >
                              <i style={{ width: `${taux}%` }} />
                            </div>
                          </div>
                        )}
                      </td>
                      <td>
                        {estGeneraleUnique ? (
                          <span className="text-secondary">—</span>
                        ) : reste > 0 ? (
                          <button
                            type="button"
                            className="btn-primary"
                            onClick={() => navigate(
                              `/paiements/nouveau?demande=${encodeURIComponent(demandeReference)}&ligne=${f.ligneServiceId ?? ''}`,
                            )}
                          >
                            Encaisser
                          </button>
                        ) : (
                          <span className="pill ok">Soldée</span>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {factures.map((f) => (
        <section key={f.id} className="card card-pad">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <button
              type="button"
              className="btn-link fw-600"
              style={{ textAlign: 'left', fontSize: 'inherit', color: 'inherit' }}
              onClick={() => setApercu({ kind: 'proforma', facture: f })}
            >
              Proforma {f.referenceFacture} — {formatMontantMGA(f.montantProforma ?? f.montant)}
              {f.montantValidee != null && ` → validée ${formatMontantMGA(f.montantValidee)}`}
            </button>
            <span className={f.statut === 'VALIDEE' ? 'pill ok' : 'pill wt'}>
              ● {f.statut === 'VALIDEE' ? 'Validée' : 'Proforma'}
            </span>
          </div>
          <p className="section-note">
            Mode {f.modeFacturation === 'UNIQUE' ? 'unique' : 'par service'}
            {f.referenceFactureValidee ? ` — réf. validée : ${f.referenceFactureValidee}` : ''}
            {f.valideParNom ? ` — validée par ${f.valideParNom}` : ''}
            {f.valideLe ? ` le ${formatDate(f.valideLe.slice(0, 10))}` : ''}
          </p>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button type="button" className="btn-ghost" onClick={() => onTelecharger(f)}>
              Télécharger le PDF
            </button>
          </div>
        </section>
      ))}

      <ConfirmDialog
        open={confirmValidation}
        titre={proformas.length === 1 ? 'Valider cette facture ?' : `Valider ces ${proformas.length} factures ?`}
        message={
          proformas.length === 1
            ? `La facture ${proformas[0]?.referenceFacture ?? ''} sera validée et le service figé (ni modification, ni retrait).`
            : `Les ${proformas.length} factures proforma seront validées et les services concernés seront figés.`
        }
        details={
          proformas.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 12, maxHeight: 360, overflowY: 'auto' }}>
              <p className="section-note" style={{ margin: 0, fontSize: 12 }}>
                Ajustez chaque montant validé (défaut = tarif proforma). Le tarif du service passe
                à cette valeur ; la valeur dans le proforma ne change pas.
              </p>
              {proformas.map((f) => {
                const totalF = f.lignes.reduce((s, l) => s + tarifEffectif(l), 0)
                return (
                  <div
                    key={f.id}
                    style={{ border: '1px solid var(--bordure, #e5e7eb)', borderRadius: 8, padding: 10 }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 12 }}>
                      <div>
                        <strong style={{ fontSize: 14 }}>{f.referenceFacture}</strong>
                        <span className="text-secondary"> · {f.modeFacturation === 'UNIQUE' ? 'unique' : 'par service'}</span>
                      </div>
                      <span className="text-secondary">
                        {f.lignes.length === 1 ? '1 ligne' : `${f.lignes.length} lignes`} — sous-total <b>{formatMontantMGA(totalF)}</b>
                      </span>
                    </div>
                    {f.lignes.map((l) => {
                      const vInput = tarifsModifies[l.ligneServiceId]
                      const debours = l.categorie === 'DEBOURS'
                      return (
                        <label key={l.ligneServiceId} className="field" style={{ marginTop: 8 }}>
                          <b>
                            {l.nomService ?? l.libelleLigne ?? `Service ${l.ligneServiceId}`}
                            {debours && (
                              <span className="pill wt" style={{ marginLeft: 8, fontSize: 11 }}>Débours — figé</span>
                            )}
                          </b>
                          {l.libelleLigne && (
                            <span className="text-secondary" style={{ fontSize: 11, display: 'block' }}>
                              {l.libelleLigne}
                            </span>
                          )}
                          <input
                            type="number"
                            min={0}
                            step="1"
                            value={debours ? montantLigneFacture(l) : vInput ?? montantLigneFacture(l)}
                            disabled={debours}
                            onChange={(e) =>
                              setTarifsModifies((prev) => ({
                                ...prev,
                                [l.ligneServiceId]: e.target.value === '' ? '' : Number(e.target.value),
                              }))
                            }
                          />
                          {debours && (
                            <span className="form-help">
                              Montant annoncé à la création — constaté tel quel à la validation.
                            </span>
                          )}
                        </label>
                      )
                    })}
                  </div>
                )
              })}
              <div style={{ display: 'flex', justifyContent: 'flex-end', fontWeight: 600 }}>
                Total à valider&nbsp;: <b style={{ marginLeft: 6 }}>{formatMontantMGA(totalProformas)}</b>
              </div>
            </div>
          ) : undefined
        }
        confirmer="Valider"
        ton="success"
        onAnnuler={() => {
          setConfirmValidation(false)
          setTarifsModifies({})
        }}
        onConfirmer={() => void handleValiderTout()}
      />
      <FactureApercuModal
        apercu={apercu}
        onClose={() => setApercu(null)}
      />
    </div>
  )
}
function ServicesTab({
  demande,
  factures,
  onChange,
  onErreur,
  onMessage,
}: {
  demande: Demande
  factures: Facture[]
  onChange: () => void
  onErreur: (message: string | null) => void
  onMessage: (message: string | null) => void
}) {
  const modifiable = demande.statutActuel === 'EN_ATTENTE'
  const statutFacture = new Map(factures.map((f) => [f.id, f.statut]))
  /** Ligne figée : facture validée (ni modification, ni retrait, ni plan à refaire). */
  const fige = (ligne: LigneService): boolean => {
    if (ligne.factureId === null) return false
    return statutFacture.get(ligne.factureId) !== 'PROFORMA'
  }
  const toutFige = (demande.lignes ?? []).length > 0
    && (demande.lignes ?? []).every((l) => fige(l))
  const [ajoutOuvert, setAjoutOuvert] = useState(false)
  const [validationDebours, setValidationDebours] = useState<LigneService | null>(null)
  const [annulationDebours, setAnnulationDebours] = useState<LigneService | null>(null)
  const [terminaisonAbo, setTerminaisonAbo] = useState<LigneService | null>(null)
  const [edition, setEdition] = useState<LigneService | null>(null)
  const contientTypeSolo = (demande.lignes ?? []).some((l) =>
    estTypeServiceSolo(l.typeService),
  )
  const [retrait, setRetrait] = useState<LigneService | null>(null)
  const [planPour, setPlanPour] = useState<LigneService | null>(null)
  const [traitement, setTraitement] = useState(false)

  const handleValiderDebours = async (montant: number) => {
    if (!validationDebours) return
    setTraitement(true)
    try {
      await lignesApi.validerMontantDebours(demande.id, validationDebours.id, { montantDebours: montant })
      setValidationDebours(null)
      onMessage('Montant du débours validé.')
      onChange()
    } catch (err) {
      onErreur(err instanceof Error ? err.message : 'Échec de la validation du montant')
    } finally {
      setTraitement(false)
    }
  }

  const handleTerminerAbonnement = async () => {
    if (!terminaisonAbo) return
    setTraitement(true)
    try {
      await lignesApi.terminerAbonnement(demande.id, terminaisonAbo.id)
      setTerminaisonAbo(null)
      onMessage("Abonnement terminé. La demande se clôturera si tous les services sont terminés ou payés.")
      onChange()
    } catch (err) {
      onErreur(err instanceof Error ? err.message : "Échec de la terminaison de l’abonnement")
    } finally {
      setTraitement(false)
    }
  }

  const handleAnnulerDebours = async () => {
    if (!annulationDebours) return
    setTraitement(true)
    try {
      await lignesApi.annulerDebours(demande.id, annulationDebours.id)
      setAnnulationDebours(null)
      onMessage('Débours annulé.')
      onChange()
    } catch (err) {
      onErreur(err instanceof Error ? err.message : 'Échec de la validation du montant')
    } finally {
      setTraitement(false)
    }
  }

  const handleRetrait = async () => {
    if (!retrait) return
    setTraitement(true)
    try {
      await lignesApi.remove(demande.id, retrait.id)
      setRetrait(null)
      onMessage('Service retiré ; facture actualisée.')
      onChange()
    } catch (err) {
      onErreur(err instanceof Error ? err.message : 'Échec du retrait')
    } finally {
      setTraitement(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {(demande.lignes ?? []).map((ligne) => (
        <LigneCard
          key={ligne.id}
          demandeId={demande.id}
          ligne={ligne}
          modifiable={ligne.categorie === 'DEBOURS' ? ligne.statut === 'EN_ATTENTE' : modifiable && !fige(ligne)}
          planEtablissable={ligne.categorie !== 'DEBOURS' && statutFacture.get(ligne.factureId ?? -1) === 'VALIDEE'}
          onEdit={() => setEdition(ligne)}
          onRemove={() => setRetrait(ligne)}
          onPlan={() => setPlanPour(ligne)}
          onValiderDebours={() => setValidationDebours(ligne)}
          onAnnulerDebours={() => setAnnulationDebours(ligne)}
          onTerminerAbonnement={() => setTerminaisonAbo(ligne)}
        />
      ))}
      {modifiable && !toutFige && !contientTypeSolo && (
        <Can permission="DEMANDE_UPDATE">
          <div>
            <button type="button" className="btn-ghost" onClick={() => setAjoutOuvert(true)}>
              + Ajouter une ligne
            </button>
          </div>
        </Can>
      )}

      <LigneFormModal
        open={ajoutOuvert || edition !== null}
        ligne={edition}
        servicesPris={(demande.lignes ?? [])
          .filter((l) => l.id !== edition?.id)
          .map((l) => l.serviceId)
          .filter((id): id is number => id !== null)}
        typesPris={(demande.lignes ?? [])
          .filter((l) => l.id !== edition?.id)
          .map((l) => l.typeService)}
        onClose={() => {
          setAjoutOuvert(false)
          setEdition(null)
        }}
        onSaved={() => {
          setAjoutOuvert(false)
          setEdition(null)
          onMessage(edition ? 'Service modifié.' : 'Service ajouté ; facture actualisée.')
          onChange()
        }}
        onErreur={onErreur}
        demandeId={demande.id}
      />

      <ConfirmDialog
        open={terminaisonAbo !== null}
        titre="Terminer cet abonnement ?"
        message={`La ligne « ${terminaisonAbo?.nomService ?? 'Abonnement mensuel'} » passera à « Terminée ». La demande se clôturera automatiquement si tous les services sont terminés ou payés.`}
        confirmer="Terminer"
        ton="success"
        onAnnuler={() => setTerminaisonAbo(null)}
        onConfirmer={() => void handleTerminerAbonnement()}
      />

      <ConfirmDialog
        open={retrait !== null}
        titre="Retirer ce service ?"
        message={`« ${retrait?.nomService ?? ''} » sera retiré de la demande et sa contribution aux factures sera actualisée.`}
        confirmer="Retirer"
        ton="danger"
        onAnnuler={() => setRetrait(null)}
        onConfirmer={() => void handleRetrait()}
      />

      <ValiderMontantDeboursModal
        ligne={validationDebours}
        onClose={() => setValidationDebours(null)}
        onConfirm={(montant) => void handleValiderDebours(montant)}
        onErreur={onErreur}
      />

      <ConfirmDialog
        open={annulationDebours !== null}
        titre="Annuler ce débours ?"
        message={`Le débours « ${annulationDebours?.designationDebours ?? ''} » (ligne ${annulationDebours?.id ?? ''}) sera annulé. Cette action n est possible que pendant « En attente ».`}
        confirmer="Annuler le débours"
        ton="danger"
        onAnnuler={() => setAnnulationDebours(null)}
        onConfirmer={() => void handleAnnulerDebours()}
      />

      <PlanModal
        ligne={planPour}
        onClose={() => setPlanPour(null)}
        onSaved={() => {
          setPlanPour(null)
          onMessage('Plan de paiement établi.')
          onChange()
        }}
        onErreur={onErreur}
      />
      {traitement && <Spinner label="Traitement…" />}
    </div>
  )
}

function LigneCard({
  demandeId,
  ligne,
  modifiable,
  planEtablissable,
  onEdit,
  onRemove,
  onPlan,
  onValiderDebours,
  onAnnulerDebours,
  onTerminerAbonnement,
}: {
  demandeId: number
  ligne: LigneService
  modifiable: boolean
  /** Bouton « Établir le plan » visible uniquement si la facture est validée. */
  planEtablissable: boolean
  onEdit: () => void
  onRemove: () => void
  onPlan: () => void
  onValiderDebours: () => void
  onAnnulerDebours: () => void
  onTerminerAbonnement: () => void
}) {
  const { data: plan } = useApi<PlanPaiement | null>(
    () => planPaiementApi.getByLigne(ligne.id).catch(() => null),
    [ligne.id],
  )
  const { data: echeances } = useApi<Echeance[]>(
    () => (plan ? echeancesApi.listByPlan(plan.id) : Promise.resolve([])),
    [plan?.id],
  )
  const echeancesTriees = useMemo(
    () =>
      [...(echeances ?? [])].sort(
        (a, b) =>
          a.dateEcheance.localeCompare(b.dateEcheance) ||
          a.numeroEcheance - b.numeroEcheance,
      ),
    [echeances],
  )
  const navigate = useNavigate()
  const [voirEcheances, setVoirEcheances] = useState(true)
  const [voirHistorique, setVoirHistorique] = useState(false)
  const estDebours = ligne.categorie === 'DEBOURS'
  const estPrestationOuAbonnement = !estDebours
    && (ligne.typePrestation === 'PRESTATION' || ligne.typePrestation === 'ABONNEMENT_MENSUEL')

  return (
    <section className="card card-pad">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <strong>
          {estDebours
            ? `${ligne.designationDebours ?? 'Débours'} — ${formatMontantMGA(ligne.tarif ?? ligne.montantDebours ?? 0)}`
            : `${ligne.nomService ?? libelleLigne(ligne)} — ${formatMontantMGA(ligne.tarif ?? ligne.montantDebours ?? 0)}`}
        </strong>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          <span className={`pill ${estDebours ? 'pill warning' : 'pill wt'}`}>● {estDebours ? 'Débours' : 'Externalisation'}</span>
          {!estDebours && ligne.typeService === 'STANDARD' && ligne.typePrestation && (
            <span className="pill info">● {getLibelleTypePrestation(ligne.typePrestation)}</span>
          )}
          <LignePill statut={ligne.statut} />
        </div>
      </div>
      <div className="info-grid" style={{ marginTop: 8 }}>
        {estDebours ? (
          <>
            <div className="info-item">
              <span>Désignation</span>
              <b>{ligne.designationDebours ?? '—'}</b>
            </div>
            <div className="info-item">
              <span>Montant validé</span>
              <b>{ligne.montantDebours ? formatMontantMGA(ligne.montantDebours) : '— (non validé)'}</b>
            </div>
            <div className="info-item">
              <span>Payé / Reste</span>
              <b>
                {formatMontantMGA(ligne.montantPaye)} / {formatMontantMGA(ligne.resteAPayer)}
              </b>
            </div>
          </>
        ) : (
          <>
        <div className="info-item">
          <span>Période</span>
          <b>
            {formatDate(ligne.debutService)} → {formatDate(ligne.finService)}
          </b>
        </div>
        <div className="info-item">
          <span>Payé / Reste</span>
          <b>
            {formatMontantMGA(ligne.montantPaye)} / {formatMontantMGA(ligne.resteAPayer)}
          </b>
        </div>
        <div className="info-item">
          <span>Facture</span>
          <b>{ligne.referenceFacture ?? '—'}</b>
        </div>
          </>
        )}
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap' }}>
        {!estDebours && (
          <button type="button" className="btn-ghost" onClick={() => setVoirEcheances((v) => !v)}>
            Échéances {voirEcheances ? '▴' : '▾'}
          </button>
        )}
        <button type="button" className="btn-ghost" onClick={() => setVoirHistorique((v) => !v)}>
          Historique {voirHistorique ? '▴' : '▾'}
        </button>
        {estDebours && ligne.statut === 'EN_ATTENTE' && (
          <Can permission="DEMANDE_STATUS_UPDATE">
            <button type="button" className="btn-primary" onClick={onValiderDebours}>
              Valider le montant
            </button>
            <button type="button" className="btn-ghost" onClick={onAnnulerDebours}>
              Annuler le débours
            </button>
          </Can>
        )}
        {estDebours && (ligne.statut === 'EN_COURS' || ligne.statut === 'PARTIEL') && (
          <Can permission="PAIEMENT_CREATE">
            <button type="button" className="btn-primary" onClick={() => navigate(`/paiements/nouveau?demande=${encodeURIComponent(ligne.referenceDemande)}&ligne=${ligne.id}`)}>
              Enregistrer un paiement
            </button>
          </Can>
        )}
        {estDebours && ligne.statut === 'PAYEE' && (
          <span className="pill ok">● Débours soldé</span>
        )}
        {ligne.typePrestation === 'ABONNEMENT_MENSUEL' && (ligne.statut === 'EN_COURS' || ligne.statut === 'PARTIEL') && (
          <Can permission="DEMANDE_STATUS_UPDATE">
            <button type="button" className="btn-ghost" onClick={onTerminerAbonnement}>
              Terminer l’abonnement
            </button>
          </Can>
        )}
        {ligne.typePrestation === 'ABONNEMENT_MENSUEL' && ligne.statut === 'TERMINE' && (
          <span className="pill ok">● Abonnement terminé</span>
        )}
      </div>

      {!estDebours && !estPrestationOuAbonnement && plan && voirEcheances && (
        <div className="table-scroll-container no-global-scroll" style={{ marginTop: 8 }}>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>N°</th>
                  <th>Date</th>
                  <th>Attendu</th>
                  <th>Payé</th>
                  <th>Reste</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                {echeancesTriees.map((e) => (
                  <tr key={e.id}>
                    <td>{e.numeroEcheance}</td>
                    <td className="text-secondary">{formatDate(e.dateEcheance)}</td>
                    <td>{formatMontantMGA(e.montantAttendu)}</td>
                    <td>{formatMontantMGA(e.montantPaye)}</td>
                    <td>{formatMontantMGA(e.reste)}</td>
                    <td>
                      <EcheancePill statut={e.statut} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
      {!estDebours && !estPrestationOuAbonnement && !plan && voirEcheances && (
        <p className="section-note">Aucun plan de paiement pour ce service.</p>
      )}
      {estPrestationOuAbonnement && voirEcheances && (
        <p className="section-note">
          {ligne.typePrestation === 'PRESTATION'
            ? 'Prestation : paiement direct après validation de la facture, sans échéance ni plan de paiement.'
            : 'Abonnement mensuel : paiement lié à la facture validée, sans échéance ni plan de paiement.'}
        </p>
      )}

      {voirHistorique && <LigneHistorique demandeId={demandeId} ligneId={ligne.id} />}

      {modifiable && (
        <Can permission="DEMANDE_UPDATE">
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button type="button" className="btn-ghost" onClick={onEdit}>
              Modifier
            </button>
            <button type="button" className="btn-ghost" onClick={onRemove}>
              Retirer
            </button>
          </div>
        </Can>
      )}
      {!estDebours && !estPrestationOuAbonnement && !plan && planEtablissable && (
        <Can permission="DEMANDE_UPDATE">
          <div style={{ marginTop: 8 }}>
            <button type="button" className="btn-primary" onClick={onPlan}>
              Établir le plan
            </button>
          </div>
        </Can>
      )}
      {!estDebours && !estPrestationOuAbonnement && !plan && !planEtablissable && (
        <p className="section-note">Le plan sera établissable après validation de la facture.</p>
      )}
    </section>
  )
}

function LigneHistorique({ demandeId, ligneId }: { demandeId: number; ligneId: number }) {
  const { data } = useApi<HistoriqueLigne[]>(
    () => lignesApi.historique(demandeId, ligneId).catch(() => []),
    [demandeId, ligneId],
  )
  if ((data ?? []).length === 0) {
    return <p className="section-note">Aucun historique pour ce service.</p>
  }
  return (
    <div className="table-scroll-container no-global-scroll" style={{ marginTop: 8 }}>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Date et heure</th>
              <th>Statut</th>
              <th>Origine</th>
              <th>Commentaire</th>
              <th>Utilisateur</th>
            </tr>
          </thead>
          <tbody>
            {(data ?? []).map((h) => (
              <tr key={h.id}>
                <td className="text-secondary">{formatDate(h.createdAt)}</td>
                <td>
                  <LignePill statut={h.statut} />
                </td>
                <td>{h.origine === 'AUTO' ? 'Automatique' : 'Manuel'}</td>
                <td>{h.commentaire ?? '—'}</td>
                <td>{h.nomUtilisateur ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function LigneFormModal({
  open,
  ligne,
  demandeId,
  servicesPris,
  typesPris,
  onClose,
  onSaved,
  onErreur,
}: {
  open: boolean
  ligne: LigneService | null
  demandeId: number
  /** Identifiants déjà utilisés : masqués du select (pas de doublon). */
  servicesPris: number[]
  /** Types déjà présents : contrôle des services qui doivent rester seuls. */
  typesPris: (string | null)[]
  onClose: () => void
  onSaved: () => void
  onErreur: (message: string | null) => void
}) {
  const [serviceId, setServiceId] = useState<number | ''>('')
  const [typePrestation, setTypePrestation] = useState<'PRESTATION' | 'ABONNEMENT_MENSUEL'>('PRESTATION')
  const [tarif, setTarif] = useState('')
  const [debut, setDebut] = useState('')
  const [fin, setFin] = useState('')
  const [categorie, setCategorie] = useState<'EXTERNALISATION' | 'DEBOURS'>('EXTERNALISATION')
  const [designation, setDesignation] = useState('')
  const [montantDebours, setMontantDebours] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const [enregistrement, setEnregistrement] = useState(false)
  const [serviceModalOpen, setServiceModalOpen] = useState(false)
  const typeCourant = typePrestation
  const { data: pageServices, reload: reloadServices } = useApi(
    () => servicesApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [open],
  )

  const estServiceStandard = ligne
    ? ligne.typeService === 'STANDARD'
    : (pageServices?.content ?? []).find((s) => s.id === serviceId)?.typeService === 'STANDARD'

  // Pré-remplissage à l'ouverture (montée de version sans effet de bord).
  const cle = open ? `ligne-${ligne?.id ?? 'nouvelle'}` : 'fermee'
  const [cleVue, setCleVue] = useState<string | null>(null)
  if (cleVue !== cle) {
    setCleVue(cle)
    if (open) {
      setServiceId(ligne?.serviceId ?? '')
      setTypePrestation((ligne?.typePrestation as 'PRESTATION' | 'ABONNEMENT_MENSUEL') ?? 'PRESTATION')
      setTarif(ligne?.tarif != null ? String(ligne.tarif) : '')
      setDebut(ligne?.debutService ?? '')
      setFin(ligne?.finService ?? '')
      setCategorie((ligne?.categorie as 'EXTERNALISATION' | 'DEBOURS') ?? 'EXTERNALISATION')
      setDesignation(ligne?.designationDebours ?? '')
      setMontantDebours(ligne?.montantDebours != null ? String(ligne.montantDebours) : '')
      setErreur(null)
    }
  }

  if (!open) return null

  const handleSaveService = async (payload: { nomService: string }) => {
    const cree = await servicesApi.create({ nomService: payload.nomService, typeService: 'STANDARD' })
    reloadServices()
    setServiceId(cree.id)
    setServiceModalOpen(false)
  }

  const handleSubmit = async () => {
    if (categorie === 'DEBOURS') {
      if (!designation.trim()) {
        setErreur('La désignation du débours est obligatoire')
        return
      }
      if (montantDebours.trim() !== '' && Number.isNaN(Number(montantDebours))) {
        setErreur('Le montant du débours doit être un nombre')
        return
      }
      if (montantDebours.trim() !== '' && Number(montantDebours) < 0) {
        setErreur('Le montant du débours doit être positif ou nul')
        return
      }
      setEnregistrement(true)
      setErreur(null)
      try {
        if (ligne) {
          await lignesApi.update(demandeId, ligne.id, { designationDebours: designation.trim() })
        } else {
          await lignesApi.create(demandeId, {
            categorie: 'DEBOURS',
            designationDebours: designation.trim(),
            montantDebours: montantDebours.trim() !== '' ? Number(montantDebours) : null,
          })
        }
        onSaved()
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Échec de la validation'
        setErreur(message)
        onErreur(message)
      } finally {
        setEnregistrement(false)
      }
      return
    }
    if (serviceId === '' && !ligne) {
      setErreur('Le service est obligatoire')
      return
    }
    if (servicesPris.includes(serviceId as number) && !ligne) {
      setErreur('Ce service est déjà présent dans la demande')
      return
    }
    if (!ligne) {
      const typeChoisi = (pageServices?.content ?? []).find((s) => s.id === serviceId)?.typeService
      if (estTypeServiceSolo(typeChoisi) && servicesPris.length > 0) {
        setErreur(
          "Un service de création d'entreprise ou de transaction immobilière ne peut pas être combiné avec d'autres services",
        )
        return
      }
      if (typesPris.some(estTypeServiceSolo)) {
        setErreur(
          'Cette demande contient déjà un service qui doit rester seul ; aucun autre service ne peut être ajouté',
        )
        return
      }
    }
    if (!tarif || Number(tarif) < 0 || Number.isNaN(Number(tarif))) {
      setErreur('Le tarif doit être positif ou nul')
      return
    }
    // Sous-type de la ligne (défaut PRESTATION — même règle que le backend).
    const estPrestation = typeCourant === 'PRESTATION'
    const estAbonnement = typeCourant === 'ABONNEMENT_MENSUEL'
    if (estAbonnement && !debut) {
      setErreur('La date de début est obligatoire pour un abonnement mensuel')
      return
    }
    if (!estPrestation && !estAbonnement && (!debut || !fin)) {
      setErreur('La période est obligatoire')
      return
    }
    if (debut && fin && fin <= debut) {
      setErreur('La fin doit être strictement postérieure au début')
      return
    }
    setEnregistrement(true)
    setErreur(null)
    try {
      if (ligne) {
        await lignesApi.update(demandeId, ligne.id, {
          tarif: Number(tarif),
          debutService: debut || null,
          finService: fin || null,
          typePrestation: estServiceStandard ? typePrestation : null,
        })
      } else {
        await lignesApi.create(demandeId, {
          serviceId: serviceId as number,
          tarif: Number(tarif),
          debutService: debut || null,
          finService: fin || null,
          categorie: 'EXTERNALISATION',
          designationDebours: null,
          typePrestation: estServiceStandard ? typePrestation : null,
        })
      }
      onSaved()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Échec de l’enregistrement'
      setErreur(message)
      onErreur(message)
    } finally {
      setEnregistrement(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 520 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={ligne ? 'Modifier le service' : 'Ajouter un service'}
      >
        <header className="modal-header">
          <h3 className="modal-title">{ligne ? 'Modifier le service' : 'Ajouter un service'}</h3>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>
        <div className="modal-body">
          {erreur && (
            <div className="alert alert-danger" role="alert">
              {erreur}
            </div>
          )}
          <div className="form-grid form-grid--2">
            {!ligne && (
              <label className="field form-grid__full">
                Catégorie <span className="required">*</span>
                <select value={categorie} onChange={(e) => setCategorie(e.target.value as 'EXTERNALISATION' | 'DEBOURS')}>
                  <option value="EXTERNALISATION">Externalisation (service catalogue)</option>
                  <option value="DEBOURS">Débours (désignation + montant à valider)</option>
                </select>
              </label>
            )}
            {categorie === 'DEBOURS' ? (
              <>
                <label className="field form-grid__full">
                  Désignation du débours <span className="required">*</span>
                  <input value={designation} onChange={(e) => setDesignation(e.target.value)} maxLength={255} placeholder="Ex. Déplacement, timbres, divers…" />
                </label>
                {!ligne && (
                  <label className="field form-grid__full">
                    Montant du débours (MGA) — facultatif
                    <input
                      type="number"
                      min={0}
                      step="1"
                      value={montantDebours}
                      onChange={(e) => setMontantDebours(e.target.value)}
                      placeholder="Laisser vide pour valider ensuite"
                    />
                    <span className="form-help">
                      Facultatif — s&apos;il est renseigné, le débours est directement validé (aucun « Valider le montant » à faire ensuite).
                    </span>
                  </label>
                )}
              </>
            ) : (
            <>
            {!ligne && (
              <label className="field form-grid__full">
                Service <span className="required">*</span>
                <select
                  value={serviceId}
                  onChange={(e) => {
                    if (e.target.value === 'autre') {
                      setServiceModalOpen(true)
                      return
                    }
                    setServiceId(e.target.value ? Number(e.target.value) : '')
                  }}
                >
                  <option value="">— Choisir —</option>
                  {(pageServices?.content ?? [])
                    .filter((s: Service) => !servicesPris.includes(s.id))
                    .map((s: Service) => (
                      <option key={s.id} value={s.id}>
                        {s.nomService}
                      </option>
                    ))}
                  <option value="autre">Autre (créer un service)</option>
                </select>
              </label>
            )}
            {estServiceStandard && categorie === 'EXTERNALISATION' && (
              <label className="field form-grid__full">
                Type de prestation <span className="required">*</span>
                <select
                  value={typePrestation}
                  onChange={(e) =>
                    setTypePrestation(e.target.value as 'PRESTATION' | 'ABONNEMENT_MENSUEL')
                  }
                >
                  <option value="PRESTATION">Prestation</option>
                  <option value="ABONNEMENT_MENSUEL">Abonnement mensuel</option>
                </select>
              </label>
            )}
            <label className="field">
              {typePrestation === 'ABONNEMENT_MENSUEL' ? 'Tarif mensuel' : 'Tarif'} (MGA) <span className="required">*</span>
              <input
                type="number"
                min={0}
                value={tarif}
                onChange={(e) => setTarif(e.target.value)}
              />
            </label>
            <span className="field" />
            <label className="field">
              Début {typeCourant === 'ABONNEMENT_MENSUEL' && <span className="required">*</span>}
              <input type="date" value={debut} onChange={(e) => setDebut(e.target.value)} />
            </label>
            <label className="field">
              Fin {typeCourant !== 'PRESTATION' && typeCourant !== 'ABONNEMENT_MENSUEL' && <span className="required">*</span>}
              <input type="date" value={fin} min={debut || undefined} onChange={(e) => setFin(e.target.value)} />
            </label>
            </>
            )}
          </div>
          <footer className="modal-footer">
            <button type="button" className="btn-ghost" onClick={onClose} disabled={enregistrement}>
              Annuler
            </button>
            <button type="button" className="btn-primary" onClick={() => void handleSubmit()} disabled={enregistrement}>
              {enregistrement ? 'Enregistrement…' : 'Enregistrer'}
            </button>
          </footer>
        </div>
      </div>
      <ServiceFormModal
        open={serviceModalOpen}
        service={null}
        onClose={() => setServiceModalOpen(false)}
        onSave={(payload) => handleSaveService(payload as { nomService: string })}
      />
    </div>
  )
}

function ValiderMontantDeboursModal({
  ligne,
  onClose,
  onConfirm,
  onErreur,
}: {
  ligne: LigneService | null
  onClose: () => void
  onConfirm: (montant: number) => void
  onErreur: (message: string | null) => void
}) {
  const [montant, setMontant] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)

  const cle = ligne ? `valider-${ligne.id}` : 'ferme'
  const [cleVue, setCleVue] = useState<string | null>(null)
  if (cleVue !== cle) {
    setCleVue(cle)
    if (ligne) {
      setMontant('')
      setErreur(null)
    }
  }

  if (!ligne) return null

  const submit = () => {
    const valeur = Number(montant)
    if (!montant || !Number.isFinite(valeur) || valeur <= 0) {
      setErreur('Le montant doit être supérieur à 0')
      return
    }
    if (erreur) setErreur(null)
    onConfirm(valeur)
    // onErreur used by parent for API failures
    void onErreur
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: 440 }} onClick={(e) => e.stopPropagation()} role="dialog" aria-label="Valider le montant du débours">
        <header className="modal-header">
          <h3 className="modal-title">Valider le montant</h3>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">✕</button>
        </header>
        <div className="modal-body">
          {erreur && <div className="alert alert-danger" role="alert">{erreur}</div>}
          <p className="section-note">Débours : « {ligne.designationDebours ?? ''} ». Le montant validé fait passer la ligne à « Non payé » (paiements partiel / payé ensuite).</p>
          <label className="field">
            Montant du débours (MGA) <span className="required">*</span>
            <input type="number" min={0} value={montant} onChange={(e) => setMontant(e.target.value)} placeholder="Ex. 500000" />
          </label>
          <footer className="modal-footer">
            <button type="button" className="btn-ghost" onClick={onClose}>Annuler</button>
            <button type="button" className="btn-primary" onClick={submit}>Valider le montant</button>
          </footer>
        </div>
      </div>
    </div>
  )
}

function PlanModal({
  ligne,
  onClose,
  onSaved,
  onErreur,
}: {
  ligne: LigneService | null
  onClose: () => void
  onSaved: () => void
  onErreur: (message: string | null) => void
}) {
  const [nombre, setNombre] = useState('1')
  const [tranches, setTranches] = useState<{ date: string; montant: string }[]>([{ date: '', montant: '' }])
  const [erreur, setErreur] = useState<string | null>(null)
  const [enregistrement, setEnregistrement] = useState(false)

  // Période et montant repris du service : affichés verrouillés, non modifiables.
  const cle = ligne ? `plan-${ligne.id}` : 'ferme'
  const [cleVue, setCleVue] = useState<string | null>(null)
  if (cleVue !== cle) {
    setCleVue(cle)
    if (ligne) {
      setNombre('1')
      setTranches([{ date: '', montant: '' }])
      setErreur(null)
    }
  }

  if (!ligne) return null

  const montantTotal = ligne.tarif ?? 0
  const debutService = ligne.debutService ?? ''
  const finService = ligne.finService ?? ''
  const nomLigne = libelleLigne(ligne)
  const sommeSaisie = tranches.reduce((s, t) => s + (Number(t.montant) || 0), 0)
  const resteASaisir = Math.round((montantTotal - sommeSaisie) * 100) / 100

  const setNombreTranches = (valeur: string) => {
    setNombre(valeur)
    const n = Math.max(1, Math.min(24, Number(valeur) || 1))
    setTranches((anciennes) => {
      const suivantes = Array.from({ length: n }, (_, i) => anciennes[i] ?? { date: '', montant: '' })
      // Pré-remplit les montants à parts égales (modifiable ensuite).
      const part = Math.floor((montantTotal / n) * 100) / 100
      return suivantes.map((t, i) =>
        t.montant === '' && i < n
          ? { ...t, montant: String(i === n - 1 ? +(montantTotal - part * (n - 1)).toFixed(2) : part) }
          : t,
      )
    })
  }

  const setTranche = (index: number, patch: Partial<{ date: string; montant: string }>) => {
    setTranches((anciennes) => anciennes.map((t, i) => (i === index ? { ...t, ...patch } : t)))
  }

  const validerEtape = (index: number): string | null => {
    if (index === 0) {
      if (!nombre || Number(nombre) < 1 || !Number.isInteger(Number(nombre))) {
        return "Le nombre d'échéances doit être un entier ≥ 1"
      }
      if (Number(nombre) > 24) return "Le nombre d'échéances est limité à 24"
      return null
    }
    for (const [i, t] of tranches.entries()) {
      if (!t.date) return `Échéance ${i + 1} : date obligatoire`
      if (t.date < debutService || t.date > finService) {
        return `Échéance ${i + 1} : doit rester dans la période du service (${debutService} → ${finService})`
      }
      if (!t.montant || Number(t.montant) <= 0 || Number.isNaN(Number(t.montant))) {
        return `Échéance ${i + 1} : montant strictement positif obligatoire`
      }
    }
    const somme = tranches.reduce((s, t) => s + Number(t.montant), 0)
    if (Math.round(somme * 100) !== Math.round(montantTotal * 100)) {
      return `La somme des échéances (${somme.toLocaleString('fr-FR')} MGA) doit égaler le montant total (${montantTotal.toLocaleString('fr-FR')} MGA)`
    }
    return null
  }

  const handleTerminer = async () => {
    setEnregistrement(true)
    setErreur(null)
    try {
      await planPaiementApi.createForLigne(ligne.id, {
        dateDebut: debutService,
        dateFin: finService,
        montantTotal,
        nombreEcheances: tranches.length,
        commentaire: null,
        echeances: tranches.map((t) => ({ dateEcheance: t.date, montantAttendu: Number(t.montant) })),
      })
      onSaved()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Échec de la création du plan'
      setErreur(message)
      onErreur(message)
    } finally {
      setEnregistrement(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 560 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Établir le plan de paiement"
      >
        <header className="modal-header">
          <h3 className="modal-title">Plan de paiement — {nomLigne}</h3>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>
        <div className="modal-body">
          <FormEtapes
            key={cle}
            etapes={[
              { titre: 'Nombre', description: 'Période et montant repris du service (non modifiables).' },
              { titre: 'Échéances', description: 'Date et montant de chaque échéance.' },
            ]}
            validerEtape={validerEtape}
            onTerminer={() => void handleTerminer()}
            onAnnuler={onClose}
            enregistrement={enregistrement}
            erreur={erreur}
            libelleTerminer="Créer le plan"
          >
            {(etape) =>
              etape === 0 ? (
                <div className="form-grid form-grid--2">
                  <label className="field">
                    Début du service
                    <input type="date" value={debutService} disabled />
                  </label>
                  <label className="field">
                    Fin du service
                    <input type="date" value={finService} disabled />
                  </label>
                  <label className="field">
                    Montant total (MGA)
                    <input type="number" value={montantTotal} disabled />
                  </label>
                  <label className="field">
                    Nombre d&apos;échéances 
                    {/* <span className="required">*</span> */}
                    <input
                      type="number"
                      min={1}
                      max={24}
                      step={1}
                      value={nombre}
                      onChange={(e) => setNombreTranches(e.target.value)}
                    />
                  </label>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div className="card card-pad" style={{ background: 'var(--c-bg)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                      <strong>
                        Total saisi : {sommeSaisie.toLocaleString('fr-FR')} /{' '}
                        {montantTotal.toLocaleString('fr-FR')} MGA
                      </strong>
                      <span className={resteASaisir === 0 ? 'pill ok' : 'pill warning'}>
                        ● {resteASaisir === 0
                          ? 'Équilibré'
                          : `Reste ${resteASaisir.toLocaleString('fr-FR')} MGA`}
                      </span>
                    </div>
                    <p className="section-note" style={{ margin: '4px 0 0' }}>
                      Les dates doivent rester dans la période du service (
                      {formatDate(debutService)} → {formatDate(finService)}).
                    </p>
                  </div>
                  {tranches.map((t, i) => (
                    <div key={i} className="form-grid form-grid--2">
                      <label className="field">
                        Échéance {i + 1} — date <span className="required">*</span>
                        <input
                          type="date"
                          min={debutService}
                          max={finService}
                          value={t.date}
                          onChange={(e) => setTranche(i, { date: e.target.value })}
                        />
                      </label>
                      <label className="field">
                        Échéance {i + 1} — montant (MGA) <span className="required">*</span>
                        <input
                          type="number"
                          min={0}
                          value={t.montant}
                          onChange={(e) => setTranche(i, { montant: e.target.value })}
                        />
                      </label>
                    </div>
                  ))}
                </div>
              )
            }
          </FormEtapes>
        </div>
      </div>
    </div>
  )
}

/** Onglet Dossier : consultation du dossier de création (demandes CREATION). */
function DossierTab({ demande }: { demande: Demande }) {
  const { data: dossier, loading } = useApi<Dossier | null>(
    () => dossierApi.get(demande.id).catch(() => null),
    [demande.id],
  )

  if (loading) return <Spinner label="Chargement du dossier…" />

  const route = `/demandes/${demande.referenceDemande}/dossier-entreprise`

  if (!dossier) {
    return (
      <section className="card card-pad">
        <h3 style={{ margin: '0 0 8px', fontSize: 17, fontWeight: 700 }}>Dossier de création</h3>
        <p className="section-note">
          Aucun dossier saisi pour cette demande de création d&apos;entreprise.
        </p>
        <Can permission="DOSSIER_CREATE">
          <Link to={route} className="btn-primary">
            Saisir le dossier
          </Link>
        </Can>
      </section>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div>
        <Can permission="DOSSIER_UPDATE">
          <Link to={route} className="btn-ghost">
            Modifier le dossier
          </Link>
        </Can>
      </div>
      <DossierEntrepriseDetail
        dossier={dossierVersFormulaire(dossier, demande.referenceDemande)}
        dateDemande={dossier.dateDemande ?? demande.dateDemande}
        numeroIM={dossier.numeroIm ?? '—'}
      />
    </div>
  )
}

/** Onglet Dossier : consultation de la fiche de transaction immobilière. */
function DossierTransactionTab({ demande }: { demande: Demande }) {
  const { data: dossier, loading } = useApi<DossierTransactionImmobiliere | null>(
    () => dossierTransactionApi.get(demande.id).catch(() => null),
    [demande.id],
  )

  if (loading) return <Spinner label="Chargement du dossier de transaction immobilière…" />

  const route = `/demandes/${demande.referenceDemande}/dossier-transaction-immobiliere`

  if (!dossier) {
    return (
      <section className="card card-pad">
        <h3 style={{ margin: '0 0 8px', fontSize: 17, fontWeight: 700 }}>
          Dossier de transaction immobilière
        </h3>
        <p className="section-note">
          Aucune fiche renseignée pour cette demande de transaction immobilière.
        </p>
        <Can permission="DOSSIER_CREATE">
          <Link to={route} className="btn-primary">
            Saisir le dossier
          </Link>
        </Can>
      </section>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div>
        <Can permission="DOSSIER_UPDATE">
          <Link to={route} className="btn-ghost">
            Modifier le dossier
          </Link>
        </Can>
      </div>
      <DossierTransactionImmobiliereDetail dossier={dossier} />
    </div>
  )
}

/** Vue globale des paiements de la demande (§7.2) : totaux + regroupement par service. */
function PaiementsTab({ demande }: { demande: Demande }) {
  const lignes = demande.lignes ?? []
  const totalPaye = lignes.reduce((s, l) => s + l.montantPaye, 0)
  const solde = lignes.every((l) => l.resteAPayer <= 0) && lignes.length > 0
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <section className="card card-pad">
        <h3 style={{ margin: '0 0 14px', fontSize: 17, fontWeight: 700 }}>
          Situation globale du règlement
        </h3>
        <div className="info-grid">
          <div className="info-item">
            <span>Montant total</span>
            <b>{formatMontantMGA(demande.montantTotal)}</b>
          </div>
          <div className="info-item">
            <span>Total payé</span>
            <b>{formatMontantMGA(totalPaye)}</b>
          </div>
          <div className="info-item">
            <span>Reste à payer</span>
            <b>{formatMontantMGA(demande.resteAPayer)}</b>
          </div>
          <div className="info-item">
            <span>Services concernés</span>
            <b>{lignes.length}</b>
          </div>
          <div className="info-item">
            <span>Statut financier</span>
            <b>
              <span className={solde ? 'pill ok' : totalPaye > 0 ? 'pill warning' : 'pill wt'}>
                ● {lignes.length === 0 ? '—' : solde ? 'Soldé' : totalPaye > 0 ? 'Partiel' : 'En attente'}
              </span>
            </b>
          </div>
        </div>
      </section>

      {lignes.map((l) => (
        <section key={l.id} className="card card-pad">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <strong>{libelleLigne(l)}</strong>
            <LignePill statut={l.statut} />
          </div>
          <div className="info-grid" style={{ marginTop: 8 }}>
            <div className="info-item">
              <span>{l.categorie === 'DEBOURS' ? 'Montant validé' : 'Tarif'}</span>
              <b>{formatMontantMGA(l.tarif ?? l.montantDebours ?? 0)}</b>
            </div>
            <div className="info-item">
              <span>Payé</span>
              <b>{formatMontantMGA(l.montantPaye)}</b>
            </div>
            <div className="info-item">
              <span>Reste</span>
              <b>{formatMontantMGA(l.resteAPayer)}</b>
            </div>
          </div>
        </section>
      ))}
    </div>
  )
}

function ModifierDemandeModal({
  open,
  demande,
  onClose,
  onSaved,
  onErreur,
}: {
  open: boolean
  demande: Demande
  onClose: () => void
  onSaved: () => void
  onErreur: (message: string | null) => void
}) {
  const [commercialId, setCommercialId] = useState<number | ''>('')
  const [commentaire, setCommentaire] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const [enregistrement, setEnregistrement] = useState(false)
  const { data: commerciaux } = useApi(
    () => commerciauxApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [open],
  )

  const cle = open ? `demande-${demande.id}` : 'fermee'
  const [cleVue, setCleVue] = useState<string | null>(null)
  if (cleVue !== cle) {
    setCleVue(cle)
    if (open) {
      setCommercialId(demande.commercialId ?? '')
      setCommentaire(demande.commentaire ?? '')
      setErreur(null)
    }
  }

  if (!open) return null

  const handleSubmit = async () => {
    setEnregistrement(true)
    setErreur(null)
    try {
      await demandesApi.update(demande.id, {
        commercialId: commercialId === '' ? null : commercialId,
        commentaire: commentaire.trim() || null,
      })
      onSaved()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Échec de la modification'
      setErreur(message)
      onErreur(message)
    } finally {
      setEnregistrement(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 520 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Modifier la demande"
      >
        <header className="modal-header">
          <h3 className="modal-title">Modifier la demande</h3>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>
        <div className="modal-body">
          {erreur && (
            <div className="alert alert-danger" role="alert">
              {erreur}
            </div>
          )}
          <label className="field">
            Commercial
            <select
              value={commercialId}
              onChange={(e) => setCommercialId(e.target.value ? Number(e.target.value) : '')}
            >
              <option value="">— Aucun —</option>
              {(commerciaux?.content ?? []).map((c: Commercial) => (
                <option key={c.id} value={c.id}>
                  {c.nomCommercial}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Commentaire
            <input value={commentaire} onChange={(e) => setCommentaire(e.target.value)} />
          </label>
          <p className="section-note">
            Les services (tarifs, périodes) se modifient dans l&apos;onglet Services.
          </p>
          <footer className="modal-footer">
            <button type="button" className="btn-ghost" onClick={onClose} disabled={enregistrement}>
              Annuler
            </button>
            <button type="button" className="btn-primary" onClick={() => void handleSubmit()} disabled={enregistrement}>
              {enregistrement ? 'Enregistrement…' : 'Enregistrer'}
            </button>
          </footer>
        </div>
      </div>
    </div>
  )
}

/**
 * Section « Factures mensuelles — Abonnement » de l'onglet Factures :
 * pour un abonnement mensuel, on crée la facture du mois avant chaque paiement.
 * La facture reprend la référence de la facture générale validée, la référence
 * de la facture proforma et la référence de la demande.
 */
/**
 * Lignes d'abonnement dont on peut encore consulter les factures :
 * EN_COURS ou PARTIEL (actif), TERMINE (terminé manuellement — historique consultable),
 * en dehors des débours.
 */
function abonnementMessageLigne(ligne: LigneService): { classe: string; libelle: string } {
  if (ligne.statut === 'TERMINE') {
    return { classe: 'pill ok', libelle: 'Abonnement terminé' }
  }
  if (ligne.statut === 'PARTIEL') {
    return { classe: 'pill warning', libelle: 'Abonnement partiel' }
  }
  return { classe: 'pill info', libelle: 'Abonnement en cours' }
}

function FacturesAbonnementSection({
  demande,
  onErreur,
  onMessage,
}: {
  demande: Demande
  onErreur: (message: string | null) => void
  onMessage: (message: string | null) => void
}) {
  /** Toutes les lignes d'abonnement, même terminées : leurs factures restent consultables. */
  const abonnementLignes = (demande.lignes ?? []).filter(
    (l) => l.categorie === 'EXTERNALISATION'
      && l.typePrestation === 'ABONNEMENT_MENSUEL'
      && ['EN_COURS', 'PARTIEL', 'TERMINE'].includes(l.statut ?? ''),
  )
  const { data: facturesValidees, reload } = useApi<FactureValidee[]>(
    () => (demande?.id ? facturesValideesApi.list(demande.id).catch(() => []) : Promise.resolve([])),
    [demande?.id],
  )
  const { data: inclusionsDisponibles, reload: reloadInclusions } = useApi<InclusionDisponible[]>(
    () => (demande?.id ? facturesValideesApi.inclusionsDisponibles(demande.id).catch(() => []) : Promise.resolve([])),
    [demande?.id],
  )
  const [creationEnCours, setCreationEnCours] = useState<number | null>(null)
  const [telechargement, setTelechargement] = useState<number | null>(null)
  const [apercu, setApercu] = useState<FactureApercu | null>(null)
  const [lignesOuvertes, setLignesOuvertes] = useState<Set<number>>(new Set())

  function toggleLignesOuvertes(id: number): void {
    setLignesOuvertes((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }
  const [cible, setCible] = useState<LigneService | null>(null)
  const [dateEmission, setDateEmission] = useState(() => new Date().toISOString().slice(0, 10))
  const [moisActuel, setMoisActuel] = useState(() => new Date().toISOString().slice(0, 7))
  const [erreurDialog, setErreurDialog] = useState<string | null>(null)
  const [selectionInclusions, setSelectionInclusions] = useState<Set<number>>(new Set())

  const ligneLabel = (l: LigneService) =>
    `${l.nomService ?? 'Abonnement'} · tarif mensuel ${formatMontantMGA(l.tarif ?? 0)}`

  // Montant appliqué dans la facture du mois : le montant validé de la ligne
  // d'abonnement (le tarif passe à cette valeur à la validation de la proforma).
  const montantFactureMensuelle = cible ? (cible.tarif ?? 0) : 0

  // Toutes les factures du mois de la demande (tous les abonnements confondus).
  const enfantsTous = (facturesValidees ?? []).filter((f) => f.type === 'ABONNEMENT_MENSUEL')
  const totalFacture = enfantsTous.reduce((s, f) => s + f.montant, 0)
  const totalPaye = enfantsTous.reduce((s, f) => s + f.montantPaye, 0)
  const totalReste = enfantsTous.reduce((s, f) => s + f.resteAPayer, 0)
  const nbRestant = enfantsTous.filter((f) => f.resteAPayer > 0).length

  const navigate = useNavigate()

  const creer = async () => {
    if (!cible) {
      return
    }
    if (!dateEmission) {
      setErreurDialog('La date d’émission est obligatoire.')
      return
    }
    if (!moisActuel) {
      setErreurDialog('Le mois facturé est obligatoire.')
      return
    }
    setCreationEnCours(cible.id)
    onErreur(null)
    onMessage(null)
    setErreurDialog(null)
    const inclusionIds = Array.from(selectionInclusions)
    try {
      const fv = await facturesValideesApi.creerFactureAbonnement(
        demande.id,
        cible.id,
        dateEmission,
        moisActuel,
        null,
        inclusionIds,
      )
      onMessage(
        `Facture du mois de ${moisLabel(moisActuel)} créée (${dateEmission}) : ${fv.referenceFactureValidee}`
        + ` (facture générale ${fv.referenceFactureGenerale ?? '—'}`
        + `${fv.referenceFactureProforma ? ` · proforma ${fv.referenceFactureProforma}` : ''})`,
      )
      setCible(null)
      setSelectionInclusions(new Set())
      reload()
      reloadInclusions()
    } catch (err) {
      setErreurDialog(messageErreur(err))
    } finally {
      setCreationEnCours(null)
    }
  }

  const telecharger = async (fv: FactureValidee) => {
    setTelechargement(fv.id)
    onErreur(null)
    try {
      const { data } = await http.get<Blob>(facturesValideesApi.abonnementUrl(fv.id), {
        responseType: 'blob',
      })
      const url = URL.createObjectURL(data)
      const a = document.createElement('a')
      a.href = url
      a.download = `${fv.referenceFactureValidee}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      onErreur('Téléchargement du PDF impossible')
    } finally {
      setTelechargement(null)
    }
  }

  if (abonnementLignes.length === 0) {
    return (
      <section className="card card-pad">
        <h3 style={{ margin: '0 0 8px', fontSize: 17, fontWeight: 700 }}>Factures mensuelles</h3>
        <p className="section-note" style={{ margin: 0 }}>
          Aucun abonnement mensuel dans cette demande. Les lignes d&apos;abonnement sont
          créées depuis l&apos;onglet Services (type de prestation « Abonnement mensuel »).
        </p>
      </section>
    )
  }

  return (
    <section className="card card-pad">
      <h3 style={{ margin: '0 0 8px', fontSize: 17, fontWeight: 700 }}>
        Factures mensuelles — Abonnement
      </h3>
      <p className="section-note" style={{ marginBottom: 12 }}>
        Créez la facture du mois avant chaque paiement : elle reprend le mois facturé, la date
        d&apos;émission, la référence de la facture générale validée et la référence de la facture
        proforma. Vous pouvez y reprendre des prestations / débours impayés du même client.
        Un abonnement terminé n&apos;accepte plus de nouvelle facture du mois ; ses factures
        restent consultables ici.
      </p>

      {/* KPI — suivi des encaissements */}
      <div className="info-grid" style={{ marginBottom: 12 }}>
        <div className="info-item">
          <span>Factures du mois</span>
          <b>{enfantsTous.length}</b>
        </div>
        <div className="info-item">
          <span>Total facturé</span>
          <b>{formatMontantMGA(totalFacture)}</b>
        </div>
        <div className="info-item">
          <span>Déjà payé</span>
          <b style={{ color: 'var(--success)' }}>{formatMontantMGA(totalPaye)}</b>
        </div>
        <div className="info-item">
          <span>Reste à encaisser</span>
          <b style={{ color: nbRestant > 0 ? 'var(--danger)' : 'var(--success)' }}>
            {formatMontantMGA(totalReste)}
          </b>
        </div>
      </div>

      {/* Création — des abonnements actifs ; la terminée reste consultable */}
      {abonnementLignes.map((ligne) => {
        const enfants = (facturesValidees ?? []).filter(
          (f) => f.ligneServiceId === ligne.id && f.type === 'ABONNEMENT_MENSUEL',
        )
        const restant = enfants.filter((f) => f.resteAPayer > 0)
        const actif = ligne.statut === 'EN_COURS' || ligne.statut === 'PARTIEL'
        const etat = abonnementMessageLigne(ligne)
        return (
          <div key={ligne.id}
            style={{ borderTop: '1px solid var(--border, #e5e7eb)', padding: '12px 0' }}>
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 12,
              flexWrap: 'wrap',
            }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <b>{ligneLabel(ligne)}</b>
                  <span className={etat.classe}>● {etat.libelle}</span>
                </div>
                <div className="text-secondary" style={{ fontSize: 13 }}>
                  {enfants.length === 0
                    ? ligne.statut === 'TERMINE'
                      ? 'Aucune facture du mois créée.'
                      : 'Aucune facture du mois créée pour cet abonnement.'
                    : `${enfants.length} facture(s) du mois · ${restant.length} à régler`}
                </div>
              </div>
              {actif && (
                <Can permission="FACTURE_ABONNEMENT_CREATE">
                  <button
                    type="button"
                    className="btn-primary"
                    disabled={creationEnCours === ligne.id}
                    onClick={() => {
                      setErreurDialog(null)
                      setDateEmission(new Date().toISOString().slice(0, 10))
                      setMoisActuel(new Date().toISOString().slice(0, 7))
                      setSelectionInclusions(new Set())
                      reloadInclusions()
                      setCible(ligne)
                    }}
                  >
                    Créer la facture du mois
                  </button>
                </Can>
              )}
            </div>
            {enfants.length > 0 && (
              <div className="table-scroll-container no-global-scroll" style={{ marginTop: 8 }}>
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Facture</th>
                        <th>Mois facturé</th>
                        <th>Émise le</th>
                        <th>Générale</th>
                        <th>Proforma</th>
                        <th>Montant</th>
                        <th>Payé</th>
                        <th>Reste</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {enfants.map((f) => {
                        const detailOuvert = lignesOuvertes.has(f.id)
                        return (
                        <Fragment key={f.id}>
                          <tr>
                            <td className="fw-600">
                              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                {(f.lignes ?? []).length > 0 && (
                                  <button
                                    type="button"
                                    className="btn-ghost"
                                    style={{
                                      minWidth: 30,
                                      width: 30,
                                      padding: 0,
                                      lineHeight: 1.4,
                                    }}
                                    onClick={() => toggleLignesOuvertes(f.id)}
                                    aria-expanded={detailOuvert}
                                    aria-label={detailOuvert ? 'Cacher les lignes de la facture' : 'Afficher les lignes de la facture'}
                                    title={detailOuvert ? 'Cacher les lignes' : 'Afficher les lignes'}
                                  >
                                    {detailOuvert ? '▾' : '▸'}
                                  </button>
                                )}
                                <button
                                  type="button"
                                  className="btn-link"
                                  onClick={() => setApercu({ kind: 'validee', facture: f })}
                                >
                                  {f.referenceFactureValidee}
                                </button>
                              </div>
                            </td>
                            <td>{moisLabel(f.mois)}</td>
                            <td className="text-secondary">
                              {f.valideLe ? formatDate(f.valideLe.slice(0, 10)) : '—'}
                            </td>
                            <td className="text-secondary">{f.referenceFactureGenerale ?? '—'}</td>
                            <td className="text-secondary">{f.referenceFactureProforma ?? '—'}</td>
                            <td>{formatMontantMGA(f.montant)}</td>
                            <td>{formatMontantMGA(f.montantPaye)}</td>
                            <td>{formatMontantMGA(f.resteAPayer)}</td>
                            <td>
                              <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                                {f.resteAPayer > 0 && demande.statutActuel !== 'TERMINEE' && demande.statutActuel !== 'ANNULEE' && (
                                  <button
                                    type="button"
                                    className="btn-primary"
                                    onClick={() => navigate(
                                      `/paiements/nouveau?demande=${encodeURIComponent(ligne.referenceDemande)}&ligne=${ligne.id}`,
                                    )}
                                  >
                                    Encaisser
                                  </button>
                                )}
                                <button
                                  type="button"
                                  className="btn-ghost"
                                  disabled={telechargement === f.id}
                                  onClick={() => void telecharger(f)}
                                >
                                  {telechargement === f.id ? '…' : 'PDF'}
                                </button>
                              </div>
                            </td>
                          </tr>
                          {detailOuvert && (f.lignes ?? []).length > 0 && (
                            <tr>
                              <td colSpan={9}>
                                <div style={{
                                  display: 'flex',
                                  flexDirection: 'column',
                                  gap: 8,
                                  fontSize: 13,
                                  padding: '8px 0 8px 40px',
                                  borderLeft: '3px solid var(--vert, #10b981)',
                                }}>
                                  {(() => {
                                    const principales = (f.lignes ?? []).filter((l) => l.type === 'PRINCIPALE')
                                    const rappels = (f.lignes ?? []).filter((l) => l.type === 'RAPPEL')
                                    return (
                                      <>
                                        {principales.length > 0 && (
                                          <div>
                                            <b style={{ color: 'var(--vert, #10b981)' }}>→ Cette facture</b>
                                            {principales.map((l) => (
                                              <div
                                                key={l.id}
                                                style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}
                                              >
                                                <span>
                                                  <b className="text-secondary" style={{ fontSize: 12 }}>{l.referenceDemande ?? '—'}</b>
                                                  {' · '}<b>{l.nomService ?? l.designation}</b>
                                                  {l.nomService && l.nomService !== l.designation && (
                                                    <span className="text-secondary"> · {l.designation}</span>
                                                  )}
                                                </span>
                                                <span className="fw-600">{formatMontantMGA(l.montant)}</span>
                                              </div>
                                            ))}
                                          </div>
                                        )}
                                        {rappels.length > 0 && (
                                          <div>
                                            <b style={{ color: 'var(--attention, #b45309)' }}>À titre de rappel (non inclus)</b>
                                            {rappels.map((l) => (
                                              <div
                                                key={l.id}
                                                style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}
                                              >
                                                <span>
                                                  <b className="text-secondary" style={{ fontSize: 12 }}>{l.referenceDemande ?? '—'}</b>
                                                  {' · '}<b>{l.nomService ?? l.designation}</b>
                                                  {l.nomService && l.nomService !== l.designation && (
                                                    <span className="text-secondary"> · {l.designation}</span>
                                                  )}
                                                </span>
                                                <span className="fw-600">{formatMontantMGA(l.montant)}</span>
                                              </div>
                                            ))}
                                            <p className="section-note" style={{ fontSize: 11, margin: '4px 0 0' }}>
                                              Ces factures ne sont pas comptabilisées dans le montant de la facture ci-dessus ;
                                              elles doivent être réglées chacune sur sa propre facture validée.
                                            </p>
                                          </div>
                                        )}
                                      </>
                                    )
                                  })()}
                                </div>
                              </td>
                            </tr>
                          )}
                        </Fragment>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )
      })}
      <ConfirmDialog
        open={cible !== null}
        titre="Créer la facture du mois"
        message={cible
          ? `Facture du mois pour « ${cible.nomService ?? cible.libelleLigne ?? 'Abonnement mensuel'} ». Vous pouvez reprendre des prestations / débours impayés du même client dans la même facture.`
          : ''}
        confirmer="Créer la facture"
        ton="primary"
        enCours={creationEnCours !== null}
        erreur={erreurDialog}
        details={(
          <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 12 }}>
            <label className="field">
              Date d&apos;émission <span className="required">*</span>
              <input
                type="date"
                value={dateEmission}
                onChange={(e) => setDateEmission(e.target.value)}
                required
                autoFocus
              />
            </label>
            <label className="field">
              Mois facturé <span className="required">*</span>
              <input
                type="month"
                lang="fr-FR"
                value={moisActuel}
                onChange={(e) => setMoisActuel(e.target.value)}
                required
              />
            </label>
            {(inclusionsDisponibles ?? []).length > 0 && (
              <fieldset
                style={{
                  border: '1px solid var(--border, #e5e7eb)',
                  borderRadius: 8,
                  padding: 12,
                }}
              >
                <legend style={{ padding: '0 6px', fontSize: 13, color: 'var(--texte-secondaire, #6b7280)' }}>
                  Reprendre des impayés dans cette facture (même société)
                </legend>
                {(inclusionsDisponibles ?? []).map((inc) => (
                  <label
                    key={inc.ligneId}
                    style={{
                      display: 'flex',
                      gap: 8,
                      alignItems: 'baseline',
                      padding: '4px 0',
                      cursor: 'pointer',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectionInclusions.has(inc.ligneId)}
                      onChange={() => {
                        setSelectionInclusions((prev) => {
                          const next = new Set(prev)
                          if (next.has(inc.ligneId)) {
                            next.delete(inc.ligneId)
                          } else {
                            next.add(inc.ligneId)
                          }
                          return next
                        })
                      }}
                    />
                    <span>
                      <b>{inc.designation}</b>
                      <span className="text-secondary"> · {inc.referenceDemande}</span>
                      <br />
                      <span className="text-secondary">
                        reste dû : {formatMontantMGA(inc.reste)}
                      </span>
                    </span>
                  </label>
                ))}
                <div style={{ marginTop: 10, fontWeight: 600 }}>
                  Total de la facture :{' '}
                  <b>
                    {formatMontantMGA(
                      montantFactureMensuelle +
                        (inclusionsDisponibles ?? [])
                          .filter((inc) => selectionInclusions.has(inc.ligneId))
                          .reduce((somme, inc) => somme + inc.reste, 0),
                    )}
                  </b>
                </div>
              </fieldset>
            )}
            {(inclusionsDisponibles ?? []).length === 0 && (
              <p className="section-note" style={{ margin: 0 }}>
                Aucun impayé prestation / débours pour les demandes en cours de ce client.
              </p>
            )}
          </div>
        )}
        onAnnuler={() => {
          setCible(null)
          setErreurDialog(null)
          setSelectionInclusions(new Set())
        }}
        onConfirmer={() => void creer()}
      />

      <div style={{ marginTop: 14, display: 'flex', justifyContent: 'flex-end' }}>
        <Link to={`/paiements/historique`} className="btn-link">
          Voir l&apos;historique des paiements ›
        </Link>
      </div>

      <FactureApercuModal
        apercu={apercu}
        onClose={() => setApercu(null)}
      />
    </section>
  )
}
