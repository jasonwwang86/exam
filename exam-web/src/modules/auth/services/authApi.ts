import axios from 'axios';
import { withTraceNo } from '../../../shared/utils/trace';

const client = axios.create();

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
