import {UserRole} from './enums.model';

export interface User {
  id: number;
  login: string;
  email: string;
  name: string | null;
  surname: string | null;
  role: UserRole;
  totalXp: number;
  avatarPath: string | null;
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
}

export interface CreateUserRequest {
  login: string;
  email: string;
  password: string;
  name: string;
  surname: string;
}

export interface UpdateUserRequest {
  name?: string | null;
  surname?: string | null;
}

export interface ChangeEmailRequest {
  newEmail: string;
  currentPassword: string;
}

export interface ChangeLoginRequest {
  newLogin: string;
  currentPassword: string;
}

export interface ChangePasswordRequest {
  newPassword: string;
  currentPassword: string;
}

export interface AdminChangeEmailRequest {
  newEmail: string;
}

export interface AdminChangeLoginRequest {
  newLogin: string;
}

export interface AdminSetPasswordRequest {
  newPassword: string;
}
