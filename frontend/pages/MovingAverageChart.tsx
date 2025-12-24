import { createChart, IChartApi, ISeriesApi, LineData, LineSeries, Time } from 'lightweight-charts';
import { Clock, Search, TrendingUp, X } from 'lucide-react';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { statisticsApi, stockListApi } from '../services/api';

interface MaChartDataPoint {
    dt: string;
    curPrc?: number;
    frgnr: number;
    orgn: number;
    fnncInvt: number;
    insrnc: number;
    invtrt: number;
    etcFnnc: number;
    bank: number;
    penfndEtc: number;
    samoFund: number;
    natn: number;
    etcCorp: number;
    natfor: number;
}

interface RawDataPoint {
    dt: string;
    curPrc?: number;
    frgnr?: number;
    orgn?: number;
    indInvsr?: number;
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
}

interface ChartResponse {
    stkCd: string;
    sector: string | null;
    period: number;
    data: RawDataPoint[];
    message: string;
}

interface TooltipData {
    time: string;
    values: {
        key: string;
        label: string;
        value: number;
        color: string;
    }[];
}

interface StockSearchResult {
    stdPdno: string;
    code: string;
    name: string;
    marketName: string;
    sector: string | null;
}

// 투자자 유형 정의
const INVESTOR_TYPES = [
    { key: 'frgnr', label: '외국인', color: '#EF4444' }, // Red
    { key: 'orgn', label: '기관계', color: '#3B82F6' }, // Blue
    { key: 'ind_invsr', label: '개인', color: '#22C55E' }, // Green
    { key: 'fnnc_invt', label: '금융투자', color: '#10B981' }, // Emerald
    { key: 'insrnc', label: '보험', color: '#F59E0B' }, // Amber
    { key: 'invtrt', label: '투신', color: '#6366F1' }, // Indigo
    { key: 'etc_fnnc', label: '기타금융', color: '#8B5CF6' }, // Violet
    { key: 'bank', label: '은행', color: '#EC4899' }, // Pink
    { key: 'penfnd_etc', label: '연기금등', color: '#14B8A6' }, // Teal
    { key: 'samo_fund', label: '사모펀드', color: '#F97316' }, // Orange
    { key: 'natn', label: '국가', color: '#64748B' }, // Slate
    { key: 'etc_corp', label: '기타법인', color: '#84CC16' }, // Lime
    { key: 'natfor', label: '내국인', color: '#A855F7' }, // Purple
];

const MA_PERIODS = [5, 10, 20, 30, 40, 50, 60, 90, 120, 140];

// 실제로 백그라운드에서 로드할 데이터 양 (많이 로드, 사용자에게 보이지 않음)
// 일단 보수적으로 시작해서 DB에 데이터가 충분한지 확인
const MA_LOAD_DAYS: Record<number, number> = {
    5: 750,    // MA5: 3년 치 데이터를 미리 로드
    10: 750,   // MA10: 3년 치 데이터를 미리 로드
    20: 1000,  // MA20: 4년 치 데이터를 미리 로드
    30: 1000,
    40: 1000,
    50: 1000,
    60: 1000,  // MA60: 4년 치 데이터를 미리 로드
    90: 1500,
    120: 1500,
    140: 1500,
};

// 초기 화면에 표시할 데이터 범위 (화면에 실제로 보이는 부분)
const MA_VISIBLE_DAYS: Record<number, number> = {
    5: 250,    // MA5: 1년만 화면에 표시
    10: 375,   // MA10: 1.5년만 화면에 표시
    20: 500,   // MA20: 2년만 화면에 표시
    30: 500,
    40: 500,
    50: 500,
    60: 500,   // MA60: 2년만 화면에 표시
    90: 750,
    120: 750,
    140: 750,
};

// 추가 로드 시 프리페칭을 위한 임계값 (로드된 전체 범위의 20%에 도달하면 미리 로드)
const PREFETCH_THRESHOLD = 0.2;

interface Props {
    stkCd?: string;
    hideSearch?: boolean;
}

