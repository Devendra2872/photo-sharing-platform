import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, type Photo } from '../api/client';

export default function GalleryPage() {
  const { slug } = useParams<{ slug: string }>();
  const [pin, setPin] = useState('');
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [eventName, setEventName] = useState('');
  const [authenticated, setAuthenticated] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedPhoto, setSelectedPhoto] = useState<Photo | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const handleAccess = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!slug) return;
    setLoading(true);
    setError('');
    try {
      const response = await api.accessGallery(slug, pin);
      setPhotos(response.photos);
      setEventName(response.eventName);
      setAuthenticated(true);
      sessionStorage.setItem(`gallery-${slug}`, response.accessToken);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Access denied');
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (
      e: React.MouseEvent,
      photoUrl: string,
      filename: string,
      photoId: string
  ) => {
    e.stopPropagation(); // don't trigger the lightbox/card onClick
    setDownloadingId(photoId);
    try {
      const response = await fetch(photoUrl);
      if (!response.ok) throw new Error('Download failed');
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename || 'photo.jpg';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download failed', err);
      setError('Failed to download photo. Please try again.');
    } finally {
      setDownloadingId(null);
    }
  };

  if (!authenticated) {
    return (
        <div className="gallery-page">
          <div className="gallery-auth-card">
            <h1>Photo Gallery</h1>
            <p className="subtitle">Enter the PIN provided by your photographer</p>
            {error && <div className="alert alert-error">{error}</div>}
            <form onSubmit={handleAccess}>
              <label>
                Gallery PIN
                <input
                    type="text"
                    inputMode="numeric"
                    placeholder="6-digit PIN"
                    value={pin}
                    onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    maxLength={6}
                    required
                />
              </label>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Verifying...' : 'View Gallery'}
              </button>
            </form>
          </div>
        </div>
    );
  }

  return (
      <div className="gallery-page">
        <header className="gallery-header">
          <h1>{eventName}</h1>
          <p>{photos.length} photos</p>
        </header>

        <div className="photo-grid gallery-grid">
          {photos.map((photo) => (
              <div key={photo.id} className="photo-card" onClick={() => setSelectedPhoto(photo)}>
                <img src={photo.url} alt={photo.filename} loading="lazy" />
                <button
                    className="photo-download-btn"
                    onClick={(e) => handleDownload(e, photo.url, photo.filename, photo.id)}
                    disabled={downloadingId === photo.id}
                    title="Download photo"
                >
                  {downloadingId === photo.id ? '...' : '⬇'}
                </button>
              </div>
          ))}
        </div>

        {selectedPhoto && (
            <div className="lightbox" onClick={() => setSelectedPhoto(null)}>
              <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
                <button className="lightbox-close" onClick={() => setSelectedPhoto(null)}>×</button>
                <img src={selectedPhoto.url} alt={selectedPhoto.filename} />
                <button
                    className="btn btn-primary lightbox-download"
                    onClick={(e) =>
                        handleDownload(e, selectedPhoto.url, selectedPhoto.filename, selectedPhoto.id)
                    }
                    disabled={downloadingId === selectedPhoto.id}
                >
                  {downloadingId === selectedPhoto.id ? 'Downloading...' : 'Download Photo'}
                </button>
              </div>
            </div>
        )}
      </div>
  );
}