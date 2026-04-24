export interface AuditEventResponse {
  eventId: string;
  occurredAt: string;
  actorUserId?: string | null;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  correlationId?: string | null;
  payload: unknown;
}

export interface AuditQuery {
  aggregateType?: string;
  aggregateId?: string;
  actorUserId?: string;
  /** Server caps at 500. */
  limit?: number;
}
