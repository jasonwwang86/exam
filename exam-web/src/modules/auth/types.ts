export type LoginFormState = {
  username: string;
  password: string;
};

export type MenuItem = {
  code: string;
  name: string;
  path: string;
};

export type CurrentUser = {
  userId: number;
  username: string;
  displayName: string;
  roles: string[];
  permissions: string[];
  menus: MenuItem[];
};
