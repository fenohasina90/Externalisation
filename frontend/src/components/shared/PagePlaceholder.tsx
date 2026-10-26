import { Alert, PageHeader } from '@/components/ui'

interface PagePlaceholderProps {
  titre: string
  sousTitre?: string
  fonctionnalites?: string[]
}

export function PagePlaceholder({ titre, sousTitre, fonctionnalites }: PagePlaceholderProps) {
  return (
    <>
      <PageHeader titre={titre} sousTitre={sousTitre} />
      <Alert tone="info">
        Ce module est en cours de développement. Les fonctionnalités suivantes sont prévues :
      </Alert>
      {fonctionnalites && fonctionnalites.length > 0 ? (
        <ul className="placeholder-list">
          {fonctionnalites.map((fonctionnalite) => (
            <li key={fonctionnalite}>{fonctionnalite}</li>
          ))}
        </ul>
      ) : null}
    </>
  )
}