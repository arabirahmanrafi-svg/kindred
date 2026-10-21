// Renders the real pages in a simulated browser with a fake backend, and clicks through them
// like a person would. Run with: npm test

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from './App.jsx'
import { AuthProvider } from './auth.jsx'

const OPTIONS = {
  languages: ['English', 'Spanish', 'Arabic', 'Bengali', 'Hindi'],
  communities: ['South Asian', 'International students', 'Muslim'],
  specialties: ['Anxiety', 'Depression', 'Academic pressure'],
  insurances: ['Aetna', 'Blue Cross Blue Shield'],
  states: ['MI', 'OH'],
}

const NADIA = {
  id: 1, fullName: 'Nadia Chowdhury', credentials: 'PhD, LP', licenseType: 'Licensed Psychologist',
  licenseNumber: 'SAMPLE-1001', licenseState: 'MI', bio: 'I work with South Asian families.',
  languages: ['Bengali', 'English', 'Hindi'], communities: ['South Asian'], specialties: ['Anxiety'],
  insurances: ['Aetna'], statesLicensed: ['MI', 'OH'], offersInPerson: true, offersTelehealth: true,
  practiceCity: 'Ann Arbor', practiceState: 'MI', feeMin: 120, feeMax: 180, slidingScale: false,
  acceptingNewClients: true, availabilityConfirmedAt: new Date(Date.now() - 5 * 86400000).toISOString(),
  contactEmail: 'sample01@example.org', phone: '(555) 010-0001', websiteUrl: null, demo: true,
  verifiedAt: new Date().toISOString(),
}

let requests
let searchResults

function json(body, status = 200) {
  return Promise.resolve(new Response(body === null ? null : JSON.stringify(body), {
    status, headers: { 'Content-Type': 'application/json' },
  }))
}

beforeEach(() => {
  requests = []
  localStorage.clear()
  searchResults = [{
    therapist: NADIA,
    match: { score: 65, matchedPreferences: 1, requestedPreferences: 1, reasons: ['Speaks Bengali'], missing: [], freshness: 'FRESH' },
  }]
  vi.stubGlobal('fetch', vi.fn((url, init = {}) => {
    const u = new URL(url, 'http://localhost')
    requests.push({ path: u.pathname, search: u.search, method: init.method || 'GET', body: init.body, headers: init.headers })
    if (u.pathname === '/api/options') return json(OPTIONS)
    if (u.pathname === '/api/search') {
      return json({ results: searchResults, total: searchResults.length, page: 0, pageSize: 10, totalPages: 1 })
    }
    if (u.pathname === '/api/therapists/1') return json(NADIA)
    if (u.pathname === '/api/auth/register') return json({ token: 'tok-123', me: { email: 'new@example.org', role: 'THERAPIST', hasProfile: false } }, 201)
    if (u.pathname === '/api/auth/me') return json({ detail: 'Please log in' }, 401)
    if (u.pathname === '/api/me/profile') return json({ detail: "You haven't created your profile yet" }, 404)
    return json({ detail: 'not mocked' }, 404)
  }))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider><App /></AuthProvider>
    </MemoryRouter>,
  )
}

describe('Kindred', () => {
  it('always shows crisis resources', async () => {
    renderAt('/')
    const bar = screen.getByRole('region', { name: /crisis resources/i })
    expect(within(bar).getByText(/call or text 988/i).closest('a').getAttribute('href')).toBe('tel:988')
  })

  it('searches by language and explains each match', async () => {
    const user = userEvent.setup()
    renderAt('/')

    await user.click(await screen.findByRole('button', { name: 'Bengali' }))
    await user.click(screen.getByRole('button', { name: 'Find therapists' }))

    expect(await screen.findByText('Speaks Bengali')).toBeTruthy()
    expect(screen.getByText(/sample profile, not a real therapist/i)).toBeTruthy()
    expect(screen.getByText(/accepting new clients · confirmed 5 days ago/i)).toBeTruthy()
    const searchCall = requests.find((r) => r.path === '/api/search')
    expect(searchCall.search).toContain('languages=Bengali')
  })

  it('offers ways to broaden a search with no results', async () => {
    searchResults = []
    renderAt('/search?format=in_person&insurance=Aetna')
    expect(await screen.findByText(/no exact matches yet/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /include video\/phone sessions/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /remove the insurance filter/i })).toBeTruthy()
  })

  it('shows the verified license and drafts a first message without sending anything', async () => {
    const user = userEvent.setup()
    renderAt('/therapists/1')

    expect(await screen.findByRole('heading', { name: /nadia chowdhury/i })).toBeTruthy()
    expect(screen.getByText(/license verified/i)).toBeTruthy()

    await user.selectOptions(screen.getByLabelText('Insurance'), 'Aetna')
    await user.selectOptions(screen.getByLabelText('Language'), 'Bengali')
    const draft = screen.getByLabelText('Draft message').value
    expect(draft).toContain('Do you accept Aetna?')
    expect(draft).toContain('sessions in Bengali')

    // Drafting must not call the server
    expect(requests.filter((r) => r.method !== 'GET')).toHaveLength(0)
  })

  it('saves a therapist on this device only', async () => {
    const user = userEvent.setup()
    renderAt('/therapists/1')
    await screen.findByRole('heading', { name: /nadia chowdhury/i })

    await user.click(screen.getByRole('button', { name: /save$/i }))
    expect(JSON.parse(localStorage.getItem('kindred.saved'))).toEqual([1])
    expect(requests.filter((r) => r.method !== 'GET')).toHaveLength(0)
  })

  it('sends logged-out therapists to sign up, then to an empty listing editor', async () => {
    const user = userEvent.setup()
    renderAt('/dashboard')

    expect(await screen.findByRole('heading', { name: /be found by the people/i })).toBeTruthy()
    await user.type(screen.getByLabelText('Email'), 'new@example.org')
    await user.type(screen.getByLabelText('Password'), 'a-long-password')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('heading', { name: /create your listing/i })).toBeTruthy()
    expect(localStorage.getItem('kindred.token')).toBe('tok-123')
    const profileCall = requests.find((r) => r.path === '/api/me/profile')
    expect(profileCall.headers.Authorization).toBe('Bearer tok-123')
  })
})
