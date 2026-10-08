/** Répartition des demandes par statut (cf. maquette). */
export function DemandesParStatut({ items }: { items: { statut: string; nombre: number }[] }) {
  return (
    <div>
      {items.map((s) => (
        <div key={s.statut} className="rc">
          <span>{s.statut}</span>
          <strong>{s.nombre}</strong>
        </div>
      ))}
    </div>
  )
}
