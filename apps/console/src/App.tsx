import CssBaseline from '@mui/material/CssBaseline'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import { QueryClientProvider } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import { Navigate, Route, Routes } from 'react-router-dom'
import { JobsPage } from './pages/JobsPage'

const theme = createTheme({ colorSchemes: { dark: true } })

export interface AppProps {
  queryClient: QueryClient
}

/**
 * One route only. The router exists so later units can add routes without
 * reshaping the shell.
 */
export function App({ queryClient }: AppProps) {
  return (
    <ThemeProvider theme={theme} defaultMode="system">
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <Routes>
          <Route path="/jobs" element={<JobsPage />} />
          <Route path="*" element={<Navigate to="/jobs" replace />} />
        </Routes>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
