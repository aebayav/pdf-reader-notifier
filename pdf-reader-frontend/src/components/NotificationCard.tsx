import type { NotificationStatus } from '../api'

interface NotificationCardProps {
    id: string
    title: string
    description?: string
    dueDate?: string  // "YYYY-MM-DD"
    createDate?: string  // "YYYY-MM-DD"
    status: NotificationStatus
}

const STATUS_LABELS: Record<NotificationStatus, string> = {
    COMPLETED: "Tamamlandı",
    IN_PROGRESS: "Devam Ediyor",
    DUE_DATE: "Son Tarih",
    CLOSED: "Kapatıldı",
}

const NotificationCard = ({title, description, dueDate, status}: NotificationCardProps) => {
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

    return (
        <div className={getCardStatusClass(status)}>
            <div className="card-body">
                <h3 className="card-title">{title}</h3>
                {description && <p className="card-desciption">{description}</p>}
                {dueDate && <p className="card-due-date">Son Tarih: {formatDate(dueDate)}</p>}
                <p className={`card-status ${getStatusClass(status)}`}>Durum: {STATUS_LABELS[status] ?? status}</p>
                <div className="card-actions">
                    <button className="update-button" title="Bu özellik yakında eklenecek">Güncelle</button>
                    <button className="delete-button" title="Bu özellik yakında eklenecek">Sil</button>
                </div>
            </div>
        </div>
    )
}

export default NotificationCard
