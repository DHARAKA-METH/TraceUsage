export type Application = {
  id: number;
  name: string;
  environment: string;
  projectId: string;
  createdAt: string;
};

export type CreateApplicationResponse = Application & {
  apiKey: string;
  publicIngestKey?: string | null;
};
