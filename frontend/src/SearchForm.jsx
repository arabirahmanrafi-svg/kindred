import { useEffect, useState } from 'react'
import { loadOptions } from './api.js'
import { ChipPicker, ErrorBox, Field, Loading } from './components.jsx'

// The most-requested options appear first; the full list is one tap away.
const COMMON_LANGUAGES = ['Spanish', 'Arabic', 'Bengali', 'Hindi', 'Urdu', 'Mandarin', 'Korean', 'Vietnamese', 'French', 'Farsi', 'Somali', 'Tagalog']

export const emptySearch = {
  state: '', format: 'any', languages: [], communities: [], specialties: [],
  insurance: '', maxFee: '', includeNotAccepting: false,
}

export function searchToParams(s, page = 0) {
  const p = new URLSearchParams()
  if (s.state) p.set('state', s.state)
  if (s.format && s.format !== 'any') p.set('format', s.format)
  s.languages.forEach((v) => p.append('languages', v))
  s.communities.forEach((v) => p.append('communities', v))
  s.specialties.forEach((v) => p.append('specialties', v))
  if (s.insurance) p.set('insurance', s.insurance)
  if (s.maxFee) p.set('maxFee', s.maxFee)
  if (s.includeNotAccepting) p.set('includeNotAccepting', 'true')
  if (page) p.set('page', String(page))
  return p
}

export function paramsToSearch(p) {
  return {
    state: p.get('state') || '',
    format: p.get('format') || 'any',
    languages: p.getAll('languages'),
    communities: p.getAll('communities'),
    specialties: p.getAll('specialties'),
    insurance: p.get('insurance') || '',
    maxFee: p.get('maxFee') || '',
    includeNotAccepting: p.get('includeNotAccepting') === 'true',
  }
}

/**
 * The search form, used on the home page (roomy) and the results page (compact sidebar).
 */
export default function SearchForm({ initial = emptySearch, onSubmit, compact = false, submitLabel = 'Find therapists' }) {
  const [options, setOptions] = useState(null)
  const [error, setError] = useState('')
  const [s, setS] = useState(initial)

  useEffect(() => { loadOptions().then(setOptions).catch(setError) }, [])
  useEffect(() => { setS(initial) }, [initial])

  if (error) return <ErrorBox error={error} />
  if (!options) return <Loading />

  const set = (key) => (value) => setS((prev) => ({ ...prev, [key]: value }))
  const languageOrder = [...COMMON_LANGUAGES, ...options.languages.filter((l) => !COMMON_LANGUAGES.includes(l))]
    .filter((l) => options.languages.includes(l))

  function submit(e) {
    e.preventDefault()
    onSubmit(s)
  }

  return (
    <form className={`search-form ${compact ? 'search-form-compact' : ''}`} onSubmit={submit}>
      <div className="search-row">
        <Field label="Where are you located?" hint="Therapists can only see you if they're licensed in your state.">
          <select value={s.state} onChange={(e) => set('state')(e.target.value)}>
            <option value="">Any state</option>
            {options.states.map((st) => <option key={st} value={st}>{st}</option>)}
          </select>
        </Field>
        <Field label="Session type">
          <div className="segmented" role="radiogroup" aria-label="Session type">
            {[['any', 'Either'], ['in_person', 'In person'], ['telehealth', 'Video / phone']].map(([v, label]) => (
              <button type="button" key={v} role="radio" aria-checked={s.format === v}
                      className={s.format === v ? 'on' : ''} onClick={() => set('format')(v)}>{label}</button>
            ))}
          </div>
        </Field>
      </div>

      <Field label="Language" hint="Sessions in the language you feel most yourself in.">
        <ChipPicker label="Languages" options={languageOrder} value={s.languages} onChange={set('languages')}
                    searchable={!compact} initialVisible={compact ? 6 : 12} />
      </Field>

      <Field label="Someone who understands…" hint="Communities the therapist has experience with.">
        <ChipPicker label="Communities" options={options.communities} value={s.communities} onChange={set('communities')}
                    initialVisible={compact ? 6 : 10} />
      </Field>

      <Field label="What would you like help with?" hint="Optional. Pick anything that fits, or nothing.">
        <ChipPicker label="Topics" options={options.specialties} value={s.specialties} onChange={set('specialties')}
                    initialVisible={compact ? 6 : 10} />
      </Field>

      <div className="search-row">
        <Field label="Insurance">
          <select value={s.insurance} onChange={(e) => set('insurance')(e.target.value)}>
            <option value="">Any / I'll pay myself</option>
            {options.insurances.map((i) => <option key={i} value={i}>{i}</option>)}
          </select>
        </Field>
        <Field label="Max per session (self-pay)">
          <select value={s.maxFee} onChange={(e) => set('maxFee')(e.target.value)}>
            <option value="">No limit</option>
            {[50, 80, 100, 130, 160, 200].map((f) => <option key={f} value={f}>Up to ${f}</option>)}
          </select>
        </Field>
      </div>

      <label className="checkbox">
        <input type="checkbox" checked={s.includeNotAccepting} onChange={(e) => set('includeNotAccepting')(e.target.checked)} />
        Also show therapists who are full right now
      </label>

      <button className="btn btn-primary btn-block">{submitLabel}</button>
      <p className="privacy-note">🔒 Private by design: no account needed, and your search isn't saved.</p>
    </form>
  )
}
