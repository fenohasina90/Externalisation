import type { ReactNode } from 'react'
import { Card, CardTitle } from '@/components/ui'

interface SectionProps {
  titre: string
  children: ReactNode
}

export function Section({ titre, children }: SectionProps) {
  return (
    <Card>
      <CardTitle>{titre}</CardTitle>
      {children}
    </Card>
  )
}