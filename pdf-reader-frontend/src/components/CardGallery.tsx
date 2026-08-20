import NotificationCard from './NotificationCard'
import type { Notification } from '../api'

interface CardGalleryProps {
  notifications: Notification[]
}

const CardGallery = ({ notifications }: CardGalleryProps) => {
  if (notifications.length === 0) {
    return (
      <p className="gallery-empty">
        Henüz bildirim yok. Analiz etmek için bir PDF yükleyin.
      </p>
    )
  }

  return (
    <div className="card-gallery">
      {notifications.map((notification) => (
        <NotificationCard
          key={notification.id}
          id={notification.id}
          title={notification.title}
          description={notification.description ?? undefined}
          dueDate={notification.dueDate ?? undefined}
          createDate={notification.createDate ?? undefined}
          status={notification.status}
        />
      ))}
    </div>
  )
}

export default CardGallery
