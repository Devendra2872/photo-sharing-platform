import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { api, type Event } from '../api/client';

export default function DashboardPage() {
  const { user } = useAuth();
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');

  const loadEvents = () => {
    api.getEvents()
      .then(setEvents)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.createEvent({ name, description });
      setName('');
      setDescription('');
      setShowCreate(false);
      loadEvents();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create event');
    }
  };

  return (
    <div className="page">
      <Navbar />
      <main className="container">
        <div className="page-header">
          <div>
            <h1>Events</h1>
            <p>{user?.role === 'ADMIN' ? 'Manage your events and galleries' : 'Your assigned events'}</p>
          </div>
          {user?.role === 'ADMIN' && (
            <button className="btn btn-primary" onClick={() => setShowCreate(!showCreate)}>
              {showCreate ? 'Cancel' : '+ New Event'}
            </button>
          )}
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {showCreate && (
          <form className="card create-form" onSubmit={handleCreate}>
            <h3>Create Event</h3>
            <label>
              Event Name
              <input value={name} onChange={(e) => setName(e.target.value)} required placeholder="Arjun & Priya Wedding" />
            </label>
            <label>
              Description
              <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
            </label>
            <button type="submit" className="btn btn-primary">Create Event</button>
          </form>
        )}

        {loading ? (
          <div className="loading">Loading events...</div>
        ) : events.length === 0 ? (
          <div className="empty-state card">
            <p>No events yet. {user?.role === 'ADMIN' ? 'Create your first event to get started.' : 'Ask your admin to assign you to an event.'}</p>
          </div>
        ) : (
          <div className="event-grid">
            {events.map((event) => (
              <Link key={event.id} to={`/events/${event.id}`} className="card event-card">
                <h3>{event.name}</h3>
                <p className="muted">{event.description || 'No description'}</p>
                <div className="stats">
                  <span>{event.totalPhotos} uploaded</span>
                  <span>{event.selectedPhotos} selected</span>
                </div>
                {event.galleryPublished && (
                  <span className="badge badge-success">Gallery Published</span>
                )}
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
