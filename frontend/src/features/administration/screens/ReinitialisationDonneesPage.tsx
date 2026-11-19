import { useEffect, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { Alert, Card, CardTitle, PageHeader } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { reinitialisationDonneesApi } from '../services/administration.service'
import type { CibleReinitialisation, ReinitialisationResponse } from '../types'

interface CibleDef {
  code: CibleReinitialisation
  libelle: string
  description: ReactNode
}

/** Libellés français des tables retournées par l'API (pour le récapitulatif). */
const LIBELLES_TABLES: Record<string, string> = {
  piece_jointe: 'Pièces jointes',
  paiement: 'Paiements',
  echeance_paiement: 'Échéances',
  propos_paiement: 'Plans de paiement',
  facture_validee_ligne: 'Lignes de facture validée',
  facture_validee: 'Factures validées',
  facture_proforma_ligne: 'Lignes de proforma',
  facture_proforma: 'Factures proforma',
  dossier_transaction_immobiliere: 'Dossiers transaction immobilière',
  dossier_associe: 'Dossiers associés',
  dossier_creation_entreprise: "Dossiers de création d'entreprise",
  historique_demande: 'Historique des statuts',
  historique_ligne_service: 'Historique des lignes de service',
  ligne_service: 'Lignes de service',
  demande_service: 'Demandes',
  notification: 'Notifications',
  services: 'Services',
  domaine_service: 'Domaines',
  societe: 'Clients (sociétés)',
  client_responsable: 'Responsables client',
  commercial: 'Commerciaux',
}

const CIBLES: CibleDef[] = [
  {
    code: 'paiement',
    libelle: 'Paiements',
    description: (
      <>
        Tous les paiements (encaissements) enregistrés, y compris les pièces jointes
        qui leur sont liées. Les demandes et les factures ne sont pas touchées.
      </>
    ),
  },
  {
    code: 'facture',
    libelle: 'Factures',
    description: (
      <>
        Toutes les factures (validées et proformas) et leurs lignes, ainsi que les
        paiements qui les règlent et les pièces jointes rattachées. Les demandes
        sont conservées.
      </>
    ),
  },
  {
    code: 'demande',
    libelle: 'Demandes',
    description: (
      <>
        Toutes les demandes et tout ce qui en dépend : lignes de service, factures,
        paiements, plans de paiement proposés et échéances, dossiers (création
        d'entreprise, transaction immobilière, dossier associé), historiques et
        pièces jointes.
      </>
    ),
  },
  {
    code: 'notification',
    libelle: 'Notifications',
    description: <>Toutes les notifications (lues et non lues).</>,
  },
  {
    code: 'service',
    libelle: 'Services',
    description: (
      <>
        Tous les services et les lignes externes qui les utilisent, avec tout ce qui
        dépend de ces lignes (lignes de facturation validées, factures, paiements,
        plans de paiement). Les demandes elles-mêmes sont conservées.
      </>
    ),
  },
  {
    code: 'domaine',
    libelle: 'Domaines',
    description: (
      <>
        Tous les domaines et tous les services qu'ils contiennent, avec les mêmes
        cascades que la suppression des services.
      </>
    ),
  },
  {
    code: 'client',
    libelle: 'Clients',
    description: (
      <>
        Toutes les sociétés clientes et tout ce qui en dépend : leurs demandes (et
        toutes les cascades mentionnées dans « Demandes »), leurs responsables client
        (cascade de la base).
      </>
    ),
  },
  {
    code: 'piece_jointe',
    libelle: 'Pièces jointes',
    description: (
      <>
        Toutes les pièces jointes de la base (documents des demandes et de leurs
        paiements), y compris les fichiers sur le disque du serveur.
      </>
    ),
  },
  {
    code: 'responsable_client',
    libelle: 'Responsables client',
    description: (
      <>
        Tous les responsables de dossiers clients (les demandes restent, leur lien est
        simplement marqué comme supprimé).
      </>
    ),
  },
  {
    code: 'commerciaux',
    libelle: 'Commerciaux',
    description: (
      <>
        Tous les commerciaux (les demandes restent, leur lien est simplement marqué
        comme supprimé).
      </>
    ),
  },
]

/** Réinitialisation de données — administration, action irréversible. */
export function ReinitialisationDonneesPage() {
  const [selection, setSelection] = useState<ReadonlySet<CibleReinitialisation>>(new Set())
  const [ouvert, setOuvert] = useState(false)
  const [confirmation, setConfirmation] = useState('')
  const [enCours, setEnCours] = useState(false)
  const [progression, setProgression] = useState(0)
  const [erreur, setErreur] = useState<string | null>(null)
  const [resultat, setResultat] = useState<ReinitialisationResponse | null>(null)

  function basculer(code: CibleReinitialisation) {
    setResultat(null)
    setErreur(null)
    setSelection((actuelles) => {
      const suivantes = new Set(actuelles)
      if (suivantes.has(code)) {
        suivantes.delete(code)
      } else {
        suivantes.add(code)
      }
      return suivantes
    })
  }

  function toutSelectionner() {
    setResultat(null)
    setErreur(null)
    setSelection(new Set(CIBLES.map((c) => c.code)))
  }

  function neRienSelectionner() {
    setResultat(null)
    setErreur(null)
    setSelection(new Set())
  }

  function fermer() {
    if (enCours) return
    setOuvert(false)
    setConfirmation('')
    setErreur(null)
  }

  async function confirmer() {
    setEnCours(true)
    setErreur(null)
    try {
      const res = await reinitialisationDonneesApi.executer([...selection])
      setProgression(100)
      // Laisse la barre atteindre 100 % avant de fermer la fenêtre.
      await new Promise((resolve) => setTimeout(resolve, 350))
      setResultat(res)
      setSelection(new Set())
      setOuvert(false)
      setConfirmation('')
      setProgression(0)
    } catch (err) {
      setProgression(0)
      setErreur(messageErreur(err))
    } finally {
      setEnCours(false)
    }
  }

  /**
   * Barre de progression d'attente : la requête unique étant traitée côté
   * serveur, la barre repart de 0 et tend doucement vers ~92 % ; elle est
   * clôturée à 100 % à la réception de la réponse.
   */
  useEffect(() => {
    if (!enCours) return undefined
    const intervalle = setInterval(() => {
      setProgression((p) => Math.min(92, Math.max(3, p + Math.max(0.25, (92 - p) * 0.03))))
    }, 150)
    return () => clearInterval(intervalle)
  }, [enCours])

  const confirme = confirmation === 'SUPPRIMER'
  const ciblesSelectionnees = CIBLES.filter((c) => selection.has(c.code))

  return (
    <div className="app-page responsables-page">
      <PageHeader
        titre="Réinitialisation de données"
        sousTitre="Suppression définitive des données métier sélectionnées. Action irréversible."
      />

      <Alert tone="danger">
        Les données sélectionnées sont <strong>supprimées définitivement</strong> de la base —
        il n'y a pas de corbeille. Les cascades indiquées sous chaque case sont automatiques :
        sélectionnez « Demande » pour effacer une demande complète, ou laissez le choix par
        catégorie. Le journal d'audit, les utilisateurs et la configuration ne sont pas supprimés.
      </Alert>

      {erreur && <Alert tone="danger">{erreur}</Alert>}
      {resultat && (
        <Alert tone="success">
          Réinitialisation effectuée. Détail :{' '}
          {Object.entries(resultat.compteurs)
            .filter(([, n]) => n > 0)
            .map(([table, n]) => `${LIBELLES_TABLES[table] ?? table} : ${n} ligne${n > 1 ? 's' : ''}`)
            .join(' · ') || 'aucune ligne'}
          {' · '}fichiers supprimés : {resultat.fichiersSupprimes}
          {resultat.fichiersIntrouvables > 0 &&
            ` (${resultat.fichiersIntrouvables} introuvable${resultat.fichiersIntrouvables > 1 ? 's' : ''})`}
          .
        </Alert>
      )}

      <Card className="card-pad">
        <div className="filters-head">
          <div>
            <CardTitle>Données à supprimer</CardTitle>
            <p className="section-note">
              {selection.size} sélectionnée{selection.size > 1 ? 's' : ''} —{' '}
              <button type="button" className="btn-link" onClick={toutSelectionner}>
                Tout sélectionner
              </button>{' '}
              ·{' '}
              <button type="button" className="btn-link" onClick={neRienSelectionner}>
                Aucune
              </button>
            </p>
          </div>
        </div>

        <ul className="placeholder-list" style={{ listStyle: 'none', display: 'grid', gap: 10, margin: 0, padding: 0 }}>
          {CIBLES.map((cible) => (
            <li key={cible.code}>
              <label
                className="rg-ligne"
                style={{
                  display: 'flex',
                  gap: 12,
                  alignItems: 'flex-start',
                  padding: '12px 14px',
                  border: `1px solid ${selection.has(cible.code) ? 'var(--g)' : 'var(--border)'}`,
                  borderRadius: 12,
                  background: selection.has(cible.code) ? 'var(--highlight-bg)' : '#fff',
                  cursor: 'pointer',
                }}
              >
                <input
                  type="checkbox"
                  className="perm-check"
                  style={{ marginTop: 3 }}
                  aria-label={cible.libelle}
                  checked={selection.has(cible.code)}
                  onChange={() => basculer(cible.code)}
                />
                <span>
                  <strong>{cible.libelle}</strong>
                  <br />
                  <small className="text-muted">{cible.description}</small>
                </span>
              </label>
            </li>
          ))}
        </ul>

        <div className="form-actions" style={{ marginTop: 16 }}>
          <button
            type="button"
            className="btn-primary"
            style={{
              background: selection.size > 0 ? 'var(--danger)' : undefined,
              borderColor: selection.size > 0 ? 'var(--danger)' : undefined,
            }}
            disabled={selection.size === 0 || enCours}
            onClick={() => {
              setProgression(0)
              setOuvert(true)
            }}
          >
            Supprimer les données sélectionnées ({selection.size})
          </button>
        </div>
      </Card>

      {ouvert &&
        createPortal(
          <div
            className="modal-backdrop modal-backdrop--center"
            onClick={() => fermer()}
          >
            <div
              className="modal-card"
              style={{ maxWidth: 560 }}
              onClick={(e) => e.stopPropagation()}
              role="alertdialog"
              aria-modal="true"
              aria-label="Confirmation de la suppression"
            >
              <header className="modal-header">
                <h3 className="modal-title">Supprimer définitivement ?</h3>
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={fermer}
                  aria-label="Fermer"
                  disabled={enCours}
                >
                  ✕
                </button>
              </header>
              <div className="modal-body" style={{ display: 'grid', gap: 12 }}>
                <Alert tone="danger">
                  Cette action est <strong>irréversible</strong> — aucune sauvegarde n'est créée
                  automatiquement. Pensez à faire une sauvegarde au préalable.
                </Alert>
                <ul className="placeholder-list" style={{ margin: 0 }}>
                  {ciblesSelectionnees.map((c) => (
                    <li key={c.code}>
                      <strong>{c.libelle}</strong>
                    </li>
                  ))}
                </ul>
                <label className="field">
                  Tapez « SUPPRIMER » pour confirmer
                  <input
                    type="text"
                    value={confirmation}
                    autoFocus
                    onChange={(e) => setConfirmation(e.target.value)}
                    placeholder="SUPPRIMER"
                    disabled={enCours}
                  />
                </label>
                {(enCours || progression > 0) && (
                  <div aria-live="polite">
                    <div
                      className="section-note"
                      style={{ display: 'flex', justifyContent: 'space-between' }}
                    >
                      <span>Suppression en cours…</span>
                      <strong>{Math.floor(progression)} %</strong>
                    </div>
                    <div
                      className="trk"
                      role="progressbar"
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={Math.floor(progression)}
                      aria-label="Progression de la suppression"
                      style={{ marginTop: 6 }}
                    >
                      <i
                        style={{
                          width: `${progression}%`,
                          background: 'var(--danger)',
                          transition: 'width 0.2s ease',
                        }}
                      />
                    </div>
                  </div>
                )}
              </div>
              <footer className="modal-footer">
                <button type="button" className="btn-ghost" onClick={fermer} disabled={enCours}>
                  Annuler
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  style={{ background: 'var(--danger)', borderColor: 'var(--danger)' }}
                  disabled={!confirme || enCours}
                  onClick={confirmer}
                >
                  {enCours ? 'Suppression…' : 'Supprimer définitivement'}
                </button>
              </footer>
            </div>
          </div>,
          document.body,
        )}
    </div>
  )
}
