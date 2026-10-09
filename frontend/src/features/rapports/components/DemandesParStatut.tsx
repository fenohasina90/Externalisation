const TON_STATUT: Record<string, string> = {
  'En attente': 'pill wt',
  Validation: 'pill ok',
  'En cours': 'pill info',
  'Terminées': 'pill ok',
  'Annulées': 'pill danger',
}

/** Répartition des demandes par statut, avec code couleur (cf. maquette). */
export function DemandesParStatut({ items }: { items: { statut: string; nombre: number }[] }) {
  return (
    <div>
      {items.map((s) => (
        <div key={s.statut} className="rc">
          <span className={TON_STATUT[s.statut] ?? 'pill wt'}>● {s.statut}</span>
          <strong>{s.nombre}</strong>
        </div>
      ))}
    </div>
  )
}
