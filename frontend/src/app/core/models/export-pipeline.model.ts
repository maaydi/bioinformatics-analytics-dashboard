import {GeneSearchRequest} from '@core/models/saved-filter.model';

export type ExportStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type ExportFormat = 'CSV' | 'TSV' | 'JSON' | 'EXCEL';

export interface ExportPipeline {
  id: number,
  name: string,
  description?: string,
  format: ExportFormat
  fieldSchema: string[],
  status: ExportStatus,
  estimatedRows?: number,
  actualRows?: number
  fileSizeBytes?: number,
  errorMessage?: string,
  createdAt: string,
  completedAt?: string

}

export interface ExportPipelineCreateRequest {
  name: string,
  description?: string,
  filter: GeneSearchRequest,
  format: ExportFormat,
  fieldSchema: string[]

}

export interface ExportJobStatus {

  pipelineId: number,
  status: ExportStatus,
  progressPercent: number,
  chunksProcessed?: number,
  chunksTotal?: number,
  currentStep?: string
}

export interface ExportFieldSchema {
  fieldName: string,
  displayName: string,
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE' | 'ARRAY',
  description: string
}

export interface DownloadUrl {
  downloadUrl: string,
  filename: string,
  fileSizeBytes: number,
  contentType: string
}

