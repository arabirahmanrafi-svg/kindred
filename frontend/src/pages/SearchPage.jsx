import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import SearchForm, { paramsToSearch, searchToParams } from '../SearchForm.jsx'
import { AvailabilityBadge, ErrorBox, formatFee, Loading, SampleBadge, SaveButton } from '../components.jsx'

export default function SearchPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const key = params.toString()
  const criteria = useMemo(() => paramsToSearch(new URLSearchParams(key)), [key])
  const page = Number(params.get('page') || 0)

  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [filtersOpen, setFiltersOpen] = useState(false)

  useEffect(() => {
    setData(null)
    setError('')
    api.search(new URLSearchParams(key)).then(setData).catch(setError)
  }, [key])

  const go = (s, p = 0) => {
    setFiltersOpen(false)
    navigate(`/search?${searchToParams(s, p)}`)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="search-layout">
      <aside className={`filters ${filtersOpen ? 'filters-open' : ''}`}>
        <div className="filters-head">
          <h2>Refine</h2>
          <button className="btn btn-ghost filters-close" onClick={() => setFiltersOpen(false)}>Close</button>
        </div>
        <SearchForm compact initial={criteria} onSubmit={(s) => go(s)} submitLabel="Update results" />
      </aside>

      <section className="results">
        <div className="results-head">
          <div>
            <h1>{data ? `${data.total} ${data.total === 1 ? 'therapist' : 'therapists'}` : 'Searching…'}</h1>
            <p className="muted">{summary(criteria)}</p>
          </div>
          <button className="btn filters-toggle" onClick={() => setFiltersOpen(true)}>Filters</button>
        </div>

        <ErrorBox error={error} />
        {!data && !error && <Loading text="Finding therapists who fit…" />}

        {data && data.total === 0 && <NoResults criteria={criteria} onBroaden={go} />}

        {data && data.results.map((r) => <ResultCard key={r.therapist.id} result={r} />)}

        {data && data.totalPages > 1 && (
          <nav className="pager" aria-label="Pages">
            <button className="btn" disabled={page === 0} onClick={() => go(criteria, page - 1)}>← Previous</button>
            <span className="muted">Page {page + 1} of {data.totalPages}</span>
            <button className="btn" disabled={page + 1 >= data.totalPages} onClick={() => go(criteria, page + 1)}>Next →</button>
          </nav>
        )}
      </section>
    </div>
  )
}

function ResultCard({ result }) {
  const { therapist: t, match } = result
  return (
    <article className="result-card">
      <div className="result-top">
        <div>
          <h2><Link to={`/therapists/${t.id}`}>{t.fullName}</Link>, <span className="creds">{t.credentials}</span></h2>
          <p className="muted small">{t.licenseType} · licensed in {t.statesLicensed.join(', ')}</p>
        </div>
        <SaveButton id={t.id} compact />
      </div>

      <div className="badges">
        <SampleBadge show={t.demo} />
        <AvailabilityBadge therapist={t} freshness={match.freshness} />
        {match.requestedPreferences > 0 && (
          <span className="badge badge-match">
            Matches {match.matchedPreferences} of {match.requestedPreferences} preferences
          </span>
        )}
      </div>

      <ul className="reasons">
        {match.reasons.map((r) => <li key={r}><span aria-hidden="true">✓</span> {r}</li>)}
      </ul>
      {match.missing.length > 0 && (
        <p className="missing">Doesn't list: {match.missing.map((m) => m.replace(/^\w+: /, '')).join(', ')}</p>
      )}

      {t.bio && <p className="bio-snippet">{t.bio.length > 200 ? `${t.bio.slice(0, 200)}…` : t.bio}</p>}

      <div className="result-foot">
        <span className="muted small">{formatFee(t)}{t.insurances.length ? ` · ${t.insurances.length} insurance plans` : ''}</span>
        <Link className="btn btn-primary" to={`/therapists/${t.id}`}>View profile</Link>
      </div>
    </article>
  )
}

function NoResults({ criteria, onBroaden }) {
  const suggestions = []
  if (criteria.format === 'in_person') suggestions.push(['Include video/phone sessions', { ...criteria, format: 'any' }])
  if (criteria.insurance) suggestions.push(['Remove the insurance filter', { ...criteria, insurance: '' }])
  if (criteria.maxFee) suggestions.push(['Remove the budget limit', { ...criteria, maxFee: '' }])
  if (!criteria.includeNotAccepting) suggestions.push(['Include therapists who are full (some keep waitlists)', { ...criteria, includeNotAccepting: true }])

  return (
    <div className="card empty">
      <h2>No exact matches yet</h2>
      <p className="muted">
        That's not a dead end. Language and community preferences only change the order of results,
        so try loosening one of these:
      </p>
      <div className="suggestions">
        {suggestions.map(([label, next]) => (
          <button key={label} className="btn" onClick={() => onBroaden(next)}>{label}</button>
        ))}
      </div>
      <p className="muted small">
        Students can often get free, confidential counseling through their college's counseling center.
      </p>
    </div>
  )
}

function summary(c) {
  const bits = []
  if (c.state) bits.push(`in ${c.state}`)
  if (c.format === 'in_person') bits.push('in person')
  if (c.format === 'telehealth') bits.push('video/phone')
  if (c.languages.length) bits.push(`speaks ${c.languages.join(' or ')}`)
  if (c.insurance) bits.push(`takes ${c.insurance}`)
  return bits.length ? `Best matches first · ${bits.join(' · ')}` : 'Best matches first'
}
