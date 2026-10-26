import { Link } from 'react-router-dom'

interface ErrorPageProps {
  code: string
  /** Couleur du code : vert (404) ou rouge (403). */
  ton?: 'vert' | 'rouge'
  titre: string
  description: string
}

/** Gabarit partagé des pages d'erreur (cf. maquettes 404 / 403). */
export function ErrorPage({ code, ton = 'vert', titre, description }: ErrorPageProps) {
  return (
    <div className="error-page">
      <img
        className="error-page__logo"
        src="/logo-cabinet-excellent.jpg"
        alt="Logo Cabinet Excellent"
      />
      <main className="error-page__card">
        <p className={`error-page__code error-page__code--${ton}`}>{code}</p>
        <span className="error-page__bar" aria-hidden="true" />
        <h1 className="error-page__title">{titre}</h1>
        <p className="error-page__text">{description}</p>
        <Link to="/" className="btn-primary error-page__action">
          Retour au tableau de bord
        </Link>
      </main>
      <footer className="error-page__footer">© Cabinet Excellent — Gestion Externalisation</footer>
    </div>
  )
}

/** 404 — Page introuvable. */
export function NotFoundPage() {
  return (
    <ErrorPage
      code="404"
      ton="vert"
      titre="Page introuvable."
      description="La page que vous cherchez n'existe pas ou a été déplacée."
    />
  )
}

/** 403 — Accès refusé. */
export function ForbiddenPage() {
  return (
    <ErrorPage
      code="403"
      ton="rouge"
      titre="Accès refusé."
      description="Vous n'avez pas les permissions nécessaires. Contactez votre administrateur si vous pensez qu'il s'agit d'une erreur."
    />
  )
}
