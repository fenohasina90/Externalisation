export type StatutPlan = 'Payé' | 'Partiel' | 'En attente' | 'En retard'
export type ModePaiement = 'Espèces' | 'Virement'

export interface EcheancePlan {
  numero: number
  dateEcheance: string
  montantAttendu: number
  montantPaye: number
}

export interface PlanPaiement {
  reference: string
  demandeRef: string
  client: string
  dateDebut: string
  montantTotal: number
  montantPaye: number
  statut: StatutPlan
  dernierMode: ModePaiement | null
  echeances: EcheancePlan[]
}

export interface PaiementHistorique {
  reference: string
  demandeRef: string
  echeance: number
  client: string
  date: string
  montant: number
  mode: ModePaiement
  refVirement: string | null
  utilisateur: string
}

/** 8 plans de la maquette (ordre d'affichage conservé). */
function heros(): PlanPaiement[] {
  const base: Array<[string, string, string, string, number, number, StatutPlan, ModePaiement | null]> = [
    ['PLN-2026-014', 'REF-2026-014', 'Atlas Conseil', '01/09/2026', 1000000, 700000, 'Partiel', 'Virement'],
    ['PLN-2026-013', 'REF-2026-013', 'Innovatech SARL', '15/08/2026', 2400000, 2400000, 'Payé', 'Virement'],
    ['PLN-2026-012', 'REF-2026-012', 'Globex France', '01/10/2026', 850000, 0, 'En attente', null],
    ['PLN-2026-011', 'REF-2026-011', 'Matrizel SARL', '01/08/2026', 1800000, 600000, 'En retard', 'Virement'],
    ['PLN-2026-010', 'REF-2026-010', 'Ammontech SAS', '01/07/2026', 500000, 500000, 'Payé', 'Espèces'],
    ['PLN-2026-009', 'REF-2026-009', 'Cobalt Consulting', '15/07/2026', 3200000, 1600000, 'Partiel', 'Virement'],
    ['PLN-2026-008', 'REF-2026-008', 'Novacore Services', '01/09/2026', 1200000, 0, 'En retard', null],
    ['PLN-2026-007', 'REF-2026-007', 'Hélios Groupe', '15/06/2026', 900000, 900000, 'Payé', 'Espèces'],
  ]
  return base.map(([reference, demandeRef, client, dateDebut, montantTotal, montantPaye, statut, dernierMode], i) => ({
    reference,
    demandeRef,
    client,
    dateDebut,
    montantTotal,
    montantPaye,
    statut,
    dernierMode,
    echeances:
      reference === 'PLN-2026-014'
        ? [
            { numero: 1, dateEcheance: '10/09/2026', montantAttendu: 300000, montantPaye: 300000 },
            { numero: 2, dateEcheance: '10/10/2026', montantAttendu: 300000, montantPaye: 200000 },
            { numero: 3, dateEcheance: '10/11/2026', montantAttendu: 400000, montantPaye: 200000 },
          ]
        : repartir(montantTotal, montantPaye, 3, i),
  }))
}

const CLIENTS_GEN = ['Atlas Conseil', 'Innovatech SARL', 'Globex France', 'Matrizel SARL', 'Ammontech SAS', 'Cobalt Consulting', 'Novacore Services', 'Hélios Groupe']

/** Répartit un montant en n échéances, le payé remplit depuis la 1ʳᵉ. */
function repartir(total: number, paye: number, n: number, seed: number): EcheancePlan[] {
  const parts: number[] = []
  let cumul = 0
  for (let k = 0; k < n; k++) {
    const part = k === n - 1 ? total - cumul : Math.round(total / n)
    parts.push(part)
    cumul += part
  }
  let restant = paye
  return parts.map((attendu, k) => {
    const verse = Math.min(attendu, restant)
    restant -= verse
    const mois = String(((seed + k * 2) % 9) + 1).padStart(2, '0')
    return { numero: k + 1, dateEcheance: `10/${mois}/2026`, montantAttendu: attendu, montantPaye: verse }
  })
}

/**
 * 40 plans générés. Sommes calibrées pour les KPI maquette :
 * total 84 500 000, payé 52 300 000, 7 en retard au total.
 */
function generer(): PlanPaiement[] {
  const FRACTION: Record<number, number> = { 2: 0.5, 4: 0.6, 5: 0.75, 7: 0.3 }
  const plans: PlanPaiement[] = []
  for (let i = 0; i < 39; i++) {
    const r = i % 8
    const total = 1200000 + (i % 7) * 200000
    let statut: StatutPlan
    let paye: number
    if (r === 0) {
      statut = 'En attente'
      paye = 0
    } else if (r === 1 || r === 3 || r === 6) {
      statut = 'Payé'
      paye = total
    } else if (r === 7) {
      statut = 'En retard'
      paye = Math.round(total * (FRACTION[r] ?? 0))
    } else {
      statut = 'Partiel'
      paye = Math.round(total * (FRACTION[r] ?? 0.5))
    }
    // 5ᵉ retard généré (7 au total avec les 2 héros).
    if (i === 38) {
      statut = 'En retard'
      paye = Math.round(total * 0.4)
    }
    // Numéros uniques : 048→015 puis 006→002 (001 réservé à l'ajusteur).
    const num = i < 34 ? 48 - i : 6 - (i - 34)
    plans.push({
      reference: `PLN-2026-${String(num).padStart(3, '0')}`,
      demandeRef: `REF-2026-${String(num).padStart(3, '0')}`,
      client: CLIENTS_GEN[i % CLIENTS_GEN.length],
      dateDebut: `${String((i % 27) + 1).padStart(2, '0')}/${String((i % 8) + 3).padStart(2, '0')}/2026`,
      montantTotal: total,
      montantPaye: paye,
      statut,
      dernierMode: paye === 0 ? null : i % 2 === 0 ? 'Virement' : 'Espèces',
      echeances: repartir(total, paye, 3, i),
    })
  }
  // Ajusteur : montants exacts pour retomber sur les KPI.
  const sommeTotal = plans.reduce((s, p) => s + p.montantTotal, 0) + 11850000
  const sommePaye = plans.reduce((s, p) => s + p.montantPaye, 0) + 6700000
  const totalAjust = 84500000 - sommeTotal
  const payeAjust = 52300000 - sommePaye
  plans.push({
    reference: 'PLN-2026-001',
    demandeRef: 'REF-2026-001',
    client: 'Atlas Conseil',
    dateDebut: '05/04/2026',
    montantTotal: totalAjust,
    montantPaye: payeAjust,
    statut: 'Partiel',
    dernierMode: 'Virement',
    echeances: repartir(totalAjust, payeAjust, 3, 99),
  })
  return plans
}

