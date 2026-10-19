import { StatutBadge } from '@/components/ui'
import { formatDate, formatMontantMGA } from '@/utils/format'
import type {
  RapportClientLigne,
  RapportDemande,
  RapportFinancier,
  RapportGeneral,
  RapportPaiementLigne,
  RapportServiceLigne,
} from '../types'

export function TableRapportGeneral({ rapport }: { rapport: RapportGeneral }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Indicateur</th>
          <th className="cell-number">Valeur</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td className="fw-700">Clients actifs</td>
          <td className="cell-number">{rapport.nombreClients}</td>
        </tr>
        <tr>
          <td className="fw-700">Demandes</td>
          <td className="cell-number">{rapport.nombreDemandes}</td>
        </tr>
        <tr>
          <td className="fw-700">Services actifs</td>
          <td className="cell-number">{rapport.nombreServices}</td>
        </tr>
        <tr>
          <td className="fw-700">Montant total prévu</td>
          <td className="cell-number text-secondary">{formatMontantMGA(rapport.montantTotalPrevu)}</td>
        </tr>
        <tr>
          <td className="fw-700">Montant encaissé</td>
          <td className="cell-number text-secondary">{formatMontantMGA(rapport.montantEncaisse)}</td>
        </tr>
        <tr>
          <td className="fw-700">Reste à payer</td>
          <td className="cell-number">{formatMontantMGA(rapport.resteAPayer)}</td>
        </tr>
        <tr>
          <td className="fw-700">Montant en retard</td>
          <td className="cell-number">{formatMontantMGA(rapport.montantEnRetard)}</td>
        </tr>
        <tr>
          <td className="fw-700">Échéances en retard</td>
          <td className="cell-number">{rapport.echeancesEnRetard}</td>
        </tr>
      </tbody>
    </table>
  )
}

export function TableRapportFinancier({ rapport }: { rapport: RapportFinancier }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Indicateur</th>
          <th className="cell-number">Montant</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td className="fw-700">Montant total prévu</td>
          <td className="cell-number text-secondary">{formatMontantMGA(rapport.montantTotalPrevu)}</td>
        </tr>
        <tr>
          <td className="fw-700">Montant encaissé</td>
          <td className="cell-number text-secondary">{formatMontantMGA(rapport.montantEncaisse)}</td>
        </tr>
        <tr>
          <td className="fw-700">Reste à payer</td>
          <td className="cell-number">{formatMontantMGA(rapport.resteAPayer)}</td>
        </tr>
        <tr>
          <td className="fw-700">Montant en retard</td>
          <td className="cell-number">{formatMontantMGA(rapport.montantEnRetard)}</td>
        </tr>
        {rapport.paiementsParMode.map((mode) => (
          <tr key={mode.mode}>
            <td className="fw-700">Encaissé — {mode.mode}</td>
            <td className="cell-number text-secondary">{formatMontantMGA(mode.montant)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function TableRapportClients({ lignes }: { lignes: RapportClientLigne[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Client</th>
          <th className="cell-number">Demandes</th>
          <th className="cell-number">Montant total</th>
          <th className="cell-number">Montant payé</th>
          <th className="cell-number">Reste à payer</th>
        </tr>
      </thead>
      <tbody>
        {lignes.map((l) => (
          <tr key={l.clientId ?? l.nomSociete}>
            <td className="fw-700">{l.nomSociete}</td>
            <td className="cell-number">{l.nombreDemandes}</td>
            <td className="cell-number text-secondary">{formatMontantMGA(l.montantTotal)}</td>
            <td className="cell-number text-secondary">{formatMontantMGA(l.montantPaye)}</td>
            <td className="cell-number fw-700">{formatMontantMGA(l.reste)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function TableRapportServices({ lignes }: { lignes: RapportServiceLigne[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Service</th>
          <th>Domaine</th>
          <th className="cell-number">Demandes</th>
          <th className="cell-number">Revenu</th>
        </tr>
      </thead>
      <tbody>
        {lignes.map((l) => (
          <tr key={l.serviceId}>
            <td className="fw-700">{l.nomService}</td>
            <td>{l.nomDomaine ?? '—'}</td>
            <td className="cell-number">{l.nombreDemandes}</td>
            <td className="cell-number fw-700">{formatMontantMGA(l.revenu)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function TableRapportDemandes({ lignes }: { lignes: RapportDemande[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Client</th>
          <th>Service</th>
          <th>Statut</th>
          <th>Date</th>
          <th className="cell-number">Tarif</th>
          <th className="cell-number">Payé</th>
          <th className="cell-number">Reste</th>
        </tr>
      </thead>
      <tbody>
        {lignes.map((d) => (
          <tr key={d.id}>
            <td className="fw-700">{d.referenceDemande}</td>
            <td>{d.nomSociete ?? '—'}</td>
            <td>{d.nomService ?? '—'}</td>
            <td>
              <StatutBadge statut={d.statutActuel} />
            </td>
            <td>{formatDate(d.dateDemande)}</td>
            <td className="cell-number text-secondary">{formatMontantMGA(d.montantTotal ?? d.tarif)}</td>
            <td className="cell-number text-secondary">{formatMontantMGA(d.montantPaye)}</td>
            <td className="cell-number fw-700">{formatMontantMGA(d.resteAPayer)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function TableRapportPaiements({ lignes }: { lignes: RapportPaiementLigne[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Demande</th>
          <th>Client</th>
          <th>Mode</th>
          <th>Date</th>
          <th className="cell-number">Montant</th>
        </tr>
      </thead>
      <tbody>
        {lignes.map((p) => (
          <tr key={p.id}>
            <td className="fw-700">{p.referencePaiement}</td>
            <td>{p.referenceDemande ?? '—'}</td>
            <td>{p.nomSociete ?? '—'}</td>
            <td>{p.modePaiement ?? '—'}</td>
            <td>{formatDate(p.datePaiement)}</td>
            <td className="cell-number fw-700">{formatMontantMGA(p.montant)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
