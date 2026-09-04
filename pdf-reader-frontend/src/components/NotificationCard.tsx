import { useState, useEffect, useRef } from 'react'
import type { NotificationStatus, UpdateNotificationPayload } from '../api'

interface NotificationCardProps {
    id: string
    title: string
    description?: string
    dueDate?: string  // "YYYY-MM-DD"
    createDate?: string  // "YYYY-MM-DD"
    contractName?: string
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

const STATUS_ICONS: Record<NotificationStatus, string> = {
    COMPLETED: "✓",
    IN_PROGRESS: "⟳",
    DUE_DATE: "⚠",
    CLOSED: "✕",
}

const ALL_STATUSES: NotificationStatus[] = ["IN_PROGRESS", "DUE_DATE", "COMPLETED", "CLOSED"]

const NotificationCard = ({ id, title, description, dueDate, contractName, status, onUpdate, onDelete }: NotificationCardProps) => {
    const [editing, setEditing] = useState(false)
    const [confirmDelete, setConfirmDelete] = useState(false)
    const [draftTitle, setDraftTitle] = useState(title)
    const [draftDescription, setDraftDescription] = useState(description ?? "")
    const [draftDueDate, setDraftDueDate] = useState(dueDate ?? "")
    const [draftStatus, setDraftStatus] = useState<NotificationStatus>(status)
    const [saving, setSaving] = useState(false)
    const titleInputRef = useRef<HTMLInputElement>(null)

    // ESC ile düzenleme modundan çık
    useEffect(() => {
        if (!editing) return
        const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") cancelEdit() }
        window.addEventListener("keydown", onKey)
        return () => window.removeEventListener("keydown", onKey)
    }, [editing])

    // Düzenleme başlayınca başlık inputuna odaklan
    useEffect(() => {
        if (editing) titleInputRef.current?.focus()
    }, [editing])

    const startEdit = () => {
        setDraftTitle(title)
        setDraftDescription(description ?? "")
        setDraftDueDate(dueDate ?? "")
        setDraftStatus(status)
        setConfirmDelete(false)
        setEditing(true)
    }

    const cancelEdit = () => setEditing(false)

    const saveEdit = async () => {
        const trimmed = draftTitle.trim()
        if (!trimmed) return
        const payload: UpdateNotificationPayload = {
            title: trimmed,
            description: draftDescription.trim() || undefined,
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

    const handleDeleteConfirm = async () => {
        await onDelete(id)
        setConfirmDelete(false)
    }

    const formatDate = (isoDate?: string) => {
        if (!isoDate) return null
        const [year, month, day] = isoDate.split("-")
        return `${day}.${month}.${year}`
    }

    const getDaysLeft = (isoDate?: string): number | null => {
        if (!isoDate) return null
        const today = new Date()
        today.setHours(0, 0, 0, 0)
        const due = new Date(isoDate)
        due.setHours(0, 0, 0, 0)
        return Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
    }

    const daysLeft = getDaysLeft(dueDate)
    const isOverdue = daysLeft !== null && daysLeft < 0 && status !== "COMPLETED" && status !== "CLOSED"
    const isSoon = daysLeft !== null && daysLeft >= 0 && daysLeft <= 7 && status !== "COMPLETED" && status !== "CLOSED"

    const cardClass = [
        "notification-card",
        `status-${status.toLowerCase().replace("_", "-")}`,
        isOverdue ? "card-overdue" : "",
    ].filter(Boolean).join(" ")

    return (
        <div className={cardClass}>
            <div className="card-body">
                {editing ? (
                    <div className="card-edit">
                        <label className="edit-label" htmlFor={`title-${id}`}>Başlık</label>
                        <input
                            id={`title-${id}`}
                            ref={titleInputRef}
                            className="edit-input"
                            value={draftTitle}
                            onChange={(e) => setDraftTitle(e.target.value)}
                            maxLength={200}
                            placeholder="Bildirim başlığı"
                        />

                        <label className="edit-label" htmlFor={`desc-${id}`}>Açıklama</label>
                        <textarea
                            id={`desc-${id}`}
                            className="edit-input edit-textarea"
                            value={draftDescription}
                            onChange={(e) => setDraftDescription(e.target.value)}
                            maxLength={2000}
                            rows={3}
                            placeholder="Açıklama (opsiyonel)"
                        />

                        <label className="edit-label" htmlFor={`date-${id}`}>Son Tarih</label>
                        <input
                            id={`date-${id}`}
                            className="edit-input"
                            type="date"
                            value={draftDueDate}
                            onChange={(e) => setDraftDueDate(e.target.value)}
                        />

                        <label className="edit-label" htmlFor={`status-${id}`}>Durum</label>
                        <select
                            id={`status-${id}`}
                            className="edit-input"
                            value={draftStatus}
                            onChange={(e) => setDraftStatus(e.target.value as NotificationStatus)}
                        >
                            {ALL_STATUSES.map((s) => (
                                <option key={s} value={s}>{STATUS_ICONS[s]} {STATUS_LABELS[s]}</option>
                            ))}
                        </select>

                        <p className="edit-hint">ESC ile iptal</p>

                        <div className="card-actions">
                            <button className="btn-save" onClick={saveEdit} disabled={saving || !draftTitle.trim()}>
                                {saving ? "Kaydediliyor..." : "Kaydet"}
                            </button>
                            <button className="btn-cancel" onClick={cancelEdit} disabled={saving}>
                                İptal
                            </button>
                        </div>
                    </div>
                ) : (
                    <>
                        <div className="card-header-row">
                            <span className={`status-pill status-pill-${status.toLowerCase().replace("_", "-")}`}>
                                {STATUS_ICONS[status]} {STATUS_LABELS[status]}
                            </span>
                            {isOverdue && (
                                <span className="card-badge badge-overdue">
                                    ⏰ {daysLeft !== null ? -daysLeft : "?"} gün GECTİ
                                </span>
                            )}
                            {isSoon && !isOverdue && (
                                <span className="card-badge badge-soon">
                                    ⏳ {daysLeft === 0 ? "BUGÜN" : `${daysLeft} gün kaldı`}
                                </span>
                            )}
                        </div>

                        <h3 className="card-title">{title}</h3>

                        {contractName && (
                            <p className="card-contract-name">Sözleşme: <strong>{contractName}</strong></p>
                        )}

                        {description && (
                            <p className="card-description">{description}</p>
                        )}

                        {dueDate && (
                            <p className="card-due-date">
                                📅 Son Tarih: <strong>{formatDate(dueDate)}</strong>
                            </p>
                        )}

                        {confirmDelete ? (
                            <div className="delete-confirm">
                                <p className="delete-confirm-text">Silinsin mi?</p>
                                <div className="card-actions">
                                    <button className="btn-danger-confirm" onClick={handleDeleteConfirm}>
                                        Evet, Sil
                                    </button>
                                    <button className="btn-cancel" onClick={() => setConfirmDelete(false)}>
                                        Vazgeç
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div className="card-actions">
                                <button className="btn-edit" onClick={startEdit}>✏ Düzenle</button>
                                <button className="btn-delete" onClick={() => setConfirmDelete(true)}>🗑 Sil</button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    )
}

export default NotificationCard
