import NotificationCard from './NotificationCard'
import type { Notification, UpdateNotificationPayload } from '../api'

interface CardGalleryProps {
  notifications: Notification[]
  groups: { id: string; name: string }[]
  onUpdate: (id: string, payload: UpdateNotificationPayload) => void | Promise<void>
  onDelete: (id: string) => void | Promise<void>
}

const CardGallery = ({ notifications, groups, onUpdate, onDelete }: CardGalleryProps) => {
  if (notifications.length === 0) {
    return (
      <p className="gallery-empty">
        Henüz bildirim yok. Analiz etmek için bir PDF yükleyin.
      </p>
    )
  }

  const sections = [
    ...groups.map((group) => ({ id: group.id, name: group.name, items: notifications.filter((n) => n.groupId === group.id) })),
    { id: "", name: "Gruplanmamış", items: notifications.filter((n) => !n.groupId) },
  ].filter((section) => section.items.length > 0)

  return (
    <div className="group-sections">
      {sections.map((section) => <section className="notification-group" key={section.id || "ungrouped"}>
        <h2>{section.name} <span>{section.items.length}</span></h2>
        <div className="card-gallery">
        {section.items.map((notification) => (
        <NotificationCard
          key={notification.id}
          id={notification.id}
          title={notification.title}
          description={notification.description ?? undefined}
          dueDate={notification.dueDate ?? undefined}
          createDate={notification.createDate ?? undefined}
          contractName={notification.contractName ?? undefined}
          status={notification.status}
          onUpdate={onUpdate}
          onDelete={onDelete}
        />
      ))}
        </div>
      </section>)}
    </div>
  )
}

export default CardGallery
