import { useState } from "react"
import Header from "./components/Header"
import FileUploader from "./components/FileUploader"
import CardGallery from "./components/CardGallery"
import { uploadPdf, Notification } from "./api"

function App() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleUpload = async (file: File) => {
    setLoading(true)
    setError(null)
    try {
      const result = await uploadPdf(file)
      setNotifications(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bilinmeyen bir hata oluştu.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <Header />
      <FileUploader onUpload={handleUpload} loading={loading} />
      {error && <p className="upload-error">⚠ {error}</p>}
      <CardGallery notifications={notifications} />
    </>
  )
}

export default App
