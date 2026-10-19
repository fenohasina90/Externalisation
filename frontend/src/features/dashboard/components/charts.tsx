import { Link } from 'react-router-dom'
import { formatMontantMGA } from '@/utils/format'
import type {
  AlerteDashboard,
  DemandeMois,
  EcheanceAlerte,
  MoisMontant,
  ServiceRevenu,
  ServiceTop,
  StatutCount,
} from '../types'

/* ---------------- utilitaires ---------------- */

const MOIS_COURTS = [
  'janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin',
  'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.',
]

/** `2026-04` → `avr.` */
function libelleMois(mois: string): string {
  const [, m] = mois.split('-').map(Number)
  return MOIS_COURTS[m - 1] ?? mois
}

const COULEURS_STATUT: Record<string, string> = {
  EN_ATTENTE: '#aeb9b0',
  VALIDATION: '#8fd0a0',
  EN_COURS: '#2D9A5C',
  TERMINEE: '#1d6b3f',
  ANNULEE: '#c0392b',
}

const LIBELLES_STATUT: Record<string, string> = {
  EN_ATTENTE: 'En attente',
  VALIDATION: 'Validation',
  EN_COURS: 'En cours',
  TERMINEE: 'Terminées',
  ANNULEE: 'Annulées',
}

/* ---------------- Courbe d'évolution (SVG) ---------------- */

export function CourbeEvolution({ donnees }: { donnees: DemandeMois[] }) {
  if (donnees.length === 0) {
    return <p className="section-note">Aucune demande enregistrée.</p>
  }

  const W = 320
  const H = 190
  const padL = 26
  const padB = 22
  const padT = 10
  const max = Math.max(1, ...donnees.map((d) => d.nombre))
  const plafond = Math.max(4, Math.ceil(max / 4) * 4)
  const xs = (i: number) =>
    padL + (i * (W - padL - 8)) / Math.max(1, donnees.length - 1)
  const ys = (v: number) => padT + (H - padB - padT) * (1 - v / plafond)
  const points = donnees.map((d, i) => `${xs(i)},${ys(d.nombre)}`).join(' ')
  const aire = `${padL},${H - padB} ${points} ${xs(donnees.length - 1)},${H - padB}`

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="chart-svg" role="img" aria-label="Évolution des demandes">
      {[0, 1, 2, 3, 4].map((i) => {
        const valeur = Math.round((plafond * i) / 4)
        return (
          <g key={i}>
            <line x1={padL} y1={ys(valeur)} x2={W - 4} y2={ys(valeur)} className="chart-grid" />
            <text x={2} y={ys(valeur) + 4} className="chart-axe">
              {valeur}
            </text>
          </g>
        )
      })}
      <polygon points={aire} className="chart-aire" />
      <polyline points={points} className="chart-ligne" />
      {donnees.map((d, i) => (
        <g key={`${d.mois}-${i}`}>
          <circle cx={xs(i)} cy={ys(d.nombre)} r={4} className="chart-point" />
          {i === donnees.length - 1 && (
            <text x={xs(i) - 6} y={ys(d.nombre) - 10} className="chart-valeur">
              {d.nombre}
            </text>
          )}
          <text x={xs(i) - 8} y={H - 6} className="chart-axe">
            {libelleMois(d.mois)}
          </text>
        </g>
      ))}
    </svg>
  )
}

/* ---------------- Donut des statuts (SVG) ---------------- */

