import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import type { Demande } from '../types'
import { DOSSIER_VIDE } from '../data/mockDossierEntreprise'
import { DossierEntrepriseFormModal } from '../components'
import { dossierVersFormulaire, dossierVersPayload, type Dossier } from '../types/dossier'
import { demandesApi, dossierApi } from '../services/demandes.service'

/** Page de saisie du dossier de création d'entreprise (route dédiée, hors onglets). */
export function DossierEntreprisePage() {
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
  } = useApi<Dossier | null>(
    () =>
      demandeId === undefined
        ? Promise.resolve(null)
        : dossierApi.get(demandeId).catch(() => null),
    [demandeId],
  )

  const retour = `/demandes/${reference}?onglet=dossier`

  const handleSave = async (formulaire: Parameters<typeof dossierVersPayload>[0]) => {
    if (demandeId === undefined) return
    setEnregistrement(true)
    setErreur(null)
    try {
      const payload = dossierVersPayload(formulaire)
      if (dossier) {
        await dossierApi.update(demandeId, payload)
      } else {
        await dossierApi.create(demandeId, payload)
      }
      navigate(retour, { replace: true })
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Échec de l’enregistrement du dossier')
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

  const formulaire = dossier
    ? dossierVersFormulaire(dossier, demande.referenceDemande)
    : { ...DOSSIER_VIDE, referenceDemande: demande.referenceDemande }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Dossier de création d’entreprise</h1>
          <p>
            Demande {demande.referenceDemande} — {demande.nomSociete ?? 'Création d’entreprise'}
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
        <Spinner label="Chargement du dossier…" />
      ) : (
        <DossierEntrepriseFormModal
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
