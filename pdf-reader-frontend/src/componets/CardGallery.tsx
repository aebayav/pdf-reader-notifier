import NotificationCard from './NotifiactionCard'

type Status = "Completed" | "In progress" | "Closed" | "Due Date"

interface NotificationCardData {
  id: string
  title: string
  description?: string
  dueDate?: Date
  createdDate?: Date
  status: Status
}

const mockCardData: NotificationCardData[] = [
  {
    id: '1',
    title: 'Q1 Report Review',
    description: 'Review and finalize the quarterly business report',
    dueDate: new Date('2026-04-20'),
    createdDate: new Date('2026-04-10'),
    status: 'In progress',
  },
  {
    id: '2',
    title: 'Project Proposal',
    description: 'Submit new project proposal to stakeholders',
    dueDate: new Date('2026-04-25'),
    createdDate: new Date('2026-04-08'),
    status: 'Completed',
  },
  {
    id: '3',
    title: 'Team Meeting Notes',
    description: 'Document and distribute meeting notes from today',
    dueDate: new Date('2026-04-15'),
    createdDate: new Date('2026-04-14'),
    status: 'Due Date',
  },
  {
    id: '4',
    title: 'Budget Approval',
    description: 'Wait for budget approval from management',
    dueDate: new Date('2026-04-30'),
    createdDate: new Date('2026-04-05'),
    status: 'In progress',
  },
  {
    id: '5',
    title: 'Old Task',
    description: 'This task has been closed',
    dueDate: new Date('2026-03-01'),
    createdDate: new Date('2026-02-15'),
    status: 'Closed',
  },
]

const CardGallery = () => {
  return (
    <div className="card-gallery">
      {mockCardData.map((card) => (
        <NotificationCard
          id={card.id}
          title={card.title}
          description={card.description}
          dueDate={card.dueDate}
          status={card.status}
        />
      ))}
    </div>
  )
}

export default CardGallery
