import { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { api, type Event, type Photo, type PublishGalleryResponse } from '../api/client';

export default function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const eventId = Number(id);
  const { user } = useAuth();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [event, setEvent] = useState<Event | null>(null);
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [memberEmail, setMemberEmail] = useState('');
  const [pin, setPin] = useState('');
  const [publishResult, setPublishResult] = useState<PublishGalleryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isAdmin = user?.role === 'ADMIN';

  const loadData = async () => {
    try {
      const [eventData, photoData] = await Promise.all([
        api.getEvent(eventId),
        api.getPhotos(eventId, !isAdmin),
      ]);
      setEvent(eventData);
      setPhotos(photoData);
      setSelectedIds(new Set(photoData.filter((p) => p.selectedForGallery).map((p) => p.id)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load event');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [eventId]);

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files?.length) return;
    setUploading(true);
    setError('');
    try {
      await api.uploadPhotos(eventId, Array.from(files));
      setSuccess('Photos uploaded successfully');
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const toggleSelect = (photoId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(photoId)) next.delete(photoId);
      else next.add(photoId);
      return next;
    });
  };

  const handleSaveSelection = async () => {
    try {
      await api.selectPhotos(eventId, Array.from(selectedIds));
      setSuccess(`Selected ${selectedIds.size} photos for gallery`);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save selection');
    }
  };

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.addMember(eventId, memberEmail);
      setMemberEmail('');
      setSuccess('Team member added');
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add member');
    }
  };

  const handlePublish = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!/^\d{6}$/.test(pin)) {
      setError('PIN must be exactly 6 digits');
      return;
    }
    try {
      const result = await api.publishGallery(eventId, pin);
      setPublishResult(result);
      setSuccess('Gallery published successfully!');
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to publish gallery');
    }
  };

  if (loading) {
    return (
      <div className="page">
        <Navbar />
        <div className="loading container">Loading event...</div>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="page">
        <Navbar />
        <div className="container"><div className="alert alert-error">{error || 'Event not found'}</div></div>
      </div>
    );
  }

  return (
    <div className="page">
      <Navbar />
      <main className="container">
        <div className="page-header">
          <div>
            <h1>{event.name}</h1>
            <p>{event.description}</p>
          </div>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <div className="stats-bar card">
          <div><strong>{event.totalPhotos}</strong> Total Uploaded</div>
          <div><strong>{event.selectedPhotos}</strong> Selected for Publishing</div>
          {event.galleryPublished && event.gallerySlug && (
            <div>
              Gallery: <a href={`/gallery/${event.gallerySlug}`} target="_blank" rel="noreferrer">
                /gallery/{event.gallerySlug}
              </a>
            </div>
          )}
        </div>

        {isAdmin && (
          <div className="admin-panel">
            <div className="card">
              <h3>Add Team Member</h3>
              <form onSubmit={handleAddMember} className="inline-form">
                <input
                  type="email"
                  placeholder="member@example.com"
                  value={memberEmail}
                  onChange={(e) => setMemberEmail(e.target.value)}
                  required
                />
                <button type="submit" className="btn btn-secondary">Add Member</button>
              </form>
              {event.members && event.members.length > 0 && (
                <ul className="member-list">
                  {event.members.map((m) => (
                    <li key={m.id}>{m.name} ({m.email})</li>
                  ))}
                </ul>
              )}
            </div>

            <div className="card">
              <h3>Publish Gallery</h3>
              <form onSubmit={handlePublish} className="inline-form">
                <input
                  type="text"
                  placeholder="6-digit PIN"
                  value={pin}
                  onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  maxLength={6}
                  required
                />
                <button type="submit" className="btn btn-primary" disabled={selectedIds.size === 0}>
                  Publish Gallery
                </button>
              </form>
              {publishResult && (
                <div className="publish-result">
                  <p><strong>Gallery URL:</strong> {publishResult.galleryUrl}</p>
                  <p><strong>Access PIN:</strong> {publishResult.pin}</p>
                  <p><strong>Photos:</strong> {publishResult.selectedPhotoCount}</p>
                </div>
              )}
            </div>
          </div>
        )}

        <div className="upload-section card">
          <h3>Upload Photos</h3>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            multiple
            onChange={handleUpload}
            disabled={uploading}
          />
          {uploading && <p className="muted">Uploading...</p>}
        </div>

        {isAdmin && photos.length > 0 && (
          <div className="selection-bar">
            <span>{selectedIds.size} photos selected</span>
            <button className="btn btn-secondary" onClick={handleSaveSelection}>
              Save Selection
            </button>
          </div>
        )}

        <div className="photo-grid">
          {photos.map((photo) => (
            <div
              key={photo.id}
              className={`photo-card ${selectedIds.has(photo.id) ? 'selected' : ''}`}
              onClick={() => isAdmin && toggleSelect(photo.id)}
            >
              <img src={photo.url} alt={photo.filename} loading="lazy" />
              <div className="photo-meta">
                <span>{photo.uploadedByName}</span>
                {photo.selectedForGallery && <span className="badge badge-success">Selected</span>}
              </div>
              {isAdmin && selectedIds.has(photo.id) && (
                <div className="select-indicator">✓</div>
              )}
            </div>
          ))}
        </div>

        {photos.length === 0 && (
          <div className="empty-state card">
            <p>No photos uploaded yet. Upload photos to get started.</p>
          </div>
        )}
      </main>
    </div>
  );
}