const MovingAverageChart: React.FC<Props> = ({ stkCd: propStkCd, hideSearch }) => {
    const [searchParams] = useSearchParams();
    const initialStkCd = propStkCd || searchParams.get('stkCd') || '';

    // 차트 관련 refs
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const subChartRef = useRef<IChartApi | null>(null); // 상관분석 차트 Ref
    const seriesMapRef = useRef<Map<string, ISeriesApi<'Line'>>>(new Map());
    const priceSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);
    const allDataRef = useRef<RawDataPoint[]>([]);
    const isLoadingMoreRef = useRef(false);
    const oldestDateRef = useRef<string | null>(null);
    const hasMoreRef = useRef(true); // 더 가져올 데이터가 있는지 여부

    // 검색 관련 상태
    const [searchKeyword, setSearchKeyword] = useState(initialStkCd);
    const [searchResults, setSearchResults] = useState<StockSearchResult[]>([]);
    const [showDropdown, setShowDropdown] = useState(false);
    const [isSearching, setIsSearching] = useState(false);
    const searchRef = useRef<HTMLDivElement>(null);

    // 선택된 종목
    const [selectedStock, setSelectedStock] = useState<StockSearchResult | null>(null);
    const [stkCd, setStkCd] = useState('');
    const [stockName, setStockName] = useState('');

    // 차트 관련 상태
    const [period, setPeriod] = useState(20); // 기본값 20일
    const [selectedInvestors, setSelectedInvestors] = useState(['frgnr', 'orgn']);
    const [showPrice, setShowPrice] = useState(true); // 현재가 표시 여부
    const [sector, setSector] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [tooltipData, setTooltipData] = useState<TooltipData | null>(null);
    const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });
    const [isSyncing, setIsSyncing] = useState(false); // 동기화 진행 상태
    const [syncError, setSyncError] = useState<string | null>(null);
    const lastRequestedRawDateRef = useRef<string | null>(null); // guard to avoid duplicate sync requests
    const selectedInvestorsRef = useRef(selectedInvestors); // To access latest state in event handler
    const [noDataWarning, setNoDataWarning] = useState<string | null>(null); // NULL 데이터 경고

    // REQ-007: 투자자별 거래 비중 (UI 통일)
    const [investorRatios, setInvestorRatios] = useState<{ [key: string]: number }>({});

    // Update ref when state changes
    useEffect(() => {
        selectedInvestorsRef.current = selectedInvestors;
    }, [selectedInvestors]);

    // Prop으로 전달된 stkCd가 변경되면 내부 상태 업데이트 및 데이터 조회
    useEffect(() => {
        if (propStkCd && propStkCd !== stkCd) {
            setStkCd(propStkCd);
            setSearchKeyword(propStkCd); // 키워드 동기화는 선택적
            fetchChartData(propStkCd);
        }
    }, [propStkCd]); // stkCd dependency removed to avoid infinite loop mixed with fetchChartData logic if not careful, 
    // but here we only react to prop change.

    // YYYYMMDD를 YYYY-MM-DD로 변환
    const formatDateForChart = (dateStr: string): string => {
        return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
    };

    // 투자자 키를 데이터 키로 변환
    const getDataKey = (investorKey: string): keyof RawDataPoint => {
        const keyMap: Record<string, keyof RawDataPoint> = {
            'frgnr': 'frgnr',
            'orgn': 'orgn',
            'ind_invsr': 'indInvsr',
            'fnnc_invt': 'fnncInvt',
            'insrnc': 'insrnc',
            'invtrt': 'invtrt',
            'etc_fnnc': 'etcFnnc',
            'bank': 'bank',
            'penfnd_etc': 'penfndEtc',
            'samo_fund': 'samoFund',
            'natn': 'natn',
            'etc_corp': 'etcCorp',
            'natfor': 'natfor',
        };
        return keyMap[investorKey] || 'frgnr';
    };

    // 데이터를 시리즈 형식으로 변환 (정렬 및 중복 제거)
    const prepareSeriesData = useCallback((investorKey: string): LineData<Time>[] => {
        const dataKey = getDataKey(investorKey);

        try {
            let seriesData: LineData<Time>[] = allDataRef.current
                .filter(d => {
                    // null, undefined 체크
                    const value = d[dataKey];
                    if (value === null || value === undefined) return false;

                    // 날짜 유효성 체크
                    if (!d.dt || d.dt.length !== 8) return false;

                    return true;
                })
                .map(d => {
                    const value = d[dataKey];
                    // number 타입으로 안전하게 변환 (BigDecimal이 number로 파싱됨)
                    const numValue = typeof value === 'number' ? value : parseFloat(String(value));

                    return {
                        time: formatDateForChart(d.dt) as Time,
                        value: isNaN(numValue) ? 0 : numValue,
                    };
                });

            // 날짜 오름차순 정렬
            seriesData.sort((a, b) => (a.time as string).localeCompare(b.time as string));

            // 중복 날짜 제거
            const uniqueData = seriesData.filter((item, index, self) =>
                index === self.findIndex((t) => t.time === item.time)
            );

            console.log(`[prepareSeriesData] ${investorKey}: ${uniqueData.length}개 데이터 포인트 준비 완료`);
            return uniqueData;
        } catch (err) {
            console.error(`[prepareSeriesData] ${investorKey} 데이터 변환 실패: `, err);
            return [];
        }
    }, []);

    // 현재가 시리즈 데이터 준비
    const preparePriceSeriesData = useCallback((): LineData<Time>[] => {
        try {
            let seriesData: LineData<Time>[] = allDataRef.current
                .filter(d => {
                    if (d.curPrc === null || d.curPrc === undefined) return false;
                    if (!d.dt || d.dt.length !== 8) return false;
                    return true;
                })
                .map(d => ({
                    time: formatDateForChart(d.dt) as Time,
                    value: d.curPrc || 0,
                }));

            // 날짜 오름차순 정렬
            seriesData.sort((a, b) => (a.time as string).localeCompare(b.time as string));

            // 중복 날짜 제거
            const uniqueData = seriesData.filter((item, index, self) =>
                index === self.findIndex((t) => t.time === item.time)
            );

            console.log(`[preparePriceSeriesData] 현재가: ${uniqueData.length}개 데이터 포인트 준비 완료`);
            return uniqueData;
        } catch (err) {
            console.error(`[preparePriceSeriesData] 현재가 데이터 변환 실패: `, err);
            return [];
        }
    }, []);

    // 시리즈만 업데이트 (재생성하지 않음)
    const updateSeriesData = useCallback(() => {
        if (!chartRef.current || allDataRef.current.length === 0) return;

        console.log(`[updateSeriesData] 데이터 업데이트 - 총 ${allDataRef.current.length}개 데이터`);

        // 기존 시리즈에 데이터만 업데이트
        seriesMapRef.current.forEach((series, investorKey) => {
            const seriesData = prepareSeriesData(investorKey);
            try {
                series.setData(seriesData);
                console.log(`  ✓ ${investorKey}: ${seriesData.length}개 데이터 포인트`);
            } catch (e) {
                console.error(`시리즈 데이터 업데이트 실패[${investorKey}]: `, e);
            }
        });

        // 현재가 시리즈 업데이트
        if (priceSeriesRef.current) {
            const priceData = preparePriceSeriesData();
            try {
                priceSeriesRef.current.setData(priceData);
                console.log(`  ✓ 현재가: ${priceData.length}개 데이터 포인트`);
            } catch (e) {
                console.error(`현재가 시리즈 데이터 업데이트 실패: `, e);
            }
        }
    }, [prepareSeriesData, preparePriceSeriesData]);

    // 시리즈 생성/제거 (투자자 선택 변경 시)
    const recreateSeries = useCallback((shouldSetVisibleRange = true) => {
        if (!chartRef.current) return;

        console.log('[recreateSeries] 시리즈 재생성 시작');

        // 기존 시리즈 모두 제거
        seriesMapRef.current.forEach((series) => {
            chartRef.current?.removeSeries(series);
        });
        seriesMapRef.current.clear();

        if (allDataRef.current.length === 0) {
            console.log('[recreateSeries] 데이터 없음 - 시리즈 생성 건너뜀');
            return;
        }

        // NULL 데이터 경고 초기화
        setNoDataWarning(null);
        let hasAnyData = false;
        let noDataInvestors: string[] = [];

        // 선택된 투자자 유형에 대해 시리즈 추가
        selectedInvestors.forEach((investorKey) => {
            const investor = INVESTOR_TYPES.find(i => i.key === investorKey);
            if (!investor) return;

            const series = chartRef.current!.addSeries(LineSeries, {
                color: investor.color,
                lineWidth: 2,
                title: investor.label,
                priceFormat: {
                    type: 'custom',
                    formatter: (price: number) => {
                        if (Math.abs(price) >= 1000000) {
                            return (price / 1000000).toFixed(1) + 'M';
                        } else if (Math.abs(price) >= 1000) {
                            return (price / 1000).toFixed(0) + 'K';
                        }
                        return price.toFixed(0);
                    },
                },
            });

            const seriesData = prepareSeriesData(investorKey);

            if (seriesData.length === 0) {
                console.warn(`  ⚠ ${investor.label}: 데이터 없음(필터링 후 0개)`);
                chartRef.current?.removeSeries(series);
                noDataInvestors.push(investor.label);
                return;
            }

            hasAnyData = true;

            try {
                series.setData(seriesData);
                seriesMapRef.current.set(investorKey, series);
                console.log(`  ✓ ${investor.label}: ${seriesData.length}개 데이터 포인트 로드됨`);

                // 첫 3개 데이터 샘플 출력
                if (seriesData.length > 0) {
                    console.log(`    첫 데이터: ${JSON.stringify(seriesData[0])} `);
                    if (seriesData.length > 1) {
                        console.log(`    마지막 데이터: ${JSON.stringify(seriesData[seriesData.length - 1])} `);
                    }
                }
            } catch (e) {
                console.error(`  ✗ ${investor.label} 시리즈 데이터 설정 실패: `, e);
                console.error(`    데이터 샘플: `, seriesData.slice(0, 3));
            }
        });

        // 현재가 시리즈 생성/제거
        if (priceSeriesRef.current) {
            chartRef.current.removeSeries(priceSeriesRef.current);
            priceSeriesRef.current = null;
        }

        if (showPrice) {
            const priceSeries = chartRef.current.addSeries(LineSeries, {
                color: '#000000', // 검정색
                lineWidth: 2,
                title: '현재가',
                priceScaleId: 'left', // 왼쪽 Y축 사용
                priceFormat: {
                    type: 'custom',
                    formatter: (price: number) => {
                        return price.toLocaleString() + '원';
                    },
                },
            });

            const priceData = preparePriceSeriesData();
            if (priceData.length > 0) {
                try {
                    priceSeries.setData(priceData);
                    priceSeriesRef.current = priceSeries;
                    console.log(`  ✓ 현재가: ${priceData.length}개 데이터 포인트 로드됨(왼쪽 Y축)`);
                } catch (e) {
                    console.error(`  ✗ 현재가 시리즈 데이터 설정 실패: `, e);
                    chartRef.current.removeSeries(priceSeries);
                }
            } else {
                console.warn(`  ⚠ 현재가: 데이터 없음`);
                chartRef.current.removeSeries(priceSeries);
            }
        }

        // 초기 로드 시 표시 범위 설정
        if (shouldSetVisibleRange && allDataRef.current.length > 0) {
            try {
                const visibleDays = MA_VISIBLE_DAYS[period]; // 250, 375, 500
                const totalDataLength = allDataRef.current.length;

                console.log(`[recreateSeries] 전체 ${totalDataLength}개 데이터 로드 완료`);
                console.log(`[recreateSeries] 화면에는 최근 ${visibleDays}일만 표시(나머지는 팬으로 볼 수 있음)`);

                // 최근 N일의 날짜 범위 계산
                const startIndex = Math.max(0, totalDataLength - visibleDays);
                const startDate = allDataRef.current[startIndex].dt; // "20231201"
                const endDate = allDataRef.current[totalDataLength - 1].dt; // "20251205"

                // 날짜를 차트 형식으로 변환
                const startTime = formatDateForChart(startDate) as string; // "2023-12-01"
                const endTime = formatDateForChart(endDate) as string; // "2025-12-05"

                console.log(`[recreateSeries] 표시 범위: ${startTime} ~${endTime} (인덱스 ${startIndex} ~${totalDataLength - 1})`);

                // 표시 범위 설정 (최근 N일만 화면에 표시)
                chartRef.current.timeScale().setVisibleRange({
                    from: startTime as Time,
                    to: endTime as Time,
                });

                console.log(`[recreateSeries] ✓ 초기 표시 범위 설정 완료`);
            } catch (e) {
                console.error('[recreateSeries] 표시 범위 설정 실패:', e);
                // 실패 시 전체 데이터 표시
                chartRef.current.timeScale().fitContent();
            }
        }

        // NULL 데이터 경고 메시지 설정
        if (!hasAnyData && noDataInvestors.length > 0) {
            setNoDataWarning(
                `선택한 투자자(${noDataInvestors.join(', ')})의 ${period}일 이동평균 데이터가 없습니다. 이동평균 계산을 실행해주세요.`
            );
        } else if (noDataInvestors.length > 0) {
            setNoDataWarning(
                `일부 투자자(${noDataInvestors.join(', ')})의 ${period}일 이동평균 데이터가 없습니다.`
            );
        }
    }, [selectedInvestors, prepareSeriesData, preparePriceSeriesData, showPrice, period]);

    // 차트 데이터 조회 (전체 데이터 로드)
    const fetchChartData = async (code?: string) => {
        const targetCode = code || stkCd;
        if (!targetCode) {
            setError('종목을 선택해주세요.');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            // 1. Raw 데이터의 최신 날짜 확인
            let rawMaxDate = '';
            try {
                const dateRange = await stockListApi.getDateRange(targetCode);
                if (dateRange.data && dateRange.data.endDate) {
                    rawMaxDate = dateRange.data.endDate; // YYYYMMDD or YYYY-MM-DD? Usually YYYY-MM-DD from API?
                    // API returns "startDate": "20000104", "endDate": "20240101" (Strings, likely YYYYMMDD based on Java View)
                    // Let's normalize to YYYYMMDD for comparison.
                    rawMaxDate = rawMaxDate.replace(/-/g, '');
                }
            } catch (e) {
                console.warn('Raw 데이터 날짜 조회 실패:', e);
            }

            // 2. 차트 데이터 요청 (API 사용)
            const loadDays = MA_LOAD_DAYS[period] || 1500;
            // 모든 투자자 데이터를 가져오도록 수정 (프론트엔드에서 필요한 것만 표시)
            const allInvestors = INVESTOR_TYPES.map(inv => inv.key).join(',');

            console.log(`[fetchChartData] ${loadDays}일 치 데이터 로드 시작 for ${targetCode}`);

            // First Attempt
            let response = await statisticsApi.getMovingAverageChart(targetCode, loadDays, allInvestors, period);
            let data: ChartResponse = response.data;

            // 순매수 이동평균 페이지용 투자자 비중 조회
            // 기간별 화면 표시 범위(MA_VISIBLE_DAYS)만큼의 데이터로 계산
            try {
                if (data && data.data && data.data.length > 0) {
                    const totalDataLength = data.data.length;
                    const visibleDays = MA_VISIBLE_DAYS[period]; // 기간별 표시 일수 (250, 375, 500...)

                    // 화면에 표시될 범위 계산 (최근 N일)
                    const startIndex = Math.max(0, totalDataLength - visibleDays);
                    const fromDate = data.data[startIndex].dt; // 표시 시작 날짜
                    const toDate = data.data[totalDataLength - 1].dt; // 최신 날짜

                    console.log(`[투자자 비중 계산] MA${period}, 범위: ${fromDate}~${toDate} (${totalDataLength - startIndex}일)`);

                    const ratioRes = await statisticsApi.getInvestorRatioMa(targetCode, period, fromDate, toDate);
                    if (ratioRes.data) {
                        setInvestorRatios({
                            frgnr: ratioRes.data.frgnr || 0,
                            orgn: ratioRes.data.orgn || 0,
                            fnnc_invt: ratioRes.data.fnncInvt || 0,
                            insrnc: ratioRes.data.insrnc || 0,
                            invtrt: ratioRes.data.invtrt || 0,
                            etc_fnnc: ratioRes.data.etcFnnc || 0,
                            bank: ratioRes.data.bank || 0,
                            penfnd_etc: ratioRes.data.penfndEtc || 0,
                            samo_fund: ratioRes.data.samoFund || 0,
                            natn: ratioRes.data.natn || 0,
                            etc_corp: ratioRes.data.etcCorp || 0,
                            natfor: ratioRes.data.natfor || 0,
                        });
                        console.log(`[투자자 비중 결과] MA${period}, 외국인: ${ratioRes.data.frgnr}%, 기관계: ${ratioRes.data.orgn}%`);
                    }
                }
            } catch (e) {
                console.warn('투자자 비중 조회 실패:', e);
            }

            // 3. 최신성 검사 및 동기화
            // 데이터가 없거나, (Raw 데이터가 존재하고 && 차트 데이터의 최신 날짜 < Raw 최신 날짜)
            let needSync = false;
            let chartMaxDate = '';

            if (data && data.data && data.data.length > 0) {
                // data.data is sorted ASC (oldest -> newest)
                chartMaxDate = data.data[data.data.length - 1].dt;
            }

            if (rawMaxDate) {
                if (!chartMaxDate || checkDateDiff(chartMaxDate, rawMaxDate)) {
                    console.log(`[AutoSync] 데이터 갱신 필요 감지.Chart(${chartMaxDate}) < Raw(${rawMaxDate})`);
                    needSync = true;
                }
            }

            // Guard to avoid repeated sync requests for the same raw date
            if (needSync && lastRequestedRawDateRef.current !== rawMaxDate) {
                lastRequestedRawDateRef.current = rawMaxDate;

                setIsSyncing(true);
                // Toast or Message can be handled by UI using isSyncing state
                console.log(`[AutoSync] 단일 종목 이동평균 계산 실행: ${targetCode} `);

                try {
                    const calcRes = await statisticsApi.calculateMovingAverage(targetCode);
                    if (calcRes.data && calcRes.data.success) {
                        console.log('[AutoSync] 계산 완료. 차트 재조회...');

                        const backendUpdated = calcRes.data.updatedMaxDate || '';

                        // Re-fetch
                        response = await statisticsApi.getMovingAverageChart(targetCode, loadDays, allInvestors, period);
                        data = response.data;

                        const newChartMax = data && data.data && data.data.length > 0 ? data.data[data.data.length - 1].dt : '';
                        if (backendUpdated && backendUpdated === rawMaxDate) {
                            console.log('[AutoSync] 백엔드가 최신 날짜를 반영했습니다:', backendUpdated);
                            lastRequestedRawDateRef.current = backendUpdated;
                        } else if (newChartMax && !checkDateDiff(newChartMax, rawMaxDate)) {
                            lastRequestedRawDateRef.current = rawMaxDate;
                        } else {
                            console.warn('[AutoSync] 계산 후에도 차트가 최신이 아님. 백엔드 상태 확인 필요');
                            lastRequestedRawDateRef.current = null;
                            setSyncError('계산은 완료되었으나 차트가 최신화되지 않았습니다. 서버 상태를 확인하세요.');
                        }
                    } else {
                        const msg = calcRes.data?.message || calcRes.error || '계산 실패';
                        console.warn('[AutoSync] 계산 실패:', msg);
                        setSyncError(msg);
                        // Allow retry later
                        lastRequestedRawDateRef.current = null;
                    }
                } catch (calcErr) {
                    console.error('[AutoSync] API 호출 오류:', calcErr);
                    // Allow retry later
                    lastRequestedRawDateRef.current = null;
                } finally {
                    setIsSyncing(false);
                }
            }

            // Data Processing (Existing Logic)
            if (data && data.data && data.data.length > 0) {
                allDataRef.current = data.data;
                oldestDateRef.current = data.data[0].dt;
                hasMoreRef.current = true;
                setSector(data.sector || null);
                setStkCd(targetCode);

                console.log(`[fetchChartData] 데이터 로드 완료: ${data.data.length} 건(${data.data[0].dt} ~${data.data[data.data.length - 1].dt})`);

                if (chartRef.current) {
                    chartRef.current.remove();
                    chartRef.current = null;
                    seriesMapRef.current.clear();
                    priceSeriesRef.current = null;
                }

                initChart();
                recreateSeries(true);
            } else {
                console.warn(`[fetchChartData] 데이터 없음`);
                allDataRef.current = [];
                oldestDateRef.current = null;
                hasMoreRef.current = false;
                setError(data?.message || '데이터가 없습니다.');
            }
        } catch (err) {
            console.error('차트 데이터 조회 오류:', err);
            setError(`조회 실패: ${err instanceof Error ? err.message : '알 수 없는 오류'} `);
            allDataRef.current = [];
        } finally {
            setLoading(false);
        }
    };

    // 날짜 비교 (A < B 로직)
    // A, B format: YYYYMMDD
    const checkDateDiff = (dateA: string, dateB: string) => {
        if (!dateA || !dateB) return false;
        return parseInt(dateA) < parseInt(dateB);
    };

    // 과거 데이터 추가 로드 (프리페칭)
    const loadMoreData = async () => {
        if (!stkCd || isLoadingMoreRef.current || !oldestDateRef.current || !hasMoreRef.current) {
            return;
        }

        console.log('[loadMoreData] 과거 4년 치 데이터 로드 시작:', oldestDateRef.current);
        isLoadingMoreRef.current = true;

        try {
            // 현재 시간 범위 저장 (날짜 기반 - 화면 유지용)
            const timeScale = chartRef.current?.timeScale();
            const currentTimeRange = timeScale?.getVisibleRange(); // 날짜 기반 (논리적 인덱스 아님)

            console.log('[loadMoreData] 현재 화면 범위 저장:', currentTimeRange);

            // 모든 투자자 데이터를 가져오도록 수정 (프론트엔드에서 필요한 것만 표시)
            const allInvestors = INVESTOR_TYPES.map(inv => inv.key).join(',');
            // 한 번에 1000일 치 추가 로드 (4년 = 약 1000 거래일)
            const response = await fetch(
                `http://localhost:8080/api/statistics/moving-average/chart/${stkCd}?days=1000&investors=${allInvestors}&period=${period}&beforeDate=${oldestDateRef.current}`
            );

            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            const data: ChartResponse = await response.json();

            if (data.data && data.data.length > 0) {
                // 기존 데이터 앞에 새 데이터 추가 (중복 제거)
                const existingDates = new Set(allDataRef.current.map(d => d.dt));
                const newData = data.data.filter(d => !existingDates.has(d.dt));

                if (newData.length > 0) {
                    const oldLength = allDataRef.current.length;
                    allDataRef.current = [...newData, ...allDataRef.current];
                    oldestDateRef.current = newData[0].dt;

                    console.log(`[loadMoreData] ${newData.length}개 데이터 추가 (${oldLength} → ${allDataRef.current.length})`);

                    // 기존 시리즈에 데이터만 업데이트 (시리즈 재생성 X)
                    updateSeriesData();

                    // 시간 범위 복원 (뷰포트 유지) - 날짜 기반으로 복원하여 화면 고정
                    if (timeScale && currentTimeRange) {
                        try {
                            timeScale.setVisibleRange(currentTimeRange);
                            console.log('[loadMoreData] 화면 범위 복원 완료 - 화면 위치 유지됨');
                        } catch (e) {
                            console.warn('시간 범위 복원 실패:', e);
                        }
                    }
                } else {
                    console.log('[loadMoreData] 중복 데이터만 있음 - 더 이상 로드 없음');
                    hasMoreRef.current = false;
                }
            } else {
                console.log('[loadMoreData] 서버에서 데이터 없음');
                hasMoreRef.current = false;
            }
        } catch (err) {
            console.error('[loadMoreData] 과거 데이터 로드 실패:', err);
        } finally {
            isLoadingMoreRef.current = false;
        }
    };

    // 차트 초기화
    const initChart = useCallback(() => {
        if (!chartContainerRef.current) {
            console.error('[initChart] 차트 컨테이너가 없습니다!');
            return;
        }

        // 컨테이너 크기 확인
        const rect = chartContainerRef.current.getBoundingClientRect();
        console.log('[initChart] 차트 컨테이너 크기:', {
            width: chartContainerRef.current.clientWidth,
            height: chartContainerRef.current.clientHeight,
            offsetWidth: chartContainerRef.current.offsetWidth,
            offsetHeight: chartContainerRef.current.offsetHeight,
            boundingRect: rect
        });

        if (chartContainerRef.current.clientWidth === 0 || chartContainerRef.current.clientHeight === 0) {
            console.error('[initChart] ⚠️ 차트 컨테이너 크기가 0입니다!', {
                width: chartContainerRef.current.clientWidth,
                height: chartContainerRef.current.clientHeight
            });
        }

        // 기존 차트 제거
        if (chartRef.current) {
            chartRef.current.remove();
            chartRef.current = null;
            seriesMapRef.current.clear();
        }

        const chart = createChart(chartContainerRef.current, {
            width: chartContainerRef.current.clientWidth || 1000, // Fallback width
            height: 700,
            layout: {
                background: { color: '#ffffff' },
                textColor: '#333',
            },
            grid: {
                vertLines: { color: '#f0f0f0' },
                horzLines: { color: '#f0f0f0' },
            },
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
                borderColor: '#e0e0e0',
                tickMarkFormatter: (time: any) => {
                    // REQ-001: 1월은 YYYY년01월, 나머지는 N월 형식
                    if (typeof time === 'string') {
                        const [year, month] = time.split('-');
                        if (month === '01') {
                            return `${year}년01월`;
                        }
                        return `${parseInt(month)}월`;
                    }
                    // 숫자(timestamp)인 경우
                    const date = new Date(time * 1000);
                    const month = date.getMonth() + 1;
                    const year = date.getFullYear();
                    if (month === 1) {
                        return `${year}년01월`;
                    }
                    return `${month}월`;
                },
            },
            rightPriceScale: {
                borderColor: '#e0e0e0',
                scaleMargins: {
                    top: 0.1,
                    bottom: 0.1,
                },
            },
            leftPriceScale: {
                visible: true,
                borderColor: '#e0e0e0',
                scaleMargins: {
                    top: 0.1,
                    bottom: 0.1,
                },
            },
            handleScroll: {
                mouseWheel: true,
                pressedMouseMove: true,
                horzTouchDrag: true,
                vertTouchDrag: false,
            },
            handleScale: {
                axisPressedMouseMove: true,
                mouseWheel: true,
                pinch: true,
            },
            crosshair: {
                mode: 1, // Magnet
                vertLine: {
                    width: 1,
                    color: '#9b9b9b',
                    labelBackgroundColor: '#9b9b9b',
                },
                horzLine: {
                    color: '#9b9b9b',
                    labelBackgroundColor: '#9b9b9b',
                },
            },
        });

        chartRef.current = chart;

        console.log('[initChart] 차트 생성 완료:', {
            chartExists: !!chart,
            chartRefExists: !!chartRef.current
        });

        // --- 차트 동기화 로직 (Main <-> Sub) ---
        if (subChartRef.current) {
            const subChart = subChartRef.current;

            // Main -> Sub
            chart.timeScale().subscribeVisibleLogicalRangeChange(range => {
                if (range) subChart.timeScale().setVisibleLogicalRange(range);
            });

            // Sub -> Main (이미 등록되어 있을 수 있으므로 주의, 하지만 createChart는 매번 새로 하므로 여기서 등록해도 됨)
            // 단, subChart 쪽 핸들러는 handleSubChartSync에서 한 번만 등록하는 게 안전함.
            // 여기서는 Main -> Sub만 확실히 재연결.
        }

        // 크로스헤어 이벤트 (툴팁용)
        chart.subscribeCrosshairMove((param) => {
            if (!param.time || param.point === undefined) {
                setTooltipData(null);
                return;
            }

            const time = param.time as string;
            const values: TooltipData['values'] = [];

            // REQ-004: 현재가 정보를 툴팁에 추가
            if (priceSeriesRef.current) {
                const priceData = param.seriesData.get(priceSeriesRef.current);
                if (priceData && 'value' in priceData) {
                    values.push({
                        key: 'curPrc',
                        label: '현재가',
                        value: priceData.value as number,
                        color: '#000000',
                    });
                }
            }

            selectedInvestorsRef.current.forEach((investorKey) => {
                const series = seriesMapRef.current.get(investorKey);
                if (series) {
                    const data = param.seriesData.get(series);
                    if (data && 'value' in data) {
                        const investor = INVESTOR_TYPES.find(i => i.key === investorKey);
                        if (investor) {
                            values.push({
                                key: investorKey,
                                label: `${investor.label} ${period}일 순매수`,
                                value: Math.round(data.value as number),
                                color: investor.color,
                            });
                        }
                    }
                }
            });

            if (values.length > 0) {
                setTooltipData({ time, values });
                if (param.point) {
                    setTooltipPosition({ x: param.point.x, y: param.point.y });
                }
            }
        });

        // 리사이즈 핸들러
        // Resize Observer
        const resizeObserver = new ResizeObserver(() => {
            if (chartContainerRef.current && chartRef.current) {
                chartRef.current.applyOptions({
                    width: chartContainerRef.current.clientWidth,
                    height: chartContainerRef.current.clientHeight,
                });
            }
        });
        resizeObserver.observe(chartContainerRef.current);

        // 시간 범위 변경 시 과거 데이터 자동 로드
        let lastLogTime = 0;
        let consecutiveDragAtEdge = 0; // 끝에서 드래그 횟수

        chart.timeScale().subscribeVisibleTimeRangeChange((timeRange) => {
            if (!timeRange || !allDataRef.current.length) return;

            // 디버깅: 1초에 한 번만 로그 (너무 많은 로그 방지)
            const now = Date.now();
            const shouldLog = now - lastLogTime > 1000;

            const oldestDataTime = allDataRef.current[0]?.dt;
            const newestDataTime = allDataRef.current[allDataRef.current.length - 1]?.dt;
            if (!oldestDataTime || !newestDataTime) return;

            const oldestDate = formatDateForChart(oldestDataTime);
            const newestDate = formatDateForChart(newestDataTime);
            const visibleFrom = timeRange.from as string;

            // 전체 데이터 범위 계산 (밀리초)
            const oldestMs = new Date(oldestDate).getTime();
            const newestMs = new Date(newestDate).getTime();
            const visibleFromMs = new Date(visibleFrom).getTime();

            const totalRange = newestMs - oldestMs;
            const distanceFromOldest = visibleFromMs - oldestMs;
            const percentageFromOldest = (distanceFromOldest / totalRange) * 100;

            if (shouldLog) {
                console.log(`[패닝 감지] 현재 위치: 시작으로부터 ${percentageFromOldest.toFixed(1)}%`);
                lastLogTime = now;
            }

            // 트리거 1 (자동): 가장 왼쪽 끝에 5% 이내로 접근하면 자동 로드
            if (percentageFromOldest < 5 && distanceFromOldest >= 0) {
                if (shouldLog) {
                    console.log(`🤖 [자동 로드] 끝에 도달 (${percentageFromOldest.toFixed(1)}%) - 과거 데이터 로드 중...`);
                }
                loadMoreData();
            }

            // 트리거 3 (제스처): 끝에 도달한 상태에서 계속 드래그 시도 감지
            if (percentageFromOldest < 1) {
                consecutiveDragAtEdge++;
                if (consecutiveDragAtEdge > 3 && shouldLog) {
                    console.log(`👆 [제스처 감지] 끝에서 계속 드래그 - 과거 데이터 로드 중...`);
                    loadMoreData();
                    consecutiveDragAtEdge = 0; // 리셋
                }
            } else {
                consecutiveDragAtEdge = 0; // 끝이 아니면 리셋
            }
        });

        return () => {
            resizeObserver.disconnect();
        };
    }, []); // 차트는 한 번만 초기화 (의존성 없음)

    // 차트 초기화 및 업데이트
    useEffect(() => {
        initChart();
        return () => {
            if (chartRef.current) {
                chartRef.current.remove();
                chartRef.current = null;
            }
        };
    }, []); // 처음 마운트 될 때만 실행

    useEffect(() => {
        // 데이터가 이미 로드된 상태에서 투자자 선택 또는 현재가 표시 변경 시 시리즈 재생성
        if (allDataRef.current.length > 0 && chartRef.current) {
            console.log('[useEffect] 투자자 선택 또는 현재가 표시 변경 - 시리즈 재생성');
            recreateSeries(false);
        }
    }, [selectedInvestors, showPrice, recreateSeries]);

    // 외부 클릭 시 드롭다운 닫기
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // 숫자(종목코드)인지 확인
    const isNumericCode = (str: string) => /^\d{6}$/.test(str.trim());

    // 종목 검색 API 호출
    const searchStocks = async (keyword: string) => {
        if (!keyword || keyword.trim().length < 1) {
            setSearchResults([]);
            setShowDropdown(false);
            return;
        }

        if (isNumericCode(keyword)) {
            setSearchResults([]);
            setShowDropdown(false);
            return;
        }

        setIsSearching(true);
        try {
            const response = await fetch(
                `http://localhost:8080/api/v1/stock-list/search?keyword=${encodeURIComponent(keyword.trim())}`
            );

            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            const data: StockSearchResult[] = await response.json();
            setSearchResults(data);
            setShowDropdown(data.length > 0);
        } catch (err) {
            console.error('검색 오류:', err);
            setSearchResults([]);
        } finally {
            setIsSearching(false);
        }
    };

    // 디바운스된 검색
    useEffect(() => {
        const timer = setTimeout(() => {
            if (searchKeyword && !isNumericCode(searchKeyword)) {
                searchStocks(searchKeyword);
            }
        }, 300);
        return () => clearTimeout(timer);
    }, [searchKeyword]);

    // 종목 선택 핸들러
    const handleSelectStock = (stock: StockSearchResult) => {
        setSelectedStock(stock);
        setStkCd(stock.code);
        setStockName(stock.name);
        setSearchKeyword(stock.name);
        setShowDropdown(false);
    };

    // 선택 해제
    const handleClearSelection = () => {
        setSelectedStock(null);
        setStkCd('');
        setStockName('');
        setSearchKeyword('');
        allDataRef.current = [];
        setSector(null);
        setError(null);

        // 시리즈 모두 제거
        seriesMapRef.current.forEach((series) => {
            chartRef.current?.removeSeries(series);
        });
        seriesMapRef.current.clear();
    };

    // 조회 버튼 클릭
    const handleSearch = async (e?: React.FormEvent) => {
        e?.preventDefault(); // Prevent default form submission if called from a form

        const keyword = searchKeyword.trim();
        if (!keyword) {
            setError('종목명 또는 종목코드를 입력해주세요.');
            return;
        }

        if (isNumericCode(keyword)) {
            setStkCd(keyword);
            setStockName(keyword);
            setSelectedStock(null);
            fetchChartData(keyword);
        } else if (selectedStock) {
            fetchChartData(selectedStock.code);
        } else if (searchResults.length === 1) {
            handleSelectStock(searchResults[0]);
            fetchChartData(searchResults[0].code);
        } else if (searchResults.length > 1) {
            // 검색어와 정확히 일치하는 종목이 있으면 자동 선택
            const exactMatch = searchResults.find(s => s.name === keyword);
            if (exactMatch) {
                handleSelectStock(exactMatch);
                fetchChartData(exactMatch.code);
            } else {
                // 첫 번째 결과를 자동 선택
                handleSelectStock(searchResults[0]);
                fetchChartData(searchResults[0].code);
            }
        } else {
            await searchStocks(keyword); // 검색 결과가 없으면 다시 검색 시도
            if (searchResults.length === 0) { // 검색 후에도 결과가 없으면 에러
                setError('종목을 찾을 수 없습니다.');
            }
        }
    };

    // period 변경 시 재조회
    useEffect(() => {
        if (stkCd) {
            fetchChartData(stkCd);
        }
    }, [period]);

    // 투자자 선택 토글
    const toggleInvestor = (key: string) => {
        setSelectedInvestors((prev) =>
            prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
        );
    };

    // 하위 차트(상관분석) 동기화 핸들러
    const handleSubChartSync = useCallback((subChart: IChartApi) => {
        subChartRef.current = subChart;
        console.log("Sub Chart Synced with Main Chart");

        // Main -> Sub (Initial Sync)
        if (chartRef.current) {
            const range = chartRef.current.timeScale().getVisibleLogicalRange();
            if (range) subChart.timeScale().setVisibleLogicalRange(range);

            // Event Binding (Main -> Sub)
            chartRef.current.timeScale().subscribeVisibleLogicalRangeChange(r => {
                if (r) subChart.timeScale().setVisibleLogicalRange(r);
            });
        }

        // Sub -> Main
        subChart.timeScale().subscribeVisibleLogicalRangeChange(range => {
            if (range && chartRef.current) {
                chartRef.current.timeScale().setVisibleLogicalRange(range);
            }
        });
    }, []);

    // Enter 키 처리
    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            if (showDropdown && searchResults.length > 0) {
                handleSelectStock(searchResults[0]);
                handleSearch(); // Call handleSearch to trigger data fetch
            } else {
                handleSearch();
            }
        }
    };

    // 차트 컨트롤
    const handleZoomIn = () => {
        chartRef.current?.timeScale().scrollToPosition(-5, true);
    };

    const handleZoomOut = () => {
        chartRef.current?.timeScale().scrollToPosition(5, true);
    };

    const handleReset = () => {
        chartRef.current?.timeScale().fitContent();
    };

    return (
        <div className="flex flex-col h-full bg-gray-50">
            {/* Main Content */}
            <div className="flex-1 w-full max-w-[1800px] mx-auto px-4 py-6 flex flex-col gap-6">

                {/* Stock Search - 상단 고정 */}
                {!hideSearch && (
                    <div className="relative z-20" ref={searchRef}>
                        {/* 종목 검색 */}
                        <div className="flex flex-wrap gap-4 mb-6">
                            <div className="flex-1 min-w-[280px]">
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    종목 검색 (종목명 또는 6자리 코드)
                                </label>
                                <div className="relative">
                                    <div className="flex gap-2">
                                        <div className="relative flex-1">
                                            <input
                                                type="text"
                                                value={searchKeyword}
                                                onChange={(e) => {
                                                    setSearchKeyword(e.target.value);
                                                    if (selectedStock) setSelectedStock(null);
                                                }}
                                                onKeyPress={handleKeyPress}
                                                onFocus={() => searchResults.length > 0 && setShowDropdown(true)}
                                                placeholder="예: 삼성전자, 한국전력, 015760"
                                                className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-purple-500"
                                            />
                                            {(searchKeyword || selectedStock) && (
                                                <button
                                                    onClick={handleClearSelection}
                                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                                                >
                                                    <X className="w-4 h-4" />
                                                </button>
                                            )}
                                        </div>

                                        <button
                                            onClick={handleSearch}
                                            disabled={loading || !searchKeyword.trim()}
                                            className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:bg-purple-300 flex items-center gap-2 whitespace-nowrap"
                                        >
                                            <Search className="w-4 h-4" />
                                            조회
                                        </button>
                                    </div>

                                    {/* 검색 결과 드롭다운 */}
                                    {showDropdown && searchResults.length > 0 && (
                                        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                                            {searchResults.map((stock, index) => (
                                                <button
                                                    key={`${stock.code}-${index}`}
                                                    onClick={() => {
                                                        handleSelectStock(stock);
                                                        fetchChartData(stock.code);
                                                    }}
                                                    className="w-full px-4 py-3 text-left hover:bg-purple-50 flex items-center justify-between border-b border-gray-100 last:border-b-0"
                                                >
                                                    <div>
                                                        <span className="font-medium text-gray-900">{stock.name}</span>
                                                        <span className="ml-2 text-gray-500 text-sm">{stock.code}</span>
                                                    </div>
                                                    <div className="flex gap-2">
                                                        <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded">
                                                            {stock.marketName}
                                                        </span>
                                                        {stock.sector && (
                                                            <span className="px-2 py-0.5 bg-purple-100 text-purple-700 text-xs rounded">
                                                                {stock.sector}
                                                            </span>
                                                        )}
                                                    </div>
                                                </button>
                                            ))}
                                        </div>
                                    )}

                                    {isSearching && (
                                        <div className="absolute right-20 top-1/2 -translate-y-1/2">
                                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-purple-600"></div>
                                        </div>
                                    )}
                                </div>
                            </div>

                        </div>

                        {/* 종목 정보 */}
                        {(selectedStock || (stkCd && allDataRef.current.length > 0)) && (
                            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 mb-6">
                                <div className="flex items-center gap-4">
                                    <span className="text-lg font-bold text-gray-900">
                                        {selectedStock?.name || stockName || stkCd}
                                    </span>
                                    <span className="text-gray-500">{stkCd}</span>
                                    {selectedStock?.marketName && (
                                        <span className="px-3 py-1 bg-gray-100 text-gray-600 rounded-full text-sm">
                                            {selectedStock.marketName}
                                        </span>
                                    )}
                                    {(selectedStock?.sector || sector) && (
                                        <span className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-sm">
                                            {selectedStock?.sector || sector}
                                        </span>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* 이동평균분석 Chart */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 relative">
                    {/* 헤더: 제목, 이동평균 기간, 차트 컨트롤 */}
                    <div className="mb-6">
                        <div className="flex items-center justify-between mb-6">
                            <div className="flex items-center gap-3">
                                {/* 이동평균 기간 선택 */}
                                <div className="flex gap-1">
                                    {MA_PERIODS.map((p) => (
                                        <button
                                            key={p}
                                            onClick={() => setPeriod(p)}
                                            className={`px-2 py-1 rounded-lg font-medium text-xs ${period === p
                                                ? 'bg-purple-600 text-white'
                                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                                }`}
                                        >
                                            {p}일
                                        </button>
                                    ))}
                                </div>
                                <button
                                    onClick={() => loadMoreData()}
                                    className="ml-2 px-3 py-1 bg-white border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50 flex items-center gap-1 shadow-sm transition-colors"
                                >
                                    <Clock className="w-3 h-3" />
                                    과거 +4년
                                </button>
                            </div>
                        </div>

                        {/* 투자자 토글 버튼 - 높이 확대 (REQ-007 UI 통일) */}
                        <div className="flex flex-wrap gap-1.5 items-end py-2">
                            {/* Current Price Toggle - 비중 표시 제외 */}
                            <div className="flex flex-col items-center flex-1 min-w-[70px]">
                                <div className="h-6 w-full mb-2 flex items-end justify-center pb-0.5">
                                    <span className="text-xs text-gray-500 font-bold tracking-tighter whitespace-nowrap">1년간 투자비중</span>
                                </div>
                                <button
                                    onClick={() => setShowPrice(!showPrice)}
                                    className={`w-full py-2 rounded-lg text-xs font-medium transition-all flex items-center justify-center gap-1.5 border-2 ${showPrice
                                        ? 'bg-black text-white border-black'
                                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200 border-gray-300'
                                        }`}
                                >
                                    <span className="w-2 h-2 rounded-full" style={{ backgroundColor: showPrice ? 'white' : '#000' }} />
                                    현재가
                                </button>
                            </div>

                            {INVESTOR_TYPES.map((inv) => {
                                const ratio = investorRatios[inv.key] || 0;
                                const barWidthPercent = ratio; // 비중이 곧 바 너비 %
                                return (
                                    <div key={inv.key} className="flex flex-col items-center flex-1 min-w-[70px]">
                                        {/* 가로 막대그래프 + 비중 */}
                                        <div className="h-6 w-full flex flex-col items-center justify-end mb-2">
                                            <span className="text-[10px] text-gray-600 font-medium mb-0.5">{ratio}%</span>
                                            <div className="w-full h-3 bg-gray-200 rounded-full overflow-hidden">
                                                <div
                                                    className="h-full rounded-full transition-all"
                                                    style={{
                                                        width: `${Math.min(barWidthPercent * 2.5, 100)}%`, // 스케일 조정
                                                        backgroundColor: inv.color,
                                                    }}
                                                />
                                            </div>
                                        </div>
                                        {/* 넓은 고정 버튼 */}
                                        <button
                                            onClick={() => toggleInvestor(inv.key)}
                                            className={`w-full py-2 rounded-lg text-xs font-medium transition-all flex items-center justify-center gap-1.5 ${selectedInvestors.includes(inv.key)
                                                ? 'text-white'
                                                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                                }`}
                                            style={{
                                                backgroundColor: selectedInvestors.includes(inv.key) ? inv.color : undefined,
                                            }}
                                        >
                                            <span
                                                className={`w-2 h-2 rounded-full ${selectedInvestors.includes(inv.key) ? 'bg-white' : ''}`}
                                                style={{ backgroundColor: !selectedInvestors.includes(inv.key) ? inv.color : undefined }}
                                            />
                                            {inv.label}
                                        </button>
                                    </div>
                                );
                            })}
                        </div>
                        <p className="text-xs text-gray-500 mt-2">
                            오른쪽 Y축: 투자자 수량 | 왼쪽 Y축: 현재가(원)
                        </p>
                    </div>
                    <div
                        ref={chartContainerRef}
                        className="w-full relative bg-white border border-gray-100 rounded-xl shadow-sm overflow-hidden"
                        style={{ height: 'calc(100vh - 380px)', minHeight: '450px' }}
                    ></div>

                    {isSyncing && (
                        <div className="absolute inset-0 bg-white/50 z-50 flex items-center justify-center">
                            <div className="bg-white p-4 rounded-lg shadow-xl border border-gray-200 flex items-center gap-3">
                                <div className="w-6 h-6 animate-spin text-purple-600">🔄</div>
                                <div>
                                    <p className="font-bold text-gray-800">최신 데이터 분석 중...</p>
                                    <p className="text-xs text-gray-500">잠시만 기다려주세요.</p>
                                </div>
                            </div>
                        </div>
                    )}

                    {syncError && (
                        <div className="mt-2 p-2 bg-red-50 border border-red-200 text-red-800 rounded text-sm">
                            계산 오류: {syncError}
                        </div>
                    )}

                    {noDataWarning && (
                        <div className="mt-2 p-3 bg-yellow-50 border border-yellow-200 text-yellow-800 rounded text-sm flex items-center gap-2">
                            <span className="text-lg">⚠️</span>
                            <span>{noDataWarning}</span>
                        </div>
                    )}

                    {/* 커스텀 툴팁 */}
                    {tooltipData && (
                        <div
                            className="absolute bg-white border border-gray-200 rounded-lg p-3 shadow-lg pointer-events-none z-20"
                            style={{
                                left: Math.min(tooltipPosition.x + 20, (chartContainerRef.current?.clientWidth ?? 0) - 200),
                                top: Math.max(0, tooltipPosition.y - 80),
                            }}
                        >
                            <p className="font-medium text-gray-900 mb-2 text-sm">{tooltipData.time}</p>
                            {tooltipData.values.map((v) => (
                                <p key={v.key} className="text-sm" style={{ color: v.color }}>
                                    {v.label}: {v.value.toLocaleString()}
                                </p>
                            ))}
                        </div>
                    )}

                    {allDataRef.current.length === 0 && !loading && !error && (
                        <div className="absolute inset-0 flex flex-col items-center justify-center text-gray-400">
                            <TrendingUp className="w-16 h-16 mb-4" />
                            <p>종목을 검색하고 조회 버튼을 클릭하세요.</p>
                            <p className="text-sm mt-2">예: 삼성전자, 한국전력, 015760</p>
                        </div>
                    )}
                </div>
            </div >
        </div >
    );
};

export default MovingAverageChart;
