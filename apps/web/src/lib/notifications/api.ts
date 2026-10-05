import { apiFetch } from '../api-client';

export interface NotificationDto {
  id: string;
  userId: string;
  studioId?: string | null;
  type: string;
  title: string;
  message: string;
  actionUrl?: string | null;
  readAt?: string | null;
  createdAt: string;
  metadata?: string | null;
}

export interface NotificationListResponse {
  notifications: NotificationDto[];
  unreadCount: number;
  limit: number;
  offset: number;
}

export interface NotificationPreferencesDto {
  userId?: string;
  inAppEnabled: boolean;
  emailEnabled: boolean;
  whatsappEnabled: boolean;
  leadNotifications: boolean;
  reviewNotifications: boolean;
  aiNotifications: boolean;
  systemNotifications: boolean;
}

export async function fetchNotifications(limit: number = 30, offset: number = 0): Promise<NotificationListResponse> {
  return apiFetch<NotificationListResponse>(`/notifications?limit=${limit}&offset=${offset}`);
}

export async function fetchUnreadNotificationCount(): Promise<number> {
  const data = await apiFetch<{ unreadCount: number }>('/notifications/unread-count');
  return data?.unreadCount || 0;
}

export async function markNotificationAsRead(id: string): Promise<void> {
  await apiFetch<{ success: boolean }>(`/notifications/${encodeURIComponent(id)}/read`, {
    method: 'POST',
  });
}

export async function markAllNotificationsAsRead(): Promise<void> {
  await apiFetch<{ success: boolean }>('/notifications/read-all', {
    method: 'POST',
  });
}

export async function fetchNotificationPreferences(): Promise<NotificationPreferencesDto> {
  return apiFetch<NotificationPreferencesDto>('/notifications/preferences');
}

export async function updateNotificationPreferences(
  prefs: Partial<NotificationPreferencesDto>
): Promise<NotificationPreferencesDto> {
  return apiFetch<NotificationPreferencesDto>('/notifications/preferences', {
    method: 'PUT',
    body: JSON.stringify(prefs),
  });
}
