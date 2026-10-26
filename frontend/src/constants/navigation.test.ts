import { describe, expect, it } from 'vitest'
import { navigationPour, type NavItem } from './navigation'

function items(permissions: string[]): NavItem[] {
  const acces = new Set(permissions)
  const visible = (permission?: string | string[]): boolean => {
    if (!permission) return true
    const codes = Array.isArray(permission) ? permission : [permission]
    return codes.some((code) => acces.has(code))
  }

  return navigationPour((permission) => acces.has(permission)).flatMap((section) =>
    section.items
      .filter((item) => visible(item.permission))
      .map((item) => ({
        ...item,
        children: item.children?.filter((child) => visible(child.permission)),
      }))
      .filter((item) => !item.children || item.children.length > 0),
  )
}

function labels(permissions: string[]): string[] {
  return items(permissions).map((item) => item.label)
}

const SUPER_ADMIN = [
  'DASHBOARD_VIEW',
  'CLIENT_VIEW',
  'RESPONSABLE_VIEW',
  'COMMERCIAL_VIEW',
  'SERVICE_VIEW',
  'DOMAINE_VIEW',
  'DEMANDE_VIEW',
  'DEMANDE_CREATE',
  'DEMANDE_UPDATE',
  'PAIEMENT_VIEW',
  'RAPPORT_DEMANDE_VIEW',
  'RAPPORT_PAIEMENT_VIEW',
  'USER_VIEW',
  'AUDIT_VIEW',
  'CONFIG_VIEW',
  'PASSWORD_RESET_MANAGE',
  'BACKUP_VIEW',
  'MODE_PAIEMENT_VIEW',
]

const ACCUEIL = [
  'DASHBOARD_VIEW',
  'CLIENT_VIEW',
  'RESPONSABLE_VIEW',
  'COMMERCIAL_VIEW',
  'SERVICE_VIEW',
  'DOMAINE_VIEW',
  'DEMANDE_VIEW',
  'DEMANDE_CREATE',
  'DEMANDE_UPDATE',
  'RAPPORT_DEMANDE_VIEW',
]

const ECONOMAT = [
  'DASHBOARD_VIEW',
  'CLIENT_VIEW',
  'DEMANDE_VIEW',
  'PAIEMENT_VIEW',
  'RAPPORT_PAIEMENT_VIEW',
  'MODE_PAIEMENT_VIEW',
]

describe('navigationPour', () => {
  it('construit le menu du super administrateur depuis ses permissions', () => {
    const resultat = labels(SUPER_ADMIN)

    expect(resultat).toEqual([
      'Tableau de bord',
      'Clients',
      'Commerciaux',
      'Services',
      'Demandes',
      'Paiements',
      'Notifications',
      'Rapports',
      'Administration',
    ])
    expect(items(SUPER_ADMIN).find((item) => item.label === 'Administration')?.children)
      .toHaveLength(8)
  })

  it('adapte le menu de l’accueil et affiche les rapports de demandes', () => {
    const resultat = items(ACCUEIL)

    expect(resultat.map((item) => item.label)).toEqual([
      'Tableau de bord',
      'Clients',
      'Commerciaux',
      'Services',
      'Demandes',
      'Notifications',
      'Rapports',
    ])
    expect(resultat.find((item) => item.label === 'Rapports')?.hint).toBe('demandes')
    expect(resultat.some((item) => item.label === 'Paiements')).toBe(false)
  })

  it('affiche immédiatement Paiements si la permission est accordée', () => {
    expect(labels([...ACCUEIL, 'PAIEMENT_VIEW'])).toContain('Paiements')
    expect(
      labels(
        ACCUEIL.filter(
          (permission) => permission !== 'CLIENT_VIEW' && permission !== 'RESPONSABLE_VIEW',
        ),
      ),
    ).not.toContain('Clients')
  })

  it('affiche les modes de paiement en accès direct pour l’économat', () => {
    const resultat = items(ECONOMAT)

    expect(resultat.map((item) => item.label)).toContain('Modes de paiement')
    expect(resultat.find((item) => item.label === 'Demandes')?.hint).toBe('lecture seule')
    expect(resultat.find((item) => item.label === 'Rapports')?.hint).toBe('financier')
  })

  it('évite le doublon Modes de paiement quand Administration est visible', () => {
    const resultat = items([...SUPER_ADMIN, 'MODE_PAIEMENT_VIEW'])

    expect(resultat.filter((item) => item.label === 'Modes de paiement')).toHaveLength(0)
    const administration = resultat.find((item) => item.label === 'Administration')
    expect(administration?.children?.some((child) => child.label === 'Modes de paiement')).toBe(true)
  })

  it('ouvre une entrée Administration pour une permission administrative isolée', () => {
    const resultat = items(['BACKUP_VIEW'])
    const administration = resultat.find((item) => item.label === 'Administration')

    expect(administration).toBeDefined()
    expect(administration?.children?.map((child) => child.label)).toEqual(['Sauvegardes'])
  })

  it('affiche Domaines sous Services lorsque seule cette permission est accordée', () => {
    const services = items(['DOMAINE_VIEW']).find((item) => item.label === 'Services')

    expect(services?.children?.map((child) => child.label)).toEqual(['Domaines'])
  })
})
