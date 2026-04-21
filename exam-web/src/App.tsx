import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { fetchCurrentUser, login as loginRequest, logout as logoutRequest } from './modules/auth/services/authApi';
import type { CurrentUser, LoginFormState } from './modules/auth/types';
import { LoginPage } from './modules/auth/pages/LoginPage';
import { DashboardPage } from './modules/dashboard/pages/DashboardPage';
import { ExamineeManagementPage } from './modules/examinees/pages/ExamineeManagementPage';
import { NoPermissionPage } from './modules/system/pages/NoPermissionPage';
import { LoadingPage } from './modules/system/pages/LoadingPage';
import { TOKEN_STORAGE_KEY } from './shared/constants/storage';
import { AdminLayout } from './shared/layouts/AdminLayout';

export function App() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AdminAuthApp />
    </BrowserRouter>
  );
}

function AdminAuthApp() {
  const navigate = useNavigate();
  const [form, setForm] = useState<LoginFormState>({ username: '', password: '' });
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [accessToken, setAccessToken] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const storedToken = window.localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!storedToken) {
      navigate('/login', { replace: true });
      setLoading(false);
      return;
    }

    void restoreSession(storedToken);
  }, [navigate]);

  async function restoreSession(token: string) {
    try {
      const user = await fetchCurrentUser(token);
      setCurrentUser(user);
      setAccessToken(token);
      navigate(resolvePostAuthPath(user, window.location.pathname), { replace: true });
    } catch {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
      setCurrentUser(null);
      setAccessToken('');
      navigate('/login', { replace: true });
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin() {
    if (!form.username.trim() || !form.password.trim()) {
      setErrorMessage('请输入用户名和密码');
      return;
    }
    setSubmitting(true);
    setErrorMessage('');
    try {
      const response = await loginRequest(form);
      window.localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
      setAccessToken(response.token);
      await restoreSession(response.token);
    } catch {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
      setCurrentUser(null);
      setAccessToken('');
      setErrorMessage('用户名或密码错误');
      setLoading(false);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLogout() {
    const token = window.localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      await logoutRequest(token);
    }
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    setCurrentUser(null);
    setAccessToken('');
    setForm({ username: '', password: '' });
    setErrorMessage('');
    navigate('/login', { replace: true });
  }

  if (loading) {
    return <LoadingPage />;
  }

  if (!currentUser) {
    return (
      <LoginPage
        form={form}
        submitting={submitting}
        errorMessage={errorMessage}
        onUsernameChange={(value) => setForm((current) => ({ ...current, username: value }))}
        onPasswordChange={(value) => setForm((current) => ({ ...current, password: value }))}
        onSubmit={() => void handleLogin()}
      />
    );
  }

  const canViewDashboard = currentUser.permissions.includes('dashboard:view');
  const canViewExaminees = currentUser.permissions.includes('examinee:view');
  const defaultAuthorizedPath = getDefaultAuthorizedPath(currentUser);

  return (
    <AdminLayout currentUser={currentUser} onLogout={() => void handleLogout()}>
      <Routes>
        <Route path="/login" element={<Navigate to={defaultAuthorizedPath} replace />} />
        <Route path="/" element={<Navigate to={defaultAuthorizedPath} replace />} />
        <Route path="/dashboard" element={canViewDashboard ? <DashboardPage /> : <NoPermissionPage />} />
        <Route
          path="/examinees"
          element={canViewExaminees ? <ExamineeManagementPage token={accessToken} permissions={currentUser.permissions} /> : <NoPermissionPage />}
        />
        <Route path="/no-permission" element={<NoPermissionPage />} />
        <Route path="*" element={<Navigate to={defaultAuthorizedPath} replace />} />
      </Routes>
    </AdminLayout>
  );
}

function getDefaultAuthorizedPath(user: CurrentUser | null) {
  if (!user) {
    return '/login';
  }
  if (user.permissions.includes('dashboard:view')) {
    return '/dashboard';
  }
  if (user.permissions.includes('examinee:view')) {
    return '/examinees';
  }
  return '/no-permission';
}

function resolvePostAuthPath(user: CurrentUser, requestedPath: string) {
  if (requestedPath === '/examinees' && user.permissions.includes('examinee:view')) {
    return '/examinees';
  }
  if (requestedPath === '/dashboard' && user.permissions.includes('dashboard:view')) {
    return '/dashboard';
  }
  return getDefaultAuthorizedPath(user);
}
