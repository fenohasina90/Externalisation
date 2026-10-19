import type { ChangeEvent } from 'react'
import { formatTailleOctets } from '@/utils/format'

interface Props {
  fichiers: File[]
  onChange: (fichiers: File[]) => void
  desactive?: boolean
}

function cleFichier(fichier: File): string {
  return `${fichier.name}-${fichier.size}-${fichier.lastModified}`
}

/**
 * Sélection de pièces jointes (tous types de fichiers) en attente d'envoi.
 * Les fichiers ne sont envoyés qu'après la création de l'entité porteuse.
 */
export function SelectionFichiers({ fichiers, onChange, desactive = false }: Props) {
  const ajouter = (evenement: ChangeEvent<HTMLInputElement>) => {
    const choisis = Array.from(evenement.target.files ?? [])
    evenement.target.value = ''
    if (choisis.length === 0) return
    const existantes = new Set(fichiers.map(cleFichier))
    const nouveaux = choisis.filter((f) => !existantes.has(cleFichier(f)))
    onChange([...fichiers, ...nouveaux])
  }

  const retirer = (fichier: File) => {
    onChange(fichiers.filter((f) => cleFichier(f) !== cleFichier(fichier)))
  }

  return (
    <div className="field form-grid__full">
      <span className="field-label">Pièces jointes</span>
      <input
        type="file"
        multiple
        onChange={ajouter}
        disabled={desactive}
        aria-label="Choisir des pièces jointes"
      />
      {fichiers.length > 0 && (
        <ul className="pj-liste" style={{ marginTop: 8, display: 'grid', gap: 6 }}>
          {fichiers.map((fichier) => (
            <li
              key={cleFichier(fichier)}
              style={{ display: 'flex', alignItems: 'center', gap: 8 }}
            >
              <span className="text-secondary" style={{ flex: 1, minWidth: 0 }}>
                {fichier.name}{' '}
                <span className="text-muted">({formatTailleOctets(fichier.size)})</span>
              </span>
              <button
                type="button"
                className="btn-link"
                onClick={() => retirer(fichier)}
                disabled={desactive}
              >
                Retirer
              </button>
            </li>
          ))}
        </ul>
      )}
      <span className="form-help">
        Tous les types de fichiers sont acceptés (image, PDF, tableur, texte, archive…).
      </span>
    </div>
  )
}
