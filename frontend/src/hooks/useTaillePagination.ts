import { useEffect, useState } from 'react'
import { getConfigurationPublique, getConfigurationPubliqueCourante } from '@/utils/affichage'

/**
 * Taille de pagination issue de la configuration (PAGINATION_TAILLE_DEFAUT,
 * 8 par défaut le temps du chargement). Toute modification depuis
 * l'administration s'applique au prochain affichage des listes.
 */
export function useTaillePagination(): number {
  const [taille, setTaille] = useState(
    () => getConfigurationPubliqueCourante()?.taillePagination ?? 8,
  )

  useEffect(() => {
    let ignore = false
    void getConfigurationPublique().then((publique) => {
      if (!ignore) setTaille(publique.taillePagination)
    })
    return () => {
      ignore = true
    }
  }, [])

  return taille
}
