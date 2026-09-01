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
  dueDate?: string;
  status?: NotificationStatus;
}

export interface AuthResponse {
  token: string;
  userId: string;
}

/* ------------------------------------------------------------------ */
/* Oturum yonetimi (sessionStorage: parola/token diske yazilmaz)       */
/* ------------------------------------------------------------------ */

const TOKEN_KEY = "pdf-reader-token";

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function setSession(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
}

export function isLoggedIn(): boolean {
  return getToken() !== null;
}

/* ------------------------------------------------------------------ */
/* API yardimcilari                                                     */
/* ------------------------------------------------------------------ */

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function parseError(response: Response): Promise<never> {
  let message = `HTTP ${response.status}`;
  try {
    const body = await response.json();
    if (body && typeof body.message === "string") {
      message = body.message;
    }
  } catch {
    // JSON degilse status mesaji yeterli
  }
  throw new ApiError(response.status, message);
}

function authHeaders(): Record<string, string> {
  const token = getToken();
  const headers: Record<string, string> = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

/* ------------------------------------------------------------------ */
/* Auth                                                                 */
/* ------------------------------------------------------------------ */

export async function register(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

/* ------------------------------------------------------------------ */
/* Bildirimler + isler (hepsi token ister)                             */
/* ------------------------------------------------------------------ */

export async function fetchNotifications(): Promise<Notification[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/notifications`, {
    headers: authHeaders(),
  });

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function fetchUpcoming(days: number = 7): Promise<Notification[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/notifications/upcoming?days=${days}`,
    { headers: authHeaders() }
  );

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function uploadPdf(file: File, useAi: boolean = false): Promise<ProcessingJob> {
  const formData = new FormData();
  formData.append("file", file);

  const endpoint = useAi
    ? `${API_BASE_URL}/api/v1/notifications/ai-upload`
    : `${API_BASE_URL}/api/v1/notifications/upload`;

  const response = await fetch(endpoint, {
    method: "POST",
    headers: authHeaders(),
    body: formData,
  });

  if (!response.ok) {
    return parseError(response);
  }

  return response.json();
}

export async function fetchJob(id: string): Promise<ProcessingJob> {
  const response = await fetch(`${API_BASE_URL}/api/v1/notifications/jobs/${id}`, {
    headers: authHeaders(),
  });

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
    headers: {
      ...authHeaders(),
      "Content-Type": "application/json",
    },
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
    headers: authHeaders(),
  });

  if (!response.ok) {
    return parseError(response);
  }
}
