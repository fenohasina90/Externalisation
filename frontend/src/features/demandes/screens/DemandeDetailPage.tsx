import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Can } from '@/components/shared/Can'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { FormEtapes } from '@/components/shared/FormEtapes'
import { Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { http } from '@/api/http'
import { formatDate, formatMontantMGA } from '@/utils/format'
import type { Commercial } from '@/features/commerciaux/types'
import type { Service } from '@/features/services/types'
import { commerciauxApi } from '@/features/commerciaux/services/commerciaux.service'
import { servicesApi } from '@/features/services/services/services.service'
import type { PlanPaiement } from '@/features/plan-paiement/types'
import { planPaiementApi } from '@/features/plan-paiement/services/plan-paiement.service'
import type { Echeance } from '@/features/echeances/types'
import { echeancesApi } from '@/features/echeances/services/echeances.service'
import { ChangerStatutModal, DemandeStepper, DossierEntrepriseDetail,
  DossierEntrepriseFormModal, StatutDemandePill } from '../components'
import { DOSSIER_VIDE, type DossierEntreprise } from '../data/mockDossierEntreprise'
import type { Demande, Facture, HistoriqueDemande, HistoriqueLigne, LigneService,
  StatutDemande } from '../types'
import { dossierVersFormulaire, dossierVersPayload, type Dossier } from '../types/dossier'
import { demandesApi, dossierApi, facturesApi, lignesApi } from '../services/demandes.service'

type Onglet = 'informations' | 'services' | 'factures' | 'paiements' | 'historique' | 'dossier'

const ONGLETS_VALIDES: Onglet[] = ['informations', 'services', 'factures', 'paiements', 'historique', 'dossier']

const LIBELLES_LIGNE: Record<string, string> = {
  VALIDE: 'Validé',
  EN_ATTENTE: 'En attente',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
}

function LignePill({ statut }: { statut: string | null }) {
  if (!statut) return <span className="pill wt">● En préparation</span>
  const classe =
    statut === 'EN_COURS' ? 'pill info'
    : statut === 'TERMINE' ? 'pill ok'
    : statut === 'VALIDE' ? 'pill ok'
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
    ['paiements', 'Paiements'],
    ['historique', 'Historique'],
    ...((demande.lignes ?? []).some((l) => l.typeService === 'CREATION')
      ? [['dossier', 'Dossier entreprise'] as [Onglet, string]]
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
          traitement={traitement}
          onTelecharger={(f) => void telechargerProforma(f)}
          onErreur={setErreur}
          onMessage={setMessage}
          onChange={toutRecharger}
        />
      )}

      {onglet === 'paiements' && <PaiementsTab demande={demande} />}

      {onglet === 'dossier' && (
        <DossierTab
          demande={demande}
          onErreur={setErreur}
          onMessage={setMessage}
        />
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
function FacturesTab({
  factures,
  traitement,
  onTelecharger,
  onErreur,
  onMessage,
  onChange,
}: {
  factures: Facture[]
  traitement: boolean
  onTelecharger: (facture: Facture) => void
  onErreur: (message: string | null) => void
  onMessage: (message: string | null) => void
  onChange: () => void
}) {
  const [confirmValidation, setConfirmValidation] = useState(false)
  const [validationEnCours, setValidationEnCours] = useState(false)
  const proformas = factures.filter((f) => f.statut === 'PROFORMA')
  const toutesLignes = factures.flatMap((f) =>
    f.lignes.map((l) => ({ ...l, referenceFacture: f.referenceFacture })),
  )
  const totalApercu = toutesLignes.reduce((s, l) => s + l.tarif, 0)

  const handleValiderTout = async () => {
    setConfirmValidation(false)
    setValidationEnCours(true)
    try {
      for (const f of proformas) {
        await facturesApi.valider(f.id, null)
      }
      onMessage(
        proformas.length === 1
          ? `Facture ${proformas[0].referenceFacture} validée.`
          : `${proformas.length} factures validées.`,
      )
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
                      <td className="fw-600">{l.nomService ?? '—'}</td>
                      <td className="text-secondary">
                        {formatDate(l.debutService)} → {formatDate(l.finService)}
                      </td>
                      <td>{formatMontantMGA(l.tarif)}</td>
                      <td className="text-secondary">{l.referenceFacture}</td>
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

      {factures.map((f) => (
        <section key={f.id} className="card card-pad">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <strong>
              {f.referenceFacture} — {formatMontantMGA(f.montant)}
            </strong>
            <span className={f.statut === 'VALIDEE' ? 'pill ok' : 'pill wt'}>
              ● {f.statut === 'VALIDEE' ? 'Validée' : 'Proforma'}
            </span>
          </div>
          <p className="section-note">
            Mode {f.modeFacturation === 'UNIQUE' ? 'unique' : 'par service'}
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
        confirmer="Valider"
        ton="success"
        onAnnuler={() => setConfirmValidation(false)}
        onConfirmer={() => void handleValiderTout()}
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
  const [edition, setEdition] = useState<LigneService | null>(null)
  const [retrait, setRetrait] = useState<LigneService | null>(null)
  const [planPour, setPlanPour] = useState<LigneService | null>(null)
  const [traitement, setTraitement] = useState(false)

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
          modifiable={modifiable && !fige(ligne)}
          planEtablissable={statutFacture.get(ligne.factureId ?? -1) === 'VALIDEE'}
          onEdit={() => setEdition(ligne)}
          onRemove={() => setRetrait(ligne)}
          onPlan={() => setPlanPour(ligne)}
        />
      ))}
      {modifiable && !toutFige && (
        <Can permission="DEMANDE_UPDATE">
          <div>
            <button type="button" className="btn-ghost" onClick={() => setAjoutOuvert(true)}>
              + Ajouter un service
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
        open={retrait !== null}
        titre="Retirer ce service ?"
        message={`« ${retrait?.nomService ?? ''} » sera retiré de la demande et sa contribution aux factures sera actualisée.`}
        confirmer="Retirer"
        ton="danger"
        onAnnuler={() => setRetrait(null)}
        onConfirmer={() => void handleRetrait()}
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
}: {
  demandeId: number
  ligne: LigneService
  modifiable: boolean
  /** Bouton « Établir le plan » visible uniquement si la facture est validée. */
  planEtablissable: boolean
  onEdit: () => void
  onRemove: () => void
  onPlan: () => void
}) {
  const { data: plan } = useApi<PlanPaiement | null>(
    () => planPaiementApi.getByLigne(ligne.id).catch(() => null),
    [ligne.id],
  )
  const { data: echeances } = useApi<Echeance[]>(
    () => (plan ? echeancesApi.listByPlan(plan.id) : Promise.resolve([])),
    [plan?.id],
  )
  const [voirEcheances, setVoirEcheances] = useState(true)
  const [voirHistorique, setVoirHistorique] = useState(false)

  return (
    <section className="card card-pad">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <strong>
          {ligne.nomService ?? '—'} — {formatMontantMGA(ligne.tarif)}
        </strong>
        <LignePill statut={ligne.statut} />
      </div>
      <div className="info-grid" style={{ marginTop: 8 }}>
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
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap' }}>
        <button type="button" className="btn-ghost" onClick={() => setVoirEcheances((v) => !v)}>
          Échéances {voirEcheances ? '▴' : '▾'}
        </button>
        <button type="button" className="btn-ghost" onClick={() => setVoirHistorique((v) => !v)}>
          Historique {voirHistorique ? '▴' : '▾'}
        </button>
      </div>

      {plan && voirEcheances && (
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
                {(echeances ?? []).map((e) => (
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
      {!plan && voirEcheances && (
        <p className="section-note">Aucun plan de paiement pour ce service.</p>
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
      {!plan && planEtablissable && (
        <Can permission="DEMANDE_UPDATE">
          <div style={{ marginTop: 8 }}>
            <button type="button" className="btn-primary" onClick={onPlan}>
              Établir le plan
            </button>
          </div>
        </Can>
      )}
      {!plan && !planEtablissable && (
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
  onClose,
  onSaved,
  onErreur,
}: {
  open: boolean
  ligne: LigneService | null
  demandeId: number
  /** Identifiants déjà utilisés : masqués du select (pas de doublon). */
  servicesPris: number[]
  onClose: () => void
  onSaved: () => void
  onErreur: (message: string | null) => void
}) {
  const [serviceId, setServiceId] = useState<number | ''>('')
  const [tarif, setTarif] = useState('')
  const [debut, setDebut] = useState('')
  const [fin, setFin] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const [enregistrement, setEnregistrement] = useState(false)
  const aujourdhui = new Date().toISOString().slice(0, 10)
  const { data: pageServices } = useApi(
    () => servicesApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [open],
  )

  // Pré-remplissage à l'ouverture (montée de version sans effet de bord).
  const cle = open ? `ligne-${ligne?.id ?? 'nouvelle'}` : 'fermee'
  const [cleVue, setCleVue] = useState<string | null>(null)
  if (cleVue !== cle) {
    setCleVue(cle)
    if (open) {
      setServiceId(ligne?.serviceId ?? '')
      setTarif(ligne ? String(ligne.tarif) : '')
      setDebut(ligne?.debutService ?? '')
      setFin(ligne?.finService ?? '')
      setErreur(null)
    }
  }

  if (!open) return null

  const handleSubmit = async () => {
    if (serviceId === '' && !ligne) {
      setErreur('Le service est obligatoire')
      return
    }
    if (servicesPris.includes(serviceId as number) && !ligne) {
      setErreur('Ce service est déjà présent dans la demande')
      return
    }
    if (!tarif || Number(tarif) < 0 || Number.isNaN(Number(tarif))) {
      setErreur('Le tarif doit être positif ou nul')
      return
    }
    if (!debut || !fin) {
      setErreur('La période est obligatoire')
      return
    }
    if (debut < aujourdhui) {
      setErreur("Le début doit être postérieur ou égal à aujourd'hui")
      return
    }
    if (fin <= debut) {
      setErreur('La fin doit être strictement postérieure au début')
      return
    }
    setEnregistrement(true)
    setErreur(null)
    try {
      if (ligne) {
        await lignesApi.update(demandeId, ligne.id, {
          tarif: Number(tarif),
          debutService: debut,
          finService: fin,
        })
      } else {
        await lignesApi.create(demandeId, {
          serviceId: serviceId as number,
          tarif: Number(tarif),
          debutService: debut,
          finService: fin,
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
                Service <span className="required">*</span>
                <select
                  value={serviceId}
                  onChange={(e) => setServiceId(e.target.value ? Number(e.target.value) : '')}
                >
                  <option value="">— Choisir —</option>
                  {(pageServices?.content ?? [])
                    .filter((s: Service) => !servicesPris.includes(s.id))
                    .map((s: Service) => (
                      <option key={s.id} value={s.id}>
                        {s.nomService}
                      </option>
                    ))}
                </select>
              </label>
            )}
            <label className="field">
              Tarif (MGA) <span className="required">*</span>
              <input
                type="number"
                min={0}
                value={tarif}
                onChange={(e) => setTarif(e.target.value)}
              />
            </label>
            <span className="field" />
            <label className="field">
              Début <span className="required">*</span>
              <input type="date" min={aujourdhui} value={debut} onChange={(e) => setDebut(e.target.value)} />
            </label>
            <label className="field">
              Fin <span className="required">*</span>
              <input type="date" min={debut || aujourdhui} value={fin} onChange={(e) => setFin(e.target.value)} />
            </label>
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

  const montantTotal = ligne.tarif
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
      if (t.date < ligne.debutService || t.date > ligne.finService) {
        return `Échéance ${i + 1} : doit rester dans la période du service (${ligne.debutService} → ${ligne.finService})`
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
        dateDebut: ligne.debutService,
        dateFin: ligne.finService,
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
          <h3 className="modal-title">Plan de paiement — {ligne.nomService}</h3>
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
                    <input type="date" value={ligne.debutService} disabled />
                  </label>
                  <label className="field">
                    Fin du service
                    <input type="date" value={ligne.finService} disabled />
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
                      {formatDate(ligne.debutService)} → {formatDate(ligne.finService)}).
                    </p>
                  </div>
                  {tranches.map((t, i) => (
                    <div key={i} className="form-grid form-grid--2">
                      <label className="field">
                        Échéance {i + 1} — date <span className="required">*</span>
                        <input
                          type="date"
                          min={ligne.debutService}
                          max={ligne.finService}
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

/** Onglet Dossier : saisie et consultation du dossier de création (demandes CREATION). */
function DossierTab({
  demande,
  onErreur,
  onMessage,
}: {
  demande: Demande
  onErreur: (message: string | null) => void
  onMessage: (message: string | null) => void
}) {
  const [formOuvert, setFormOuvert] = useState(false)
  const [enregistrement, setEnregistrement] = useState(false)
  const { data: dossier, loading, reload } = useApi<Dossier | null>(
    () => dossierApi.get(demande.id).catch(() => null),
    [demande.id],
  )

  const handleSave = async (formulaire: DossierEntreprise) => {
    setEnregistrement(true)
    try {
      const payload = dossierVersPayload(formulaire)
      if (dossier) {
        await dossierApi.update(demande.id, payload)
        onMessage('Dossier mis à jour.')
      } else {
        await dossierApi.create(demande.id, payload)
        onMessage('Dossier enregistré.')
      }
      setFormOuvert(false)
      reload()
    } catch (err) {
      onErreur(err instanceof Error ? err.message : 'Échec de l’enregistrement du dossier')
    } finally {
      setEnregistrement(false)
    }
  }

  if (loading) return <Spinner label="Chargement du dossier…" />

  if (!dossier) {
    return (
      <section className="card card-pad">
        <h3 style={{ margin: '0 0 8px', fontSize: 17, fontWeight: 700 }}>Dossier de création</h3>
        <p className="section-note">
          Aucun dossier saisi pour cette demande de création d&apos;entreprise.
        </p>
        <Can permission="DOSSIER_CREATE">
          <button type="button" className="btn-primary" onClick={() => setFormOuvert(true)}>
            Saisir le dossier
          </button>
        </Can>
        <DossierEntrepriseFormModal
          open={formOuvert}
          dossier={{ ...DOSSIER_VIDE, referenceDemande: demande.referenceDemande }}
          onClose={() => setFormOuvert(false)}
          onSave={(d) => void handleSave(d)}
        />
        {enregistrement && <Spinner label="Enregistrement…" />}
      </section>
    )
  }

  const formulaire = dossierVersFormulaire(dossier, demande.referenceDemande)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div>
        <Can permission="DOSSIER_UPDATE">
          <button type="button" className="btn-ghost" onClick={() => setFormOuvert(true)}>
            Modifier le dossier
          </button>
        </Can>
      </div>
      <DossierEntrepriseDetail
        dossier={formulaire}
        dateDemande={dossier.dateDemande ?? demande.dateDemande}
        numeroIM={dossier.numeroIm ?? '—'}
      />
      <DossierEntrepriseFormModal
        open={formOuvert}
        dossier={formulaire}
        onClose={() => setFormOuvert(false)}
        onSave={(d) => void handleSave(d)}
      />
      {enregistrement && <Spinner label="Enregistrement…" />}
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
            <strong>{l.nomService ?? '—'}</strong>
            <LignePill statut={l.statut} />
          </div>
          <div className="info-grid" style={{ marginTop: 8 }}>
            <div className="info-item">
              <span>Tarif</span>
              <b>{formatMontantMGA(l.tarif)}</b>
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
