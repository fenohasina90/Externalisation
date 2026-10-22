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

/** `2026-04` → `avr. 26` */
function libelleMois(mois: string): string {
  const [annee, m] = mois.split('-').map(Number)
  const label = MOIS_COURTS[m - 1] ?? mois
  return annee ? `${label} ${String(annee).slice(-2)}` : label
}

/** Liste continue de mois `AAAA-MM` entre deux bornes incluses. */
function moisEntre(debut: string, fin: string): string[] {
  const [anneeDebut, moisDebut] = debut.split('-').map(Number)
  const [anneeFin, moisFin] = fin.split('-').map(Number)
  if (!anneeDebut || !moisDebut || !anneeFin || !moisFin) return []

  const curseur = new Date(Date.UTC(anneeDebut, moisDebut - 1, 1))
  const borne = new Date(Date.UTC(anneeFin, moisFin - 1, 1))
  const mois: string[] = []

  while (curseur <= borne && mois.length < 240) {
    mois.push(
      `${curseur.getUTCFullYear()}-${String(curseur.getUTCMonth() + 1).padStart(2, '0')}`,
    )
    curseur.setUTCMonth(curseur.getUTCMonth() + 1)
  }

  return mois
}

/**
 * Complète les mois sans activité par une valeur nulle, afin que la courbe et
 * les barres représentent une période continue et non des mois juxtaposés.
 */
function completerMois<T extends { mois: string }>(
  donnees: T[],
  creerVide: (mois: string) => T,
  dateDebut?: string,
  dateFin?: string,
): T[] {
  const parMois = new Map(donnees.map((d) => [d.mois, d]))
  const moisTries = [...parMois.keys()].sort()
  const debut = dateDebut?.slice(0, 7) || moisTries[0]
  const fin = dateFin?.slice(0, 7) || moisTries[moisTries.length - 1]

  if (!debut || !fin) return []

  return moisEntre(debut, fin).map((mois) => parMois.get(mois) ?? creerVide(mois))
}

const formaterCompact = new Intl.NumberFormat('fr-FR', {
  notation: 'compact',
  maximumFractionDigits: 1,
})

function montantCompact(valeur: number): string {
  return formaterCompact.format(valeur)
}

