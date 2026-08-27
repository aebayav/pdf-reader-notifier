import { useState } from 'react'
import type { NotificationStatus, UpdateNotificationPayload } from '../api'

interface NotificationCardProps {
    id: string
    title: string
    description?: string
    dueDate?: string  // "YYYY-MM-DD"
    createDate?: string  // "YYYY-MM-DD"
    status: NotificationStatus
    onUpdate: (id: string, payload: UpdateNotificationPayload) => void | Promise<void>
    onDelete: (id: string) => void | Promise<void>
}

const STATUS_LABELS: Record<NotificationStatus, string> = {
    COMPLETED: "Tamamlandı",
    IN_PROGRESS: "Devam Ediyor",
    DUE_DATE: "Son Tarih",
    CLOSED: "Kapatıldı",
}

const ALL_STATUSES: NotificationStatus[] = ["IN_PROGRESS", "DUE_DATE", "COMPLETED", "CLOSED"]

const NotificationCard = ({ id, title, description, dueDate, status, onUpdate, onDelete }: NotificationCardProps) => {
    const [editing, setEditing] = useState(false)
    const [draftTitle, setDraftTitle] = useState(title)
    const [draftDueDate, setDraftDueDate] = useState(dueDate ?? "")
    const [draftStatus, setDraftStatus] = useState<NotificationStatus>(status)
    const [saving, setSaving] = useState(false)

    const getStatusClass = (status: NotificationStatus) => {
        switch(status) {
            case "COMPLETED":
                return "completed"
            case "IN_PROGRESS":
                return "in-progress"
            case "CLOSED":
                return "closed"
            default:
                return ""
        }
    }

    const getCardStatusClass = (status: NotificationStatus) => {
        switch(status) {
            case "COMPLETED":
                return "notification-card status-completed"
            case "IN_PROGRESS":
                return "notification-card status-in-progress"
            case "CLOSED":
                return "notification-card status-closed"
            default:
                return "notification-card"
        }
    }

    const formatDate = (isoDate?: string) => {
        if (!isoDate) {
            return null
        }
        // "YYYY-MM-DD" -> "GG.AA.YYYY"
        const [year, month, day] = isoDate.split("-")
        return `${day}.${month}.${year}`
    }

    const startEdit = () => {
        setDraftTitle(title)
        setDraftDueDate(dueDate ?? "")
        setDraftStatus(status)
        setEditing(true)
    }

    const saveEdit = async () => {
        const payload: UpdateNotificationPayload = {
            title: draftTitle.trim() || undefined,
            dueDate: draftDueDate || undefined,
            status: draftStatus,
        }
        setSaving(true)
        try {
            await onUpdate(id, payload)
            setEditing(false)
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async () => {
        if (!window.confirm(`"${title}" bildirimi silinsin mi?`)) {
            return
        }
        await onDelete(id)
    }

    return (
        <div className={getCardStatusClass(status)}>
            <div className="card-body">
                {editing ? (
                    <div className="card-edit">
                        <input
                            className="edit-input"
                            value={draftTitle}
                            onChange={(e) => setDraftTitle(e.target.value)}
                            aria-label="Başlık"
                        />
                        <input
                            className="edit-input"
                            type="date"
                            value={draftDueDate}
                            onChange={(e) => setDraftDueDate(e.target.value)}
                            aria-label="Son tarih"
                        />
                        <select
                            className="edit-input"
                            value={draftStatus}
                            onChange={(e) => setDraftStatus(e.target.value as NotificationStatus)}
                            aria-label="Durum"
                        >
                            {ALL_STATUSES.map((s) => (
                                <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                            ))}
                        </select>
                        <div className="card-actions">
                            <button className="update-button" onClick={saveEdit} disabled={saving}>
                                {saving ? "Kaydediliyor..." : "Kaydet"}
                            </button>
                            <button className="delete-button" onClick={() => setEditing(false)} disabled={saving}>
                                İptal
                            </button>
                        </div>
                    </div>
                ) : (
                    <>
                        <h3 className="card-title">{title}</h3>
                        {description && <p className="card-desciption">{description}</p>}
                        {dueDate && <p className="card-due-date">Son Tarih: {formatDate(dueDate)}</p>}
                        <p className={`card-status ${getStatusClass(status)}`}>Durum: {STATUS_LABELS[status] ?? status}</p>
                        <div className="card-actions">
                            <button className="update-button" onClick={startEdit}>Güncelle</button>
                            <button className="delete-button" onClick={handleDelete}>Sil</button>
                        </div>
                    </>
                )}
            </div>
        </div>
    )
}

export default NotificationCard
