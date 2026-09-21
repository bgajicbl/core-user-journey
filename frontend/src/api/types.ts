export interface Course {
  id: number;
  subject: string;
  yearRange: string;
  price: number;
}

export interface CheckoutResponse {
  purchaseId: number;
  invitationToken: string;
  course: Course;
}

export interface OnboardingInfo {
  parentEmail: string;
  status: "PENDING_ONBOARDING" | "COMPLETED";
  course: Course;
}

export interface AuthResponse {
  token: string;
  studentId: number;
  studentName: string;
}

export interface LessonSummary {
  id: number;
  title: string;
  orderIndex: number;
}

export interface LessonDetail {
  id: number;
  title: string;
  content: string;
  orderIndex: number;
}

export interface Dashboard {
  studentName: string;
  course: Course;
  lessons: LessonSummary[];
}
