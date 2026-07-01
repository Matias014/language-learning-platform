export interface ProficiencyLevel {
  code: string;
  name: string;
  orderNumber: number;
}

export interface CreateProficiencyLevelRequest {
  code: string;
  name: string;
  orderNumber: number;
}

export interface UpdateProficiencyLevelRequest {
  name?: string;
  orderNumber?: number;
}
