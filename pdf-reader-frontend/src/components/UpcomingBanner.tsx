import type { Notification } from '../api'

interface UpcomingBannerProps {
  upcoming: Notification[]
}

const daysUntil = (dueDate: string): number => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const due = new Date(dueDate)
  due.setHours(0, 0, 0, 0)
  return Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

const UpcomingBanner = ({ upcoming }: UpcomingBannerProps) => {
  if (upcoming.length === 0) {
    return null
  }

  const overdue = upcoming.filter((n) => n.dueDate && daysUntil(n.dueDate) < 0)
  const soon = upcoming.filter((n) => n.dueDate && daysUntil(n.dueDate) >= 0)

  const parts: string[] = []
  if (soon.length > 0) parts.push(`${soon.length} bildirim yaklaşıyor`)
  if (overdue.length > 0) parts.push(`${overdue.length} gecikmiş`)

  return (
    <div className={`upcoming-banner${overdue.length > 0 ? " has-overdue" : ""}`} role="alert">
      <strong>⚠ {parts.join(", ")}</strong>
      <ul>
        {upcoming.slice(0, 5).map((n) => (
          <li key={n.id}>
            {n.dueDate && daysUntil(n.dueDate) < 0
              ? `${-daysUntil(n.dueDate)} gün GECTİ`
              : n.dueDate && daysUntil(n.dueDate) === 0
                ? "BUGÜN"
                : `${daysUntil(n.dueDate!)} gün kaldı`}
            {" — "}
            {n.title}
          </li>
        ))}
        {upcoming.length > 5 && <li>... ve {upcoming.length - 5} daha</li>}
      </ul>
    </div>
  )
}

export default UpcomingBanner
