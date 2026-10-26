import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import type { Demande } from '../types'
import { DossierTransactionImmobiliereFormModal } from '../components'
import {
  DOSSIER_TRANSACTION_VIDE,
  dossierTransactionVersFormulaire,
  dossierTransactionVersPayload,
  type DossierTransactionForm,
  type DossierTransactionImmobiliere,
} from '../types/dossierTransaction'
import { demandesApi, dossierTransactionApi } from '../services/demandes.service'

/** Page de saisie de la fiche de transaction immobilière (route dédiée, hors onglets). */
export function DossierTransactionImmobilierePage() {
  const { reference } = useParams<{ reference: string }>()
  const navigate = useNavigate()
  const [enregistrement, setEnregistrement] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  const { data: demande, loading, error } = useApi<Demande | null>(
    () => (!reference ? Promise.resolve(null) : demandesApi.getByReference(reference)),
    [reference],
  )
  const demandeId = demande?.id
  const {
    data: dossier,
    loading: chargementDossier,
    error: erreurDossier,
  } = useApi<DossierTransactionImmobiliere | null>(
    () =>
      demandeId === undefined
        ? Promise.resolve(null)
        : dossierTransactionApi.get(demandeId).catch(() => null),
    [demandeId],
  )

  const retour = `/demandes/${reference}?onglet=dossier-transaction`

  const formulaire: DossierTransactionForm = dossier
    ? dossierTransactionVersFormulaire(dossier)
    : DOSSIER_TRANSACTION_VIDE

  const handleSave = async (f: DossierTransactionForm) => {
    if (demandeId === undefined) return
    setEnregistrement(true)
    setErreur(null)
    try {
      const payload = dossierTransactionVersPayload(f)
      if (dossier) {
        await dossierTransactionApi.update(demandeId, payload)
      } else {
        await dossierTransactionApi.create(demandeId, payload)
      }
      navigate(retour, { replace: true })
    } catch (err) {
      setErreur(
        err instanceof Error
          ? err.message
          : 'Échec de l’enregistrement du dossier de transaction immobilière',
      )
      setEnregistrement(false)
    }
  }

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

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Dossier de transaction immobilière</h1>
          <p>
            Demande {demande.referenceDemande} — {demande.nomSociete ?? 'Sans société'}
          </p>
        </div>
        <div className="actions">
          <Link to={retour} className="btn-ghost">
            Retour à la demande
          </Link>
        </div>
      </header>

      {erreur && <div className="alert alert-danger">{erreur}</div>}
      {erreurDossier && <div className="alert alert-danger">{erreurDossier}</div>}

      {chargementDossier ? (
        <Spinner label="Chargement du dossier de transaction immobilière…" />
      ) : (
        <DossierTransactionImmobiliereFormModal
          inline
          open
          dossier={formulaire}
          enregistrement={enregistrement}
          onClose={() => navigate(retour)}
          onSave={(d) => void handleSave(d)}
        />
      )}
    </div>
  )
}
