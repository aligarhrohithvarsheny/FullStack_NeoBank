// AI Security Models for NeoBank Advanced AI Security System

export interface AiSecurityEvent {
  id: number;
  eventType: string;
  channel: string;
  severity: string;
  riskScore: number;
  sourceEntityId: string;
  sourceEntityType: string;
  title: string;
  description: string;
  detailsJson: string;
  clientIp: string;
  location: string;
  deviceFingerprint: string;
  userAgent: string;
  sessionId: string;
  aiModelVersion: string;
  aiConfidence: number;
  status: string;
  actionTaken: string;
  resolvedBy: string;
  resolvedAt: string;
  resolutionNotes: string;
  createdAt: string;
}

export interface AiThreatScore {
  id: number;
  entityId: string;
  entityType: string;
  overallRiskScore: number;
  loginRiskScore: number;
  transactionRiskScore: number;
  behavioralRiskScore: number;
  deviceRiskScore: number;
  networkRiskScore: number;
  riskLevel: string;
  riskFactors: string;
  lastActivity: string;
  totalEvents: number;
  falsePositives: number;
  confirmedThreats: number;
  isWatchlisted: boolean;
  watchlistReason: string;
  lastEvaluatedAt: string;
  createdAt: string;
}

export interface AiDeviceFingerprint {
  id: number;
  entityId: string;
  entityType: string;
  deviceHash: string;
  deviceType: string;
  browser: string;
  os: string;
  screenResolution: string;
  timezone: string;
  language: string;
  ipAddress: string;
  geoLocation: string;
  isTrusted: boolean;
  trustScore: number;
  loginCount: number;
  lastSeenAt: string;
  firstSeenAt: string;
}

export interface AiSecurityRule {
  id: number;
  ruleName: string;
  ruleCategory: string;
  channel: string;
  description: string;
  conditionJson: string;
  actionType: string;
  severity: string;
  isActive: boolean;
  priority: number;
  hitCount: number;
  lastTriggeredAt: string;
  createdBy: string;
  createdAt: string;
}

export interface AiSecurityDashboard {
  summary: {
    totalEvents24h: number;
    totalEvents7d: number;
    totalEvents30d: number;
    criticalEvents24h: number;
    highEvents24h: number;
    activeThreats: number;
    blockedThreats: number;
    resolvedThreats: number;
    avgRiskScore: number;
  };
  channelDistribution: { [key: string]: number };
  eventTypeDistribution: { [key: string]: number };
  severityDistribution: { [key: string]: number };
  dailyTrend: { date: string; count: number }[];
  highRiskEntities: AiThreatScore[];
  watchlistedEntities: AiThreatScore[];
  riskLevelCounts: { [key: string]: number };
  activeRules: AiSecurityRule[];
  totalRules: number;
  aiInfo: {
    modelVersion: string;
    engine: string;
    capabilities: string[];
    channels: string[];
  };
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
