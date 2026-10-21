import { lazy, Suspense } from 'react'
import { Link, NavLink, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import { CrisisBar, Loading } from './components.jsx'
import HomePage from './pages/HomePage.jsx'
import SearchPage from './pages/SearchPage.jsx'
import TherapistPage from './pages/TherapistPage.jsx'
import SavedPage from './pages/SavedPage.jsx'

// Therapist and admin screens load only when needed; people searching never download them.
const ForTherapistsPage = lazy(() => import('./pages/ForTherapistsPage.jsx'))
const DashboardPage = lazy(() => import('./pages/DashboardPage.jsx'))
const AdminPage = lazy(() => import('./pages/AdminPage.jsx'))

export default function App() {
  const { me } = useAuth()

  return (
    <div className="app">
      <CrisisBar />
      <header className="topbar">
        <Link to="/" className="brand" aria-label="Kindred home">
          <svg width="28" height="28" viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="9" fill="var(--primary)" />
            <circle cx="12" cy="16" r="6" fill="none" stroke="var(--accent)" strokeWidth="2.5" />
            <circle cx="20" cy="16" r="6" fill="none" stroke="var(--accent)" strokeWidth="2.5" />
          </svg>
          Kindred
        </Link>
        <nav>
          <NavLink to="/" end>Find a therapist</NavLink>
          <NavLink to="/saved">Saved</NavLink>
          {me?.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
          {me?.role === 'THERAPIST' && <NavLink to="/dashboard">My listing</NavLink>}
          {!me && <NavLink to="/for-therapists">For therapists</NavLink>}
        </nav>
      </header>

      <main className="content">
        <Suspense fallback={<Loading />}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/therapists/:id" element={<TherapistPage />} />
            <Route path="/saved" element={<SavedPage />} />
            <Route path="/for-therapists" element={<ForTherapistsPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/admin" element={<AdminPage />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </main>

      <footer className="footer">
        <p>
          Kindred is a directory, not a healthcare provider or crisis service. It doesn't give medical advice.
          If you are in danger, call 911. For emotional crisis support, call or text 988 (US).
        </p>
        <p className="muted">Profiles marked "Sample" are fictional and used for demonstration.</p>
      </footer>
    </div>
  )
}

function NotFound() {
  return (
    <div className="narrow center">
      <h1>Page not found</h1>
      <Link to="/" className="btn btn-primary">Back to search</Link>
    </div>
  )
}
