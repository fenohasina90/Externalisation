import {
  configurationApi,
  type ConfigurationPublique,
} from '@/features/administration/services/administration.service'
import { setDevise } from './format'

let promesse: Promise<ConfigurationPublique> | null = null
let courante: ConfigurationPublique | null = null

function repli(): ConfigurationPublique {
  return { nomApplication: document.title, devise: 'MGA', taillePagination: 8 }
}

/**
 * Paramètres d'affichage publics, chargés une seule fois puis mis en cache.
 * Applique aussitôt le titre de l'onglet et la devise des montants.
 */
export function getConfigurationPublique(): Promise<ConfigurationPublique> {
  if (promesse === null) {
    promesse = configurationApi
      .publique()
      .then((publique) => {
        courante = publique
        document.title = publique.nomApplication
        setDevise(publique.devise)
        return publique
      })
      .catch(() => repli())
  }
  return promesse
}

/** Dernière valeur connue (null avant le premier chargement). */
export function getConfigurationPubliqueCourante(): ConfigurationPublique | null {
  return courante
}
