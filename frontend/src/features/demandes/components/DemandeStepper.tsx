const ETAPES: { id: string; label: string }[] = [
  { id: 'EN_ATTENTE', label: 'En attente' },
  { id: 'EN_COURS', label: 'En cours' },
  { id: 'TERMINEE', label: 'Terminée' },
]

/** Stepper du cycle de vie (cf. maquette du détail demande). */
export function DemandeStepper({ statut }: { statut: string }) {
  if (statut === 'ANNULEE') {
    return (
      <div className="card card-pad">
        <span className="pill danger">● Demande annulée</span>
      </div>
    )
  }

  const normalise = statut === 'VALIDATION' ? 'EN_COURS' : statut
  const indexActuel = ETAPES.findIndex((e) => e.id === normalise)

  return (
    <div className="card card-pad">
      <div className="stepper">
        {ETAPES.map((etape, i) => (
          <span key={etape.id} className="stepper__etape">
            <span
              className={
                i < indexActuel
                  ? 'pill ok'
                  : i === indexActuel
                    ? 'pill ok stepper__actif'
                    : 'pill wt'
              }
            >
              {i < indexActuel ? `✓ ${etape.label}` : etape.label}
            </span>
            {i < ETAPES.length - 1 && <span className="stepper__sep">›</span>}
          </span>
        ))}
      </div>
      <p className="section-note" style={{ marginTop: 10 }}>
        En attente → En cours : automatique (validation de la facture) · En cours → Terminée :
        automatique (toutes lignes terminées ou montant facturé payé) ou manuelle · Annulation
        possible uniquement à la création.
      </p>
    </div>
  )
}
