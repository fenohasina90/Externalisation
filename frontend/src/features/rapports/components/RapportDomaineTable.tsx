import { formatMGA, type LigneDomaine } from '../data/mockRapports'

/** Tableau « par domaine » du rapport (montants formatés MGA). */
export function RapportDomaineTable({ lignes }: { lignes: LigneDomaine[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Domaine</th>
          <th>Demandes</th>
          <th>Montant total</th>
          <th>Montant payé</th>
          <th>Reste à payer</th>
        </tr>
      </thead>
      <tbody>
        {lignes.map((l) => (
          <tr key={l.domaine}>
            <td className="fw-700">{l.domaine}</td>
            <td className="cell-number">{l.demandes}</td>
            <td className="cell-number text-secondary">{formatMGA(l.montantTotal)}</td>
            <td className="cell-number text-secondary">{formatMGA(l.montantPaye)}</td>
            <td className="cell-number fw-700">{formatMGA(l.resteAPayer)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
