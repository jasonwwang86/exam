import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { fetchCurrentUser, login as loginRequest, logout as logoutRequest } from './modules/auth/services/authApi';
import type { CurrentUser, LoginFormState } from './modules/auth/types';
import { LoginPage } from './modules/auth/pages/LoginPage';
import {
  confirmCandidateProfile,
  fetchCandidateProfile,
  listCandidateExams,
  loginCandidate,
} from './modules/candidate-portal/services/candidateApi';
import { CandidateConfirmationPage } from './modules/candidate-portal/pages/CandidateConfirmationPage';
import { CandidateExamListPage } from './modules/candidate-portal/pages/CandidateExamListPage';
import { CandidateLoadingPage } from './modules/candidate-portal/pages/CandidateLoadingPage';
import { CandidateLoginPage } from './modules/candidate-portal/pages/CandidateLoginPage';
import type { CandidateExam, CandidateLoginFormState, CandidateProfile } from './modules/candidate-portal/types';
import { DashboardPage } from './modules/dashboard/pages/DashboardPage';
import { ExamPlanManagementPage } from './modules/exam-plan-management/pages/ExamPlanManagementPage';
import { ExamineeManagementPage } from './modules/examinees/pages/ExamineeManagementPage';
import { PaperManagementPage } from './modules/paper-management/pages/PaperManagementPage';
import { QuestionBankManagementPage } from './modules/question-bank/pages/QuestionBankManagementPage';
import { NoPermissionPage } from './modules/system/pages/NoPermissionPage';
import { LoadingPage } from './modules/system/pages/LoadingPage';
import {
  CANDIDATE_EXAMS_STORAGE_KEY,
  CANDIDATE_PROFILE_STORAGE_KEY,
  CANDIDATE_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY,
} from './shared/constants/storage';
import { AdminLayout } from './shared/layouts/AdminLayout';
import { extractErrorMessage } from './shared/utils/http';

export function App() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AppRouter />
    </BrowserRouter>
  );
}

