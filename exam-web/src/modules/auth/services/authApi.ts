import axios from 'axios';

const client = axios.create();

const TRACE_NO_HEADER = 'TraceNo';
const TRACE_NO_LENGTH = 32;

export type LoginPayload = {
  username: string;
  password: string;
};

export type LoginResponse = {
  token: string;
  tokenType: string;
  user: {
    userId: number;
    username: string;
    displayName: string;
  };
};

export type CurrentUserResponse = {
  userId: number;
  username: string;
  displayName: string;
  roles: string[];
  permissions: string[];
  menus: Array<{
    code: string;
    name: string;
    path: string;
  }>;
};

function createTraceNo() {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID().replace(/-/g, '');
  }

  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}${Math.random().toString(16).slice(2)}`
    .slice(0, TRACE_NO_LENGTH)
    .padEnd(TRACE_NO_LENGTH, '0');
}

function withTraceNo(headers: Record<string, string> = {}) {
  return {
    headers: {
      ...headers,
      [TRACE_NO_HEADER]: createTraceNo(),
    },
  };
}

export async function login(payload: LoginPayload) {
  const response = await client.post<LoginResponse>('/api/admin/auth/login', payload, withTraceNo());
  return response.data;
}

export async function fetchCurrentUser(token: string) {
  const response = await client.get<CurrentUserResponse>(
    '/api/admin/auth/me',
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}

export async function logout(token: string) {
  await client.post(
    '/api/admin/auth/logout',
    {},
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
}
