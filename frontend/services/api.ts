/**
 * API Service
 * Handles all API calls with token management
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface ApiResponse<T> {
  data?: T;
  error?: string;
  status: number;
}

/**
 * Get OAuth token from localStorage
 */
const getToken = (): string | null => {
  return localStorage.getItem('oauth_token');
};

/**
 * Save OAuth token to localStorage
 */
export const saveToken = (token: string): void => {
  localStorage.setItem('oauth_token', token);
};

/**
 * Clear OAuth token from localStorage
 */
export const clearToken = (): void => {
  localStorage.removeItem('oauth_token');
};

/**
 * Generic API request handler
 */
async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const token = getToken();

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    const data = await response.json();

    // Check both HTTP status and returnCode field
    const isSuccess = response.ok && (data.returnCode === undefined || data.returnCode === 0);
    const errorMessage = !response.ok
      ? (data.message || data.error || 'Request failed')
      : (data.returnCode === -1 ? (data.returnMsg || data.message || 'Request failed') : undefined);

    return {
      data: isSuccess ? data : undefined,
      error: errorMessage,
      status: response.status,
    };
  } catch (error) {
    return {
      error: error instanceof Error ? error.message : 'Network error',
      status: 0,
    };
  }
}

/**
 * OAuth API
 */
export interface OAuthTokenStatusResponse {
  success: boolean;
  message: string;
  token?: string;
  expiresDt?: string;
  tokenType?: string;
}

export const oauthApi = {
  /**
   * Get OAuth token
   */
  async getToken() {
    return apiRequest<{ token: string; expiresDt: string; success: boolean; message: string }>('/api/v1/oauth/token', {
      method: 'POST',
    });
  },

  /**
   * Get token status
   */
  async getTokenStatus() {
    return apiRequest<OAuthTokenStatusResponse>('/api/v1/oauth/token/status');
  },
};

/**
 * Investor Chart API
 */
export interface InvestorChartRequest {
  stkCd: string;      // 종목코드
  dt: string;         // 일자 (YYYYMMDD)
  amtQtyTp: string;   // 금액수량구분 (1: 금액, 2: 수량)
  trdeTp: string;     // 매매구분 (0: 순매수, 1: 매수, 2: 매도)
  unitTp: string;     // 단위구분 (1: 원/주, 1000: 천원/천주, etc)
}

export interface InvestorChartBatchRequest {
  stkCd: string;
  dateFrom: string;   // YYYYMMDD
  dateTo: string;     // YYYYMMDD
  amtQtyTp: string;
  trdeTp: string;
  unitTp: string;
}

