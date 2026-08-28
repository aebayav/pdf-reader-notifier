import { useEffect, useState } from "react"
import Header from "./components/Header"
import FileUploader from "./components/FileUploader"
import CardGallery from "./components/CardGallery"
import UpcomingBanner from "./components/UpcomingBanner"
import {
  uploadPdf,
  fetchNotifications,
  fetchUpcoming,
  updateNotification,
  deleteNotification,
  Notification,
  UpdateNotificationPayload,
} from "./api"

function App() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [upcoming, setUpcoming] = useState<Notification[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [useAi, setUseAi] = useState(true)

  const reload = async () => {
    try {
      const [all, near] = await Promise.all([
        fetchNotifications(),
        fetchUpcoming(7),
      ])
      setNotifications(all)
      setUpcoming(near)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bilinmeyen bir hata oluştu.")
    }
  }

  useEffect(() => {
    reload()
  }, [])

  const handleUpload = async (file: File) => {
    setLoading(true)
    setError(null)
    try {
      await uploadPdf(file, useAi)
      await reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bilinmeyen bir hata oluştu.")
    } finally {
      setLoading(false)
    }
  }

  const handleUpdate = async (id: string, payload: UpdateNotificationPayload) => {
    setError(null)
    try {
      const updated = await updateNotification(id, payload)
      setNotifications((current) =>
        current.map((n) => (n.id === id ? updated : n))
      )
      await reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bilinmeyen bir hata oluştu.")
    }
  }

  const handleDelete = async (id: string) => {
    setError(null)
    try {
      await deleteNotification(id)
      setNotifications((current) => current.filter((n) => n.id !== id))
      setUpcoming((current) => current.filter((n) => n.id !== id))
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bilinmeyen bir hata oluştu.")
    }
  }

  return (
    <>
      <Header />
      <FileUploader
        onUpload={handleUpload}
        loading={loading}
        useAi={useAi}
        onAiChange={setUseAi}
      />
      {error && <p className="upload-error">⚠ {error}</p>}
      <UpcomingBanner upcoming={upcoming} />
      <CardGallery
        notifications={notifications}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
      />
    </>
  )
}

export default App
