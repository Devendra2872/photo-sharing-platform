import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <Link to="/dashboard" className="brand">
        PhotoShare
      </Link>
      {user && (
        <div className="nav-right">
          <span className="user-badge">
            {user.name} ({user.role === 'ADMIN' ? 'Admin' : 'Team Member'})
          </span>
          <button onClick={handleLogout} className="btn btn-outline">
            Logout
          </button>
        </div>
      )}
    </nav>
  );
}
