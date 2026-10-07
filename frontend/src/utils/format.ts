const formaterMontant = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function formatMontant(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return formaterMontant.format(value)
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