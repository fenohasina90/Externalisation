import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from '@/features/auth'
import { getConfigurationPublique } from '@/utils/affichage'
import '@/styles/index.css'

// Paramètres d'affichage publics : appliqués sans attendre (titre + devise),
// avec repli silencieux sur les valeurs par défaut si l'API est indisponible.
void getConfigurationPublique()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
