import type { ReactNode } from 'react'

interface AuthLayoutProps {
  title: string
  subtitle: string
  children: ReactNode
}

export function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  return (
    <div className="auth">
      <section className="auth__brand" aria-label="Cabinet Excellent">
        <img
          className="auth__logo"
          src="/logo-cabinet-excellent.jpg"
          alt="Logo Cabinet Excellent"
        />
        <span className="auth__bar" aria-hidden="true" />
        <h2 className="auth__brand-title">Gestion Externalisation</h2>
        <p className="auth__brand-text">
          Suivez vos services d’externalisation, des demandes jusqu’aux paiements.
        </p>
      </section>

      <main className="auth__panel">
        <div className="auth__content">
          <header className="auth__header">
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </header>
          {children}
        </div>
        <footer className="auth__footer">© Cabinet Excellent — Gestion Externalisation</footer>
      </main>
    </div>
  )
}