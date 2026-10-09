import { Link } from 'react-router-dom'
import {
  DASH_MOIS,
  DEMANDES_PAR_STATUT,
  EVOLUTION_DEMANDES,
  MONTANT_SERVICES,
  PAIEMENTS_PAR_MOIS,
  RETARDS,
  SERVICES_TOP,
  formatMGA,
  type AlerteDash,
} from '../data/mockDashboard'

/* ---------------- Courbe d'évolution (SVG) ---------------- */

export function CourbeEvolution() {
  const W = 320
  const H = 190
  const padL = 26
  const padB = 22
  const padT = 10
  const max = 40
  const xs = (i: number) => padL + (i * (W - padL - 8)) / (EVOLUTION_DEMANDES.length - 1)
  const ys = (v: number) => padT + (H - padB - padT) * (1 - v / max)
  const points = EVOLUTION_DEMANDES.map((v, i) => `${xs(i)},${ys(v)}`).join(' ')
  const aire = `${padL},${H - padB} ${points} ${xs(EVOLUTION_DEMANDES.length - 1)},${H - padB}`

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="chart-svg" role="img" aria-label="Évolution des demandes">
      {[0, 10, 20, 30, 40].map((v) => (
        <g key={v}>
          <line x1={padL} y1={ys(v)} x2={W - 4} y2={ys(v)} className="chart-grid" />
          <text x={2} y={ys(v) + 4} className="chart-axe">
            {v}
          </text>
        </g>
      ))}
      <polygon points={aire} className="chart-aire" />
      <polyline points={points} className="chart-ligne" />
      {EVOLUTION_DEMANDES.map((v, i) => (
        <g key={i}>
          <circle cx={xs(i)} cy={ys(v)} r={4} className="chart-point" />
          {i === EVOLUTION_DEMANDES.length - 1 && (
            <text x={xs(i) - 6} y={ys(v) - 10} className="chart-valeur">
              {v}
            </text>
          )}
          <text x={xs(i) - 8} y={H - 6} className="chart-axe">
            {DASH_MOIS[i]}
          </text>
        </g>
      ))}
    </svg>
  )
}

/* ---------------- Donut des statuts (SVG) ---------------- */

export function DonutStatuts() {
  const total = DEMANDES_PAR_STATUT.reduce((s, d) => s + d.nombre, 0)
  const R = 70
  const C = 2 * Math.PI * R

  // Longueurs et décalages cumulés calculés avant le rendu (pas de mutation en JSX).
  const segments = DEMANDES_PAR_STATUT.reduce<
    { statut: string; couleur: string; nombre: number; longueur: number; decalage: number }[]
  >((acc, d) => {
    const precedent = acc.length === 0 ? 0 : acc[acc.length - 1].decalage + acc[acc.length - 1].longueur
    return [...acc, { ...d, longueur: (d.nombre / total) * C, decalage: precedent }]
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
        {DEMANDES_PAR_STATUT.map((d) => (
          <li key={d.statut}>
            <i style={{ background: d.couleur }} />
            {d.statut} · <strong>{d.nombre}</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}

/* ---------------- Barres verticales groupées ---------------- */

export function BarresPaiementsMensuels() {
  const max = Math.max(...PAIEMENTS_PAR_MOIS.map((m) => m.attendu))
  return (
    <div>
      <div className="vbars" role="img" aria-label="Paiements par mois">
        {PAIEMENTS_PAR_MOIS.map((m) => (
          <div key={m.mois} className="vbars__mois">
            <div className="vbars__colonnes">
              <span style={{ height: `${(m.attendu / max) * 100}%` }} className="vbars__attendu" title={`Attendu : ${m.attendu} M`} />
              <span style={{ height: `${(m.paye / max) * 100}%` }} className="vbars__paye" title={`Payé : ${m.paye} M`} />
            </div>
            <span className="vbars__label">{m.mois}</span>
          </div>
        ))}
      </div>
      <p className="chart-legende">
        <i className="dot dot--clair" /> Attendu <i className="dot dot--vert" /> Payé · 16,3 M MGA en
        septembre
      </p>
    </div>
  )
}

/* ---------------- Retards par tranche ---------------- */

export function TranchesRetard() {
  const max = Math.max(...RETARDS.map((r) => r.montant))
  return (
    <div>
      {RETARDS.map((r) => (
        <div key={r.tranche} className="retard-ligne">
          <div className="retard-haut">
            <span>
              <strong>{r.tranche}</strong> · {r.echeances} échéance{r.echeances > 1 ? 's' : ''}
            </span>
            <strong>{formatMGA(r.montant)}</strong>
          </div>
          <div className="hbar">
            <i
              className={r.ton === 'rouge' ? 'hbar__remplissage--rouge' : 'hbar__remplissage--orange'}
              style={{ width: `${(r.montant / max) * 100}%` }}
            />
          </div>
        </div>
      ))}
      <p className="retard-total">
        <strong>7 échéances</strong> en retard · 6 500 000 MGA
      </p>
    </div>
  )
}

/* ---------------- Barres horizontales ---------------- */

export function TopServices() {
  const max = Math.max(...SERVICES_TOP.map((s) => s.nombre))
  return (
    <div>
      {SERVICES_TOP.map((s) => (
        <div key={s.service} className="hbar-ligne">
          <div className="hbar-haut">
            <span>{s.service}</span>
            <strong>{s.nombre}</strong>
          </div>
          <div className="hbar">
            <i className="hbar__remplissage" style={{ width: `${(s.nombre / max) * 100}%` }} />
          </div>
        </div>
      ))}
    </div>
  )
}

export function MontantServices() {
  const max = Math.max(...MONTANT_SERVICES.map((s) => s.millions))
  return (
    <div>
      {MONTANT_SERVICES.map((s) => (
        <div key={s.service} className="hbar-ligne">
          <div className="hbar-haut">
            <span>{s.service}</span>
            <strong>{s.montant}</strong>
          </div>
          <div className="hbar">
            <i className="hbar__remplissage" style={{ width: `${(s.millions / max) * 100}%` }} />
          </div>
        </div>
      ))}
    </div>
  )
}

/* ---------------- Cartes d'alerte ---------------- */

const TON_PILL: Record<AlerteDash['ton'], string> = {
  gris: 'pill wt',
  orange: 'pill warning',
  rouge: 'pill danger',
  vert: 'pill ok',
}

export function CartesAlertes({ alertes }: { alertes: AlerteDash[] }) {
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