function formatEntier(valeur: number): string {
  return valeur.toLocaleString('fr-FR')
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

export function CourbeEvolution({
  donnees,
  dateDebut,
  dateFin,
}: {
  donnees: DemandeMois[]
  dateDebut?: string
  dateFin?: string
}) {
  const serie = completerMois(donnees, (mois) => ({ mois, nombre: 0 }), dateDebut, dateFin)
  const total = serie.reduce((somme, d) => somme + d.nombre, 0)

  if (serie.length === 0 || total === 0) {
    return <p className="section-note">Aucune demande sur la période.</p>
  }

  const W = Math.max(360, serie.length * 58 + 56)
  const H = 230
  const padL = 34
  const padR = 28
  const padB = 28
  const padT = 18
  const max = Math.max(1, ...serie.map((d) => d.nombre))
  const plafond = max <= 4 ? 4 : Math.ceil(max / 4) * 4
  const moyenne = total / serie.length
  const pic = serie.reduce((meilleur, d) => (d.nombre > meilleur.nombre ? d : meilleur), serie[0])
  const xs = (i: number) =>
    serie.length === 1
      ? W / 2
      : padL + (i * (W - padL - padR)) / (serie.length - 1)
  const ys = (v: number) => padT + (H - padB - padT) * (1 - v / plafond)
  const points = serie.map((d, i) => `${xs(i)},${ys(d.nombre)}`).join(' ')
  const baseGauche = serie.length === 1 ? W / 2 : padL
  const aire = `${baseGauche},${H - padB} ${points} ${xs(serie.length - 1)},${H - padB}`

  return (
    <div>
      <div className="chart-scroll">
        <svg
          viewBox={`0 0 ${W} ${H}`}
          className="chart-svg"
          style={{ minWidth: `${W}px` }}
          role="img"
          aria-labelledby="evolution-demandes-titre"
          aria-describedby="evolution-demandes-desc"
        >
          <title id="evolution-demandes-titre">Évolution mensuelle des demandes</title>
          <desc id="evolution-demandes-desc">
            {`${formatEntier(total)} demandes sur la période, ${moyenne.toLocaleString('fr-FR', {
              maximumFractionDigits: 1,
            })} par mois en moyenne. Pic à ${formatEntier(pic.nombre)} en ${libelleMois(pic.mois)}.`}
          </desc>
          {[0, 1, 2, 3, 4].map((i) => {
            const valeur = Math.round((plafond * i) / 4)
            return (
              <g key={i}>
                <line x1={padL} y1={ys(valeur)} x2={W - padR} y2={ys(valeur)} className="chart-grid" />
                <text x={2} y={ys(valeur) + 4} className="chart-axe">
                  {valeur}
                </text>
              </g>
            )
          })}
          <polygon points={aire} className="chart-aire" />
          <polyline points={points} className="chart-ligne" />
          {serie.map((d, i) => (
            <g key={d.mois}>
              <circle cx={xs(i)} cy={ys(d.nombre)} r={4} className="chart-point">
                <title>{`${libelleMois(d.mois)} : ${formatEntier(d.nombre)} demande${
                  d.nombre > 1 ? 's' : ''
                }`}</title>
              </circle>
              <text
                x={xs(i)}
                y={Math.max(padT + 9, ys(d.nombre) - 10)}
                textAnchor="middle"
                className="chart-valeur"
              >
                {formatEntier(d.nombre)}
              </text>
              <text x={xs(i)} y={H - 8} textAnchor="middle" className="chart-axe">
                {libelleMois(d.mois)}
              </text>
            </g>
          ))}
        </svg>
      </div>
      <div className="chart-resume">
        <span>
          <strong>{formatEntier(total)}</strong> demande{total > 1 ? 's' : ''} sur la période
        </span>
        <span>
          <strong>{moyenne.toLocaleString('fr-FR', { maximumFractionDigits: 1 })}</strong> / mois en
          moyenne
        </span>
        <span>
          Pic : <strong>{formatEntier(pic.nombre)}</strong> en {libelleMois(pic.mois)}
        </span>
      </div>
    </div>
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

export function BarresPaiementsMensuels({
  donnees,
  dateDebut,
  dateFin,
}: {
  donnees: MoisMontant[]
  dateDebut?: string
  dateFin?: string
}) {
  const serie = completerMois(donnees, (mois) => ({ mois, montant: 0 }), dateDebut, dateFin)
  const total = serie.reduce((somme, m) => somme + m.montant, 0)

  if (serie.length === 0 || total === 0) {
    return <p className="section-note">Aucun paiement sur la période.</p>
  }

  const max = Math.max(1, ...serie.map((m) => m.montant))
  const moyenne = total / serie.length
  const pic = serie.reduce((meilleur, m) => (m.montant > meilleur.montant ? m : meilleur), serie[0])

  return (
    <div>
      <div className="chart-scroll">
        <div
          className="vbars"
          style={{ minWidth: `${Math.max(serie.length * 64, 320)}px` }}
          role="img"
          aria-label={`Paiements par mois. Total ${formatMontantMGA(total)}, moyenne ${formatMontantMGA(
            moyenne,
          )} par mois, meilleur mois ${libelleMois(pic.mois)} avec ${formatMontantMGA(pic.montant)}.`}
        >
          {serie.map((m) => {
            const hauteur = m.montant > 0 ? Math.max(3, (m.montant / max) * 100) : 0
            return (
              <div
                key={m.mois}
                className="vbars__mois"
                title={`${libelleMois(m.mois)} : ${formatMontantMGA(m.montant)}`}
              >
                <div className="vbars__colonnes">
                  <div className="vbars__barre" style={{ height: `${hauteur}%` }}>
                    <span className="vbars__valeur">{montantCompact(m.montant)}</span>
                    <span className="vbars__paye" />
                  </div>
                </div>
                <span className="vbars__label">{libelleMois(m.mois)}</span>
              </div>
            )
          })}
        </div>
      </div>
      <div className="chart-resume">
        <span>
          <strong>{formatMontantMGA(total)}</strong> encaissés sur la période
        </span>
        <span>
          <strong>{formatMontantMGA(moyenne)}</strong> / mois en moyenne
        </span>
        <span>
          Meilleur mois : <strong>{libelleMois(pic.mois)}</strong> ({formatMontantMGA(pic.montant)})
        </span>
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
