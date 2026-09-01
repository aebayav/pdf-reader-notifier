interface HeaderProps {
  onLogout: () => void
}

const Header = ({ onLogout }: HeaderProps) => {
  return (
    <div className="header-row">
      <h1 className="header-title">PDF okuma ve notify sistemi</h1>
      <button className="header-logout" type="button" onClick={onLogout}>
        Çıkış Yap
      </button>
    </div>
  )
}

export default Header
