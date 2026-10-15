const formaterMontant = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function formatMontant(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return formaterMontant.format(value)
}

const formaterEntier = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 })

let deviseCourante = 'MGA'

/** Devise affichée (issue de la configuration, « MGA » par défaut). */
export function setDevise(devise: string): void {
  if (devise.trim()) deviseCourante = devise.trim()
}

/** Montant entier suivi de la devise (ex. « 18 400 000 MGA »). */
export function formatMontantMGA(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return `${formaterEntier.format(value)} ${deviseCourante}`
}

/** Taille de fichier lisible (o, Ko, Mo). */
export function formatTailleOctets(octets: number | null | undefined): string {
  if (octets === null || octets === undefined) return '—'
  if (octets < 1024) return `${octets} o`
  const ko = octets / 1024
  if (ko < 1024) return `${Math.round(ko)} Ko`
  return `${(ko / 1024).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} Mo`
}

export function formatDate(date: string | null | undefined): string {
  if (!date) return '—'
  const parsed = new Date(date)
  if (Number.isNaN(parsed.getTime())) return date
  return parsed.toLocaleDateString('fr-FR')
}

export function formatDateHeure(date: string | null | undefined): string {
  if (!date) return '—'
  const parsed = new Date(date)
  if (Number.isNaN(parsed.getTime())) return date
  return parsed.toLocaleString('fr-FR')
}

export function dateISO(date: Date): string {
  return date.toISOString().slice(0, 10)
}

export function initiales(nom: string, prenom?: string | null): string {
  return `${nom.charAt(0) ?? ''}${prenom?.charAt(0) ?? ''}`.toUpperCase() || '—'
}

/**
 * Valeur d'audit en texte propre : les anciennes traces stockaient
 * `{"valeur":"..."}` — l'enveloppe est retirée à l'affichage.
 */
export function formatValeurAudit(valeur: string | null | undefined): string {
  if (valeur === null || valeur === undefined || valeur === '') return '—'
  const texte = valeur.trim()
  if (texte.startsWith('{"valeur":') || texte.startsWith('{"valeur" :')) {
    try {
      const parsed: unknown = JSON.parse(texte)
      if (typeof parsed === 'object' && parsed !== null && 'valeur' in parsed) {
        const contenu = (parsed as { valeur: unknown }).valeur
        return contenu === null || contenu === undefined ? '—' : String(contenu)
      }
    } catch {
      // Format inattendu : affiche tel quel.
    }
  }
  return valeur
}