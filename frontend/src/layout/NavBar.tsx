import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function NavBar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <Link to="/" className="nav-brand">FoodPlan</Link>
      {isAuthenticated && (
        <div className="nav-links">
          <Link to="/recipes">Recipes</Link>
          <Link to="/ingredients">Ingredients</Link>
          <Link to="/calendar">Calendar</Link>
          <Link to="/inventory">Pantry</Link>
          <Link to="/quick-cook">Quick Cook</Link>
          <Link to="/shopping-list">Shopping List</Link>
          <Link to="/settings">Settings</Link>
          <span className="nav-user">{user?.email}</span>
          <button onClick={handleLogout} className="btn btn-small">Logout</button>
        </div>
      )}
    </nav>
  );
}
