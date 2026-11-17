import { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { checkHealth } from '../services/api';
import Navbar from './Navbar';
import ServerWakingNotice from './ServerWakingNotice';

const currentYear = new Date().getFullYear();

export default function Layout() {
  // The free backend host stops idle apps, and starting one takes about two minutes. Asking for
  // its health as soon as the site opens lets it wake up while the visitor is still reading.
  // The answer isn't needed, so failures are ignored.
  useEffect(() => {
    checkHealth().catch(() => {});
  }, []);

  return (
    <div className="app">
      <Navbar />
      <main className="container page-content">
        <ServerWakingNotice />
        <Outlet />
      </main>
      <footer className="footer">
        <div className="container">&copy; {currentYear} Resume Analyzer</div>
      </footer>
    </div>
  );
}