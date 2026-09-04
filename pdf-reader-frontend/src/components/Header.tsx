interface HeaderProps {
  onLogout: () => void
}

const Header = ({ onLogout }: HeaderProps) => {
  return (
    <header className="header-row">
      <div className="header-brand">
        <span className="header-icon" aria-hidden="true">📄</span>
        <h1 className="header-title">PDF Reader Notifier</h1>
      </div>
      <button className="header-logout" type="button" onClick={onLogout}>
        <span aria-hidden="true">→</span> Çıkış Yap
      </button>
    </header>
  )
}

export default Header