export function DonutStatuts({ donnees }: { donnees: StatutCount[] }) {
  const total = donnees.reduce((s, d) => s + d.nombre, 0)
  if (total === 0) {
    return <p className="section-note">Aucune demande enregistrée.</p>
  }

  const R = 70
  const C = 2 * Math.PI * R
  const segments = donnees.reduce<
    { statut: string; label: string; couleur: string; nombre: number; longueur: number; decalage: number }[]
  >((acc, d) => {
    const precedent =
      acc.length === 0 ? 0 : acc[acc.length - 1].decalage + acc[acc.length - 1].longueur
    return [
      ...acc,
      {
        statut: d.statut,
        label: LIBELLES_STATUT[d.statut] ?? d.statut,
        couleur: COULEURS_STATUT[d.statut] ?? '#aeb9b0',
        nombre: d.nombre,
        longueur: (d.nombre / total) * C,
        decalage: precedent,
      },
    ]
  }, [])

  return (
    <div className="donut-wrap">
      <svg viewBox="0 0 180 180" className="donut-svg" role="img" aria-label="Demandes par statut">
        {segments.map((d) => (
          <circle
            key={d.statut}
            cx={90}
            cy={90}
            r={R}
            fill="none"
            stroke={d.couleur}
            strokeWidth={22}
            strokeDasharray={`${Math.max(0, d.longueur - 2)} ${C}`}
            strokeDashoffset={-d.decalage}
            transform="rotate(-90 90 90)"
          />
        ))}
        <text x={90} y={86} textAnchor="middle" className="donut-total">
          {total}
        </text>
        <text x={90} y={106} textAnchor="middle" className="donut-label">
          demandes
        </text>
      </svg>
      <ul className="donut-legende">
        {segments.map((d) => (
          <li key={d.statut}>
            <i style={{ background: d.couleur }} />
            {d.label} · <strong>{d.nombre}</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}

/* ---------------- Barres verticales ---------------- */

export function BarresPaiementsMensuels({ donnees }: { donnees: MoisMontant[] }) {
  if (donnees.length === 0) {
    return <p className="section-note">Aucun paiement enregistré.</p>
  }
  const max = Math.max(1, ...donnees.map((m) => m.montant))
  return (
    <div>
      <div className="vbars" role="img" aria-label="Paiements par mois">
        {donnees.map((m) => (
          <div key={m.mois} className="vbars__mois">
            <div className="vbars__colonnes">
              <span
                style={{ height: `${(m.montant / max) * 100}%` }}
                className="vbars__paye"
                title={`Payé : ${formatMontantMGA(m.montant)}`}
              />
            </div>
            <span className="vbars__label">{libelleMois(m.mois)}</span>
          </div>
        ))}
      </div>
      <p className="chart-legende">
        <i className="dot dot--vert" /> Montant encaissé par mois
      </p>
    </div>
  )
}

/* ---------------- Retards par tranche ---------------- */

export function TranchesRetard({ echeances }: { echeances: EcheanceAlerte[] }) {
  const aujourdHui = new Date()
  aujourdHui.setHours(0, 0, 0, 0)

  const tranches = [
    { tranche: '1 à 15 jours', min: 0, max: 15, ton: 'orange' as const, echeances: 0, montant: 0 },
    { tranche: '16 à 30 jours', min: 16, max: 30, ton: 'orange' as const, echeances: 0, montant: 0 },
    { tranche: 'Plus de 30 jours', min: 31, max: Infinity, ton: 'rouge' as const, echeances: 0, montant: 0 },
  ]

  echeances.forEach((e) => {
    const date = new Date(`${e.dateEcheance}T00:00:00`)
    const jours = Math.max(0, Math.floor((aujourdHui.getTime() - date.getTime()) / 86400000))
    const tranche = tranches.find((t) => jours >= t.min && jours <= t.max)
    if (tranche) {
      tranche.echeances += 1
      tranche.montant += e.reste
    }
  })

  const visibles = tranches.filter((t) => t.echeances > 0)
  if (visibles.length === 0) {
    return <p className="section-note">Aucune échéance en retard.</p>
  }

  const max = Math.max(1, ...visibles.map((t) => t.montant))
  const totalEcheances = visibles.reduce((s, t) => s + t.echeances, 0)
  const totalMontant = visibles.reduce((s, t) => s + t.montant, 0)

  return (
    <div>
      {visibles.map((t) => (
        <div key={t.tranche} className="retard-ligne">
          <div className="retard-haut">
            <span>
              <strong>{t.tranche}</strong> · {t.echeances} échéance{t.echeances > 1 ? 's' : ''}
            </span>
            <strong>{formatMontantMGA(t.montant)}</strong>
          </div>
          <div className="hbar">
            <i
              className={t.ton === 'rouge' ? 'hbar__remplissage--rouge' : 'hbar__remplissage--orange'}
              style={{ width: `${(t.montant / max) * 100}%` }}
            />
          </div>
        </div>
      ))}
      <p className="retard-total">
        <strong>{totalEcheances} échéance{totalEcheances > 1 ? 's' : ''}</strong> en retard ·{' '}
        {formatMontantMGA(totalMontant)}
      </p>
    </div>
  )
}

/* ---------------- Barres horizontales ---------------- */

export function TopServices({ donnees }: { donnees: ServiceTop[] }) {
  const top = [...donnees].sort((a, b) => b.nombreDemandes - a.nombreDemandes).slice(0, 5)
  if (top.length === 0) {
    return <p className="section-note">Aucune donnée service.</p>
  }
  const max = Math.max(1, ...top.map((s) => s.nombreDemandes))
  return (
    <div>
      {top.map((s) => (
        <div key={s.serviceId} className="hbar-ligne">
          <div className="hbar-haut">
            <span>{s.nomService}</span>
            <strong>{s.nombreDemandes}</strong>
          </div>
          <div className="hbar">
            <i className="hbar__remplissage" style={{ width: `${(s.nombreDemandes / max) * 100}%` }} />
          </div>
        </div>
      ))}
    </div>
  )
}

export function MontantServices({ donnees }: { donnees: ServiceRevenu[] }) {
  const top = [...donnees].sort((a, b) => b.revenu - a.revenu).slice(0, 5)
  if (top.length === 0) {
    return <p className="section-note">Aucune donnée service.</p>
  }
  const max = Math.max(1, ...top.map((s) => s.revenu))
  return (
    <div>
      {top.map((s) => (
        <div key={s.serviceId} className="hbar-ligne">
          <div className="hbar-haut">
            <span>{s.nomService}</span>
            <strong>{formatMontantMGA(s.revenu)}</strong>
          </div>
          <div className="hbar">
            <i className="hbar__remplissage" style={{ width: `${(s.revenu / max) * 100}%` }} />
          </div>
        </div>
      ))}
    </div>
  )
}

/* ---------------- Cartes d'alerte ---------------- */

const TON_PILL: Record<AlerteDashboard['ton'], string> = {
  gris: 'pill wt',
  orange: 'pill warning',
  rouge: 'pill danger',
  vert: 'pill ok',
}

export function CartesAlertes({ alertes }: { alertes: AlerteDashboard[] }) {
  if (alertes.length === 0) {
    return <p className="section-note">Aucune alerte à afficher.</p>
  }
  return (
    <div className="alert-grid">
      {alertes.map((a) => (
        <article key={a.etiquette} className="alert-card">
          <span className={TON_PILL[a.ton]}>● {a.etiquette}</span>
          <h4>{a.titre}</h4>
          <p>{a.detail}</p>
          <Link to={a.lien} className="lk">
            Voir ›
          </Link>
        </article>
      ))}
    </div>
  )
}
