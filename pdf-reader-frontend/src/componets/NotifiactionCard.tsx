type Status = "Completed" | "In progress" | "Closed" | "Due Date"

interface NotificationCard {
    id: string
    title: string
    description?: string
    dueDate?: Date
    createdDate?: Date
    status: Status
}

const NotificationCard = ({title, description, dueDate, status}: NotificationCard) => {
    const getStatusClass = (status: Status) => {
        switch(status) {
            case "Completed":
                return "completed"
            case "In progress":
                return "in-progress"
            case "Closed":
                return "closed"
            default:
                return ""
        }
    }

    const getCardStatusClass = (status: Status) => {
        switch(status) {
            case "Completed":
                return "notification-card status-completed"
            case "In progress":
                return "notification-card status-in-progress"
            case "Closed":
                return "notification-card status-closed"
            default:
                return "notification-card"
        }
    }

    return (
        <div className={getCardStatusClass(status)}>
            <div className="card-body">
                <h3 className="card-title">{title}</h3>
                {description && <p className="card-desciption">{description}</p>}
                {dueDate && <p className="card-due-date">Son Tarih: {dueDate?.toString()}</p>}
                <p className={`card-status ${getStatusClass(status)}`}>Durum: {status}</p>
                <div className="card-actions">
                    <button className="update-button">Güncelle</button>
                    <button className="delete-button">Sil</button>
                </div>
            </div>
        </div>
    )
}

export default NotificationCard