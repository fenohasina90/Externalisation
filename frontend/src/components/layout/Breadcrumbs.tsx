import { Link, useLocation } from 'react-router-dom'
import { buildBreadcrumbs } from '@/constants/breadcrumbs'

export function Breadcrumbs() {
  const { pathname } = useLocation()
  const crumbs = buildBreadcrumbs(pathname)

  return (
    <nav className="breadcrumbs" aria-label="Fil d'Ariane">
      <ol>
        <li>
          <Link to="/">Cabinet Excellent</Link>
        </li>
        {crumbs.map((crumb) => (
          <li key={crumb.label + (crumb.to ?? '')}>
            {crumb.to ? (
              <Link to={crumb.to}>{crumb.label}</Link>
            ) : (
              <span aria-current="page">{crumb.label}</span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  )
}