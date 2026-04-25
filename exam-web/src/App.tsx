import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchCurrentUser, login as loginRequest, logout as logoutRequest } from './modules/auth/services/authApi';
import type { CurrentUser, LoginFormState } from './modules/auth/types';
import { LoginPage } from './modules/auth/pages/LoginPage';
import {
  confirmCandidateProfile,
  fetchCandidateScoreReport,
  fetchCandidateProfile,
  listCandidateExams,
  loadCandidateAnswerSession,
  loginCandidate,
  saveCandidateAnswer,
  submitCandidateExam,
} from './modules/candidate-portal/services/candidateApi';
import { CandidateAnsweringPage } from './modules/candidate-portal/pages/CandidateAnsweringPage';
import { CandidateConfirmationPage } from './modules/candidate-portal/pages/CandidateConfirmationPage';
import { CandidateExamListPage } from './modules/candidate-portal/pages/CandidateExamListPage';
import { CandidateLoadingPage } from './modules/candidate-portal/pages/CandidateLoadingPage';
import { CandidateLoginPage } from './modules/candidate-portal/pages/CandidateLoginPage';
import { CandidateScoreReportPage } from './modules/candidate-portal/pages/CandidateScoreReportPage';
import type {
  CandidateAnswerSession,
  CandidateExam,
  CandidateExamSubmissionResult,
  CandidateLoginFormState,
  CandidateProfile,
  CandidateScoreReport,
} from './modules/candidate-portal/types';
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
  const [answerSession, setAnswerSession] = useState<CandidateAnswerSession | null>(null);
  const [scoreReport, setScoreReport] = useState<CandidateScoreReport | null>(null);
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
    setAnswerSession(null);
    setScoreReport(null);
  }

  async function restoreCandidateSession(token: string) {
    const requestedPath = window.location.pathname;
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
        navigate(resolveCandidatePath(requestedPath, true), { replace: true });
      } else {
        setExams([]);
        setAnswerSession(null);
        setScoreReport(null);
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
      setAnswerSession(null);
      setScoreReport(null);
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
      setAnswerSession(null);
      setScoreReport(null);
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

  async function handleEnterAnswering(planId: number) {
    if (!accessToken || !profile?.profileConfirmed) {
      navigate('/candidate/login', { replace: true });
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const session = await loadCandidateAnswerSession(accessToken, planId);
      setAnswerSession(session);
      setScoreReport(null);
      navigate(`/candidate/exams/${planId}/answer`, { replace: false });
    } catch (error) {
      setAnswerSession(null);
      setErrorMessage(extractErrorMessage(error, '进入答题失败，请稍后重试'));
      navigate('/candidate/exams', { replace: true });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLoadAnsweringSession(planId: number) {
    if (!accessToken || !profile?.profileConfirmed) {
      navigate('/candidate/login', { replace: true });
      return;
    }
    if (answerSession?.planId === planId) {
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const session = await loadCandidateAnswerSession(accessToken, planId);
      setAnswerSession(session);
      setScoreReport(null);
    } catch (error) {
      setAnswerSession(null);
      setErrorMessage(extractErrorMessage(error, '进入答题失败，请稍后重试'));
      navigate('/candidate/exams', { replace: true });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSaveAnswer(planId: number, paperQuestionId: number, answerContent: Record<string, unknown> | null) {
    if (!accessToken) {
      throw new Error('missing candidate token');
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      return await saveCandidateAnswer(accessToken, planId, paperQuestionId, answerContent);
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, '保存答案失败，请稍后重试'));
      throw error;
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSubmitPaper(planId: number) {
    if (!accessToken) {
      throw new Error('missing candidate token');
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const submission = await submitCandidateExam(accessToken, planId);
      setAnswerSession((current) => mergeSubmissionIntoSession(current, submission));
      setScoreReport(null);
      try {
        const candidateExams = await listCandidateExams(accessToken);
        setExams(candidateExams);
        writeCandidateExamsCache(candidateExams);
      } catch {
        setExams((current) => {
          const next = current.map((exam) => (exam.planId === planId ? mergeSubmissionIntoExam(exam, submission) : exam));
          writeCandidateExamsCache(next);
          return next;
        });
      }
      return submission;
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, '提交试卷失败，请稍后重试'));
      throw error;
    } finally {
      setSubmitting(false);
    }
  }

  async function handleViewScoreReport(planId: number) {
    if (!accessToken || !profile?.profileConfirmed) {
      navigate('/candidate/login', { replace: true });
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const report = await fetchCandidateScoreReport(accessToken, planId);
      setScoreReport(report);
      setAnswerSession(null);
      navigate(`/candidate/exams/${planId}/report`, { replace: false });
    } catch (error) {
      setScoreReport(null);
      setErrorMessage(extractErrorMessage(error, '成绩详情加载失败，请稍后重试'));
      navigate('/candidate/exams', { replace: true });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLoadScoreReport(planId: number) {
    if (!accessToken || !profile?.profileConfirmed) {
      navigate('/candidate/login', { replace: true });
      return;
    }
    if (scoreReport?.planId === planId) {
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const report = await fetchCandidateScoreReport(accessToken, planId);
      setScoreReport(report);
      setAnswerSession(null);
    } catch (error) {
      setScoreReport(null);
      setErrorMessage(extractErrorMessage(error, '成绩详情加载失败，请稍后重试'));
      navigate('/candidate/exams', { replace: true });
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
              onEnterAnswering={(planId) => void handleEnterAnswering(planId)}
              onViewScoreReport={(planId) => void handleViewScoreReport(planId)}
              onLogout={handleCandidateLogout}
            />
          ) : (
            <Navigate to="/candidate/confirm" replace />
          )
        }
      />
      <Route
        path="/candidate/exams/:planId/answer"
        element={
          profile.profileConfirmed ? (
            <CandidateAnsweringRoute
              accessToken={accessToken}
              answerSession={answerSession}
              submitting={submitting}
              errorMessage={errorMessage}
              onLoadAnswerSession={(planId) => void handleLoadAnsweringSession(planId)}
              onSaveAnswer={handleSaveAnswer}
              onSubmitPaper={(planId) => handleSubmitPaper(planId)}
              onBackToExamList={() => {
                setErrorMessage('');
                navigate('/candidate/exams', { replace: true });
              }}
              onLogout={handleCandidateLogout}
            />
          ) : (
            <Navigate to="/candidate/confirm" replace />
          )
        }
      />
      <Route
        path="/candidate/exams/:planId/report"
        element={
          profile.profileConfirmed ? (
            <CandidateScoreReportRoute
              accessToken={accessToken}
              scoreReport={scoreReport}
              errorMessage={errorMessage}
              onLoadScoreReport={(planId) => void handleLoadScoreReport(planId)}
              onBackToExamList={() => {
                setErrorMessage('');
                navigate('/candidate/exams', { replace: true });
              }}
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

type CandidateAnsweringRouteProps = {
  accessToken: string;
  answerSession: CandidateAnswerSession | null;
  submitting: boolean;
  errorMessage: string;
  onLoadAnswerSession: (planId: number) => void;
  onSaveAnswer: (planId: number, paperQuestionId: number, answerContent: Record<string, unknown> | null) => Promise<any>;
  onSubmitPaper: (planId: number) => Promise<any>;
  onBackToExamList: () => void;
  onLogout: () => void;
};

type CandidateScoreReportRouteProps = {
  accessToken: string;
  scoreReport: CandidateScoreReport | null;
  errorMessage: string;
  onLoadScoreReport: (planId: number) => void;
  onBackToExamList: () => void;
  onLogout: () => void;
};

function CandidateAnsweringRoute({
  accessToken,
  answerSession,
  submitting,
  errorMessage,
  onLoadAnswerSession,
  onSaveAnswer,
  onSubmitPaper,
  onBackToExamList,
  onLogout,
}: CandidateAnsweringRouteProps) {
  const params = useParams<{ planId: string }>();
  const planId = Number(params.planId);

  useEffect(() => {
    if (!accessToken || !Number.isFinite(planId) || planId <= 0) {
      return;
    }
    if (answerSession?.planId !== planId) {
      onLoadAnswerSession(planId);
    }
  }, [accessToken, answerSession?.planId, planId]);

  if (!Number.isFinite(planId) || planId <= 0) {
    return <Navigate to="/candidate/exams" replace />;
  }

  if (!answerSession || answerSession.planId !== planId) {
    return <CandidateLoadingPage />;
  }

  return (
    <CandidateAnsweringPage
      session={answerSession}
      submitting={submitting}
      errorMessage={errorMessage}
      onSaveAnswer={(paperQuestionId, answerContent) => onSaveAnswer(planId, paperQuestionId, answerContent)}
      onSubmitPaper={() => onSubmitPaper(planId)}
      onBackToExamList={onBackToExamList}
      onLogout={onLogout}
    />
  );
}

function CandidateScoreReportRoute({
  accessToken,
  scoreReport,
  errorMessage,
  onLoadScoreReport,
  onBackToExamList,
  onLogout,
}: CandidateScoreReportRouteProps) {
  const params = useParams<{ planId: string }>();
  const planId = Number(params.planId);

  useEffect(() => {
    if (!accessToken || !Number.isFinite(planId) || planId <= 0) {
      return;
    }
    if (scoreReport?.planId !== planId) {
      onLoadScoreReport(planId);
    }
  }, [accessToken, onLoadScoreReport, planId, scoreReport?.planId]);

  if (!Number.isFinite(planId) || planId <= 0) {
    return <Navigate to="/candidate/exams" replace />;
  }

  if (!scoreReport || scoreReport.planId !== planId) {
    return <CandidateLoadingPage />;
  }

  return <CandidateScoreReportPage report={scoreReport} errorMessage={errorMessage} onBackToExamList={onBackToExamList} onLogout={onLogout} />;
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

function mergeSubmissionIntoSession(current: CandidateAnswerSession | null, submission: CandidateExamSubmissionResult) {
  if (!current || current.planId !== submission.planId) {
    return current;
  }
  return {
    ...current,
    sessionStatus: submission.sessionStatus,
    submissionMethod: submission.submissionMethod,
    submittedAt: submission.submittedAt,
    answeredCount: submission.answeredCount,
    totalQuestionCount: submission.totalQuestionCount,
  };
}

function mergeSubmissionIntoExam(exam: CandidateExam, submission: CandidateExamSubmissionResult): CandidateExam {
  return {
    ...exam,
    canEnterAnswering: false,
    answeringStatus: submission.sessionStatus,
    remainingSeconds: 0,
    submittedAt: submission.submittedAt,
    submissionMethod: submission.submissionMethod,
  };
}

function resolveCandidatePath(requestedPath: string, profileConfirmed: boolean) {
  if (!profileConfirmed) {
    return '/candidate/confirm';
  }
  if (/^\/candidate\/exams\/\d+\/answer$/.test(requestedPath)) {
    return requestedPath;
  }
  if (/^\/candidate\/exams\/\d+\/report$/.test(requestedPath)) {
    return requestedPath;
  }
  if (requestedPath === '/candidate/exams') {
    return requestedPath;
  }
  return '/candidate/exams';
}
