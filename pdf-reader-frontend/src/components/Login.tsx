import { useState } from "react"
import { register, login, setSession } from "../api"

interface LoginProps {
  onSuccess: () => void
}

function Login({ onSuccess }: LoginProps) {
  const [mode, setMode] = useState<"login" | "register">("login")
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [email, setEmail] = useState("")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const auth = mode === "login"
        ? await login(username, password)
        : await register(username, password, email || undefined)
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
    setEmail("")
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={handleSubmit} noValidate>
        <div className="login-brand">
          <span className="login-brand-icon" aria-hidden="true">📄</span>
          <span className="login-brand-name">PDF Reader Notifier</span>
        </div>

        <h2 className="login-title">
          {mode === "login" ? "Giriş Yap" : "Hesap Oluştur"}
        </h2>

        <div className="login-field">
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
            minLength={3}
            maxLength={64}
          />
        </div>

        <div className="login-field">
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
            minLength={6}
            maxLength={128}
          />
        </div>

        {mode === "register" && (
          <div className="login-field">
            <label className="login-label" htmlFor="login-email">
              E-posta <span className="login-optional">(opsiyonel)</span>
            </label>
            <input
              id="login-email"
              className="login-input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="bildirim özetleri gönderilir"
              autoComplete="email"
              maxLength={255}
            />
          </div>
        )}

        {error && <p className="login-error" role="alert">⚠ {error}</p>}

        <button className="login-button" type="submit" disabled={busy}>
          {busy
            ? "Lütfen bekleyin..."
            : mode === "login" ? "Giriş Yap" : "Kayıt Ol"}
        </button>

        <button className="login-switch" type="button" onClick={switchMode}>
          {mode === "login"
            ? "Hesabın yok mu? Kayıt ol →"
            : "← Hesabın var mı? Giriş yap"}
        </button>
      </form>
    </div>
  )
}

export default Login
