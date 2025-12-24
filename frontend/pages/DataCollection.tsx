import { Activity, ArrowLeft, Calendar, Database, Download, Loader2, Lock, Search, ShieldAlert, ShieldCheck, StopCircle, X } from 'lucide-react';
import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  investorChartApi,
  statisticsApi,
  stockListApi,
  type DateRangeResponse,
  type InvestorChartBatchRequest,
  type InvestorChartRequest,
  type Kospi200ProgressDto,
  type StockSearchResult
} from '../services/api';

type SelectionMode = 'single-stock' | 'kospi200';

const DataCollection: React.FC = () => {
  const navigate = useNavigate();
  const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));
  const [selectionMode, setSelectionMode] = useState<SelectionMode>('single-stock');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [statusPanel, setStatusPanel] = useState<{ savedDates: string[]; duplicateDates: string[]; errorDates?: string[] } | null>(null);
  const [lastResult, setLastResult] = useState<{
    fromDate?: string;
    toDate?: string;
    requestedCount: number;
    receivedCount: number;
    savedCount: number;
    duplicateCount: number;
    errorCount: number;
    savedDates: string[];
    duplicateDates: string[];
    errorDates: string[];
    mode: 'single' | 'recent' | 'batch';
  } | null>(null);

  // KOSPI200 batch progress state
  const [kospi200Progress, setKospi200Progress] = useState<Kospi200ProgressDto | null>(null);
  const [kospi200TotalStats, setKospi200TotalStats] = useState<{
    totalReceived: number;
    totalSaved: number;
    totalDuplicate: number;
    totalError: number;
  }>({ totalReceived: 0, totalSaved: 0, totalDuplicate: 0, totalError: 0 });

  // KOSPI200 Phase progress state
  const [kospi200PhaseProgress, setKospi200PhaseProgress] = useState<{
    phase: 'collection' | 'statistics' | null;
    collectionComplete: boolean;
    statisticsComplete: boolean;
    currentStatisticsTask: string | null;  // "이동평균", "투자자상관분석", "수급분석"
    currentStatisticsDate: string | null;  // "2025-12-16"
  } | null>(null);

  // KOSPI200 collection control
  const [isKospi200Collecting, setIsKospi200Collecting] = useState(false);
  const kospi200AbortRef = useRef<(() => void) | null>(null);

  // Auth status state
  const [authStatus, setAuthStatus] = useState<'connected' | 'disconnected' | 'expired'>('disconnected');
  const [tokenExpiry, setTokenExpiry] = useState<string | null>(null);

  // Current task and date status
  const [currentTask, setCurrentTask] = useState<string | null>(null);
  const [currentDate, setCurrentDate] = useState<string | null>(null);

  // Token validation on page load
  useEffect(() => {
    checkAuthStatus();
  }, [navigate]);

  const checkAuthStatus = () => {
    const token = localStorage.getItem('oauth_token');
    const tokenExpiryStr = localStorage.getItem('oauth_token_expiry');

    if (!token || !tokenExpiryStr) {
      setAuthStatus('disconnected');
      setTokenExpiry(null);
      setMessage({ type: 'error', text: '로그인이 필요합니다. 홈페이지에서 서버접속을 먼저 해주세요.' });
      setTimeout(() => navigate('/'), 2000);
      return;
    }

    const expiryDate = new Date(tokenExpiryStr);
    const now = new Date();

    if (expiryDate <= now) {
      localStorage.removeItem('oauth_token');
      localStorage.removeItem('oauth_token_expiry');
      setAuthStatus('expired');
      setTokenExpiry(null);
      setMessage({ type: 'error', text: '토큰이 만료되었습니다. 홈페이지에서 재접속해주세요.' });
      setTimeout(() => navigate('/'), 2000);
      return;
    }

    setAuthStatus('connected');
    setTokenExpiry(tokenExpiryStr);
  };

  // Stock search states
  const [stockInput, setStockInput] = useState('');
  const [selectedStock, setSelectedStock] = useState<{ code: string; name: string; marketName?: string; sector?: string } | null>(null);
  const [searchResults, setSearchResults] = useState<StockSearchResult[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [dateRange, setDateRange] = useState<DateRangeResponse | null>(null);
  const searchRef = useRef<HTMLDivElement>(null);
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);;

  // Helper: check if string is 6-digit number

  // Helper: check if string is 6-digit number
  const isNumericCode = (str: string) => /^\d{6}$/.test(str.trim());

  // Debounced Search Effect
  useEffect(() => {
    const timer = setTimeout(() => {
      // 검색어가 있고, 선택된 상태가 아니고, 6자리 코드가 아닐 때 검색
      if (stockInput && !selectedStock && !isNumericCode(stockInput)) {
        searchStocks(stockInput);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [stockInput, selectedStock]);

  // Click Outside to close dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Handle 6-digit code input directly
  useEffect(() => {
    if (isNumericCode(stockInput) && !selectedStock) {
      // If 6 digits, try to find or fetch directly?
      // For simplicity, search it. Use unique search to get details.
      searchStocks(stockInput);
    }
  }, [stockInput]);

  // Single date form
  const [singleForm, setSingleForm] = useState<InvestorChartRequest>({
    stkCd: '',
    dt: new Date().toISOString().split('T')[0].replace(/-/g, ''),
    amtQtyTp: '2', // 수량 (default)
    trdeTp: '0',   // 순매수 (default)
    unitTp: '1',   // 원/주 (default)
  });

  // Batch form
  const [batchForm, setBatchForm] = useState<InvestorChartBatchRequest>({
    stkCd: '',
    dateFrom: '',
    dateTo: new Date().toISOString().split('T')[0].replace(/-/g, ''),
    amtQtyTp: '2', // 수량 (default)
    trdeTp: '0',   // 순매수 (default)
    unitTp: '1',   // 원/주 (default)
  });

  // Recent fetch stock code
  const [recentStockCode, setRecentStockCode] = useState('');

  // Single stock progress state
  const [singleStockProgress, setSingleStockProgress] = useState<{
    phase: 'past' | 'recent' | 'statistics' | null;
    currentDate: string;
    totalRequests: number;
    receivedCount: number;
    savedCount: number;
    duplicateCount: number;
    errorCount: number;
    cumulativeReceived: number;
    cumulativeSaved: number;
    cumulativeDuplicate: number;
    cumulativeError: number;
    pastPhaseComplete: boolean;
    recentPhaseComplete: boolean;
    statisticsPhaseComplete: boolean;
    currentStatisticsTask: string | null;  // 예: "이동평균"
    currentStatisticsDate: string | null;  // 예: "2025-12-02"
    errors: string[];
  } | null>(null);

  /**
   * Handle stock search (확인 button)
   */
  /**
   * Search Stocks (Async)
   */
  const searchStocks = async (keyword: string) => {
    if (!keyword.trim()) return;

    setIsSearching(true);
    setFocusedIndex(-1); // Reset focus on new search
    try {
      const result = await stockListApi.search(keyword.trim());
      if (result.data) {
        setSearchResults(result.data);
        setShowDropdown(true);

        // If exact 6-digit match (and only one?), maybe select auto?
        // MovingAverageChart selects automatically if numeric code match.
        if (isNumericCode(keyword) && result.data.length === 1 && result.data[0].code === keyword) {
          selectStock(result.data[0]);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setIsSearching(false);
    }
  };

  // Scroll to focused item
  useEffect(() => {
    if (showDropdown && focusedIndex >= 0 && itemRefs.current[focusedIndex]) {
      itemRefs.current[focusedIndex]?.scrollIntoView({
        block: 'nearest',
      });
    }
  }, [focusedIndex, showDropdown]);

  // Handle Keyboard Navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.nativeEvent.isComposing) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setFocusedIndex(prev => (prev + 1) % searchResults.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setFocusedIndex(prev => (prev - 1 + searchResults.length) % searchResults.length);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (searchResults.length > 0) {
        const index = focusedIndex >= 0 ? focusedIndex : 0;
        selectStock(searchResults[index]);
      }
    } else if (e.key === 'Escape') {
      setShowDropdown(false);
    }
  };

  /**
   * Clear Selection
   */
  const handleClearSelection = () => {
    setStockInput('');
    setSelectedStock(null);
    setSearchResults([]);
    setShowDropdown(false);
    setFocusedIndex(-1);
    setDateRange(null);
    setSingleForm(prev => ({ ...prev, stkCd: '' }));
    setBatchForm(prev => ({ ...prev, stkCd: '' }));
  };

  /**
   * Select stock and load date range
   */
  const selectStock = async (stock: StockSearchResult) => {
    setSelectedStock({
      code: stock.code,
      name: stock.name,
      marketName: stock.marketName,
      sector: stock.sector
    });
    setStockInput(stock.name);
    setShowDropdown(false);

    // Load date range for this stock
    const dateRangeResult = await stockListApi.getDateRange(stock.code);
    if (dateRangeResult.data) {
      setDateRange(dateRangeResult.data);
    } else {
      setDateRange(null);
    }

    setMessage(null);
  };

  /**
   * Format date for display (YYYY-MM-DD)
   */
  const formatDateDisplay = (dateStr: string) => {
    if (!dateStr) return '';
    // Handle both "YYYY-MM-DD" and LocalDate formats
    const date = new Date(dateStr);
    return date.toISOString().split('T')[0];
  };

  /**
   * Format date for progress display (YYYYMMDD -> YYYY-MM-DD)
   */
  const formatDateForDisplay = (dateStr: string): string => {
    if (!dateStr || dateStr.length !== 8) return dateStr;
    return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
  };

  /**
   * Handle full data collection (Phase 1: Past + Phase 2: Recent)
   */
  const handleFullDataCollection = async () => {
    if (!selectedStock) {
      setMessage({ type: 'error', text: '종목을 선택해주세요.' });
      return;
    }

    setLoading(true);
    setMessage(null);
    setCurrentTask('데이터 수집 준비 중...');
    setCurrentDate(null);

    // ========================================
    // 진행 상황 초기화 (처음부터 화면에 표시)
    // ========================================
    let currentDt: string;
    if (dateRange) {
      const dbStartDate = new Date(dateRange.startDate);
      dbStartDate.setDate(dbStartDate.getDate() - 1);
      currentDt = dbStartDate.toISOString().split('T')[0].replace(/-/g, '');
    } else {
      // 데이터가 없으면 오늘부터 시작
      currentDt = new Date().toISOString().split('T')[0].replace(/-/g, '');
    }

    // 진행 상황 화면을 처음부터 표시
    setSingleStockProgress({
      phase: 'past',
      currentDate: currentDt,
      totalRequests: 0,
      receivedCount: 0,
      savedCount: 0,
      duplicateCount: 0,
      errorCount: 0,
      cumulativeReceived: 0,
      cumulativeSaved: 0,
      cumulativeDuplicate: 0,
      cumulativeError: 0,
      pastPhaseComplete: false,
      recentPhaseComplete: false,
      statisticsPhaseComplete: false,
      currentStatisticsTask: null,
      currentStatisticsDate: null,
      errors: [],
    });

    let totalRequestCount = 0;
    let totalReceivedCount = 0;
    let totalSavedCount = 0;
    let totalDuplicateCount = 0;
    let totalErrorCount = 0;
    const allSavedDates: string[] = [];
    const allDuplicateDates: string[] = [];
    const allErrorDates: string[] = [];

    try {

      // ========================================
      // Phase 1: 과거 데이터 수집
      // ========================================
      console.log('=== Phase 1: 과거 데이터 수집 시작 ===');

      const MIN_DATE = '20000101';
      let shouldContinuePast = true;

      while (shouldContinuePast) {
        setCurrentTask('과거 데이터 수집 (Phase 1)');
        setCurrentDate(currentDt);
        totalRequestCount++;

        const requestForm = {
          stkCd: selectedStock.code,
          dt: currentDt,
          amtQtyTp: '2',
          trdeTp: '0',
          unitTp: '1',
        };

        const result = await investorChartApi.fetchWithStatus(requestForm);

        if (result.error) {
          console.error(`[과거] 오류: ${result.error}`);
          totalErrorCount++;
          allErrorDates.push(`${currentDt}: ${result.error}`);

          setSingleStockProgress(prev => prev ? {
            ...prev,
            errorCount: prev.errorCount + 1,
            cumulativeError: totalErrorCount,
            errors: [...prev.errors, `${currentDt}: ${result.error}`],
          } : null);

          break;
        }

        if (result.data) {
          const savedDates = ((result.data as any).savedDates || []).map((d: any) => d.toString());
          const duplicateDates = ((result.data as any).duplicateDates || []).map((d: any) => d.toString());
          const receivedThisRound = savedDates.length + duplicateDates.length;

          totalReceivedCount += receivedThisRound;
          totalSavedCount += savedDates.length;
          totalDuplicateCount += duplicateDates.length;
          allSavedDates.push(...savedDates);
          allDuplicateDates.push(...duplicateDates);

          setSingleStockProgress(prev => prev ? {
            ...prev,
            currentDate: currentDt,
            totalRequests: totalRequestCount,
            receivedCount: receivedThisRound,
            savedCount: savedDates.length,
            duplicateCount: duplicateDates.length,
            cumulativeReceived: totalReceivedCount,
            cumulativeSaved: totalSavedCount,
            cumulativeDuplicate: totalDuplicateCount,
            cumulativeError: totalErrorCount,
          } : null);

          const allDates = [...savedDates, ...duplicateDates];

          if (allDates.length === 0 || currentDt <= MIN_DATE) {
            console.log('[과거] 종료: 데이터 없음 또는 최소 날짜 도달');
            shouldContinuePast = false;
          } else {
            allDates.sort();
            const oldestReceivedDate = allDates[0];
            const next = new Date(oldestReceivedDate.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3'));
            next.setDate(next.getDate() - 1);
            currentDt = next.toISOString().split('T')[0].replace(/-/g, '');
          }
        } else {
          shouldContinuePast = false;
        }

        if (totalRequestCount % 5 === 0 || !shouldContinuePast) {
          let currentStartDate = dateRange?.startDate || ''; // Fallback

          if (selectedStock) {
            const updatedDateRange = await stockListApi.getDateRange(selectedStock.code);
            if (updatedDateRange.data) {
              setDateRange(updatedDateRange.data);
              currentStartDate = updatedDateRange.data.startDate;
            }
          }

          setLastResult({
            fromDate: currentDt,
            toDate: currentStartDate,
            requestedCount: totalRequestCount,
            receivedCount: totalReceivedCount,
            savedCount: totalSavedCount,
            duplicateCount: totalDuplicateCount,
            errorCount: totalErrorCount,
            savedDates: allSavedDates.slice(),
            duplicateDates: allDuplicateDates.slice(),
            errorDates: allErrorDates.slice(),
            mode: 'single',
          });

          if (shouldContinuePast) {
            await sleep(2000);
          }
        }

        if (totalRequestCount >= 200) {
          console.warn('[과거] 최대 요청 횟수 도달');
          break;
        }
      }

      setSingleStockProgress(prev => prev ? {
        ...prev,
        pastPhaseComplete: true,
        phase: 'recent',
      } : null);

      console.log('=== Phase 1 완료 ===');

      // ========================================
      // Phase 2: 최신 데이터 수집
      // ========================================
      console.log('=== Phase 2: 최신 데이터 수집 시작 ===');

      const updatedRange = await stockListApi.getDateRange(selectedStock.code);
      const latestEndDate = updatedRange.data?.endDate || dateRange?.endDate || '20000101';
      const latestSavedYmd = latestEndDate.replace(/-/g, '');

      currentDt = new Date().toISOString().split('T')[0].replace(/-/g, '');
      let shouldContinueRecent = true;

      while (shouldContinueRecent) {
        setCurrentTask('최신 데이터 수집 (Phase 2)');
        setCurrentDate(currentDt);
        totalRequestCount++;

        const requestForm = {
          stkCd: selectedStock.code,
          dt: currentDt,
          amtQtyTp: '2',
          trdeTp: '0',
          unitTp: '1',
        };

        const result = await investorChartApi.fetchWithStatus(requestForm);

        if (result.error) {
          console.error(`[최신] 오류: ${result.error}`);
          totalErrorCount++;
          allErrorDates.push(`${currentDt}: ${result.error}`);

          setSingleStockProgress(prev => prev ? {
            ...prev,
            errorCount: prev.errorCount + 1,
            cumulativeError: totalErrorCount,
            errors: [...prev.errors, `${currentDt}: ${result.error}`],
          } : null);

          break;
        }

        if (result.data) {
          const savedDates = ((result.data as any).savedDates || []).map((d: any) => d.toString());
          const duplicateDates = ((result.data as any).duplicateDates || []).map((d: any) => d.toString());
          const receivedThisRound = savedDates.length + duplicateDates.length;

          totalReceivedCount += receivedThisRound;
          totalSavedCount += savedDates.length;
          totalDuplicateCount += duplicateDates.length;
          allSavedDates.push(...savedDates);
          allDuplicateDates.push(...duplicateDates);

          setSingleStockProgress(prev => prev ? {
            ...prev,
            currentDate: currentDt,
            totalRequests: totalRequestCount,
            receivedCount: receivedThisRound,
            savedCount: savedDates.length,
            duplicateCount: duplicateDates.length,
            cumulativeReceived: totalReceivedCount,
            cumulativeSaved: totalSavedCount,
            cumulativeDuplicate: totalDuplicateCount,
            cumulativeError: totalErrorCount,
          } : null);

          const allDates = [...savedDates, ...duplicateDates];

          if (allDates.length === 0) {
            console.log('[최신] 종료: 데이터 없음');
            shouldContinueRecent = false;
          } else {
            allDates.sort();
            const oldestReceivedDate = allDates[0];

            if (oldestReceivedDate <= latestSavedYmd) {
              console.log(`[최신] 종료: 기존 데이터와 연결됨`);
              shouldContinueRecent = false;
            } else {
              const next = new Date(oldestReceivedDate.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3'));
              next.setDate(next.getDate() - 1);
              currentDt = next.toISOString().split('T')[0].replace(/-/g, '');
            }
          }
        } else {
          shouldContinueRecent = false;
        }

        if (totalRequestCount % 5 === 0 || !shouldContinueRecent) {
          if (selectedStock) {
            const updatedDateRange = await stockListApi.getDateRange(selectedStock.code);
            if (updatedDateRange.data) {
              setDateRange(updatedDateRange.data);
            }
          }

          if (dateRange) {
            setLastResult({
              fromDate: dateRange.startDate,
              toDate: currentDt,
              requestedCount: totalRequestCount,
              receivedCount: totalReceivedCount,
              savedCount: totalSavedCount,
              duplicateCount: totalDuplicateCount,
              errorCount: totalErrorCount,
              savedDates: allSavedDates.slice(),
              duplicateDates: allDuplicateDates.slice(),
              errorDates: allErrorDates.slice(),
              mode: 'recent',
            });
          }

          if (shouldContinueRecent) {
            await sleep(2000);
          }
        }

        if (totalRequestCount >= 200) {
          console.warn('[최신] 최대 요청 횟수 도달');
          break;
        }
      }

      setSingleStockProgress(prev => prev ? {
        ...prev,
        recentPhaseComplete: true,
      } : null);

      console.log('=== Phase 2 완료 ===');

    } catch (error) {
      console.error('전체 데이터 수집 중 예외 발생:', error);
      setMessage({ type: 'error', text: `데이터 수집 중 오류가 발생했습니다: ${error}` });
    } finally {
      // ========================================
      // Phase 3: Perform Batch Jobs (이동평균, 상관분석, 수급분석)
      // 데이터 수집 성공/실패 여부와 관계없이 무조건 실행
      // ========================================
      console.log('=== Phase 3: Perform Batch Jobs for Single Stock (무조건 실행) ===');

      try {
        if (selectedStock) {
          // Phase 3 시작 - 통계DB작업
          const todayDate = new Date().toISOString().split('T')[0];  // YYYY-MM-DD 형식
          setSingleStockProgress(prev => prev ? {
            ...prev,
            phase: 'statistics',
            currentStatisticsTask: null,
            currentStatisticsDate: null,
          } : null);

          // Call 이동평균 API
          setCurrentTask('이동평균 계산 (Batch)');
          setCurrentDate(null);
          setMessage({ type: 'success', text: '데이터 수집 완료. 이동평균 계산 중...' });
          setSingleStockProgress(prev => prev ? {
            ...prev,
            currentStatisticsTask: '이동평균',
            currentStatisticsDate: todayDate,
          } : null);
          await statisticsApi.calculateMovingAverage(selectedStock.code);
          console.log('이동평균 계산 완료');

          // Call 상관분석 API
          setCurrentTask('상관분석 계산 (Batch)');
          setMessage({ type: 'success', text: '이동평균 완료. 상관분석 계산 중...' });
          setSingleStockProgress(prev => prev ? {
            ...prev,
            currentStatisticsTask: '투자자상관분석',
            currentStatisticsDate: todayDate,
          } : null);
          await statisticsApi.calculateCorrelation(selectedStock.code);
          console.log('상관분석 계산 완료');

          // Call 수급분석 API
          setCurrentTask('수급분석 계산 (Batch)');
          setMessage({ type: 'success', text: '상관분석 완료. 수급분석 계산 중...' });
          setSingleStockProgress(prev => prev ? {
            ...prev,
            currentStatisticsTask: '수급분석',
            currentStatisticsDate: todayDate,
          } : null);
          await statisticsApi.calculateSupplyDemand(selectedStock.code);
          console.log('수급분석 계산 완료');

          // Phase 3 완료
          setSingleStockProgress(prev => prev ? {
            ...prev,
            statisticsPhaseComplete: true,
            currentStatisticsTask: null,
            currentStatisticsDate: null,
          } : null);

          setCurrentTask('모든 작업 완료');
          setMessage({
            type: 'success',
            text: `${selectedStock.name} 전체 데이터 수집 및 통계 분석이 완료되었습니다!`
          });
        }
      } catch (error) {
        console.error('단일 종목 배치 작업 중 오류 발생:', error);
        totalErrorCount++;
        setMessage({ type: 'error', text: '통계 분석 작업 중 오류가 발생했습니다.' });
        setCurrentTask('작업 오류 발생');
      } finally {
        // 통계 API 완료 후 정리 작업
        setLoading(false);

        if (selectedStock) {
          const finalDateRange = await stockListApi.getDateRange(selectedStock.code);
          if (finalDateRange.data) {
            setDateRange(finalDateRange.data);
          }
        }

        setStatusPanel({
          savedDates: allSavedDates,
          duplicateDates: allDuplicateDates,
          errorDates: allErrorDates
        });

        setLastResult({
          requestedCount: totalRequestCount,
          receivedCount: totalReceivedCount,
          savedCount: totalSavedCount,
          duplicateCount: totalDuplicateCount,
          errorCount: totalErrorCount,
          savedDates: allSavedDates,
          duplicateDates: allDuplicateDates,
          errorDates: allErrorDates,
          mode: 'recent',
        });

        // 진행 상황 화면을 계속 유지 (제거하지 않음)
        // 사용자가 다른 작업을 시작하면 자동으로 초기화됨
      }
    }
  };

  const handleKospi200Submit = () => {
    console.log('⚠️⚠️⚠️ handleKospi200Submit 호출됨! (이 로그가 여러 번 나오면 문제!)');

    if (isKospi200Collecting) {
      console.error('❌ 이미 수집 중입니다! 중복 호출 차단');
      return;
    }

    setLoading(true);
    setIsKospi200Collecting(true);
    setMessage(null);
    setCurrentTask('KOSPI200 데이터 수집 준비');
    setCurrentDate(null);
    setKospi200Progress(null);
    setKospi200TotalStats({ totalReceived: 0, totalSaved: 0, totalDuplicate: 0, totalError: 0 });

    // Initialize KOSPI200 Phase progress
    setKospi200PhaseProgress({
      phase: 'collection',
      collectionComplete: false,
      statisticsComplete: false,
      currentStatisticsTask: null,
      currentStatisticsDate: null,
    });

    console.log('🚀 KOSPI200 수집 시작');

    // 통계 분석 작업을 실행하는 헬퍼 함수 (데이터 수집 성공/실패 관계없이 무조건 실행)
    const runKospi200StatisticsJobs = async (errorCount: number) => {
      try {
        // Phase 2 시작 - 통계DB작업
        const todayDate = new Date().toISOString().split('T')[0];
        setKospi200PhaseProgress(prev => prev ? {
          ...prev,
          phase: 'statistics',
          currentStatisticsTask: null,
          currentStatisticsDate: null,
        } : null);

        // 이동평균
        setCurrentTask('이동평균 계산 (KOSPI200)');
        setMessage({ type: 'success', text: '이동평균 계산 중...' });
        setKospi200PhaseProgress(prev => prev ? {
          ...prev,
          currentStatisticsTask: '이동평균',
          currentStatisticsDate: todayDate,
        } : null);
        await statisticsApi.calculateMovingAverageForKospi200();
        console.log('이동평균 계산 완료');

        // 상관분석
        setCurrentTask('상관분석 계산 (KOSPI200)');
        setMessage({ type: 'success', text: '이동평균 완료. 상관분석 진행 중...' });
        setKospi200PhaseProgress(prev => prev ? {
          ...prev,
          currentStatisticsTask: '투자자상관분석',
          currentStatisticsDate: todayDate,
        } : null);
        await statisticsApi.calculateCorrelationForKospi200();
        console.log('상관분석 계산 완료');

        // 수급분석
        setCurrentTask('수급분석 계산 (KOSPI200)');
        setMessage({ type: 'success', text: '상관분석 완료. 수급분석 진행 중...' });
        setKospi200PhaseProgress(prev => prev ? {
          ...prev,
          currentStatisticsTask: '수급분석',
          currentStatisticsDate: todayDate,
        } : null);
        await statisticsApi.calculateSupplyDemandForKospi200();
        console.log('수급분석 계산 완료');

        // Phase 2 완료
        setKospi200PhaseProgress(prev => prev ? {
          ...prev,
          statisticsComplete: true,
          currentStatisticsTask: null,
          currentStatisticsDate: null,
        } : null);

        setCurrentTask('모든 작업 완료');

        if (errorCount > 0) {
          setMessage({ type: 'error', text: `KOSPI200 배치 완료 (수집 오류: ${errorCount}건)` });
        } else {
          setMessage({ type: 'success', text: 'KOSPI200 배치 수집 및 분석이 완료되었습니다!' });
        }
      } catch (err) {
        console.error('Batch Job Error:', err);
        setMessage({ type: 'error', text: '통계 분석 배치 작업 중 오류가 발생했습니다.' });
        setCurrentTask('작업 오류 발생');
      } finally {
        setLoading(false);
        setIsKospi200Collecting(false);
        kospi200AbortRef.current = null;
        // Don't clear currentTask immediately so user can see 'Complete'
      }
    };

    // abort 함수를 저장
    const abortFn = investorChartApi.fetchKospi200Batch(
      {
        amtQtyTp: '2',
        trdeTp: '0',
        unitTp: '1',
      },
      (progress) => {
        console.log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
        console.log(`📨 Progress 수신`);
        console.log(`  종목: ${progress.currentStockName} (${progress.currentStockCode})`);
        console.log(`  진행: ${progress.processedCount}/${progress.totalCount}`);
        console.log(`  현재 종목: 수신=${progress.receivedCount}, 저장=${progress.savedCount}`);
        console.log(`  전체 누적: 수신=${progress.cumulativeReceivedCount}, 저장=${progress.cumulativeSavedCount}`);

        setCurrentTask(`KOSPI200 데이터 수집: ${progress.currentStockName}`);
        // progress doesn't provide date, so leave it null or use today
        setCurrentDate(null);

        // progress를 그대로 표시 (현재 종목의 통계)
        setKospi200Progress(progress);

        // 누적 통계 (백엔드에서 계산된 값 그대로 사용)
        const totalStats = {
          totalReceived: progress.cumulativeReceivedCount,  // 백엔드 값 직접 사용
          totalSaved: progress.cumulativeSavedCount,        // 백엔드 값 직접 사용
          totalDuplicate: progress.duplicateCount,
          totalError: progress.errors.length,
        };

        console.log(`  🎯 화면 업데이트:`);
        console.log(`     현재 종목: 수신=${progress.receivedCount}, 저장=${progress.savedCount}`);
        console.log(`     전체 누적: 수신=${totalStats.totalReceived}, 저장=${totalStats.totalSaved}`);

        setKospi200TotalStats(totalStats);

        // 완료 시 처리 (성공적으로 완료된 경우)
        if (progress.completed) {
          console.log(`\n🎉🎉🎉 수집 완료!`);
          console.log(`   최종 누적: 수신=${progress.cumulativeReceivedCount}, 저장=${progress.cumulativeSavedCount}`);

          // Phase 1 완료 표시
          setKospi200PhaseProgress(prev => prev ? {
            ...prev,
            collectionComplete: true,
          } : null);

          setMessage({ type: 'success', text: '데이터 수집 완료. 통계 분석 작업을 시작합니다...' });

          // 통계 분석 작업 실행 (무조건 실행)
          runKospi200StatisticsJobs(progress.errors.length);
        }
      },
      (error) => {
        console.error('❌ 에러 발생:', error);

        // Phase 1 완료 표시 (에러가 발생해도 완료로 처리)
        setKospi200PhaseProgress(prev => prev ? {
          ...prev,
          collectionComplete: true,
        } : null);

        setMessage({ type: 'error', text: `KOSPI200 배치 수집 중 오류 발생. 통계 분석을 시작합니다...` });

        // 에러가 발생해도 통계 분석 작업 실행 (무조건 실행)
        runKospi200StatisticsJobs(0);
      },
      () => {
        console.log('🛑 수집 중단');
        setLoading(false);
        setIsKospi200Collecting(false);
        kospi200AbortRef.current = null;
      }
    );

    kospi200AbortRef.current = abortFn;
  };

  const handleKospi200Stop = () => {
    if (kospi200AbortRef.current) {
      kospi200AbortRef.current();
      setLoading(false);
      setIsKospi200Collecting(false);
      setMessage({ type: 'error', text: '수집이 중단되었습니다.' });
      kospi200AbortRef.current = null;
    }
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <Link
            to="/"
            className="inline-flex items-center gap-2 text-gray-600 hover:text-blue-600 transition-colors mb-4"
          >
            <ArrowLeft className="w-4 h-4" />
            <span className="text-sm font-medium">홈으로 돌아가기</span>
          </Link>

          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Database className="w-6 h-6 text-blue-600" />
            </div>
            <h1 className="text-3xl font-bold text-gray-900">투자자 차트 데이터 수집</h1>
          </div>
          <p className="text-gray-600 mt-2">
            일별 개인, 외국인, 기관세부(투신, 은행, 연금 등)별 순매수 데이터를 수집합니다.
          </p>
        </div>

        {/* Auth Status Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex flex-col md:flex-row items-center gap-6 w-full md:w-auto">
              <div className="flex items-center gap-2">
                <Activity className="w-6 h-6 text-gray-700" />
                <h2 className="text-[25px] font-bold text-gray-900 whitespace-nowrap">
                  인증 상태
                </h2>
              </div>
              <div className="flex-shrink-0">
                {authStatus === 'connected' ? (
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2 text-[#16a34a] bg-green-50 px-3 py-1 rounded-md border border-green-100">
                      <ShieldCheck className="w-6 h-6" />
                      <span className="font-bold">인증 성공</span>
                    </div>
                    {tokenExpiry && (
                      <p className="text-xs text-gray-500 ml-1">
                        만료: {new Date(tokenExpiry).toLocaleString('ko-KR')}
                      </p>
                    )}
                  </div>
                ) : authStatus === 'expired' ? (
                  <div className="flex items-center gap-2 text-[#dc2626] bg-red-50 px-3 py-1 rounded-md border border-red-100">
                    <ShieldAlert className="w-6 h-6" />
                    <span className="font-bold">토큰 만료</span>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 text-gray-400 bg-gray-50 px-3 py-1 rounded-md border border-gray-200">
                    <Lock className="w-5 h-5" />
                    <span className="font-medium">연결 대기</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Message Alert */}
        {message && (
          <div
            className={`mb-6 p-4 rounded-lg ${message.type === 'success'
              ? 'bg-green-50 border border-green-200 text-green-800'
              : 'bg-red-50 border border-red-200 text-red-800'
              }`}
          >
            {message.text}
          </div>
        )}

        {/* Stock Selection Mode */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          {/* Single Stock Search Section */}
          <div className={`bg-white rounded-xl shadow-sm border-2 p-6 transition-all ${selectionMode === 'single-stock'
            ? 'border-blue-500 ring-2 ring-blue-200'
            : 'border-gray-200'
            }`}>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-900">종목 선택</h2>
              <input
                type="radio"
                name="selectionMode"
                checked={selectionMode === 'single-stock'}
                onChange={() => setSelectionMode('single-stock')}
                className="w-4 h-4"
              />
            </div>

            {/* Inline Search UI */}
            <div className="relative mb-6" ref={searchRef}>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="text"
                  placeholder={selectionMode === 'single-stock' ? "종목명 또는 코드(6자리) 입력..." : "KOSPI200 모드에서는 종목 선택 불필요"}
                  value={stockInput}
                  onChange={(e) => {
                    setStockInput(e.target.value);
                    if (selectedStock) setSelectedStock(null); // Allow re-search
                    setFocusedIndex(-1);
                    if (!e.target.value) {
                      setSearchResults([]);
                      setShowDropdown(false);
                    }
                  }}
                  onKeyDown={handleKeyDown}
                  onFocus={() => {
                    if (searchResults.length > 0 && selectionMode === 'single-stock') setShowDropdown(true);
                  }}
                  disabled={selectionMode !== 'single-stock'}
                  className="w-full pl-10 pr-10 py-3 bg-white border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all disabled:bg-gray-100 disabled:cursor-not-allowed"
                />

                {/* Right Icons: Loader or Clear */}
                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                  {isSearching && (
                    <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
                  )}
                  {(stockInput || selectedStock) && selectionMode === 'single-stock' && (
                    <button onClick={handleClearSelection} className="text-gray-400 hover:text-gray-600">
                      <X className="w-5 h-5" />
                    </button>
                  )}
                </div>
              </div>

              {/* Dropdown */}
              {showDropdown && searchResults.length > 0 && selectionMode === 'single-stock' && (
                <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-xl max-h-60 overflow-y-auto">
                  {searchResults.map((stock, i) => (
                    <button
                      key={stock.code}
                      ref={el => itemRefs.current[i] = el}
                      onClick={() => selectStock(stock)}
                      className={`w-full px-4 py-3 text-left flex items-center justify-between border-b border-gray-50 last:border-0 transition-colors ${i === focusedIndex ? 'bg-blue-50 ring-1 ring-inset ring-blue-500' : 'hover:bg-gray-50'
                        }`}
                    >
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-gray-900">{stock.name}</span>
                        <span className="text-sm text-gray-500">{stock.code}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs px-2 py-1 bg-gray-100 rounded text-gray-600">{stock.marketName}</span>
                        {stock.sector && (
                          <span className="text-xs px-2 py-1 bg-blue-50 rounded text-blue-600">{stock.sector}</span>
                        )}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Date Range Display */}
            {/* Enhanced Information Card */}
            {selectedStock && selectionMode === 'single-stock' && (
              <div className="mb-6 bg-white rounded-xl shadow-sm border border-blue-200 p-5 relative overflow-hidden">
                <div className="absolute top-0 right-0 p-4 opacity-10">
                  <Database className="w-24 h-24 text-blue-600" />
                </div>

                <div className="relative z-10">
                  <div className="flex items-center gap-3 mb-3">
                    <h3 className="text-xl font-bold text-gray-900">{selectedStock.name}</h3>
                    <span className="text-base text-gray-500 font-medium">{selectedStock.code}</span>
                    {selectedStock.marketName && (
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                        {selectedStock.marketName}
                      </span>
                    )}
                    {selectedStock.sector && (
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        {selectedStock.sector}
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 text-sm text-gray-600 bg-gray-50 p-3 rounded-lg border border-gray-100 inline-flex">
                    <Calendar className="w-4 h-4 text-blue-500" />
                    <span>데이터 범위: </span>
                    {dateRange ? (
                      <span className="font-semibold text-gray-900">
                        {formatDateDisplay(dateRange.startDate)} ~ {formatDateDisplay(dateRange.endDate)}
                      </span>
                    ) : (
                      <span className="text-gray-400 italic">저장된 데이터 없음</span>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Full Data Collection Button */}
            {selectedStock && selectionMode === 'single-stock' && (
              <div className="mt-4">
                <button
                  onClick={handleFullDataCollection}
                  disabled={loading}
                  className="w-full py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
                >
                  <Download className="w-5 h-5" />
                  {loading ? '수집 중...' : `${selectedStock.name} 전체데이터 가져오기`}
                </button>
              </div>
            )}
          </div>

          {/* KOSPI200 Selection Section */}
          <div className={`bg-white rounded-xl shadow-sm border-2 p-6 transition-all ${selectionMode === 'kospi200'
            ? 'border-blue-500 ring-2 ring-blue-200'
            : 'border-gray-200'
            }`}>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-900">KOSPI200 선택</h2>
              <input
                type="radio"
                name="selectionMode"
                checked={selectionMode === 'kospi200'}
                onChange={() => setSelectionMode('kospi200')}
                className="w-4 h-4"
              />
            </div>

            <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-blue-800 mb-2">
                <strong>KOSPI200 전체 종목</strong> 배치 수집
              </p>
              <p className="text-xs text-blue-700">
                코스피 시장의 모든 종목을 순차적으로 수집합니다.
                <br />
                종목간 5초 대기로 안정적인 수집이 진행됩니다.
              </p>
            </div>

            {selectionMode === 'kospi200' && (
              <div className="mt-4">
                {!isKospi200Collecting ? (
                  <button
                    onClick={handleKospi200Submit}
                    disabled={loading}
                    className="w-full py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
                  >
                    <Download className="w-5 h-5" />
                    {loading ? 'KOSPI200 수집 중...' : 'KOSPI200 전체 수집 시작'}
                  </button>
                ) : (
                  <button
                    onClick={handleKospi200Stop}
                    className="w-full py-3 bg-red-600 text-white font-semibold rounded-lg hover:bg-red-700 transition-colors flex items-center justify-center gap-2"
                  >
                    <StopCircle className="w-5 h-5" />
                    수신중단
                  </button>
                )}
              </div>
            )}
          </div>
        </div>



        {/* Single Stock Progress Display - 종목선택 모드일 때 항상 표시 */}
        {selectionMode === 'single-stock' && (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">진행상황</h2>

            {/* Current Task Status */}
            {(currentTask || currentDate) && (
              <div className="mb-4 p-4 bg-indigo-50 border border-indigo-200 rounded-lg shadow-sm">
                {currentTask && (
                  <div className="mb-2">
                    <div className="text-xs text-indigo-600 font-semibold mb-1 uppercase tracking-wide">Current Task</div>
                    <div className="text-xl font-bold text-indigo-900">{currentTask}</div>
                  </div>
                )}
                {currentDate && (
                  <div className="flex items-center gap-2 mt-2 pt-2 border-t border-indigo-100">
                    <Calendar className="w-4 h-4 text-indigo-500" />
                    <span className="text-sm font-medium text-indigo-800">
                      Processing Date: <span className="font-bold">{formatDateForDisplay(currentDate)}</span>
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* Current Phase Indicator */}
            {singleStockProgress && (
              <div className="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <p className="text-sm text-blue-800 mb-1">현재 단계</p>
                <p className="text-lg font-semibold text-blue-900">
                  {singleStockProgress.phase === 'past'
                    ? 'Phase 1: 과거 데이터 수집'
                    : singleStockProgress.phase === 'recent'
                      ? 'Phase 2: 최신 데이터 수집'
                      : 'Phase 3: 통계DB작업'}
                </p>
                {/* 통계 작업 진행 중일 때 작업명-날짜 표시 */}
                {singleStockProgress.phase === 'statistics' && singleStockProgress.currentStatisticsTask && singleStockProgress.currentStatisticsDate && (
                  <p className="text-md font-medium text-blue-700 mt-2">
                    {singleStockProgress.currentStatisticsTask}-{singleStockProgress.currentStatisticsDate}
                  </p>
                )}
              </div>
            )}

            {/* Phase Progress Indicators */}
            <div className="grid grid-cols-3 gap-4 mb-4">
              <div className={`p-3 rounded-lg text-center ${
                singleStockProgress?.pastPhaseComplete
                  ? 'bg-green-100 border-2 border-green-500'
                  : singleStockProgress?.phase === 'past'
                    ? 'bg-blue-100 border-2 border-blue-500'
                    : 'bg-gray-100 border-2 border-gray-300'
              }`}>
                <div className="text-sm font-semibold text-gray-900">Phase 1: 과거 데이터</div>
                <div className="text-xs text-gray-600 mt-1">
                  {singleStockProgress?.pastPhaseComplete ? '✓ 완료' : singleStockProgress?.phase === 'past' ? '진행 중...' : '대기 중'}
                </div>
              </div>
              <div className={`p-3 rounded-lg text-center ${
                singleStockProgress?.recentPhaseComplete
                  ? 'bg-green-100 border-2 border-green-500'
                  : singleStockProgress?.phase === 'recent'
                    ? 'bg-blue-100 border-2 border-blue-500'
                    : 'bg-gray-100 border-2 border-gray-300'
              }`}>
                <div className="text-sm font-semibold text-gray-900">Phase 2: 최신 데이터</div>
                <div className="text-xs text-gray-600 mt-1">
                  {singleStockProgress?.recentPhaseComplete ? '✓ 완료' : singleStockProgress?.phase === 'recent' ? '진행 중...' : '대기 중'}
                </div>
              </div>
              <div className={`p-3 rounded-lg text-center ${
                singleStockProgress?.statisticsPhaseComplete
                  ? 'bg-green-100 border-2 border-green-500'
                  : singleStockProgress?.phase === 'statistics'
                    ? 'bg-blue-100 border-2 border-blue-500'
                    : 'bg-gray-100 border-2 border-gray-300'
              }`}>
                <div className="text-sm font-semibold text-gray-900">Phase 3: 통계DB작업</div>
                <div className="text-xs text-gray-600 mt-1">
                  {singleStockProgress?.statisticsPhaseComplete ? '✓ 완료' : singleStockProgress?.phase === 'statistics' ? '진행 중...' : '대기 중'}
                </div>
              </div>
            </div>

            {/* Current Round Stats */}
            <div className="grid grid-cols-4 gap-4 mb-4">
              <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-center">
                <div className="text-xs text-blue-700 mb-1">요청횟수</div>
                <div className="text-2xl font-bold text-blue-900">{singleStockProgress?.totalRequests || 0}</div>
              </div>
              <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-center">
                <div className="text-xs text-green-700 mb-1">현재수신</div>
                <div className="text-2xl font-bold text-green-900">{singleStockProgress?.receivedCount || 0}</div>
              </div>
              <div className="p-3 bg-purple-50 border border-purple-200 rounded-lg text-center">
                <div className="text-xs text-purple-700 mb-1">현재저장</div>
                <div className="text-2xl font-bold text-purple-900">{singleStockProgress?.savedCount || 0}</div>
              </div>
              <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-center">
                <div className="text-xs text-yellow-700 mb-1">현재중복</div>
                <div className="text-2xl font-bold text-yellow-900">{singleStockProgress?.duplicateCount || 0}</div>
              </div>
            </div>

            {/* Cumulative Stats */}
            <div className="grid grid-cols-4 gap-4 mb-4">
              <div className="p-3 bg-blue-100 border border-blue-300 rounded-lg text-center">
                <div className="text-xs text-blue-800 mb-1">누적수신</div>
                <div className="text-xl font-bold text-blue-900">{singleStockProgress?.cumulativeReceived || 0}</div>
              </div>
              <div className="p-3 bg-green-100 border border-green-300 rounded-lg text-center">
                <div className="text-xs text-green-800 mb-1">누적저장</div>
                <div className="text-xl font-bold text-green-900">{singleStockProgress?.cumulativeSaved || 0}</div>
              </div>
              <div className="p-3 bg-yellow-100 border border-yellow-300 rounded-lg text-center">
                <div className="text-xs text-yellow-800 mb-1">누적중복</div>
                <div className="text-xl font-bold text-yellow-900">{singleStockProgress?.cumulativeDuplicate || 0}</div>
              </div>
              <div className="p-3 bg-red-100 border border-red-300 rounded-lg text-center">
                <div className="text-xs text-red-800 mb-1">누적오류</div>
                <div className="text-xl font-bold text-red-900">{singleStockProgress?.cumulativeError || 0}</div>
              </div>
            </div>

            {/* Data Range */}
            {singleStockProgress?.currentDate && (
              <div className="mb-4 p-3 bg-gray-50 border border-gray-200 rounded-lg">
                <div className="text-xs text-gray-700 mb-1">현재 처리 중인 날짜</div>
                <div className="text-lg font-semibold text-gray-900">
                  {formatDateForDisplay(singleStockProgress.currentDate)}
                </div>
              </div>
            )}

            {/* Errors */}
            {singleStockProgress?.errors && singleStockProgress.errors.length > 0 && (
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg max-h-60 overflow-y-auto mb-4">
                <h4 className="text-sm font-semibold text-red-800 mb-2">
                  오류 내역 ({singleStockProgress.errors.length}건)
                </h4>
                <ul className="space-y-1 text-xs text-red-900">
                  {singleStockProgress.errors.map((error: any, idx: any) => (
                    <li key={idx} className="border-b border-red-100 pb-1">{error}</li>
                  ))}
                </ul>
              </div>
            )}

            {/* Completion Message */}
            {singleStockProgress?.pastPhaseComplete && singleStockProgress?.recentPhaseComplete && (
              <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-center">
                <p className="text-sm font-semibold text-green-800">
                  ✓ 전체 데이터 수집이 완료되었습니다!
                </p>
              </div>
            )}
          </div>
        )}

        {/* KOSPI200 Progress Display */}
        {selectionMode === 'kospi200' && (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">진행상황</h2>

            {/* Current Task Status (Batch) */}
            {(currentTask) && (
              <div className="mb-4 p-4 bg-indigo-50 border border-indigo-200 rounded-lg shadow-sm">
                <div className="mb-1">
                  <div className="text-xs text-indigo-600 font-semibold mb-1 uppercase tracking-wide">Current Task</div>
                  <div className="text-xl font-bold text-indigo-900">{currentTask}</div>
                </div>
              </div>
            )}

            {/* Current Phase Indicator - KOSPI200 선택 시 항상 표시 */}
            <div className="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-blue-800 mb-1">현재 단계</p>
              <p className="text-lg font-semibold text-blue-900">
                {!kospi200PhaseProgress
                  ? '대기 중'
                  : kospi200PhaseProgress.phase === 'collection'
                    ? 'Phase 1: 데이터 수집'
                    : 'Phase 2: 통계DB작업'}
              </p>
              {/* 통계 작업 진행 중일 때 작업명-날짜 표시 */}
              {kospi200PhaseProgress?.phase === 'statistics' && kospi200PhaseProgress.currentStatisticsTask && kospi200PhaseProgress.currentStatisticsDate && (
                <p className="text-md font-medium text-blue-700 mt-2">
                  {kospi200PhaseProgress.currentStatisticsTask}-{kospi200PhaseProgress.currentStatisticsDate}
                </p>
              )}
            </div>

            {/* Phase Progress Indicators - KOSPI200 선택 시 항상 표시 */}
            <div className="grid grid-cols-2 gap-4 mb-4">
              <div className={`p-3 rounded-lg text-center ${
                kospi200PhaseProgress?.collectionComplete
                  ? 'bg-green-100 border-2 border-green-500'
                  : kospi200PhaseProgress?.phase === 'collection'
                    ? 'bg-blue-100 border-2 border-blue-500'
                    : 'bg-gray-100 border-2 border-gray-300'
              }`}>
                <div className="text-sm font-semibold text-gray-900">Phase 1: 데이터 수집</div>
                <div className="text-xs text-gray-600 mt-1">
                  {kospi200PhaseProgress?.collectionComplete ? '✓ 완료' : kospi200PhaseProgress?.phase === 'collection' ? '진행 중...' : '대기 중'}
                </div>
              </div>
              <div className={`p-3 rounded-lg text-center ${
                kospi200PhaseProgress?.statisticsComplete
                  ? 'bg-green-100 border-2 border-green-500'
                  : kospi200PhaseProgress?.phase === 'statistics'
                    ? 'bg-blue-100 border-2 border-blue-500'
                    : 'bg-gray-100 border-2 border-gray-300'
              }`}>
                <div className="text-sm font-semibold text-gray-900">Phase 2: 통계DB작업</div>
                <div className="text-xs text-gray-600 mt-1">
                  {kospi200PhaseProgress?.statisticsComplete ? '✓ 완료' : kospi200PhaseProgress?.phase === 'statistics' ? '진행 중...' : '대기 중'}
                </div>
              </div>
            </div>

            {kospi200Progress ? (
              <>
                {/* Current Stock */}
                {kospi200Progress.currentStockName && (
                  <div className="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-sm text-blue-800 mb-1">현재 처리 중인 종목</p>
                    <p className="text-lg font-semibold text-blue-900">
                      {kospi200Progress.currentStockName} ({kospi200Progress.currentStockCode})
                    </p>
                  </div>
                )}

                {/* Progress Bar */}
                <div className="mb-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-700">진행 상황</span>
                    <span className="text-sm font-semibold text-blue-600">
                      {kospi200Progress.processedCount} / {kospi200Progress.totalCount}
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-4 overflow-hidden">
                    <div
                      className="bg-blue-600 h-4 transition-all duration-300"
                      style={{ width: `${(kospi200Progress.processedCount / kospi200Progress.totalCount) * 100}%` }}
                    />
                  </div>
                  <div className="text-xs text-gray-500 mt-1 text-right">
                    {((kospi200Progress.processedCount / kospi200Progress.totalCount) * 100).toFixed(1)}% 완료
                  </div>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-4 gap-4 mb-4">
                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-center">
                    <div className="text-xs text-blue-700 mb-1">현재종목수신건수</div>
                    <div className="text-2xl font-bold text-blue-900">{kospi200Progress.receivedCount}</div>
                  </div>
                  <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-center">
                    <div className="text-xs text-green-700 mb-1">현재종목저장건수</div>
                    <div className="text-2xl font-bold text-green-900">{kospi200Progress.savedCount}</div>
                  </div>
                  <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-center">
                    <div className="text-xs text-yellow-700 mb-1">현재 종목 중복</div>
                    <div className="text-2xl font-bold text-yellow-900">{kospi200Progress.duplicateCount}</div>
                  </div>
                  <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-center">
                    <div className="text-xs text-red-700 mb-1">현재 종목 오류</div>
                    <div className="text-2xl font-bold text-red-900">{kospi200Progress.errorCount}</div>
                  </div>
                </div>

                {/* Cumulative Stats */}
                <div className="grid grid-cols-4 gap-4 mb-4">
                  <div className="p-3 bg-blue-100 border border-blue-300 rounded-lg text-center">
                    <div className="text-xs text-blue-800 mb-1">누적수신건수</div>
                    <div className="text-xl font-bold text-blue-900">{kospi200TotalStats.totalReceived}</div>
                  </div>
                  <div className="p-3 bg-green-100 border border-green-300 rounded-lg text-center">
                    <div className="text-xs text-green-800 mb-1">누적저장건수</div>
                    <div className="text-xl font-bold text-green-900">{kospi200TotalStats.totalSaved}</div>
                  </div>
                  <div className="p-3 bg-yellow-100 border border-yellow-300 rounded-lg text-center">
                    <div className="text-xs text-yellow-800 mb-1">누적 중복</div>
                    <div className="text-xl font-bold text-yellow-900">{kospi200TotalStats.totalDuplicate}</div>
                  </div>
                  <div className="p-3 bg-red-100 border border-red-300 rounded-lg text-center">
                    <div className="text-xs text-red-800 mb-1">누적 오류</div>
                    <div className="text-xl font-bold text-red-900">{kospi200TotalStats.totalError}</div>
                  </div>
                </div>

                {/* Errors */}
                {kospi200Progress.errors && kospi200Progress.errors.length > 0 && (
                  <div className="p-4 bg-red-50 border border-red-200 rounded-lg max-h-60 overflow-y-auto">
                    <h4 className="text-sm font-semibold text-red-800 mb-2">
                      오류 내역 ({kospi200Progress.errors.length}건)
                    </h4>
                    <ul className="space-y-1 text-xs text-red-900">
                      {kospi200Progress.errors.map((error, idx) => (
                        <li key={idx} className="border-b border-red-100 pb-1">{error}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {kospi200Progress.completed && (
                  <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg text-center">
                    <p className="text-sm font-semibold text-green-800">
                      ✓ KOSPI200 배치 수집이 완료되었습니다!
                    </p>
                  </div>
                )}
              </>
            ) : (
              <>
                {/* Stats - Waiting State */}
                <div className="grid grid-cols-4 gap-4 mb-4">
                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-center">
                    <div className="text-xs text-blue-700 mb-1">현재종목수신건수</div>
                    <div className="text-2xl font-bold text-blue-900">0</div>
                  </div>
                  <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-center">
                    <div className="text-xs text-green-700 mb-1">현재종목저장건수</div>
                    <div className="text-2xl font-bold text-green-900">0</div>
                  </div>
                  <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-center">
                    <div className="text-xs text-yellow-700 mb-1">현재 종목 중복</div>
                    <div className="text-2xl font-bold text-yellow-900">0</div>
                  </div>
                  <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-center">
                    <div className="text-xs text-red-700 mb-1">현재 종목 오류</div>
                    <div className="text-2xl font-bold text-red-900">0</div>
                  </div>
                </div>

                {/* Cumulative Stats - Waiting State */}
                <div className="grid grid-cols-4 gap-4">
                  <div className="p-3 bg-blue-100 border border-blue-300 rounded-lg text-center">
                    <div className="text-xs text-blue-800 mb-1">누적수신건수</div>
                    <div className="text-xl font-bold text-blue-900">0</div>
                  </div>
                  <div className="p-3 bg-green-100 border border-green-300 rounded-lg text-center">
                    <div className="text-xs text-green-800 mb-1">누적저장건수</div>
                    <div className="text-xl font-bold text-green-900">0</div>
                  </div>
                  <div className="p-3 bg-yellow-100 border border-yellow-300 rounded-lg text-center">
                    <div className="text-xs text-yellow-800 mb-1">누적 중복</div>
                    <div className="text-xl font-bold text-yellow-900">0</div>
                  </div>
                  <div className="p-3 bg-red-100 border border-red-300 rounded-lg text-center">
                    <div className="text-xs text-red-800 mb-1">누적 오류</div>
                    <div className="text-xl font-bold text-red-900">0</div>
                  </div>
                </div>
              </>
            )}
          </div>
        )}


        {/* Info Box */}
        <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h4 className="font-semibold text-blue-900 mb-2">처리 결과</h4>
          {lastResult ? (
            <div className="space-y-3">
              <div className="text-sm text-blue-900">
                데이터 수집 날짜: {lastResult.fromDate || '-'} ~ {lastResult.toDate || '-'}
              </div>
              <div className="grid grid-cols-2 md:grid-cols-6 gap-2 text-xs">
                <InfoPill label="요청" value={lastResult.requestedCount} />
                <InfoPill label="수신" value={lastResult.receivedCount} />
                <InfoPill label="저장" value={lastResult.savedCount} highlight="green" />
                <InfoPill label="중복" value={lastResult.duplicateCount} highlight="yellow" />
                <InfoPill label="오류" value={lastResult.errorCount} highlight="red" />
                <InfoPill label="모드" value={lastResult.mode} />
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <ScrollableList title="저장된 데이터" items={lastResult.savedDates} color="green" />
                <ScrollableList title="중복 데이터" items={lastResult.duplicateDates} color="yellow" />
                <ScrollableList title="오류 데이터" items={lastResult.errorDates} color="red" />
              </div>
            </div>
          ) : (
            <div className="text-sm text-blue-800">
              아직 처리된 결과가 없습니다. 구간/단일 수집을 실행하면 결과가 표시됩니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DataCollection;

// ===== 요약 Pill =====
const InfoPill: React.FC<{ label: string; value: any; highlight?: 'green' | 'yellow' | 'red' }>
  = ({ label, value, highlight }) => {
    const color = highlight === 'green' ? 'text-green-800 bg-green-50 border-green-200'
      : highlight === 'yellow' ? 'text-yellow-800 bg-yellow-50 border-yellow-200'
        : highlight === 'red' ? 'text-red-800 bg-red-50 border-red-200'
          : 'text-blue-800 bg-white border-blue-200';
    return (
      <div className={`px-2 py-1 border rounded-lg text-center ${color}`}>
        <div className="text-[11px] font-medium">{label}</div>
        <div className="text-sm font-semibold">{String(value)}</div>
      </div>
    );
  };

// ===== 스크롤 리스트 =====
const ScrollableList: React.FC<{ title: string; items: string[]; color?: 'yellow' | 'red' | 'green' }>
  = ({ title, items, color }) => {
    const headerColor = color === 'yellow' ? 'text-yellow-800'
      : color === 'red' ? 'text-red-800'
        : color === 'green' ? 'text-green-800'
          : 'text-gray-800';
    const borderColor = color === 'yellow' ? 'border-yellow-200'
      : color === 'red' ? 'border-red-200'
        : color === 'green' ? 'border-green-200'
          : 'border-gray-200';
    const bgColor = color === 'yellow' ? 'bg-yellow-50'
      : color === 'red' ? 'bg-red-50'
        : color === 'green' ? 'bg-green-50'
          : 'bg-gray-50';
    return (
      <div className={`p-3 rounded-lg ${bgColor} border ${borderColor} max-h-48 overflow-y-auto`}>
        <div className={`text-sm font-semibold ${headerColor} mb-2`}>{title} ({items.length})</div>
        {items.length === 0 ? (
          <div className="text-xs text-gray-600">해당 데이터가 없습니다.</div>
        ) : (
          <ul className="space-y-1 text-xs text-gray-800">
            {items.map((d) => (<li key={d}>{d}</li>))}
          </ul>
        )}
      </div>
    );
  };

