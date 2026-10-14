export interface PermissionLigne {
  module: string
  voir: boolean
  creer: boolean
  modifier: boolean
  desactiver: boolean
  /** Actions sans permission correspondante : affichées « — » au lieu d'une case. */
  indisponibles?: ('voir' | 'creer' | 'modifier' | 'desactiver')[]
}

interface Props {
  lignes: PermissionLigne[]
  /** SUPER_ADMIN : affichage seul (non modifiable). */
  modifiable: boolean
  onToggle: (module: string, champ: 'voir' | 'creer' | 'modifier' | 'desactiver') => void
}

/** Matrice des permissions d'un rôle (cf. maquette Rôles et permissions). */
export function PermissionsMatrix({ lignes, modifiable, onToggle }: Props) {
  const cellule = (
    l: PermissionLigne,
    champ: 'voir' | 'creer' | 'modifier' | 'desactiver',
    label: string,
  ) => {
    if (l.indisponibles?.includes(champ)) {
      return <span className="text-muted">—</span>
    }
    return (
      <input
        type="checkbox"
        className="perm-check"
        checked={l[champ]}
        disabled={!modifiable}
        onChange={() => onToggle(l.module, champ)}
        aria-label={`${l.module} : ${label}`}
      />
    )
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Module</th>
          <th>Voir</th>
          <th>Créer</th>
          <th>Modifier</th>
          <th>Désactiver / Archiver</th>
        </tr>
      </thead>
      <tbody>
        {lignes.map((l) => (
          <tr key={l.module}>
            <td className="fw-600">{l.module}</td>
            <td>{cellule(l, 'voir', 'voir')}</td>
            <td>{cellule(l, 'creer', 'créer')}</td>
            <td>{cellule(l, 'modifier', 'modifier')}</td>
            <td>{cellule(l, 'desactiver', 'désactiver')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
