import { z } from "zod";

export const courseSchema = z.object({
  id: z.number(),
  subject: z.string(),
  yearRange: z.string(),
  price: z.number(),
});
export type Course = z.infer<typeof courseSchema>;

export const courseListSchema = z.array(courseSchema);

export const checkoutResponseSchema = z.object({
  purchaseId: z.number(),
  invitationToken: z.string(),
  course: courseSchema,
});
export type CheckoutResponse = z.infer<typeof checkoutResponseSchema>;

export const onboardingInfoSchema = z.object({
  parentEmail: z.string(),
  status: z.enum(["PENDING_ONBOARDING", "COMPLETED"]),
  course: courseSchema,
});
export type OnboardingInfo = z.infer<typeof onboardingInfoSchema>;

export const authResponseSchema = z.object({
  token: z.string(),
  studentId: z.number(),
  studentName: z.string(),
});
export type AuthResponse = z.infer<typeof authResponseSchema>;

export const lessonSummarySchema = z.object({
  id: z.number(),
  title: z.string(),
  orderIndex: z.number(),
});
export type LessonSummary = z.infer<typeof lessonSummarySchema>;

export const lessonDetailSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  orderIndex: z.number(),
});
export type LessonDetail = z.infer<typeof lessonDetailSchema>;

export const dashboardSchema = z.object({
  studentName: z.string(),
  course: courseSchema,
  lessons: z.array(lessonSummarySchema),
});
export type Dashboard = z.infer<typeof dashboardSchema>;
