export const API_BASE_URL = "http://localhost:8080";

export type NotificationStatus = "COMPLETED" | "IN_PROGRESS" | "DUE_DATE" | "CLOSED";

export interface Notification {
  id: string;
  title: string;
  description?: string | null;
  dueDate?: string | null; // "YYYY-MM-DD"
  createDate?: string | null; // "YYYY-MM-DD"
  status: NotificationStatus;
}

export async function uploadPdf(file: File): Promise<Notification[]> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/api/v1/notifications/upload`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
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

  return response.json();
}
