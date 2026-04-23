/** RFC 9457 problem+json shape returned by the backend. */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  code: string;
  violations?: string[];
}
