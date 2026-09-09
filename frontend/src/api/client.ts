const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export interface User {
  id: number;
  name: string;
  email: string;
  role: 'ADMIN' | 'TEAM_MEMBER';
}

export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: 'ADMIN' | 'TEAM_MEMBER';
}

export interface Event {
  id: number;
  name: string;
  description: string;
  createdByName: string;
  createdAt: string;
  totalPhotos: number;
  selectedPhotos: number;
  galleryPublished: boolean;
  gallerySlug?: string;
  members?: User[];
}

export interface Photo {
  id: number;
  filename: string;
  url: string;
  fileSize: number;
  contentType: string;
  selectedForGallery: boolean;
  uploadedByName: string;
  createdAt: string;
}

export interface PublishGalleryResponse {
  galleryUrl: string;
  slug: string;
  pin: string;
  selectedPhotoCount: number;
}

export interface GalleryAccessResponse {
  accessToken: string;
  eventName: string;
  photoCount: number;
  photos: Photo[];
}

class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const token = localStorage.getItem('token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: 'Request failed' }));
    throw new ApiError(body.message || 'Request failed', response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export const api = {
  register: (data: { name: string; email: string; password: string; role?: string }) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(data) }),

  login: (data: { email: string; password: string }) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(data) }),

  me: () => request<User>('/auth/me'),

  getEvents: () => request<Event[]>('/events'),

  getEvent: (id: number) => request<Event>(`/events/${id}`),

  createEvent: (data: { name: string; description?: string }) =>
    request<Event>('/events', { method: 'POST', body: JSON.stringify(data) }),

  addMember: (eventId: number, email: string) =>
    request<Event>(`/events/${eventId}/members`, {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  getPhotos: (eventId: number, mineOnly = false) =>
    request<Photo[]>(`/events/${eventId}/photos?mineOnly=${mineOnly}`),

  uploadPhotos: (eventId: number, files: File[]) => {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return request<Photo[]>(`/events/${eventId}/photos`, { method: 'POST', body: formData });
  },

  selectPhotos: (eventId: number, photoIds: number[]) =>
    request<void>(`/events/${eventId}/photos/select`, {
      method: 'PUT',
      body: JSON.stringify({ photoIds }),
    }),

  publishGallery: (eventId: number, pin: string) =>
    request<PublishGalleryResponse>(`/events/${eventId}/gallery/publish`, {
      method: 'POST',
      body: JSON.stringify({ pin }),
    }),

  accessGallery: (slug: string, pin: string) =>
    request<GalleryAccessResponse>(`/public/gallery/${slug}/access`, {
      method: 'POST',
      body: JSON.stringify({ pin }),
    }),

  getGalleryPhotos: (slug: string, galleryToken: string) =>
    request<Photo[]>(`/public/gallery/${slug}/photos`, {
      headers: { 'X-Gallery-Token': galleryToken },
    }),
};

export { ApiError };
