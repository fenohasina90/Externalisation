import { useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  Field,
  Input,
  PageHeader,
  Pagination,
  Select,
  Spinner,
} from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { messageErreur } from '@/constants/messages'
import type { ApiPage, PageQuery } from '@/types'
import { formatDateHeure } from '@/utils/format'
import { reinitialisationsApi } from '../services/administration.service'
import type {
  DemandeReinitialisation,
  MotDePasseTemporaire,
  StatutDemandeReinitialisation,
} from '../types'

const TAILLE_PAGE = 20

const LIBELLES_STATUT: Record<StatutDemandeReinitialisation, string> = {
  EN_ATTENTE: 'En attente',
  APPROUVEE: 'Approuvée',
  REFUSEE: 'Refusée',
  EXPIREE: 'Expirée',
  TERMINEE: 'Terminée',
}

type Tone = 'warning' | 'success' | 'danger' | 'neutral' | 'info' | 'default'

function tonStatut(statut: StatutDemandeReinitialisation): Tone {
  switch (statut) {
    case 'EN_ATTENTE':
      return 'warning'
    case 'APPROUVEE':
      return 'info'
    case 'REFUSEE':
      return 'danger'
    case 'EXPIREE':
      return 'neutral'
    case 'TERMINEE':
      return 'success'
    default:
      return 'neutral'
  }
}

