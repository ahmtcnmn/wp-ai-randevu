// Tek yerden tüm API client'lar
import api from "./axios";

export { authApi } from "./auth";
export { billingApi } from "./billing";
export { publicContactApi } from "./publicContact";
export { customerApi } from "./customer";
export { appointmentApi } from "./appointment";
export { notificationApi } from "./notification";
export { userDeviceApi } from "./userDevice";
export { serviceApi, serviceCategoryApi } from "./service";
export { userApi } from "./user";
export { tenantApi } from "./tenant";
export { branchApi } from "./branch";
export { chatApi } from "./chat";
export { conversationApi, whatsappConfigApi, whatsappTemplateApi } from "./whatsapp";
export { aiConfigApi } from "./aiConfig";
export { featureApi } from "./feature";
export { reminderApi, reminderTemplateApi } from "./reminder";
export { campaignApi } from "./campaign";
export { commissionApi } from "./commission";
export { productApi } from "./product";
export { reportApi } from "./report";
export { feedbackApi } from "./feedback";
export { calismaSaatiApi } from "./calismaSaati";
export { auditApi } from "./audit";
export { adminApi } from "./admin";
export { jobApi } from "./job";
export { sectorApi, DEFAULT_SECTOR_LABELS } from "./sector";
export type { BusinessType, SectorLabelDictionary, SectorLabelsResponse, SectorTypeInfo, PositionInfo } from "./sector";
export { unwrap } from "./axios";
export { api };
export default api;

// Type re-exports
export type {
  PlanResponse, QuotaUsageResponse, SubscriptionResponse, SubscriptionStatus,
  InvoiceResponse, CheckoutInitResponse,
} from "./billing";
export type {
  CustomerRequest, CustomerResponse, SegmentType, SegmentSummaryResponse,
  CustomerSegmentResponse, CustomerImportResponse,
} from "./customer";
export type {
  AppointmentRequest, AppointmentUpdateRequest, AppointmentResponse,
  AppointmentServiceItem, RandevuDurumu, RandevuKaynak,
  ProductSaleRequest, ProductSaleResponse,
} from "./appointment";
export type { NotificationResponse } from "./notification";
export type { UserDeviceResponse } from "./userDevice";
export type {
  ServiceRequestBody, ServiceResponseBody, ServiceCategoryRequest, ServiceCategoryResponse,
} from "./service";
export type { UserCreateRequest, UserUpdateRequest } from "./user";
export type { TenantResponse, TenantUpdateRequest, CancellationPolicy } from "./tenant";
export type { BranchRequest, BranchResponse } from "./branch";
export type { ContactFormRequest } from "./publicContact";
export type {
  ConversationResponse, MessageResponse, AssignConversationRequest, SendMessageRequest,
  WhatsappConfigResponse, WhatsappConfigUpdateRequest,
  WhatsappTemplateRequest, WhatsappTemplateResponse,
  ConversationDurum, SenderType, ActiveHandler,
} from "./whatsapp";
export type { AiConfigResponse, AiConfigUpdateRequest } from "./aiConfig";
export type { FeatureResponse } from "./feature";
export type {
  AppointmentReminderResponse, AppointmentReminderRequest, ReminderStatsResponse,
  ReminderTemplateRequest, ReminderTemplateResponse, ReminderKanal, ReminderBirim, ReminderStatus,
} from "./reminder";
export type {
  SlotCampaignResponse, SegmentCampaignRequest, SegmentCampaignResponse,
} from "./campaign";
export type {
  CommissionType, CommissionScope, CommissionRuleRequest, CommissionRuleResponse,
  EarningResponse, EarningsSummaryResponse, EarningPeriodRequest, EarningPeriodResponse,
} from "./commission";
export type { ProductRequest, ProductResponse, ProductSalesSummaryResponse } from "./product";
export type {
  AppointmentReportResponse, RevenueReportResponse, CustomerReportResponse, CampaignReportResponse,
} from "./report";
export type { FeedbackResponse } from "./feedback";
export type { CalismaSaatiEntry } from "./calismaSaati";
export type { AuditLogResponse } from "./audit";
export type { AdminTenantResponse, ContactRequestResponse } from "./admin";
export type { BackgroundJobResponse, DeadLetterJobResponse } from "./job";
