export interface Language {
  code: string;
  name: string;
}

export interface CreateLanguageRequest {
  code: string;
  name: string;
}

export interface UpdateLanguageRequest {
  name?: string;
}