/** 48 plans : 8 héros puis le complément (numéros uniques). */
export const MOCK_PLANS: PlanPaiement[] = (() => {
  const tous = [...heros(), ...generer()]
  const vus = new Set<string>()
  return tous.filter((p) => (vus.has(p.reference) ? false : (vus.add(p.reference), true)))
})()

/** 8 paiements de la maquette, du plus récent au plus ancien. */
const HISTO_HEROS: PaiementHistorique[] = [
  { reference: 'PAI-2026-152', demandeRef: 'REF-2026-014', echeance: 2, client: 'Atlas Conseil', date: '03/10/2026', montant: 200000, mode: 'Virement', refVirement: 'VIR-2026-1003', utilisateur: 'Amine Martin' },
  { reference: 'PAI-2026-151', demandeRef: 'REF-2026-009', echeance: 1, client: 'Cobalt Consulting', date: '02/10/2026', montant: 800000, mode: 'Virement', refVirement: 'VIR-2026-1001', utilisateur: 'Sarah Cohen' },
  { reference: 'PAI-2026-150', demandeRef: 'REF-2026-014', echeance: 2, client: 'Atlas Conseil', date: '02/10/2026', montant: 100000, mode: 'Virement', refVirement: 'VIR-2026-1002', utilisateur: 'Sarah Cohen' },
  { reference: 'PAI-2026-149', demandeRef: 'REF-2026-011', echeance: 1, client: 'Matrizel SARL', date: '30/09/2026', montant: 600000, mode: 'Virement', refVirement: 'VIR-2026-0994', utilisateur: 'Amine Martin' },
  { reference: 'PAI-2026-148', demandeRef: 'REF-2026-014', echeance: 2, client: 'Atlas Conseil', date: '28/09/2026', montant: 100000, mode: 'Espèces', refVirement: null, utilisateur: 'Sarah Cohen' },
  { reference: 'PAI-2026-147', demandeRef: 'REF-2026-013', echeance: 1, client: 'Innovatech SARL', date: '25/09/2026', montant: 1200000, mode: 'Virement', refVirement: 'VIR-2026-0981', utilisateur: 'Amine Martin' },
  { reference: 'PAI-2026-146', demandeRef: 'REF-2026-010', echeance: 1, client: 'Ammontech SAS', date: '22/09/2026', montant: 500000, mode: 'Espèces', refVirement: null, utilisateur: 'Sarah Cohen' },
  { reference: 'PAI-2026-145', demandeRef: 'REF-2026-007', echeance: 1, client: 'Hélios Groupe', date: '15/09/2026', montant: 900000, mode: 'Virement', refVirement: 'VIR-2026-0963', utilisateur: 'Amine Martin' },
]

/** 152 paiements : 8 héros + complément déterministe (PAI-2026-144 → 001). */
function genererHisto(): PaiementHistorique[] {
  const lignes: PaiementHistorique[] = []
  for (let i = 0; i < 144; i++) {
    const num = 144 - i
    const virement = i % 3 !== 2
    const jour = String(14 - (i % 14)).padStart(2, '0')
    const mois = i < 60 ? '09' : '08'
    lignes.push({
      reference: `PAI-2026-${String(num).padStart(3, '0')}`,
      demandeRef: `REF-2026-${String((i % 26) + 1).padStart(3, '0')}`,
      echeance: (i % 3) + 1,
      client: CLIENTS_GEN[i % CLIENTS_GEN.length],
      date: `${jour}/${mois}/2026`,
      montant: 100000 + (i % 12) * 100000,
      mode: virement ? 'Virement' : 'Espèces',
      refVirement: virement ? `VIR-2026-${String(950 - i).padStart(4, '0')}` : null,
      utilisateur: i % 2 === 0 ? 'Amine Martin' : 'Sarah Cohen',
    })
  }
  return lignes
}

export const MOCK_HISTORIQUE: PaiementHistorique[] = [...HISTO_HEROS, ...genererHisto()]

export const PAIEMENT_OPTIONS = {
  clients: CLIENTS_GEN,
  periodes: ['Sept. 2026 - Oct. 2026', 'Juil. 2026 - Août 2026', 'Jan. 2026 - Juin 2026'],
}

export function dansPeriode(dateFR: string, periode: string): boolean {
  if (periode === 'tous') return true
  const mois = Number(dateFR.split('/')[1])
  if (periode.startsWith('Sept')) return mois === 9 || mois === 10
  if (periode.startsWith('Juil')) return mois === 7 || mois === 8
  return mois >= 1 && mois <= 6
}

export function formatMGA(n: number): string {
  return `${n.toLocaleString('fr-FR')} MGA`
}
