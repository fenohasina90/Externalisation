export interface Domaine {
  id: number
  nomDomaine: string
  codeDomaine: string
  description: string | null
  actif: boolean
}

export type TypeService = 'STANDARD' | 'CREATION' | 'TRANSACTION_IMMOBILIERE'

export const TYPES_SERVICE: { valeur: TypeService; label: string }[] = [
  { valeur: 'STANDARD', label: 'Standard' },
  { valeur: 'CREATION', label: "Création d'entreprise ou de société" },
  { valeur: 'TRANSACTION_IMMOBILIERE', label: 'Transaction immobilière' },
]

export function libelleTypeService(type: string | null): string {
  return TYPES_SERVICE.find((t) => t.valeur === type)?.label ?? type ?? 'Standard'
}

/** Les services CREATION et TRANSACTION_IMMOBILIERE doivent être seuls dans une demande. */
export function estTypeServiceSolo(type: string | null | undefined): boolean {
  return type === 'CREATION' || type === 'TRANSACTION_IMMOBILIERE'
}

export interface Service {
  id: number
  nomService: string
  codeService: string | null
  typeService: TypeService | string | null
  description: string | null
  actif: boolean
  domaineId: number | null
  codeDomaine: string | null
}

export interface CreateServicePayload {
  nomService: string
  typeService?: string | null
  description?: string | null
  domaineId?: number | null
}

export type UpdateServicePayload = {
  nomService: string
  typeService?: string | null
  description?: string | null
  domaineId?: number | null
}