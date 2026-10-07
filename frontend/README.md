# Frontend — CABEX Gestion des services d'externalisation

Interface web de l'application de gestion et de suivi des services d'externalisation.

## Stack

- React 19 + TypeScript + Vite
- React Router (v7) pour la navigation
- Axios pour les appels à l'API REST (`http://localhost:8080/api`)

## Démarrage

```bash
npm install
npm run dev      # serveur de développement (http://localhost:5173)
npm run build    # compilation de type + build de production
npm run lint     # vérification ESLint
```

Le backend doit être démarré sur le port 8080 (CORS autorisé pour `localhost:5173`).
L'URL de l'API peut être surchargée via la variable `VITE_API_URL`.

## Architecture — modules autonomes, organisés en sous-dossiers

Chaque domaine fonctionnel vit dans `features/<module>/` et est **découpé par responsabilité** :

```
features/<module>/
├── components/   Composants d'interface propres au module
├── constants/    Libellés / valeurs figées du module (ex. labels.ts)
├── context/      Contexte React du module (ex. auth)
├── hooks/        Hooks de données du module (ex. useClientsList)
├── screens/      Écrans / pages du module
├── services/     Appels à l'API REST (<module>.service.ts)
├── types/        Types & DTO du module (index.ts)
└── index.ts      Barrel : surface publique (écrans, providers)
```

```
src/
├── api/http.ts          Client HTTP commun (axios) + intercepteurs JWT / 401
├── components/
│   ├── layout/          Coquille de l'application (sidebar, routes protégées)
│   ├── shared/          Composants transverses (PagePlaceholder)
│   └── ui/              Kit UI générique (Button, Input, Card, Badge, …)
├── config/env.ts        URL de l'API, clés de stockage
├── constants/           navigation (menu par rôle), libellés communs, messages
├── hooks/               useApi, usePaginatedList, useDebounce (génériques)
├── styles/              CSS (variables + composants)
├── types/common.ts      Types communs (ApiPage, ApiError, NomRole, PageQuery)
├── utils/               format, storage
├── features/
│   ├── auth/            context/, hooks/, screens/, services/, types/
│   ├── dashboard/       components/, hooks/, screens/, services/, types/
│   ├── clients/         components/, hooks/, screens/, services/, types/
│   ├── responsables/    screens/, services/, types/
│   ├── commerciaux/     screens/, services/, types/
│   ├── services/        screens/, services/, types
│   ├── demandes/        constants/, screens/, services/, types
│   ├── plan-paiement/   screens/, services/, types
│   ├── echeances/       constants/, screens/, services/, types
│   ├── paiements/       screens/, services/, types
│   ├── rapports/        screens/, services/, types
│   ├── recherche/       screens/, services/, types
│   ├── notifications/   screens/, services/, types
│   ├── documents/       screens/
│   └── administration/  constants/, screens/, services/, types
├── App.tsx              Déclaration des routes (importe les barrels de features)
└── main.tsx             Providers + Router
```

> Les sous-dossiers encore vides sont conservés via un `.gitkeep` : la structure est
> prête à accueillir `components/`, `hooks/`, etc. sans réorganisation ultérieure.

## Conventions

- **Imports** : alias `@/` pointant vers `src/`.
- **Un module = un dossier** `features/<module>/` ; à l'intérieur, **un dossier par
  responsabilité** (`services/`, `screens/`, `hooks/`, `components/`, `types/`, …).
- **Surface publique** : `features/<module>/index.ts` réexporte les écrans (et le provider pour
  `auth`) ; le routage (`App.tsx`, `main.tsx`) n'importe **que** ces barrels, jamais les
  chemins internes.
- **Types** : imports de type avec `import type { … }` ; aucun `enum` (types union à la place).
- **Hooks d'accès aux données** :
  - génériques — `useApi(loader, deps)`, `usePaginatedList({ loader, size })` ;
  - métier — un hook par module dans `features/<module>/hooks/` (ex. `useClientsList`).
- **Composants métier** : dans `features/<module>/components/` ; le kit réutilisable reste dans
  `components/ui/`.
- **Modules non encore développés** : leur `screens/` affiche le composant partagé
  `PagePlaceholder` décrivant les fonctionnalités prévues (aucune duplication de code).
- **Dossiers vides** : conservés via un fichier `.gitkeep`.

## Exemples

- Connexion (fonctionnelle) : `features/auth/screens/LoginPage.tsx` +
  `features/auth/components/{AuthLayout,PasswordField}` +
  `features/auth/context/AuthContext.tsx`, `features/auth/hooks/useAuth.ts`.
- Tableau de bord (fonctionnel) : `features/dashboard/screens/DashboardPage.tsx` +
  `features/dashboard/{hooks,components}/`.
- Liste paginée (fonctionnelle) : `features/clients/screens/ClientsPage.tsx` (pattern de
  référence `hooks/useClientsList` + `services/clients.service.ts` + `components/ClientsTable`).