import { useMemo, useState, type ChangeEvent } from 'react'
import { Can } from '@/components/shared/Can'
import { Alert, Spinner } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { useApi } from '@/hooks/useApi'
import { formatDateHeure, formatTailleOctets } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { documentsApi } from '../services/documents.service'
import type { CiblePieceJointe, PieceJointe } from '../types'

interface Props {
  cible: CiblePieceJointe
  id: number
  titre?: string
}

const PERMISSIONS = {
  demande: {
    voir: 'DOCUMENT_DEMANDE_VIEW',
    deposer: 'DOCUMENT_DEMANDE_UPLOAD',
  },
  paiement: {
    voir: 'DOCUMENT_PAIEMENT_VIEW',
    deposer: 'PAIEMENT_CREATE',
  },
} as const

const PERMISSIONS_TELECHARGEMENT = ['DOCUMENT_DEMANDE_VIEW', 'DOCUMENT_PAIEMENT_VIEW']

/** Liste, dépôt et téléchargement des pièces jointes d'une demande ou d'un paiement. */
export function PiecesJointes({ cible, id, titre }: Props) {
  const { hasPermission } = useAuth()
  const permissions = PERMISSIONS[cible]

  const { data, loading, error, reload } = useApi<PieceJointe[]>(
    () =>
      cible === 'demande'
        ? documentsApi.listByDemande(id)
        : documentsApi.listByPaiement(id),
    [cible, id],
  )

  const [fichier, setFichier] = useState<File | null>(null)
  const [description, setDescription] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [erreurDepot, setErreurDepot] = useState<string | null>(null)

  const documents = useMemo(() => data ?? [], [data])

  const choisirFichier = (evenement: ChangeEvent<HTMLInputElement>) => {
    setFichier(evenement.target.files?.[0] ?? null)
    setErreurDepot(null)
  }

  const deposer = async () => {
    if (!fichier) {
      setErreurDepot('Sélectionnez un fichier.')
      return
    }
    setEnvoi(true)
    setErreurDepot(null)
    try {
      if (cible === 'demande') {
        await documentsApi.uploadDemande(id, fichier, description)
      } else {
        await documentsApi.uploadPaiement(id, fichier, description)
      }
      setFichier(null)
      setDescription('')
      reload()
    } catch (err) {
      setErreurDepot(err instanceof Error ? err.message : "Échec de l'envoi du fichier.")
    } finally {
      setEnvoi(false)
    }
  }

  const telecharger = async (document: PieceJointe) => {
    setErreurDepot(null)
    try {
      const blob = await documentsApi.download(document.id)
      telechargerBlob(blob, document.nomOriginal)
    } catch {
      setErreurDepot(`Téléchargement de « ${document.nomOriginal} » impossible.`)
    }
  }

  const supprimer = async (document: PieceJointe) => {
    setErreurDepot(null)
    try {
      await documentsApi.remove(document.id)
      reload()
    } catch (err) {
      setErreurDepot(err instanceof Error ? err.message : 'Suppression impossible.')
    }
  }

  return (
    <section className="card card-pad" style={{ display: 'grid', gap: 12 }}>
      <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700 }}>
        {titre ?? 'Pièces jointes'} ({documents.length})
      </h3>

      {erreurDepot && (
        <Alert tone="danger">{erreurDepot}</Alert>
      )}
      {error && <Alert tone="danger">{error}</Alert>}

      {hasPermission(permissions.deposer) && (
        <div className="form-grid form-grid--2">
          <label className="field">
            Fichier
            <input type="file" onChange={choisirFichier} disabled={envoi} />
          </label>
          <label className="field">
            Description (facultatif)
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={255}
              disabled={envoi}
              placeholder="Ex. copie du contrat, reçu signé…"
            />
          </label>
          <div className="form-grid__full">
            <button
              type="button"
              className="btn-primary"
              onClick={() => void deposer()}
              disabled={envoi || !fichier}
            >
              {envoi ? 'Envoi…' : 'Ajouter le fichier'}
            </button>
          </div>
        </div>
      )}

      {loading ? (
        <Spinner label="Chargement des pièces jointes…" />
      ) : documents.length === 0 ? (
        <p className="section-note">Aucune pièce jointe pour le moment.</p>
      ) : (
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Fichier</th>
                  <th>Type</th>
                  <th>Taille</th>
                  {cible === 'demande' && <th>Origine</th>}
                  <th>Ajouté par</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {documents.map((document) => (
                  <tr key={document.id}>
                    <td className="fw-600" title={document.description ?? undefined}>
                      {document.nomOriginal}
                    </td>
                    <td className="text-secondary">
                      {(document.typeFichier ?? '—').toUpperCase()}
                    </td>
                    <td className="cell-number">{formatTailleOctets(document.tailleFichier)}</td>
                    {cible === 'demande' && (
                      <td className="text-secondary">
                        {document.referencePaiement ?? 'Demande'}
                      </td>
                    )}
                    <td>{document.uploadedByName ?? '—'}</td>
                    <td className="text-secondary">{formatDateHeure(document.createdAt)}</td>
                    <td>
                      <Can permission={PERMISSIONS_TELECHARGEMENT}>
                        <button
                          type="button"
                          className="btn-link success"
                          onClick={() => void telecharger(document)}
                        >
                          Télécharger ›
                        </button>
                      </Can>
                      <Can permission="DOCUMENT_DEMANDE_DELETE">
                        <button
                          type="button"
                          className="btn-link danger"
                          style={{ marginLeft: 10 }}
                          onClick={() => void supprimer(document)}
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
      )}
    </section>
  )
}
