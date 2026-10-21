import { cloneElement, isValidElement, useEffect, useId, useMemo, useState } from 'react'
import { saved } from './storage.js'

/** Always visible on every page. Kindred is a directory, not a crisis service. */
export function CrisisBar() {
  return (
    <div className="crisis-bar" role="region" aria-label="Crisis resources">
      <span>In crisis or thinking about suicide?</span>
      <a href="tel:988"><strong>Call or text 988</strong></a>
      <span className="dot" aria-hidden="true">·</span>
      <a href="sms:988">Text 988</a>
      <span className="dot" aria-hidden="true">·</span>
      <span>Emergency: <a href="tel:911"><strong>911</strong></a></span>
    </div>
  )
}

/**
 * Multi-select as toggleable chips. For long lists, shows a filter box and the most common
 * options first, with "show all" to expand.
 */
export function ChipPicker({ options, value, onChange, max, searchable = false, initialVisible = 12, label }) {
  const [query, setQuery] = useState('')
  const [expanded, setExpanded] = useState(false)
  const selected = useMemo(() => new Set(value), [value])

  const filtered = options.filter((o) => o.toLowerCase().includes(query.trim().toLowerCase()))
  // Selected options always stay visible even when the list is collapsed.
  const visible = expanded || query
    ? filtered
    : [...filtered.slice(0, initialVisible), ...filtered.slice(initialVisible).filter((o) => selected.has(o))]

  function toggle(option) {
    if (selected.has(option)) onChange(value.filter((v) => v !== option))
    else if (!max || value.length < max) onChange([...value, option])
  }

  return (
    <div className="chip-picker" role="group" aria-label={label}>
      {searchable && (
        <input className="chip-search" value={query} onChange={(e) => setQuery(e.target.value)}
               placeholder={`Search ${label?.toLowerCase() || 'options'}…`} aria-label={`Search ${label}`} />
      )}
      <div className="chips">
        {visible.map((o) => (
          <button type="button" key={o} className={`chip ${selected.has(o) ? 'chip-on' : ''}`}
                  aria-pressed={selected.has(o)} onClick={() => toggle(o)}>
            {selected.has(o) && <span aria-hidden="true">✓ </span>}{o}
          </button>
        ))}
        {!query && filtered.length > initialVisible && (
          <button type="button" className="chip chip-more" onClick={() => setExpanded((x) => !x)}>
            {expanded ? 'Show fewer' : `+${filtered.length - initialVisible} more`}
          </button>
        )}
      </div>
      {max && value.length >= max && <small className="muted">You can choose up to {max}.</small>}
    </div>
  )
}

/**
 * A labeled form field. When the child is a single input/select/textarea, the label is linked to
 * it (and hint/error text is announced with it), so screen readers read "Email, edit text".
 */
export function Field({ label, hint, error, children, className = '' }) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const isControl = isValidElement(children) && ['input', 'select', 'textarea'].includes(children.type)
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined

  const control = isControl
    ? cloneElement(children, { id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined })
    : children

  return (
    <div className={`field ${error ? 'field-error' : ''} ${className}`}>
      {isControl
        ? <label className="field-label" htmlFor={id}>{label}</label>
        : <span className="field-label">{label}</span>}
      {hint && <span className="field-hint" id={hintId}>{hint}</span>}
      {control}
      {error && <small className="field-message" id={errorId} role="alert">{error}</small>}
    </div>
  )
}

export function daysAgo(iso) {
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000)
  if (days <= 0) return 'today'
  if (days === 1) return 'yesterday'
  return `${days} days ago`
}

export function AvailabilityBadge({ therapist, freshness }) {
  if (!therapist.acceptingNewClients) {
    return <span className="badge badge-muted">Not accepting new clients</span>
  }
  if (freshness === 'STALE') {
    return <span className="badge badge-warn">Accepting? Not confirmed recently</span>
  }
  return (
    <span className="badge badge-ok" title={`Availability confirmed ${daysAgo(therapist.availabilityConfirmedAt)}`}>
      Accepting new clients · confirmed {daysAgo(therapist.availabilityConfirmedAt)}
    </span>
  )
}

export function SampleBadge({ show }) {
  if (!show) return null
  return (
    <span className="badge badge-sample" title="Fictional profile used for demonstration">
      Sample profile, not a real therapist
    </span>
  )
}

export function SaveButton({ id, compact = false }) {
  const [isSaved, setIsSaved] = useState(saved.has(id))
  useEffect(() => {
    const sync = () => setIsSaved(saved.has(id))
    window.addEventListener('kindred-saved', sync)
    return () => window.removeEventListener('kindred-saved', sync)
  }, [id])
  return (
    <button type="button" className={`save-btn ${isSaved ? 'save-on' : ''}`} aria-pressed={isSaved}
            onClick={() => saved.toggle(id)} title={isSaved ? 'Remove from saved' : 'Save to this device'}>
      <span aria-hidden="true">{isSaved ? '♥' : '♡'}</span>
      {!compact && (isSaved ? ' Saved' : ' Save')}
    </button>
  )
}

export function formatFee(t) {
  const parts = []
  if (t.feeMin != null) parts.push(t.feeMax != null && t.feeMax !== t.feeMin ? `$${t.feeMin}–$${t.feeMax}` : `$${t.feeMin}`)
  if (t.slidingScale) parts.push('sliding scale')
  return parts.length ? parts.join(' · ') : 'Fee not listed'
}

export function formatSessions(t) {
  const out = []
  if (t.offersInPerson) out.push(`In person · ${t.practiceCity}, ${t.practiceState}`)
  if (t.offersTelehealth) out.push(`Telehealth · ${t.statesLicensed.join(', ')}`)
  return out
}

export function Loading({ text = 'Loading…' }) {
  return <p className="muted loading">{text}</p>
}

export function ErrorBox({ error }) {
  if (!error) return null
  return <p className="alert alert-error" role="alert">{typeof error === 'string' ? error : error.message}</p>
}
