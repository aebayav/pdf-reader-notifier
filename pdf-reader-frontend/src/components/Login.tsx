import { useState } from "react"
import { register, login, setSession } from "../api"

interface LoginProps {
  onSuccess: () => void
}

function Login({ onSuccess }: LoginProps) {
  const [mode, setMode] = useState<"login" | "register">("login")
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const auth = mode === "login"
        ? await login(username, password)
        : await register(username, password)
      setSession(auth.token)
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bilinmeyen bir hata oluştu.")
    } finally {
      setBusy(false)
    }
  }

  const switchMode = () => {
    setMode(mode === "login" ? "register" : "login")
    setError(null)
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={handleSubmit}>
        <h2 className="login-title">
          {mode === "login" ? "Giriş Yap" : "Kayıt Ol"}
        </h2>
        <p className="login-subtitle">PDF Reader Notifier</p>

        <label className="login-label" htmlFor="login-username">Kullanıcı Adı</label>
        <input
          id="login-username"
          className="login-input"
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="en az 3 karakter"
          autoComplete="username"
          required
        />

        <label className="login-label" htmlFor="login-password">Parola</label>
        <input
          id="login-password"
          className="login-input"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="en az 6 karakter"
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          required
        />

        {error && <p className="login-error">⚠ {error}</p>}

        <button className="login-button" type="submit" disabled={busy}>
          {busy ? "Lütfen bekleyin..." : mode === "login" ? "Giriş" : "Kayıt Ol"}
        </button>

        <button className="login-switch" type="button" onClick={switchMode}>
          {mode === "login"
            ? "Hesabın yok mu? Kayıt ol"
            : "Hesabın var mı? Giriş yap"}
        </button>
      </form>
    </div>
  )
}

export default Login