export const investorChartApi = {
  /**
   * Fetch investor chart data for a single date
   */
  async fetch(request: InvestorChartRequest) {
    return apiRequest('/api/v1/investor-chart/fetch', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  /**
   * Fetch with status (saved vs duplicate)
   */
  async fetchWithStatus(request: InvestorChartRequest) {
    return apiRequest('/api/v1/investor-chart/fetch/status', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  /**
   * Fetch RAW (no save) for buffering 5 requests in FE
   */
  async fetchRaw(request: InvestorChartRequest) {
    return apiRequest('/api/v1/investor-chart/fetch/raw', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  /**
   * Fetch recent investor chart data (last 30 days)
   */
  async fetchRecent(stockCode: string) {
    return apiRequest(`/api/v1/investor-chart/fetch/recent/${stockCode}`, {
      method: 'POST',
    });
  },

  /**
   * Fetch recent with status (single-day status)
   */
  async fetchRecentWithStatus(stockCode: string, params?: { amtQtyTp?: string; trdeTp?: string; unitTp?: string }) {
    const q = new URLSearchParams();
    if (params?.amtQtyTp) q.set('amtQtyTp', params.amtQtyTp);
    if (params?.trdeTp) q.set('trdeTp', params.trdeTp);
    if (params?.unitTp) q.set('unitTp', params.unitTp);
    const query = q.toString();
    const endpoint = `/api/v1/investor-chart/fetch/recent/status/${stockCode}${query ? `?${query}` : ''}`;
    return apiRequest(endpoint, { method: 'POST' });
  },

  /**
   * Fetch batch data by date range
   */
  async fetchBatch(request: InvestorChartBatchRequest) {
    return apiRequest('/api/v1/investor-chart/fetch/batch', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  /**
   * Fetch batch with status (counts and date lists)
   */
  async fetchBatchWithStatus(request: InvestorChartBatchRequest) {
    return apiRequest('/api/v1/investor-chart/fetch/batch?withStatus=true', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  /**
   * Save batch from buffered items (5 requests)
   */
  async saveBatch(items: any[]) {
    return apiRequest('/api/v1/investor-chart/save-batch', {
      method: 'POST',
      body: JSON.stringify(items),
    });
  },

  /**
   * Query investor chart data
   */
  async query(stockCode: string, date: string) {
    return apiRequest(`/api/v1/investor-chart/${stockCode}?date=${date}`);
  },

  /**
   * Query by period
   */
  async queryByPeriod(stockCode: string, startDate: string, endDate: string) {
    return apiRequest(`/api/v1/investor-chart/${stockCode}/period?startDate=${startDate}&endDate=${endDate}`);
  },

  /**
   * Fetch KOSPI200 batch with Server-Sent Events
   * Returns an abort function to stop the collection
   */
  fetchKospi200Batch(
    request: { amtQtyTp: string; trdeTp: string; unitTp: string },
    onProgress: (progress: Kospi200ProgressDto) => void,
    onError: (error: string) => void,
    onComplete: () => void
  ): () => void {
    const url = `${API_BASE_URL}/api/v1/investor-chart/fetch/kospi200-batch`;
    const abortController = new AbortController();

    // POST request를 위한 fetch 사용 (EventSource는 GET만 지원)
    // SSE with POST는 fetch를 사용하여 스트림을 읽어야 함
    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
      signal: abortController.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        if (!response.body) {
          throw new Error('Response body is null');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim();
              try {
                const progress: Kospi200ProgressDto = JSON.parse(data);
                onProgress(progress);
                if (progress.completed) {
                  onComplete();
                }
              } catch (e) {
                console.error('Failed to parse SSE data:', e);
              }
            }
          }
        }
      })
      .catch((error) => {
        if (error.name === 'AbortError') {
          console.log('KOSPI200 batch collection aborted by user');
          onError('사용자가 수집을 중단했습니다.');
        } else {
          onError(error.message || 'Failed to fetch KOSPI200 batch');
        }
      });

    // abort 함수 반환
    return () => abortController.abort();
  },
};

/**
 * KOSPI200 Progress DTO
 */
export interface Kospi200ProgressDto {
  currentStockCode: string | null;
  currentStockName: string | null;
  processedCount: number;
  totalCount: number;
  receivedCount: number;      // 현재 종목 수신
  savedCount: number;          // 현재 종목 저장
  duplicateCount: number;      // 현재 종목 중복
  errorCount: number;          // 현재 종목 오류
  cumulativeReceivedCount: number;  // 전체 누적 수신
  cumulativeSavedCount: number;     // 전체 누적 저장
  errors: string[];
  completed: boolean;
}

/**
 * Stock List API
 */
export interface StockListResponse {
  success: boolean;
  message: string;
  totalCount?: number;
  markets?: {
    marketType: string;
    count: number;
  }[];
}

export interface StockSearchResult {
  code: string;
  name: string;
  marketName: string;
  sector?: string;
}

export interface DateRangeResponse {
  startDate: string;
  endDate: string;
}

export const stockListApi = {
  /**
   * Refresh all markets stock list from Kiwoom API
   */
  async refreshAll() {
    const response = await apiRequest<StockListResponse>('/api/v1/stock-list/refresh', {
      method: 'POST',
    });

    return {
      success: response.data?.success || false,
      message: response.data?.message || response.error || '알 수 없는 오류',
      totalCount: response.data?.totalCount || 0,
      markets: response.data?.markets || [],
      error: response.error,
    };
  },

  /**
   * Get stock list by market type
   */
  async getByMarket(marketType: string) {
    return apiRequest<StockListResponse>(`/api/v1/stock-list/market/${marketType}`);
  },

  /**
   * Refresh stock list from Kiwoom API (deprecated, use refreshAll)
   */
  async refresh() {
    return this.refreshAll();
  },

  /**
   * Get all stocks
   */
  async getAll() {
    return apiRequest('/api/v1/stock-list');
  },

  /**
   * Search stocks by name
   */
  async search(keyword: string) {
    return apiRequest<StockSearchResult[]>(`/api/v1/stock-list/search?keyword=${encodeURIComponent(keyword)}`);
  },

  /**
   * Get date range for a stock
   */
  async getDateRange(stockCode: string) {
    return apiRequest<DateRangeResponse>(`/api/v1/investor-chart/${stockCode}/date-range`);
  },

  /**
   * Get KOSPI200 stock list (marketCode = 0)
   */
  async getKospi200Stocks() {
    return apiRequest<StockSearchResult[]>('/api/v1/stock-list/kospi200');
  },
};

/**
 * Analysis API
 */
export const analysisApi = {
  /**
   * 투자자별 수급분석 데이터 조회
   */
  async getSupplyDemand(stkCd: string, startDt?: string, endDt?: string) {
    let query = `stkCd=${stkCd}`;
    if (startDt) query += `&startDt=${startDt}`;
    if (endDt) query += `&endDt=${endDt}`;
    return apiRequest<any[]>(`/api/v1/analysis/supply-demand?${query}`);
  },
};

/**
 * Statistics API
 */
export const statisticsApi = {
  /**
   * 이동평균 계산 (단일 종목)
   */
  async calculateMovingAverage(stkCd: string) {
    return apiRequest<{ success: boolean; message: string; rowCount?: number; elapsedSeconds?: number; updatedMaxDate?: string }>(
      `/api/statistics/moving-average/calculate/${stkCd}`,
      { method: 'POST' }
    );
  },

  /**
   * 상관분석 계산 (단일 종목)
   */
  async calculateCorrelation(stkCd: string) {
    return apiRequest<{ success: boolean; message: string; elapsedSeconds?: number; updatedMaxDate?: string }>(
      `/api/statistics/correlation/calculate/${stkCd}`,
      { method: 'POST' }
    );
  },

  /**
   * 수급분석 계산 (단일 종목)
   */
  async calculateSupplyDemand(stkCd: string) {
    return apiRequest<{ success: boolean; message: string; elapsedSeconds?: number }>(
      `/api/statistics/supply-demand/calculate/${stkCd}`,
      { method: 'POST' }
    );
  },

  /**
   * 이동평균 계산 (전체 종목 - KOSPI200 포함)
   */
  async calculateMovingAverageForKospi200() {
    return apiRequest<{ success: boolean; message: string; rowCount?: number; elapsedSeconds?: number }>(
      '/api/statistics/moving-average/calculate',
      { method: 'POST' }
    );
  },

  /**
   * 상관분석 계산 (전체 종목)
   */
  async calculateCorrelationForKospi200() {
    return apiRequest<{ success: boolean; message: string; elapsedSeconds?: number }>(
      '/api/statistics/correlation/calculate',
      { method: 'POST' }
    );
  },

  /**
   * 수급분석 계산 (전체 종목)
   */
  async calculateSupplyDemandForKospi200() {
    return apiRequest<{ success: boolean; message: string; rowCount?: number; elapsedSeconds?: number }>(
      '/api/statistics/supply-demand/calculate',
      { method: 'POST' }
    );
  },

  /**
   * 이동평균 차트 데이터 조회
   */
  async getMovingAverageChart(stkCd: string, days: number = 120, investors: string = 'frgnr,orgn', period: number = 5, beforeDate?: string) {
    let query = `days=${days}&investors=${investors}&period=${period}`;
    if (beforeDate) query += `&beforeDate=${beforeDate}`;
    return apiRequest<any>( // Type definition omitted for brevity, match component
      `/api/statistics/moving-average/chart/${stkCd}?${query}`
    );
  },

  /**
   * 상관분석 차트 데이터 조회
   */
  async getCorrelationChart(stkCd: string, corrDays: number = 20, limit: number = 1000, beforeDate?: string) {
    let query = `corrDays=${corrDays}&limit=${limit}`;
    if (beforeDate) query += `&beforeDate=${beforeDate}`;
    return apiRequest<any>(
      `/api/statistics/correlation/chart/${stkCd}?${query}`
    );
  },

  /**
   * REQ-006: 투자자별 거래 비중 조회 (1년 고정)
   */
  async getInvestorRatio(stkCd: string) {
    return apiRequest<InvestorRatioResponse>(
      `/api/statistics/investor-ratio/${stkCd}`
    );
  },

  /**
   * 투자자별 이동평균 기반 비중 조회 (기간별 동적)
   */
  async getInvestorRatioMa(stkCd: string, period: number = 20, fromDate?: string, toDate?: string) {
    let query = `period=${period}`;
    if (fromDate) query += `&fromDate=${fromDate}`;
    if (toDate) query += `&toDate=${toDate}`;
    return apiRequest<InvestorRatioMaResponse>(
      `/api/statistics/investor-ratio-ma/${stkCd}?${query}`
    );
  },
};

/**
 * REQ-006: 투자자별 거래 비중 응답 타입 (1년 고정)
 */
export interface InvestorRatioResponse {
  stkCd: string;
  dataCount?: number;
  frgnr?: number;
  orgn?: number;
  ind?: number;
  fnncInvt?: number;
  insrnc?: number;
  invtrt?: number;
  bank?: number;
  etcFnnc?: number;
  penfndEtc?: number;
  samoFund?: number;
  natn?: number;
  etcCorp?: number;
  natfor?: number;
  message?: string;
}

/**
 * 투자자별 이동평균 기반 비중 응답 타입 (기간별 동적)
 */
export interface InvestorRatioMaResponse {
  stkCd: string;
  period?: number;
  fromDate?: string;
  toDate?: string;
  dataCount?: number;
  frgnr?: number;
  orgn?: number;
  fnncInvt?: number;
  insrnc?: number;
  invtrt?: number;
  bank?: number;
  etcFnnc?: number;
  penfndEtc?: number;
  samoFund?: number;
  natn?: number;
  etcCorp?: number;
  natfor?: number;
  message?: string;
}

/**
 * 섹터 정보 타입
 */
export interface SectorInfo {
  sectorCd: string;
  sectorNm: string;
}

/**
 * 섹터별 차트 데이터 포인트 타입
 */
export interface SectorMaChartDataPoint {
  dt: string;
  frgnr?: number;
  orgn?: number;
  fnncInvt?: number;
  insrnc?: number;
  invtrt?: number;
  etcFnnc?: number;
  bank?: number;
  penfndEtc?: number;
  samoFund?: number;
  natn?: number;
  etcCorp?: number;
  natfor?: number;
  indInvsr?: number;
}

/**
 * 섹터별 차트 데이터 응답 타입
 */
export interface SectorMaChartResponse {
  sectorCd: string;
  sectorNm?: string;
  period?: number;
  data: SectorMaChartDataPoint[];
  message?: string;
}

/**
 * 종목 정보 타입 (REQ-004-1)
 */
export interface StockInfo {
  code: string;
  name: string;
}

/**
 * 모든 섹터 차트 응답 타입 (REQ-005)
 */
export interface AllSectorsMaChartResponse {
  sectors: { [key: string]: SectorMaChartResponse };
  message?: string;
}

/**
 * 섹터 MA API
 */
export const sectorMaApi = {
  /**
   * 전체 섹터 목록 조회
   */
  async getAllSectors() {
    return apiRequest<SectorInfo[]>('/api/v1/sector-ma/sectors');
  },

  /**
   * 섹터별 종목 목록 조회 (REQ-004-1)
   */
  async getStocksBySector(sectorCd: string) {
    return apiRequest<StockInfo[]>(`/api/v1/sector-ma/stocks/${sectorCd}`);
  },

  /**
   * 섹터별 차트 데이터 조회
   */
  async getSectorMaChart(
    sectorCd: string,
    days: number = 120,
    investors: string = 'frgnr,orgn',
    period: number = 5,
    beforeDate?: string
  ) {
    let query = `days=${days}&investors=${investors}&period=${period}`;
    if (beforeDate) query += `&beforeDate=${beforeDate}`;
    return apiRequest<SectorMaChartResponse>(
      `/api/v1/sector-ma/chart/${sectorCd}?${query}`
    );
  },

  /**
   * 모든 섹터 차트 데이터 조회 (REQ-005)
   */
  async getAllSectorsChart(
    days: number = 120,
    investors: string = 'frgnr,orgn',
    period: number = 5,
    beforeDate?: string
  ) {
    let query = `days=${days}&investors=${investors}&period=${period}`;
    if (beforeDate) query += `&beforeDate=${beforeDate}`;
    return apiRequest<AllSectorsMaChartResponse>(
      `/api/v1/sector-ma/chart/all?${query}`
    );
  }
};
