export interface Task {
  id: number;
  title: string;
  description: string | null;
  done: boolean;
  createdAt: string;
}

export interface TaskRequest {
  title?: string;
  description?: string | null;
  done?: boolean;
}
