import type { Service } from '../types'

/**
 * Données statiques d'interface (étape 1 — sans backend).
 * 22 services : 21 actifs, 1 archivé.
 */
export const MOCK_SERVICES: Service[] = [
  { id: 1, nomService: 'Audit comptable', tarifService: 1500000, description: 'Vérification et certification des comptes annuels.', actif: true },
  { id: 2, nomService: 'Conseil fiscal', tarifService: 800000, description: 'Optimisation et accompagnement fiscal des sociétés.', actif: true },
  { id: 3, nomService: 'Paie & RH', tarifService: 600000, description: 'Gestion de la paie et des ressources humaines.', actif: true },
  { id: 4, nomService: 'Expertise juridique', tarifService: 900000, description: 'Conseil et assistance juridique aux entreprises.', actif: true },
  { id: 5, nomService: 'Bilan annuel', tarifService: 1200000, description: 'Établissement du bilan et des états financiers.', actif: true },
  { id: 6, nomService: 'Déclaration TVA', tarifService: 350000, description: 'Préparation et dépôt des déclarations de TVA.', actif: true },
  { id: 7, nomService: 'Accompagnement RH', tarifService: 500000, description: "Appui au recrutement et à l'organisation RH.", actif: true },
  { id: 8, nomService: 'Restructuration', tarifService: 2000000, description: 'Réorganisation juridique et financière.', actif: false },
  { id: 9, nomService: 'Tenue comptable', tarifService: 450000, description: 'Saisie et suivi comptable mensuel.', actif: true },
  { id: 10, nomService: 'Audit interne', tarifService: 1100000, description: 'Évaluation des processus et du contrôle interne.', actif: true },
  { id: 11, nomService: "Création d'entreprise", tarifService: 700000, description: 'Accompagnement à la création de sociétés.', actif: true },
  { id: 12, nomService: 'Domiciliation', tarifService: 250000, description: 'Domiciliation commerciale et administrative.', actif: true },
  { id: 13, nomService: 'Recouvrement', tarifService: 400000, description: 'Suivi et recouvrement des créances clients.', actif: true },
  { id: 14, nomService: 'Formation comptable', tarifService: 300000, description: 'Formation des équipes aux outils comptables.', actif: true },
  { id: 15, nomService: 'Externalisation DAF', tarifService: 1800000, description: 'Direction administrative et financière externalisée.', actif: true },
  { id: 16, nomService: 'Contrôle de gestion', tarifService: 950000, description: 'Tableaux de bord et analyse de performance.', actif: true },
  { id: 17, nomService: 'Secrétariat juridique', tarifService: 380000, description: 'Procès-verbaux et formalités juridiques.', actif: true },
  { id: 18, nomService: 'Déclarations sociales', tarifService: 320000, description: 'Établissement des déclarations sociales.', actif: true },
  { id: 19, nomService: 'Inventaire physique', tarifService: 550000, description: 'Organisation et valorisation des inventaires.', actif: true },
  { id: 20, nomService: 'Fusion-acquisition', tarifService: 2500000, description: 'Accompagnement aux opérations de fusion.', actif: true },
  { id: 21, nomService: 'Diagnostic financier', tarifService: 750000, description: 'Analyse de la santé financière.', actif: true },
  { id: 22, nomService: 'Assistance contrôle fiscal', tarifService: 650000, description: 'Accompagnement lors des contrôles fiscaux.', actif: true },
]

export type StatutServiceFilter = 'tous' | 'actifs' | 'archives'
