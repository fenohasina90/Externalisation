import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Alert, EmptyState, Icon, PageHeader, Spinner } from '@/components/ui'
import type { IconName } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { StatutDemandePill } from '@/features/demandes'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { rechercheApi } from '../services/recherche.service'
import type { RechercheGlobale } from '../types'

const VIDE: RechercheGlobale = {
  clients: [],
  services: [],
  demandes: [],
  paiements: [],
}

function Ligne({
  icone,
  titre,
  detail,
  meta,
  to,
}: {
  icone: IconName
  titre: string
  detail?: string | null
  meta?: ReactNode
  to: string
}) {
  return (
    <li>
      <Link to={to} className="recherche-ligne">
        <span className="recherche-ligne__icone">
          <Icon name={icone} width={18} height={18} />
        </span>
        <span className="recherche-ligne__corps">
          <span className="recherche-ligne__titre">{titre}</span>
          {detail ? <span className="recherche-ligne__detail">{detail}</span> : null}
        </span>
        {meta ? <span className="recherche-ligne__meta">{meta}</span> : null}
      </Link>
    </li>
  )
}

function Groupe({
  titre,
  compte,
  lien,
  children,
}: {
  titre: string
  compte: number
  lien: string
  children: ReactNode
}) {
  return (
    <section className="card recherche-groupe">
      <header className="recherche-groupe__entete">
        <h2 className="recherche-groupe__titre">
          {titre}
          <span className="recherche-groupe__compte">{compte}</span>
        </h2>
        <Link to={lien} className="recherche-groupe__lien">
          Tout voir
        </Link>
      </header>
      <ul className="recherche-liste">{children}</ul>
    </section>
  )
}

/** Recherche globale branchée sur `GET /api/recherche?q=…`. */
export function RecherchePage() {
  const { hasPermission } = useAuth()
  const [terme, setTerme] = useState('')
  const recherche = useDebounce(terme.trim(), 350)

  const { data, loading, error } = useApi<RechercheGlobale>(
    () => (recherche === '' ? Promise.resolve(VIDE) : rechercheApi.rechercher(recherche)),
    [recherche],
  )

  const resultats = data ?? VIDE
  const peutClients = hasPermission('CLIENT_VIEW')
  const peutServices = hasPermission('SERVICE_VIEW')
  const peutDemandes = hasPermission('DEMANDE_VIEW')
  const peutPaiements = hasPermission('PAIEMENT_VIEW')

  const total = resultats.clients.length + resultats.services.length
    + resultats.demandes.length + resultats.paiements.length
  const lancee = recherche.length > 0

  return (
    <div className="app-page recherche-page">
      <PageHeader
        titre="Recherche globale"
        sousTitre="Clients, services, demandes et paiements — résultats limités aux droits de votre rôle."
      />

      <section className="card card-pad">
        <form role="search" className="recherche-form" onSubmit={(e) => e.preventDefault()}>
          <Icon name="search" width={22} height={22} />
          <input
            type="search"
            autoFocus
            value={terme}
            onChange={(e) => setTerme(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Escape') setTerme('')
            }}
            placeholder="Référence, client, service, paiement…"
            aria-label="Rechercher dans l’application"
          />
          {terme !== '' && (
            <button
              type="button"
              className="recherche-form__effacer"
              onClick={() => setTerme('')}
              aria-label="Effacer la recherche"
              title="Effacer"
            >
              <Icon name="close" width={16} height={16} />
            </button>
          )}
        </form>
        <p className="section-note">
          La recherche porte sur les références, les noms de clients et de services, les contacts
          et les modes de paiement.
        </p>
      </section>

      {!lancee && (
        <EmptyState
          titre="Saisissez un terme pour lancer la recherche"
          hint="Par exemple une référence DEM-2026-00001, un nom de société ou un code service."
        />
      )}

      {lancee && loading && <Spinner label="Recherche en cours…" />}

      {lancee && !loading && error && <Alert tone="danger">{error}</Alert>}

      {lancee && !loading && !error && total === 0 && (
        <EmptyState
          titre={`Aucun résultat pour « ${recherche} »`}
          hint="Vérifiez l’orthographe ou essayez une référence, un nom ou un code plus court."
        />
      )}

      {lancee && !loading && !error && total > 0 && (
        <div className="recherche-groupes">
          {peutClients && resultats.clients.length > 0 && (
            <Groupe titre="Clients" compte={resultats.clients.length} lien="/clients">
              {resultats.clients.map((client) => (
                <Ligne
                  key={`client-${client.id}`}
                  icone="users"
                  titre={client.nomSociete}
                  detail={[client.contact, client.email].filter(Boolean).join(' · ')}
                  meta={<span className="recherche-ligne__type">Client</span>}
                  to={`/clients/${client.id}`}
                />
              ))}
            </Groupe>
          )}

          {peutServices && resultats.services.length > 0 && (
            <Groupe titre="Services" compte={resultats.services.length} lien="/services">
              {resultats.services.map((service) => (
                <Ligne
                  key={`service-${service.id}`}
                  icone="layers"
                  titre={service.nomService}
                  detail={service.codeService ?? 'Service du catalogue'}
                  meta={<span className="recherche-ligne__type">Service</span>}
                  to="/services"
                />
              ))}
            </Groupe>
          )}

          {peutDemandes && resultats.demandes.length > 0 && (
            <Groupe titre="Demandes" compte={resultats.demandes.length} lien="/demandes">
              {resultats.demandes.map((demande) => (
                <Ligne
                  key={`demande-${demande.id}`}
                  icone="file"
                  titre={demande.referenceDemande}
                  detail={[demande.nomSociete ?? 'Sans société', demande.nomService]
                    .filter(Boolean)
                    .join(' · ')}
                  meta={
                    <>
                      {demande.statutActuel ? (
                        <StatutDemandePill statut={demande.statutActuel} />
                      ) : null}
                      <span>{formatDate(demande.dateDemande)}</span>
                    </>
                  }
                  to={`/demandes/${demande.referenceDemande}`}
                />
              ))}
            </Groupe>
          )}

          {peutPaiements && resultats.paiements.length > 0 && (
            <Groupe titre="Paiements" compte={resultats.paiements.length} lien="/paiements/historique">
              {resultats.paiements.map((paiement) => (
                <Ligne
                  key={`paiement-${paiement.id}`}
                  icone="card"
                  titre={paiement.referencePaiement}
                  detail={[paiement.referenceDemande ?? 'Sans demande', paiement.nomSociete]
                    .filter(Boolean)
                    .join(' · ')}
                  meta={
                    <>
                      <b>{formatMontantMGA(paiement.montant ?? 0)}</b>
                      <span>{formatDate(paiement.datePaiement)}</span>
                    </>
                  }
                  to={
                    paiement.referenceDemande
                      ? `/paiements/echeanciers/${paiement.referenceDemande}`
                      : '/paiements/historique'
                  }
                />
              ))}
            </Groupe>
          )}
        </div>
      )}
    </div>
  )
}
