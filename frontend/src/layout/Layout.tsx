import { Outlet } from 'react-router-dom';
import { NavBar } from './NavBar';

export function Layout() {
  return (
    <div className="app">
      <NavBar />
      <main className="content">
        <Outlet />
      </main>
      <footer className="app-footer">
        v{__APP_VERSION__} · {new Date(__BUILD_TIME__).toLocaleDateString()}
      </footer>
    </div>
  );
}
