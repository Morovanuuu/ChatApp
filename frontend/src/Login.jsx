import { useState } from 'react'

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('')
  const [parola, setParola] = useState('')
  const [eroare, setEroare] = useState(null)
  const [seTrimite, setSeTrimite] = useState(false)

  async function trimite(e) {
    e.preventDefault()
    setEroare(null)
    setSeTrimite(true)
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, parola }),
      })
      if (!res.ok) {
        setEroare(res.status === 401 ? 'Email sau parolă greșită.' : 'Eroare de server. Încearcă din nou.')
        return
      }
      const { accessToken } = await res.json()
      onLogin(accessToken)
    } catch {
      setEroare('Serverul nu răspunde. Backend-ul este pornit?')
    } finally {
      setSeTrimite(false)
    }
  }

  return (
    <main className="card">
      <h1>ChatApp</h1>
      <p className="subtitlu">Intră în cont ca să începi o conversație.</p>

      <form onSubmit={trimite}>
        <label htmlFor="email">Email</label>
        <input id="email" type="email" autoComplete="username" required
               value={email} onChange={(e) => setEmail(e.target.value)} />

        <label htmlFor="parola">Parolă</label>
        <input id="parola" type="password" autoComplete="current-password" required
               value={parola} onChange={(e) => setParola(e.target.value)} />

        {eroare && <p className="eroare" role="alert">{eroare}</p>}

        <button type="submit" disabled={seTrimite}>
          {seTrimite ? 'Se verifică…' : 'Intră în cont'}
        </button>
      </form>
    </main>
  )
}
