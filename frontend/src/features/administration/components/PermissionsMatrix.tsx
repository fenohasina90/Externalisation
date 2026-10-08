import type { PermissionLigne } from '../data/mockRolesPermissions'

interface Props {
  lignes: PermissionLigne[]
  /** SUPER_ADMIN : affichage seul (non modifiable). */
  modifiable: boolean
  onToggle: (module: string, champ: 'voir' | 'creer' | 'modifier' | 'desactiver') => void
}

/** Matrice des permissions d'un rôle (cf. maquette Rôles et permissions). */
export function PermissionsMatrix({ lignes, modifiable, onToggle }: Props) {
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
            <td>
              <input
                type="checkbox"
                className="perm-check"
                checked={l.voir}
                disabled={!modifiable}
                onChange={() => onToggle(l.module, 'voir')}
                aria-label={`${l.module} : voir`}
              />
            </td>
            <td>
              <input
                type="checkbox"
                className="perm-check"
                checked={l.creer}
                disabled={!modifiable}
                onChange={() => onToggle(l.module, 'creer')}
                aria-label={`${l.module} : créer`}
              />
            </td>
            <td>
              <input
                type="checkbox"
                className="perm-check"
                checked={l.modifier}
                disabled={!modifiable}
                onChange={() => onToggle(l.module, 'modifier')}
                aria-label={`${l.module} : modifier`}
              />
            </td>
            <td>
              <input
                type="checkbox"
                className="perm-check"
                checked={l.desactiver}
                disabled={!modifiable}
                onChange={() => onToggle(l.module, 'desactiver')}
                aria-label={`${l.module} : désactiver`}
              />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
