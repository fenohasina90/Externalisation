import { useState } from 'react'
import {
  Alert,
  Badge,
  Card,
  EmptyState,
  PageHeader,
  Pagination,
  Select,
  Spinner,
} from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import type { ApiPage, PageQuery } from '@/types'
import { formatDateHeure } from '@/utils/format'
import { reinitialisationsApi } from '../services/administration.service'
import type {
  DemandeReinitialisation,
  StatutDemandeReinitialisation,
} from '../types'

const TAILLE_PAGE = 20

const LIBELLES_STATUT: Record<StatutDemandeReinitialisation, string> = {
  ENVOYEE: 'E-mail envoyé',
  TERMINEE: 'Terminée',
  EXPIREE: 'Expirée',
  // Ancien flux (historique).
  EN_ATTENTE: 'En attente (ancien flux)',
  APPROUVEE: 'Approuvée (ancien flux)',
  REFUSEE: 'Refusée (ancien flux)',
}

type Tone = 'warning' | 'success' | 'danger' | 'neutral' | 'info' | 'default'

function tonStatut(statut: StatutDemandeReinitialisation): Tone {
  switch (statut) {
    case 'ENVOYEE':
      return 'info'
    case 'TERMINEE':
      return 'success'
    case 'EXPIREE':
      return 'neutral'
    case 'REFUSEE':
      return 'danger'
    default:
      return 'warning'
  }
}

/**
 * Consultation des demandes de réinitialisation. Le mot de passe temporaire est
 * généré et envoyé directement à l'utilisateur par e-mail : il n'existe plus
 * d'action « Approuver » / « Refuser » / « Générer » côté administrateur.
 */
export function ReinitialisationsPage() {
  const [statut, setStatut] = useState<StatutDemandeReinitialisation | ''>('')
  const [page, setPage] = useState(0)

  const { data, loading, error } = useApi<ApiPage<DemandeReinitialisation>>(
    () =>
      reinitialisationsApi.list(statut || null, {
        page,
        size: TAILLE_PAGE,
        recherche: null,
      } as PageQuery),
    [page, statut],
  )

  const demandes = data?.content ?? []

  return (
    <>
      <PageHeader
        titre="Demandes de réinitialisation"
        sousTitre="Traitement automatique : un mot de passe temporaire est envoyé par e-mail directement à l'utilisateur. Aucune approbation n'est nécessaire."
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

      {loading ? (
        <Spinner label="Chargement des demandes…" />
      ) : error ? (
        <Alert tone="danger">{error}</Alert>
      ) : demandes.length === 0 ? (
        <Card>
          <EmptyState
            titre="Aucune demande de réinitialisation"
            hint="Les demandes envoyées par les utilisateurs apparaîtront ici avec le statut de l'e-mail envoyé."
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
                  <th>E-mail envoyé le</th>
                  <th>Expire le</th>
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
                      {demande.dateGenerationMdp
                        ? formatDateHeure(demande.dateGenerationMdp)
                        : '—'}
                      {demande.genereePar ? (
                        <div className="cell-hint">Par {demande.genereePar}</div>
                      ) : (
                        <div className="cell-hint">Par e-mail</div>
                      )}
                    </td>
                    <td>
                      {demande.dateExpirationMdp
                        ? formatDateHeure(demande.dateExpirationMdp)
                        : '—'}
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
