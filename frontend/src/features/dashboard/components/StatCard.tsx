import { Card } from '@/components/ui'

interface StatCardProps {
  label: string
  valeur: string | number
  accent?: string
}

export function StatCard({ label, valeur, accent }: StatCardProps) {
  return (
    <Card className="stat-card">
      {accent ? <span className={`stat-dot stat-${accent}`} aria-hidden="true" /> : null}
      <span className="stat-value">{valeur}</span>
      <span className="stat-label">{label}</span>
    </Card>
  )
}