export interface ConfigChamp {
  cle: string
  label: string
  valeur: string
  hint?: string
  type?: 'text' | 'number' | 'select'
  options?: { valeur: string; label: string }[]
}

/** Valeurs statiques de configuration (cf. maquette). */
export const CONFIG_SECTIONS: { titre: string; sousTitre: string; champs: ConfigChamp[] }[] = [
  {
    titre: 'Application',
    sousTitre: 'Identité et devise.',
    champs: [
      { cle: 'app.nom', label: "Nom de l'application", valeur: 'Gestion Externalisation' },
      {
        cle: 'app.devise',
        label: 'Devise',
        valeur: 'MGA',
        hint: 'Ariary malgache',
      },
    ],
  },
  {
    titre: 'Paiements et alertes',
    sousTitre: 'Échéances et fiscalité.',
    champs: [
      {
        cle: 'paiement.jours_alerte',
        label: "Nombre de jours avant alerte d'échéance",
        valeur: '7',
        hint: 'Une alerte «Échéance proche» est créée à l’approche de la date.',
        type: 'number',
      },
      { cle: 'paiement.tva', label: 'Taux de TVA (%)', valeur: '20', type: 'number' },
    ],
  },
  {
    titre: 'Fichiers, session et affichage',
    sousTitre: 'Limites et confort d’utilisation.',
    champs: [
      {
        cle: 'fichiers.taille_max',
        label: 'Taille maximale des fichiers (Mo)',
        valeur: '10',
        type: 'number',
      },
      {
        cle: 'session.duree',
        label: 'Durée de session (minutes)',
        valeur: '60',
        type: 'number',
      },
      {
        cle: 'affichage.pagination',
        label: 'Pagination par défaut',
        valeur: '10',
        type: 'select',
        options: [
          { valeur: '8', label: '8 lignes par page' },
          { valeur: '10', label: '10 lignes par page' },
          { valeur: '20', label: '20 lignes par page' },
        ],
      },
    ],
  },
]
