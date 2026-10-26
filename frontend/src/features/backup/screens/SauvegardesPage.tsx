import { useMemo, useState, type ChangeEvent } from 'react'
import { createPortal } from 'react-dom'
import { Can } from '@/components/shared/Can'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Alert, Badge, Button, EmptyState, PageHeader, Spinner } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { formatDateHeure, formatTailleOctets } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { useBackupInfos, useSauvegardes } from '../hooks/useSauvegardes'
import { backupApi } from '../services/backup.service'
import type { Sauvegarde, TypeSauvegarde } from '../types'

const LIBELLES: Record<TypeSauvegarde, string> = {
  manuel: 'Manuelle',
  auto: 'Automatique',
  securite: 'Sécurité',
}

const TONS: Record<TypeSauvegarde, 'info' | 'default' | 'warning'> = {
  manuel: 'info',
  auto: 'default',
  securite: 'warning',
}

const LONGUEUR_MIN_PHRASE = 8

function nomTelechargementLocal(chiffre: boolean): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const base = `backup_${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}_${pad(
    d.getHours(),
  )}${pad(d.getMinutes())}.sql.gz`
  return chiffre ? `${base}.enc` : base
}

/** Gestion des sauvegardes hors ligne : création, téléchargement, restauration, suppression. */
export function SauvegardesPage() {
  const { hasPermission } = useAuth()
  const { data, loading, error, reload } = useSauvegardes()
  const { data: infos } = useBackupInfos()

  const sauvegardes = useMemo(() => data ?? [], [data])

  const [succes, setSucces] = useState<string | null>(null)
  const [erreurAction, setErreurAction] = useState<string | null>(null)
  const [creation, setCreation] = useState(false)

  const [telechargerOpen, setTelechargerOpen] = useState(false)
  const [telechargerChiffre, setTelechargerChiffre] = useState(false)
  const [telechargerPhrase, setTelechargerPhrase] = useState('')
  const [telechargerEnCours, setTelechargerEnCours] = useState(false)
  const [telechargerErreur, setTelechargerErreur] = useState<string | null>(null)

  const [restaurerOpen, setRestaurerOpen] = useState(false)
  const [restaurerFichier, setRestaurerFichier] = useState<File | null>(null)
  const [restaurerPhrase, setRestaurerPhrase] = useState('')
  const [restaurerConfirmation, setRestaurerConfirmation] = useState('')
  const [restaurerEnCours, setRestaurerEnCours] = useState(false)
  const [restaurerErreur, setRestaurerErreur] = useState<string | null>(null)

  const [aSupprimer, setASupprimer] = useState<Sauvegarde | null>(null)
  const [suppressionEnCours, setSuppressionEnCours] = useState(false)

  const reinitialiserMessages = () => {
    setSucces(null)
    setErreurAction(null)
  }

  const sauvegarderMaintenant = async () => {
    reinitialiserMessages()
    setCreation(true)
    try {
      const fichier = await backupApi.creer()
      setSucces(`Sauvegarde créée : ${fichier.nomFichier}`)
      reload()
    } catch (err) {
      setErreurAction(err instanceof Error ? err.message : 'Échec de la création')
    } finally {
      setCreation(false)
    }
  }

  const ouvrirTelechargement = () => {
    setTelechargerChiffre(false)
    setTelechargerPhrase('')
    setTelechargerErreur(null)
    setTelechargerOpen(true)
  }

  const telechargerNouvelle = async () => {
    if (telechargerChiffre && telechargerPhrase.length < LONGUEUR_MIN_PHRASE) {
      setTelechargerErreur(`La phrase secrète doit contenir au moins ${LONGUEUR_MIN_PHRASE} caractères.`)
      return
    }
    setTelechargerEnCours(true)
    setTelechargerErreur(null)
    try {
      const blob = await backupApi.telechargerNouvelle(telechargerChiffre, telechargerPhrase)
      telechargerBlob(blob, nomTelechargementLocal(telechargerChiffre))
      setTelechargerOpen(false)
      setSucces('Sauvegarde téléchargée.')
    } catch (err) {
      setTelechargerErreur(err instanceof Error ? err.message : 'Téléchargement impossible.')
    } finally {
      setTelechargerEnCours(false)
    }
  }

  const telecharger = async (sauvegarde: Sauvegarde) => {
    reinitialiserMessages()
    try {
      const blob = await backupApi.telecharger(sauvegarde.nomFichier)
      telechargerBlob(blob, sauvegarde.nomFichier)
    } catch (err) {
      setErreurAction(err instanceof Error ? err.message : 'Téléchargement impossible.')
    }
  }

  const supprimer = async () => {
    if (!aSupprimer) return
    setSuppressionEnCours(true)
    try {
      await backupApi.supprimer(aSupprimer.nomFichier)
      setASupprimer(null)
      setSucces('Sauvegarde supprimée.')
      reload()
    } catch (err) {
      setErreurAction(err instanceof Error ? err.message : 'Suppression impossible.')
    } finally {
      setSuppressionEnCours(false)
    }
  }

  const ouvrirRestauration = () => {
    setRestaurerFichier(null)
    setRestaurerPhrase('')
    setRestaurerConfirmation('')
    setRestaurerErreur(null)
    setRestaurerOpen(true)
  }

  const choisirFichier = (evenement: ChangeEvent<HTMLInputElement>) => {
    setRestaurerFichier(evenement.target.files?.[0] ?? null)
    setRestaurerErreur(null)
  }

  const restaurer = async () => {
    if (!restaurerFichier) {
      setRestaurerErreur('Sélectionnez un fichier de sauvegarde.')
      return
    }
    if (restaurerConfirmation !== infos?.baseDeDonnees) {
      setRestaurerErreur('Le nom de la base saisi ne correspond pas.')
      return
    }
    setRestaurerEnCours(true)
    setRestaurerErreur(null)
    try {
      const resultat = await backupApi.restaurer(restaurerFichier, restaurerPhrase)
      setRestaurerOpen(false)
      setSucces(`${resultat.message} Sauvegarde de sécurité : ${resultat.sauvegardeSecurite}.`)
      reload()
    } catch (err) {
      setRestaurerErreur(err instanceof Error ? err.message : 'Restauration impossible.')
    } finally {
      setRestaurerEnCours(false)
    }
  }

  if (loading) return <Spinner label="Chargement des sauvegardes…" />

  return (
    <div className="app-page responsables-page">
      <PageHeader
        titre="Sauvegardes"
        sousTitre="Sauvegarde hors ligne de la base de données"
        actions={
          <Can permission="BACKUP_CREATE">
            <div style={{ display: 'flex', gap: 8 }}>
              <Button variant="secondary" onClick={ouvrirTelechargement}>
                Télécharger une sauvegarde
              </Button>
              <Button onClick={() => void sauvegarderMaintenant()} loading={creation} loadingLabel="Sauvegarde…">
                Sauvegarder maintenant
              </Button>
            </div>
          </Can>
        }
      />

      {succes && <Alert tone="success">{succes}</Alert>}
      {erreurAction && <Alert tone="danger">{erreurAction}</Alert>}
      {error && <Alert tone="danger">{error}</Alert>}

      {infos && (
        <section className="card card-pad" style={{ display: 'grid', gap: 8 }}>
          <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700 }}>Configuration</h3>
          <p className="section-note" style={{ margin: 0 }}>
            Base : <strong>{infos.baseDeDonnees}</strong> — rétention {infos.retentionJours} jours —
            sauvegarde automatique quotidienne à{' '}
            <code className="code-inline">{infos.heureSauvegarde}</code> (heure du serveur).
          </p>
          <p className="section-note" style={{ margin: 0 }}>
            Dossier de stockage : <code className="code-inline">{infos.repertoire}</code>
          </p>
          <p className="section-note" style={{ margin: 0 }}>
            La restauration remplace l’intégralité des données. Une sauvegarde de sécurité est créée
            automatiquement avant chaque restauration.
          </p>
        </section>
      )}

      {sauvegardes.length === 0 ? (
        <EmptyState
          titre="Aucune sauvegarde"
          hint="Les sauvegardes automatiques sont générées selon la planification ; une sauvegarde manuelle peut être créée à tout moment."
        />
      ) : (
        <div className="card card-pad">
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Fichier</th>
                    <th>Type</th>
                    <th>Chiffrée</th>
                    <th>Taille</th>
                    <th>Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {sauvegardes.map((sauvegarde) => (
                    <tr key={sauvegarde.nomFichier}>
                      <td className="fw-600">{sauvegarde.nomFichier}</td>
                      <td>
                        <Badge tone={TONS[sauvegarde.type]}>
                          {LIBELLES[sauvegarde.type]}
                        </Badge>
                      </td>
                      <td>
                        <Badge tone={sauvegarde.chiffre ? 'success' : 'neutral'}>
                          {sauvegarde.chiffre ? 'Oui' : 'Non'}
                        </Badge>
                      </td>
                      <td className="cell-number">{formatTailleOctets(sauvegarde.tailleOctets)}</td>
                      <td className="text-secondary">{formatDateHeure(sauvegarde.dateCreation)}</td>
                      <td>
                        <Can permission="BACKUP_VIEW">
                          <button
                            type="button"
                            className="btn-link success"
                            onClick={() => void telecharger(sauvegarde)}
                          >
                            Télécharger ›
                          </button>
                        </Can>
                        <Can permission="BACKUP_RESTORE">
                          <button
                            type="button"
                            className="btn-link"
                            style={{ marginLeft: 10 }}
                            onClick={ouvrirRestauration}
                          >
                            Restaurer
                          </button>
                        </Can>
                        <Can permission="BACKUP_DELETE">
                          <button
                            type="button"
                            className="btn-link danger"
                            style={{ marginLeft: 10 }}
                            onClick={() => setASupprimer(sauvegarde)}
                          >
                            Supprimer
                          </button>
                        </Can>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {hasPermission('BACKUP_CREATE') &&
        telechargerOpen &&
        createPortal(
          <div
            className="modal-backdrop modal-backdrop--center"
            onClick={telechargerEnCours ? undefined : () => setTelechargerOpen(false)}
          >
            <div
              className="modal-card"
              style={{ maxWidth: 460 }}
              onClick={(e) => e.stopPropagation()}
              role="dialog"
              aria-modal="true"
              aria-label="Télécharger une sauvegarde"
            >
              <header className="modal-header">
                <h3 className="modal-title">Télécharger une sauvegarde</h3>
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={() => setTelechargerOpen(false)}
                  aria-label="Fermer"
                  disabled={telechargerEnCours}
                >
                  ✕
                </button>
              </header>
              <div className="modal-body">
                <p className="section-note" style={{ fontSize: 15 }}>
                  La sauvegarde est générée à la volée au format <code>.sql.gz</code>.
                </p>
                <label className="field" style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
                  <input
                    type="checkbox"
                    checked={telechargerChiffre}
                    onChange={(e) => setTelechargerChiffre(e.target.checked)}
                    disabled={telechargerEnCours}
                  />
                  Chiffrer la sauvegarde (AES-256)
                </label>
                {telechargerChiffre && (
                  <label className="field">
                    Phrase secrète
                    <input
                      type="password"
                      value={telechargerPhrase}
                      onChange={(e) => setTelechargerPhrase(e.target.value)}
                      disabled={telechargerEnCours}
                      autoComplete="new-password"
                    />
                    <span className="form-help">
                      Au moins {LONGUEUR_MIN_PHRASE} caractères. Elle sera exigée à la restauration.
                    </span>
                  </label>
                )}
                {telechargerErreur && (
                  <Alert tone="danger">{telechargerErreur}</Alert>
                )}
              </div>
              <footer className="modal-footer">
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={() => setTelechargerOpen(false)}
                  disabled={telechargerEnCours}
                >
                  Annuler
                </button>
                <Button onClick={() => void telechargerNouvelle()} loading={telechargerEnCours} loadingLabel="Génération…">
                  Télécharger
                </Button>
              </footer>
            </div>
          </div>,
          document.body,
        )}

      {restaurerOpen &&
        createPortal(
          <div
            className="modal-backdrop modal-backdrop--center"
            onClick={restaurerEnCours ? undefined : () => setRestaurerOpen(false)}
          >
            <div
              className="modal-card"
              style={{ maxWidth: 520 }}
              onClick={(e) => e.stopPropagation()}
              role="alertdialog"
              aria-modal="true"
              aria-label="Restaurer une sauvegarde"
            >
              <header className="modal-header">
                <h3 className="modal-title">Restaurer une sauvegarde</h3>
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={() => setRestaurerOpen(false)}
                  aria-label="Fermer"
                  disabled={restaurerEnCours}
                >
                  ✕
                </button>
              </header>
              <div className="modal-body" style={{ display: 'grid', gap: 12 }}>
                <Alert tone="warning">
                  Cette opération écrase toutes les données actuelles de la base «{' '}
                  {infos?.baseDeDonnees ?? '—'} ». Une sauvegarde de sécurité est créée avant la
                  restauration.
                </Alert>

                <label className="field">
                  Fichier de sauvegarde (.sql, .sql.gz ou .sql.gz.enc)
                  <input
                    type="file"
                    accept=".sql,.gz,.enc"
                    onChange={choisirFichier}
                    disabled={restaurerEnCours}
                  />
                </label>

                <label className="field">
                  Phrase secrète (si le fichier est chiffré)
                  <input
                    type="password"
                    value={restaurerPhrase}
                    onChange={(e) => setRestaurerPhrase(e.target.value)}
                    disabled={restaurerEnCours}
                    autoComplete="new-password"
                  />
                </label>

                <label className="field">
                  Saisissez le nom de la base pour confirmer : <strong>{infos?.baseDeDonnees}</strong>
                  <input
                    value={restaurerConfirmation}
                    onChange={(e) => setRestaurerConfirmation(e.target.value)}
                    disabled={restaurerEnCours}
                    autoComplete="off"
                  />
                </label>

                {restaurerErreur && <Alert tone="danger">{restaurerErreur}</Alert>}
              </div>
              <footer className="modal-footer">
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={() => setRestaurerOpen(false)}
                  disabled={restaurerEnCours}
                >
                  Annuler
                </button>
                <Button
                  variant="danger"
                  onClick={() => void restaurer()}
                  loading={restaurerEnCours}
                  loadingLabel="Restauration…"
                  disabled={restaurerConfirmation !== infos?.baseDeDonnees}
                >
                  Restaurer
                </Button>
              </footer>
            </div>
          </div>,
          document.body,
        )}

      <ConfirmDialog
        open={aSupprimer !== null}
        titre="Supprimer la sauvegarde"
        message={`Confirmez-vous la suppression définitive de « ${aSupprimer?.nomFichier ?? ''} » ?`}
        confirmer="Supprimer"
        ton="danger"
        enCours={suppressionEnCours}
        onAnnuler={() => setASupprimer(null)}
        onConfirmer={() => void supprimer()}
      />
    </div>
  )
}
