export const API_BASE_URL = "http://localhost:8080";

export type NotificationStatus = "COMPLETED" | "IN_PROGRESS" | "DUE_DATE" | "CLOSED";
export type JobStatus = "QUEUED" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface Notification {
  id: string;
  title: string;
  description?: string | null;
  dueDate?: string | null; // "YYYY-MM-DD"
  createDate?: string | null; // "YYYY-MM-DD"
  status: NotificationStatus;
}

export interface ProcessingJob {
  id: string;
  status: JobStatus;
  fileName: string;
  useAi: boolean;
  submittedAt?: string | null;
  completedAt?: string | null;
  notificationCount: number;
  errorMessage?: string | null;
}

export interface UpdateNotificationPayload {
  title?: string;
  description?: string;
  dueDate?: string; // "YYYY-MM-DD"
  status?: NotificationStatus;
}

async function parseError(response: Response): Promise<never> {
  let message = `Sunucu hatası (${response.status})`;
  try {
    const errorBody = await response.json();
    if (errorBody?.message) {
      message = errorBody.message;
    }
  } catch {
    // Yanıt JSON değilse varsayılan mesajı kullan
  }
  throw new Error(message);
}

export async function uploadPdf(file: File, useAi: boolean = false): Promise<ProcessingJob> {
  const formData = new FormData();
  formData.append("file", file);

  const endpoint = useAi
    ? `${API_BASE_URL}/api/v1/notifications/ai-upload`
    : `${API_BASE_URL}/api/v1/notifications/upload`;

  const response = await fetch(endpoint, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function fetchJob(id: string): Promise<ProcessingJob> {
  const response = await fetch(`${API_BASE_URL}/api/v1/notifications/jobs/${id}`);

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function fetchNotifications(): Promise<Notification[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/notifications`);

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function fetchUpcoming(days: number = 7): Promise<Notification[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/notifications/upcoming?days=${days}`);

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function updateNotification(
  id: string,
  payload: UpdateNotificationPayload
): Promise<Notification> {
  const response = await fetch(`${API_BASE_URL}/api/v1/notifications/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function deleteNotification(id: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/notifications/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    return parseError(response);
  }
}