export function ReinitialisationsPage() {
  const [statut, setStatut] = useState<StatutDemandeReinitialisation | ''>('')
  const [page, setPage] = useState(0)

  const [actionError, setActionError] = useState<string | null>(null)
  const [traitementId, setTraitementId] = useState<number | null>(null)

  const [refus, setRefus] = useState<DemandeReinitialisation | null>(null)
  const [motif, setMotif] = useState('')

  const [motDePasse, setMotDePasse] = useState<MotDePasseTemporaire | null>(null)

  const { data, loading, error, reload } = useApi<ApiPage<DemandeReinitialisation>>(
    () =>
      reinitialisationsApi.list(statut || null, {
        page,
        size: TAILLE_PAGE,
        recherche: null,
      } as PageQuery),
    [page, statut],
  )

  const demandes = data?.content ?? []

  async function approuver(demande: DemandeReinitialisation): Promise<void> {
    setActionError(null)
    setTraitementId(demande.id)
    try {
      await reinitialisationsApi.approuver(demande.id)
      reload()
    } catch (err) {
      setActionError(messageErreur(err))
    } finally {
      setTraitementId(null)
    }
  }

  async function confirmerRefus(): Promise<void> {
    if (!refus) return
    if (!motif.trim()) {
      setActionError('Le motif de refus est obligatoire.')
      return
    }
    setActionError(null)
    setTraitementId(refus.id)
    try {
      await reinitialisationsApi.refuser(refus.id, motif.trim())
      setRefus(null)
      setMotif('')
      reload()
    } catch (err) {
      setActionError(messageErreur(err))
    } finally {
      setTraitementId(null)
    }
  }

  async function generer(demande: DemandeReinitialisation): Promise<void> {
    setActionError(null)
    setMotDePasse(null)
    setTraitementId(demande.id)
    try {
      const resultat = await reinitialisationsApi.reinitialiser(demande.id)
      setMotDePasse(resultat)
      reload()
    } catch (err) {
      setActionError(messageErreur(err))
    } finally {
      setTraitementId(null)
    }
  }

  async function copierMotDePasse(): Promise<void> {
    if (!motDePasse) return
    try {
      await navigator.clipboard.writeText(motDePasse.motDePasseTemporaire)
    } catch {
      setActionError('Impossible de copier automatiquement. Sélectionnez le mot de passe manuellement.')
    }
  }

  return (
    <>
      <PageHeader
        titre="Demandes de réinitialisation"
        sousTitre="Approuvez ou refusez les demandes, puis générez un mot de passe temporaire."
      />

      <div className="toolbar">
        <Select
          aria-label="Filtrer par statut"
          value={statut}
          onChange={(event) => {
            setPage(0)
            setStatut(event.target.value as StatutDemandeReinitialisation | '')
          }}
        >
          <option value="">Tous les statuts</option>
          {(Object.keys(LIBELLES_STATUT) as StatutDemandeReinitialisation[]).map((valeur) => (
            <option key={valeur} value={valeur}>
              {LIBELLES_STATUT[valeur]}
            </option>
          ))}
        </Select>
      </div>

      {actionError ? <Alert tone="danger">{actionError}</Alert> : null}

      {motDePasse ? (
        <Alert tone="success">
          <strong>Mot de passe temporaire généré.</strong> Communiquez-le à l’utilisateur.
          <div className="mot-de-passe-temp">
            <code className="code-inline">{motDePasse.motDePasseTemporaire}</code>
            <Button variant="ghost" onClick={() => void copierMotDePasse()}>
              Copier
            </Button>
          </div>
          <span>
            Valable jusqu’au {formatDateHeure(motDePasse.expiration)}. L’utilisateur devra le changer
            à sa prochaine connexion.
          </span>
        </Alert>
      ) : null}

      {refus ? (
        <Card className="form-card">
          <CardTitle>
            Refuser la demande de {refus.nomComplet} ({refus.username})
          </CardTitle>
          <Field label="Motif du refus" id="motif-refus">
            <Input
              id="motif-refus"
              placeholder="Expliquez la raison du refus"
              value={motif}
              onChange={(event) => setMotif(event.target.value)}
              autoFocus
            />
          </Field>
          <div className="form-actions">
            <Button
              variant="danger"
              loading={traitementId === refus.id}
              loadingLabel="Enregistrement…"
              onClick={() => void confirmerRefus()}
            >
              Confirmer le refus
            </Button>
            <Button
              variant="ghost"
              onClick={() => {
                setRefus(null)
                setMotif('')
              }}
            >
              Annuler
            </Button>
          </div>
        </Card>
      ) : null}

      {loading ? (
        <Spinner label="Chargement des demandes…" />
      ) : error ? (
        <Alert tone="danger">{error}</Alert>
      ) : demandes.length === 0 ? (
        <Card>
          <EmptyState
            titre="Aucune demande de réinitialisation"
            hint="Les demandes envoyées par les utilisateurs apparaîtront ici."
          />
        </Card>
      ) : (
        <Card className="table-card">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Utilisateur</th>
                  <th>Identifiant</th>
                  <th>E-mail</th>
                  <th>Demandée le</th>
                  <th>Statut</th>
                  <th>Traitée par</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {demandes.map((demande) => (
                  <tr key={demande.id}>
                    <td className="cell-strong">{demande.nomComplet}</td>
                    <td>{demande.username}</td>
                    <td>{demande.email}</td>
                    <td>{formatDateHeure(demande.dateDemande)}</td>
                    <td>
                      <Badge tone={tonStatut(demande.statut)}>
                        {LIBELLES_STATUT[demande.statut] ?? demande.statut}
                      </Badge>
                      {demande.motifRefus ? (
                        <div className="cell-hint">{demande.motifRefus}</div>
                      ) : null}
                    </td>
                    <td>
                      {demande.traiteePar ?? '—'}
                      {demande.dateTraitement ? (
                        <div className="cell-hint">{formatDateHeure(demande.dateTraitement)}</div>
                      ) : null}
                    </td>
                    <td>
                      <div className="table-actions">
                        {demande.statut === 'EN_ATTENTE' ? (
                          <>
                            <Button
                              loading={traitementId === demande.id}
                              onClick={() => void approuver(demande)}
                            >
                              Approuver
                            </Button>
                            <Button
                              variant="danger"
                              disabled={traitementId === demande.id}
                              onClick={() => {
                                setActionError(null)
                                setRefus(demande)
                                setMotif('')
                              }}
                            >
                              Refuser
                            </Button>
                          </>
                        ) : demande.statut === 'APPROUVEE' ? (
                          <Button
                            loading={traitementId === demande.id}
                            onClick={() => void generer(demande)}
                          >
                            Générer un mot de passe
                          </Button>
                        ) : (
                          <span className="cell-hint">Aucune action</span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={data?.totalPages ?? 0} onPageChange={setPage} />
        </Card>
      )}
    </>
  )
}