import { apiGet, apiPut } from './client';

export interface OrgSettings {
  timezone: string;
  defaultServings: number;
  allowedRecipeSites: string[];
  defaultRecipeSites: string[];
}

export interface UpdateOrgSettingsRequest {
  timezone?: string;
  defaultServings?: number;
  allowedRecipeSites?: string[];
}

export function getSettings(): Promise<OrgSettings> {
  return apiGet('/api/v1/settings');
}

export function updateSettings(req: UpdateOrgSettingsRequest): Promise<OrgSettings> {
  return apiPut('/api/v1/settings', req);
}
