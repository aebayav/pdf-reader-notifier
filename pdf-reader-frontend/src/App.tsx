import { useEffect, useRef, useState } from "react"
import Header from "./components/Header"
import FileUploader from "./components/FileUploader"
import CardGallery from "./components/CardGallery"
import UpcomingBanner from "./components/UpcomingBanner"
import Login from "./components/Login"
import {
  uploadPdf,
  fetchJob,
  fetchNotifications,
  fetchUpcoming,
  updateNotification,
  deleteNotification,
  isLoggedIn,
  clearSession,
  Notification,
  ProcessingJob,
  UpdateNotificationPayload,
} from "./api"

const JOB_STATUS_LABELS: Record<string, string> = {
  QUEUED: "✓ Kabul edildi - sıraya alındı",
  PROCESSING: "⚙ İşleniyor...",
  COMPLETED: "✓ İşlem tamamlandı",
  FAILED: "✗ İşlem başarısız",
}

function App() {
  const [loggedIn, setLoggedIn] = useState(isLoggedIn())
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [upcoming, setUpcoming] = useState<Notification[]>([])
  const [loading, setLoading] = useState(false)
  const [job, setJob] = useState<ProcessingJob | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [useAi, setUseAi] = useState(true)
  const pollRef = useRef<number | null>(null)

  const handleLogout = () => {
    if (pollRef.current !== null) {
      window.clearInterval(pollRef.current)
      pollRef.current = null
    }
    clearSession()
    setLoggedIn(false)
    setNotifications([])
    setUpcoming([])
    setJob(null)
  }

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
    return () => {
      if (pollRef.current !== null) {
        window.clearInterval(pollRef.current)
      }
    }
  }, [])

  const handleUpload = async (file: File) => {
    setLoading(true)
    setError(null)
    try {
      const created = await uploadPdf(file, useAi)
      setJob(created)

      pollRef.current = window.setInterval(async () => {
        try {
          const current = await fetchJob(created.id)
          setJob(current)

          if (current.status === "COMPLETED" || current.status === "FAILED") {
            if (pollRef.current !== null) {
              window.clearInterval(pollRef.current)
              pollRef.current = null
            }
            setLoading(false)
            if (current.status === "COMPLETED") {
              await reload()
            } else {
              setError(current.errorMessage ?? "İşlem başarısız oldu.")
            }
          }
        } catch (err) {
          if (pollRef.current !== null) {
            window.clearInterval(pollRef.current)
            pollRef.current = null
          }
          setLoading(false)
          setError(err instanceof Error ? err.message : "Durum sorgulanamadı.")
        }
      }, 2500)
    } catch (err) {
      setLoading(false)
      setError(err instanceof Error ? err.message : "Bilinmeyen bir hata oluştu.")
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

  if (!loggedIn) {
    return (
      <>
        <Login onSuccess={() => setLoggedIn(true)} />
      </>
    )
  }

  return (
    <>
      <Header onLogout={handleLogout} />
      <FileUploader
        onUpload={handleUpload}
        loading={loading}
        useAi={useAi}
        onAiChange={setUseAi}
      />
      {job && (
        <p className={`job-status job-${job.status.toLowerCase()}`} role="status" aria-live="polite">
          {JOB_STATUS_LABELS[job.status] ?? job.status}
          {job.status === "COMPLETED" && ` - ${job.notificationCount} bildirim oluşturuldu`}
          {job.status === "FAILED" && job.errorMessage && `: ${job.errorMessage}`}
        </p>
      )}
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