function AppRouter() {
  const location = useLocation();

  if (location.pathname.startsWith('/candidate')) {
    return <CandidatePortalApp />;
  }

  return <AdminAuthApp />;
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
  const canViewQuestionBank = currentUser.permissions.includes('question-bank:view');
  const canViewPapers = currentUser.permissions.includes('paper-management:view');
  const canViewExamPlans = currentUser.permissions.includes('exam-plan-management:view');
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
        <Route
          path="/question-bank"
          element={canViewQuestionBank ? <QuestionBankManagementPage token={accessToken} permissions={currentUser.permissions} /> : <NoPermissionPage />}
        />
        <Route
          path="/papers"
          element={canViewPapers ? <PaperManagementPage token={accessToken} permissions={currentUser.permissions} /> : <NoPermissionPage />}
        />
        <Route
          path="/exam-plans"
          element={canViewExamPlans ? <ExamPlanManagementPage token={accessToken} permissions={currentUser.permissions} /> : <NoPermissionPage />}
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
  if (user.permissions.includes('question-bank:view')) {
    return '/question-bank';
  }
  if (user.permissions.includes('paper-management:view')) {
    return '/papers';
  }
  if (user.permissions.includes('exam-plan-management:view')) {
    return '/exam-plans';
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
  if (requestedPath === '/question-bank' && user.permissions.includes('question-bank:view')) {
    return '/question-bank';
  }
  if (requestedPath === '/papers' && user.permissions.includes('paper-management:view')) {
    return '/papers';
  }
  if (requestedPath === '/exam-plans' && user.permissions.includes('exam-plan-management:view')) {
    return '/exam-plans';
  }
  return getDefaultAuthorizedPath(user);
}

function CandidatePortalApp() {
  const navigate = useNavigate();
  const [form, setForm] = useState<CandidateLoginFormState>({ examineeNo: '', idCardNo: '' });
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [exams, setExams] = useState<CandidateExam[]>([]);
  const [accessToken, setAccessToken] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const storedToken = window.localStorage.getItem(CANDIDATE_TOKEN_STORAGE_KEY);
    if (!storedToken) {
      navigate('/candidate/login', { replace: true });
      setLoading(false);
      return;
    }

    void restoreCandidateSession(storedToken);
  }, [navigate]);

  function clearCandidateSession() {
    window.localStorage.removeItem(CANDIDATE_TOKEN_STORAGE_KEY);
    window.localStorage.removeItem(CANDIDATE_PROFILE_STORAGE_KEY);
    window.localStorage.removeItem(CANDIDATE_EXAMS_STORAGE_KEY);
    setAccessToken('');
    setProfile(null);
    setExams([]);
  }

  async function restoreCandidateSession(token: string) {
    try {
      const cachedProfile = readCandidateProfileCache();
      const candidateProfile = cachedProfile ?? (await fetchCandidateProfile(token));
      setAccessToken(token);
      setProfile(candidateProfile);
      writeCandidateProfileCache(candidateProfile);
      if (candidateProfile.profileConfirmed) {
        const cachedExams = readCandidateExamsCache();
        const candidateExams = cachedExams ?? (await listCandidateExams(token));
        setExams(candidateExams);
        writeCandidateExamsCache(candidateExams);
        navigate('/candidate/exams', { replace: true });
      } else {
        setExams([]);
        window.localStorage.removeItem(CANDIDATE_EXAMS_STORAGE_KEY);
        navigate('/candidate/confirm', { replace: true });
      }
    } catch {
      clearCandidateSession();
      navigate('/candidate/login', { replace: true });
    } finally {
      setLoading(false);
    }
  }

  async function handleCandidateLogin() {
    if (!form.examineeNo.trim() || !form.idCardNo.trim()) {
      setErrorMessage('请输入考生编号和身份证号');
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const response = await loginCandidate({
        examineeNo: form.examineeNo.trim(),
        idCardNo: form.idCardNo.trim(),
      });
      window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, response.token);
      setAccessToken(response.token);
      setProfile({
        ...response.profile,
        profileConfirmed: response.profileConfirmed,
        message: response.profileConfirmed ? '身份信息已确认，可查看可参加考试' : '请先确认身份信息后查看可参加考试',
      });
      writeCandidateProfileCache({
        ...response.profile,
        profileConfirmed: response.profileConfirmed,
        message: response.profileConfirmed ? '身份信息已确认，可查看可参加考试' : '请先确认身份信息后查看可参加考试',
      });
      setExams([]);
      window.localStorage.removeItem(CANDIDATE_EXAMS_STORAGE_KEY);
      navigate(response.profileConfirmed ? '/candidate/exams' : '/candidate/confirm', { replace: true });
    } catch (error) {
      clearCandidateSession();
      setErrorMessage(extractErrorMessage(error, '考生编号或身份证号错误'));
    } finally {
      setSubmitting(false);
      setLoading(false);
    }
  }

  async function handleCandidateConfirm() {
    if (!accessToken || !profile) {
      navigate('/candidate/login', { replace: true });
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const response = await confirmCandidateProfile(accessToken);
      window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, response.token);
      setAccessToken(response.token);
      setProfile({
        ...response.profile,
        profileConfirmed: response.profileConfirmed,
        message: '身份信息已确认，可查看可参加考试',
      });
      writeCandidateProfileCache({
        ...response.profile,
        profileConfirmed: response.profileConfirmed,
        message: '身份信息已确认，可查看可参加考试',
      });
      const candidateExams = await listCandidateExams(response.token);
      setExams(candidateExams);
      writeCandidateExamsCache(candidateExams);
      navigate('/candidate/exams', { replace: true });
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, '身份确认失败，请稍后重试'));
    } finally {
      setSubmitting(false);
    }
  }

  function handleCandidateLogout() {
    clearCandidateSession();
    setForm({ examineeNo: '', idCardNo: '' });
    setErrorMessage('');
    setLoading(false);
    navigate('/candidate/login', { replace: true });
  }

  async function handleRefreshCandidateExams() {
    if (!accessToken || !profile) {
      handleCandidateLogout();
      return;
    }
    if (!profile.profileConfirmed) {
      navigate('/candidate/confirm', { replace: true });
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const candidateExams = await listCandidateExams(accessToken);
      setExams(candidateExams);
      writeCandidateExamsCache(candidateExams);
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, '刷新考试列表失败，请稍后重试'));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <CandidateLoadingPage />;
  }

  if ((!accessToken || !profile) && window.localStorage.getItem(CANDIDATE_TOKEN_STORAGE_KEY)) {
    return <CandidateLoadingPage />;
  }

  if (!accessToken || !profile) {
    return (
      <CandidateLoginPage
        form={form}
        submitting={submitting}
        errorMessage={errorMessage}
        onExamineeNoChange={(value) => setForm((current) => ({ ...current, examineeNo: value }))}
        onIdCardNoChange={(value) => setForm((current) => ({ ...current, idCardNo: value }))}
        onSubmit={() => void handleCandidateLogin()}
      />
    );
  }

  return (
    <Routes>
      <Route path="/candidate/login" element={<Navigate to={profile.profileConfirmed ? '/candidate/exams' : '/candidate/confirm'} replace />} />
      <Route
        path="/candidate/confirm"
        element={
          <CandidateConfirmationPage
            profile={profile}
            submitting={submitting}
            errorMessage={errorMessage}
            onConfirm={() => void handleCandidateConfirm()}
            onLogout={handleCandidateLogout}
          />
        }
      />
      <Route
        path="/candidate/exams"
        element={
          profile.profileConfirmed ? (
            <CandidateExamListPage
              exams={exams}
              submitting={submitting}
              errorMessage={errorMessage}
              onRefresh={() => void handleRefreshCandidateExams()}
              onLogout={handleCandidateLogout}
            />
          ) : (
            <Navigate to="/candidate/confirm" replace />
          )
        }
      />
      <Route path="*" element={<Navigate to={profile.profileConfirmed ? '/candidate/exams' : '/candidate/confirm'} replace />} />
    </Routes>
  );
}

function readCandidateProfileCache() {
  const raw = window.localStorage.getItem(CANDIDATE_PROFILE_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as CandidateProfile;
  } catch {
    window.localStorage.removeItem(CANDIDATE_PROFILE_STORAGE_KEY);
    return null;
  }
}

function writeCandidateProfileCache(profile: CandidateProfile) {
  window.localStorage.setItem(CANDIDATE_PROFILE_STORAGE_KEY, JSON.stringify(profile));
}

function readCandidateExamsCache() {
  const raw = window.localStorage.getItem(CANDIDATE_EXAMS_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as CandidateExam[];
  } catch {
    window.localStorage.removeItem(CANDIDATE_EXAMS_STORAGE_KEY);
    return null;
  }
}

function writeCandidateExamsCache(exams: CandidateExam[]) {
  window.localStorage.setItem(CANDIDATE_EXAMS_STORAGE_KEY, JSON.stringify(exams));
}
