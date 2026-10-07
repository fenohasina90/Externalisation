export interface Domaine {
  id: number
  nomDomaine: string
  description: string | null
  actif: boolean
  nombreSocietes: number
}

export interface Service {
  id: number
  nomService: string
  tarifService: number
  description: string | null
  actif: boolean
}

export interface HistoriqueTarif {
  id: number
  serviceId: number
  tarif: number
  dateDebut: string | null
  dateFin: string | null
}