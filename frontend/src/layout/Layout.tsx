import { Outlet } from 'react-router-dom';
import { NavBar } from './NavBar';

export function Layout() {
  return (
    <div className="app">
      <NavBar />
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
