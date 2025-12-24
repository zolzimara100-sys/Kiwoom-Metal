import { createChart, IChartApi, ISeriesApi, LineData, LineSeries, Time } from 'lightweight-charts';
import { Clock, TrendingUp } from 'lucide-react';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { sectorMaApi, SectorInfo, SectorMaChartDataPoint, StockInfo, statisticsApi } from '../services/api';

interface RawDataPoint {
    dt: string;
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
    sectorCd: string;
    sectorNm?: string;
    period: number;
    data: RawDataPoint[];
    message?: string;
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
    sectorCd?: string;
}

const SectorMovingAverageChart: React.FC<Props> = ({ sectorCd: propSectorCd }) => {
    const initialSectorCd = propSectorCd || '';

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

    // 섹터 관련 상태
    const [sectors, setSectors] = useState<SectorInfo[]>([]);
    const [sectorCd, setSectorCd] = useState(initialSectorCd);
    const [sectorNm, setSectorNm] = useState('');
    const [selectedSector, setSelectedSector] = useState<SectorInfo | null>(null);
    const [showSectorDropdown, setShowSectorDropdown] = useState(false);
    const sectorDropdownRef = useRef<HTMLDivElement>(null);

    // 종목 관련 상태 (REQ-004-1)
    const [stocks, setStocks] = useState<StockInfo[]>([]);
    const [selectedStock, setSelectedStock] = useState<StockInfo | null>(null);
    const [showStockDropdown, setShowStockDropdown] = useState(false);
    const stockDropdownRef = useRef<HTMLDivElement>(null);
    const stockSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);
    const stockDataRef = useRef<any[]>([]);
    const [stockCurrentPrice, setStockCurrentPrice] = useState<number | null>(null);
    const [stockMaValue, setStockMaValue] = useState<number | null>(null);

    // 업종전체 모드 관련 상태 (REQ-005)
    const [isAllSectorsMode, setIsAllSectorsMode] = useState(false);
    const [allSectorsData, setAllSectorsData] = useState<any>(null);
    const allSectorsSeriesRef = useRef<Map<string, ISeriesApi<'Line'>>>(new Map());

    // 차트 관련 상태
    const [period, setPeriod] = useState(20); // 기본값 20일
    const [selectedInvestors, setSelectedInvestors] = useState(['frgnr', 'orgn']);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [tooltipData, setTooltipData] = useState<TooltipData | null>(null);
    const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });
    const selectedInvestorsRef = useRef(selectedInvestors); // To access latest state in event handler
    const [noDataWarning, setNoDataWarning] = useState<string | null>(null); // NULL 데이터 경고

    // REQ-007: 투자자별 거래 비중 (UI 통일)
    const [investorRatios, setInvestorRatios] = useState<{ [key: string]: number }>({});

    // Update ref when state changes
    useEffect(() => {
        selectedInvestorsRef.current = selectedInvestors;
    }, [selectedInvestors]);

    // Load sectors on mount
    useEffect(() => {
        const loadSectors = async () => {
            try {
                const response = await sectorMaApi.getAllSectors();
                if (response.data && response.data.length > 0) {
                    setSectors(response.data);
                    // 초기 로드 시 첫 번째 섹터 자동 선택
                    if (!sectorCd) {
                        const firstSector = response.data[0];
                        setSectorCd(firstSector.sectorCd);
                        setSectorNm(firstSector.sectorNm);
                        setSelectedSector(firstSector);
                    }
                }
            } catch (err) {
                console.error('섹터 목록 조회 실패:', err);
            }
        };
        loadSectors();
    }, []);

    // 드롭다운 외부 클릭 감지
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (sectorDropdownRef.current && !sectorDropdownRef.current.contains(event.target as Node)) {
                setShowSectorDropdown(false);
            }
            if (stockDropdownRef.current && !stockDropdownRef.current.contains(event.target as Node)) {
                setShowStockDropdown(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // 섹터 변경 시 종목 목록 조회 (REQ-004-1)
    useEffect(() => {
        const fetchStocks = async () => {
            if (!sectorCd) {
                setStocks([]);
                setSelectedStock(null);
                setStockCurrentPrice(null);
                setStockMaValue(null);
                return;
            }

            try {
                const response = await sectorMaApi.getStocksBySector(sectorCd);
                if (response.data && response.data.length > 0) {
                    setStocks(response.data);
                } else {
                    setStocks([]);
                }
            } catch (err) {
                console.error('종목 목록 조회 실패:', err);
                setStocks([]);
            }
        };

        fetchStocks();
        // 섹터 변경 시 선택된 종목 초기화
        setSelectedStock(null);
        setStockCurrentPrice(null);
        setStockMaValue(null);
        // 종목 시리즈 제거 (안전하게)
        if (stockSeriesRef.current && chartRef.current) {
            try {
                chartRef.current.removeSeries(stockSeriesRef.current);
            } catch (e) {
                console.log('[섹터 변경] 종목 시리즈 제거 중 오류 (무시됨):', e);
            }
            stockSeriesRef.current = null;
        }
        // 종목 데이터 참조 초기화
        stockDataRef.current = [];
    }, [sectorCd]);

    // Prop으로 전달된 sectorCd가 변경되면 내부 상태 업데이트 및 데이터 조회
    useEffect(() => {
        if (propSectorCd && propSectorCd !== sectorCd) {
            setSectorCd(propSectorCd);
            fetchChartData(propSectorCd);
        }
    }, [propSectorCd]);

    // sectorCd가 변경되면 차트 데이터 로드
    useEffect(() => {
        if (sectorCd) {
            fetchChartData(sectorCd);
        }
    }, [sectorCd, period]);

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
    }, [prepareSeriesData]);

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
    }, [selectedInvestors, prepareSeriesData, period]);

    // 차트 데이터 조회 (전체 데이터 로드)
    const fetchChartData = async (code?: string, clearStock: boolean = false) => {
        const targetSector = code || sectorCd;
        if (!targetSector) {
            setError('섹터를 선택해주세요.');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const loadDays = MA_LOAD_DAYS[period] || 1500;
            // 모든 투자자 데이터를 가져오도록 수정 (프론트엔드에서 필요한 것만 표시)
            const allInvestors = INVESTOR_TYPES.map(inv => inv.key).join(',');

            console.log(`[fetchChartData] ${loadDays}일 치 데이터 로드 시작 for ${targetSector}`);

            // 섹터 이동평균 차트 데이터 요청
            let response = await sectorMaApi.getSectorMaChart(targetSector, loadDays, allInvestors, period);
            let data: ChartResponse = response.data;

            // Data Processing
            if (data && data.data && data.data.length > 0) {
                allDataRef.current = data.data;
                oldestDateRef.current = data.data[0].dt;
                hasMoreRef.current = true;
                setSectorCd(targetSector);
                if (data.sectorNm) {
                    setSectorNm(data.sectorNm);
                }

                console.log(`[fetchChartData] 데이터 로드 완료: ${data.data.length} 건(${data.data[0].dt} ~${data.data[data.data.length - 1].dt})`);

                // 종목 시리즈 참조를 먼저 초기화
                stockSeriesRef.current = null;

                if (chartRef.current) {
                    chartRef.current.remove();
                    chartRef.current = null;
                    seriesMapRef.current.clear();
                    priceSeriesRef.current = null;
                }

                initChart();
                recreateSeries(true);

                // 차트가 재생성된 후 종목 데이터를 다시 로드
                // clearStock이 true이면 종목을 로드하지 않음 (섹터 변경 시)
                if (selectedStock && !clearStock) {
                    setTimeout(() => {
                        fetchStockMaData(selectedStock.code);
                    }, 100); // 차트가 완전히 초기화된 후 실행
                }
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

    // 과거 데이터 추가 로드 (프리페칭)
    const loadMoreData = async () => {
        // 업종전체 모드에서는 모든 섹터의 과거 데이터를 로드 (REQ-005)
        if (isAllSectorsMode) {
            if (isLoadingMoreRef.current || !hasMoreRef.current) {
                return;
            }

            console.log('[loadMoreData] 업종전체 모드 - 모든 섹터 과거 데이터 로드 시작');
            isLoadingMoreRef.current = true;

            try {
                // 현재 시간 범위 저장
                const timeScale = chartRef.current?.timeScale();
                const currentTimeRange = timeScale?.getVisibleRange();

                // 모든 섹터의 과거 데이터 로드
                const allInvestors = INVESTOR_TYPES.map(inv => inv.key).join(',');

                // 각 섹터의 가장 오래된 날짜 찾기
                let oldestDate: string | null = null;
                if (allSectorsData && allSectorsData.sectors) {
                    Object.values(allSectorsData.sectors).forEach((sectorData: any) => {
                        if (sectorData.data && sectorData.data.length > 0) {
                            const sectorOldest = sectorData.data[0].dt;
                            if (!oldestDate || sectorOldest < oldestDate) {
                                oldestDate = sectorOldest;
                            }
                        }
                    });
                }

                if (!oldestDate) {
                    console.log('[loadMoreData] 업종전체 모드 - 기준 날짜 없음');
                    return;
                }

                const response = await sectorMaApi.getAllSectorsChart(
                    1000, // 과거 4년치
                    allInvestors,
                    period,
                    oldestDate
                );

                if (response.data && response.data.sectors) {
                    // 각 섹터의 데이터 업데이트
                    const updatedSectors = { ...allSectorsData.sectors };
                    let hasNewData = false;

                    Object.entries(response.data.sectors).forEach(([sectorCd, sectorData]: [string, any]) => {
                        if (sectorData.data && sectorData.data.length > 0) {
                            const existingSectorData = updatedSectors[sectorCd]?.data || [];
                            const existingDates = new Set(existingSectorData.map((d: any) => d.dt));
                            const newData = sectorData.data.filter((d: any) => !existingDates.has(d.dt));

                            if (newData.length > 0) {
                                updatedSectors[sectorCd] = {
                                    ...sectorData,
                                    data: [...newData, ...existingSectorData]
                                };
                                hasNewData = true;
                            }
                        }
                    });

                    if (hasNewData) {
                        setAllSectorsData({ sectors: updatedSectors });
                        displayAllSectorsData({ sectors: updatedSectors });

                        // 시간 범위 복원
                        if (timeScale && currentTimeRange) {
                            try {
                                timeScale.setVisibleRange(currentTimeRange);
                                console.log('[loadMoreData] 업종전체 모드 - 화면 범위 복원 완료');
                            } catch (e) {
                                console.warn('시간 범위 복원 실패:', e);
                            }
                        }
                    } else {
                        console.log('[loadMoreData] 업종전체 모드 - 새로운 데이터 없음');
                        hasMoreRef.current = false;
                    }
                }
            } catch (err) {
                console.error('[loadMoreData] 업종전체 모드 과거 데이터 로드 실패:', err);
            } finally {
                isLoadingMoreRef.current = false;
            }
            return;
        }

        // 기존 개별 섹터 모드 로직
        if (!sectorCd || isLoadingMoreRef.current || !oldestDateRef.current || !hasMoreRef.current) {
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
                `http://localhost:8080/api/v1/sector-ma/chart/${sectorCd}?days=1000&investors=${allInvestors}&period=${period}&beforeDate=${oldestDateRef.current}`
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

                    // 종목이 선택되어 있으면 종목 데이터도 추가 로드
                    if (selectedStock) {
                        await loadMoreStockData(selectedStock.code);
                    }

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

    // 종목 과거 데이터 추가 로드
    const loadMoreStockData = async (stkCd: string) => {
        if (!stkCd || !stockDataRef.current || stockDataRef.current.length === 0) return;

        try {
            console.log(`[loadMoreStockData] 종목 ${stkCd} 과거 데이터 로드 시작`);

            const oldestStockDate = stockDataRef.current[0].dt;
            const allInvestors = INVESTOR_TYPES.map(inv => inv.key).join(',');

            // 종목의 과거 데이터 1000일 치 추가 로드
            const response = await statisticsApi.getMovingAverageChart(stkCd, 1000, allInvestors, period, oldestStockDate);
            const data = response.data;

            if (data.data && data.data.length > 0) {
                // 기존 데이터 앞에 새 데이터 추가 (중복 제거)
                const existingDates = new Set(stockDataRef.current.map(d => d.dt));
                const newData = data.data.filter((d: any) => !existingDates.has(d.dt));

                if (newData.length > 0) {
                    const oldLength = stockDataRef.current.length;
                    stockDataRef.current = [...newData, ...stockDataRef.current];

                    console.log(`[loadMoreStockData] 종목 ${stkCd}: ${newData.length}개 데이터 추가 (${oldLength} → ${stockDataRef.current.length})`);

                    // 종목 시리즈 업데이트
                    if (stockSeriesRef.current && chartRef.current) {
                        // 선택된 투자자들의 합산값 계산
                        const stockMaData: LineData<Time>[] = stockDataRef.current
                            .map(d => {
                                const investors = selectedInvestors.map(inv => getDataKey(inv));
                                const sum = investors.reduce((acc, key) => acc + (d[key] || 0), 0);
                                return {
                                    time: formatDateForChart(d.dt) as Time,
                                    value: sum
                                };
                            })
                            .filter(d => d.value !== null && d.value !== undefined)
                            .sort((a, b) => (a.time as string).localeCompare(b.time as string));

                        stockSeriesRef.current.setData(stockMaData);
                        console.log(`[loadMoreStockData] 종목 시리즈 업데이트 완료`);
                    }
                }
            }
        } catch (err) {
            console.error(`[loadMoreStockData] 종목 ${stkCd} 과거 데이터 로드 실패:`, err);
        }
    };

    // 종목 이동평균 데이터 조회 및 차트 추가 (REQ-004-1)
    const fetchStockMaData = async (stkCd: string) => {
        if (!stkCd || !chartRef.current) return;

        try {
            console.log(`[fetchStockMaData] 종목 ${stkCd} 이동평균 데이터 조회 시작`);

            const loadDays = MA_LOAD_DAYS[period] || 1500;
            const allInvestors = INVESTOR_TYPES.map(inv => inv.key).join(',');

            const response = await statisticsApi.getMovingAverageChart(stkCd, loadDays, allInvestors, period);
            const data = response.data;

            if (data && data.data && data.data.length > 0) {
                stockDataRef.current = data.data;

                // 최신 데이터에서 현재가와 MA 값 추출
                const latestData = data.data[data.data.length - 1];
                if (latestData) {
                    setStockCurrentPrice(latestData.curPrc || null);
                    // 선택된 투자자들의 합계를 MA 값으로 설정
                    const investors = selectedInvestors.map(inv => getDataKey(inv));
                    const maSum = investors.reduce((acc, key) => acc + (latestData[key] || 0), 0);
                    setStockMaValue(maSum);
                }

                // 기존 종목 시리즈 제거 (안전하게)
                if (stockSeriesRef.current && chartRef.current) {
                    try {
                        chartRef.current.removeSeries(stockSeriesRef.current);
                    } catch (e) {
                        console.log('[fetchStockMaData] 기존 시리즈 제거 중 오류 (무시됨):', e);
                    }
                    stockSeriesRef.current = null;
                }

                // 선택된 투자자들의 합산값 계산
                const stockMaData: LineData<Time>[] = stockDataRef.current
                    .map(d => {
                        const investors = selectedInvestors.map(inv => getDataKey(inv));
                        const sum = investors.reduce((acc, key) => acc + (d[key] || 0), 0);
                        return {
                            time: formatDateForChart(d.dt) as Time,
                            value: sum
                        };
                    })
                    .filter(d => d.value !== null && d.value !== undefined)
                    .sort((a, b) => (a.time as string).localeCompare(b.time as string));

                // 종목 시리즈 추가 (좌측 Y축 사용)
                const stockSeries = chartRef.current.addSeries(LineSeries, {
                    color: '#000000', // Black color for stock
                    lineWidth: 2,
                    title: `${selectedStock?.name || stkCd} ${period}일 MA`,
                    priceScaleId: 'left',
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

                stockSeries.setData(stockMaData);
                stockSeriesRef.current = stockSeries;

                console.log(`[fetchStockMaData] 종목 ${stkCd} 시리즈 추가 완료: ${stockMaData.length}개 데이터`);
            } else {
                console.warn(`[fetchStockMaData] 종목 ${stkCd} 데이터 없음`);
            }
        } catch (err) {
            console.error(`[fetchStockMaData] 종목 ${stkCd} 데이터 조회 실패:`, err);
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
                borderColor: '#000000', // Black for stock MA
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

            // 종목 MA 데이터 추가 (선택된 경우)
            if (stockSeriesRef.current) {
                const stockData = param.seriesData.get(stockSeriesRef.current);
                if (stockData && 'value' in stockData) {
                    values.push({
                        key: 'stock',
                        label: `${selectedStock?.name || '종목'} ${period}일 MA`,
                        value: Math.round(stockData.value as number),
                        color: '#000000', // 검은색
                    });
                }
            }

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
        // 데이터가 이미 로드된 상태에서 투자자 선택 변경 시 시리즈 재생성
        if (allDataRef.current.length > 0 && chartRef.current) {
            console.log('[useEffect] 투자자 선택 변경 - 시리즈 재생성');
            recreateSeries(false);
        }
    }, [selectedInvestors, recreateSeries]);

    // period 변경 시 재조회
    useEffect(() => {
        if (sectorCd) {
            fetchChartData(sectorCd);
            // 종목 데이터는 fetchChartData 내부에서 처리됨
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

    // 모든 섹터 데이터 조회 (REQ-005)
    const fetchAllSectorsData = async () => {
        setLoading(true);
        setError(null);

        try {
            const loadDays = MA_LOAD_DAYS[period] || 1500;
            const allInvestors = INVESTOR_TYPES.map(inv => inv.key).join(',');

            console.log('[fetchAllSectorsData] 모든 섹터 데이터 로드 시작');

            const response = await sectorMaApi.getAllSectorsChart(loadDays, allInvestors, period);
            const data = response.data;

            if (data && data.sectors) {
                setAllSectorsData(data);
                setIsAllSectorsMode(true);

                // 섹터와 종목 선택 해제
                setSectorCd('');
                setSectorNm('선택해제');
                setSelectedSector(null);
                setSelectedStock(null);

                // 차트 초기화 및 모든 섹터 데이터 표시
                if (chartRef.current) {
                    chartRef.current.remove();
                    chartRef.current = null;
                    seriesMapRef.current.clear();
                }

                initChart();
                displayAllSectorsData(data);

                console.log('[fetchAllSectorsData] 모든 섹터 데이터 로드 완료');
            } else {
                setError('모든 섹터 데이터를 가져올 수 없습니다.');
            }
        } catch (err) {
            console.error('모든 섹터 데이터 조회 오류:', err);
            setError(`조회 실패: ${err instanceof Error ? err.message : '알 수 없는 오류'}`);
        } finally {
            setLoading(false);
        }
    };

    // 모든 섹터 데이터 차트에 표시 (REQ-005)
    const displayAllSectorsData = (data: any) => {
        if (!chartRef.current || !data.sectors) return;

        allSectorsSeriesRef.current.clear();

        // 섹터별 색상 정의
        const sectorColors: { [key: string]: string } = {
            semicon: '#FF6B6B',
            ai_infra: '#4ECDC4',
            auto: '#45B7D1',
            battery: '#96CEB4',
            petro: '#FFEAA7',
            defense: '#DDA0DD',
            culture: '#FFB6C1',
            robot: '#87CEEB',
            bio: '#98D8C8'
        };

        Object.entries(data.sectors).forEach(([sectorCd, sectorData]: [string, any]) => {
            if (sectorData && sectorData.data && sectorData.data.length > 0) {
                const color = sectorColors[sectorCd] || '#' + Math.floor(Math.random()*16777215).toString(16);

                // 각 섹터의 투자자 합산 데이터 계산
                const seriesData = sectorData.data
                    .map((d: any) => {
                        const investors = selectedInvestors.map(inv => getDataKey(inv));
                        const sum = investors.reduce((acc: number, key: string) => acc + (d[key] || 0), 0);
                        return {
                            time: formatDateForChart(d.dt) as Time,
                            value: sum
                        };
                    })
                    .filter((d: any) => d.value !== null && d.value !== undefined)
                    .sort((a: any, b: any) => (a.time as string).localeCompare(b.time as string));

                if (seriesData.length > 0) {
                    const series = chartRef.current.addSeries(LineSeries, {
                        color: color,
                        lineWidth: 2,
                        title: sectorData.sectorNm || sectorCd,
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

                    series.setData(seriesData);
                    allSectorsSeriesRef.current.set(sectorCd, series);
                }
            }
        });
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

                {/* 이동평균분석 Chart */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 relative">
                    {/* 헤더: 제목, 이동평균 기간, 차트 컨트롤 */}
                    <div className="mb-6">
                        <div className="flex items-center justify-between mb-6">
                            <div className="flex items-center gap-3">
                                {/* 섹터 선택 드롭다운 */}
                                <div className="relative" ref={sectorDropdownRef}>
                                    <button
                                        onClick={() => setShowSectorDropdown(!showSectorDropdown)}
                                        className="px-3 py-1.5 text-sm bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 flex items-center gap-2 min-w-[140px] justify-between"
                                    >
                                        <span className="font-medium text-gray-900">{sectorNm || '섹터 선택'}</span>
                                        <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </button>

                                    {/* 드롭다운 메뉴 */}
                                    {showSectorDropdown && (
                                        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                                            {/* 선택 해제 옵션 (REQ-005) */}
                                            <button
                                                onClick={() => {
                                                    setSectorCd('');
                                                    setSectorNm('');
                                                    setSelectedSector(null);
                                                    setShowSectorDropdown(false);
                                                    setIsAllSectorsMode(false);

                                                    // 종목 관련 상태 초기화
                                                    setSelectedStock(null);
                                                    setStockCurrentPrice(null);
                                                    setStockMaValue(null);

                                                    // 차트 초기화
                                                    if (chartRef.current) {
                                                        // 모든 시리즈 제거
                                                        seriesMapRef.current.forEach(series => {
                                                            try {
                                                                chartRef.current?.removeSeries(series);
                                                            } catch (e) {
                                                                console.log('[선택 해제] 시리즈 제거 중 오류 (무시됨):', e);
                                                            }
                                                        });
                                                        seriesMapRef.current.clear();

                                                        // 종목 시리즈 제거
                                                        if (stockSeriesRef.current) {
                                                            try {
                                                                chartRef.current.removeSeries(stockSeriesRef.current);
                                                            } catch (e) {
                                                                console.log('[선택 해제] 종목 시리즈 제거 중 오류 (무시됨):', e);
                                                            }
                                                            stockSeriesRef.current = null;
                                                        }

                                                        // 모든 섹터 시리즈 제거
                                                        allSectorsSeriesRef.current.forEach(series => {
                                                            try {
                                                                chartRef.current?.removeSeries(series);
                                                            } catch (e) {
                                                                console.log('[선택 해제] 섹터 시리즈 제거 중 오류 (무시됨):', e);
                                                            }
                                                        });
                                                        allSectorsSeriesRef.current.clear();
                                                    }

                                                    // 데이터 초기화
                                                    allDataRef.current = [];
                                                    setAllSectorsData(null);
                                                }}
                                                className="w-full px-4 py-3 text-left hover:bg-gray-50 flex items-center justify-between border-b border-gray-200"
                                            >
                                                <span className="font-medium text-gray-500 text-sm italic">선택 해제</span>
                                            </button>
                                            {sectors.map((sector) => (
                                                <button
                                                    key={sector.sectorCd}
                                                    onClick={() => {
                                                        setSectorCd(sector.sectorCd);
                                                        setSectorNm(sector.sectorNm);
                                                        setSelectedSector(sector);
                                                        setShowSectorDropdown(false);
                                                        setIsAllSectorsMode(false);

                                                        // 종목 관련 상태 초기화
                                                        setSelectedStock(null);
                                                        setStockCurrentPrice(null);
                                                        setStockMaValue(null);

                                                        // 모든 섹터 시리즈 제거
                                                        if (chartRef.current) {
                                                            allSectorsSeriesRef.current.forEach(series => {
                                                                try {
                                                                    chartRef.current?.removeSeries(series);
                                                                } catch (e) {
                                                                    console.log('[섹터 변경] 모든 섹터 시리즈 제거 중 오류 (무시됨):', e);
                                                                }
                                                            });
                                                            allSectorsSeriesRef.current.clear();
                                                        }

                                                        // clearStock을 true로 설정하여 종목 데이터를 로드하지 않음
                                                        fetchChartData(sector.sectorCd, true);
                                                    }}
                                                    className="w-full px-4 py-3 text-left hover:bg-purple-50 flex items-center justify-between border-b border-gray-100 last:border-b-0"
                                                >
                                                    <span className="font-medium text-gray-900 text-sm">{sector.sectorNm}</span>
                                                    <span className="text-gray-500 text-xs">{sector.sectorCd}</span>
                                                </button>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* 종목 선택 드롭다운 (REQ-004-1) */}
                                {sectorCd && stocks.length > 0 && (
                                    <div className="relative ml-2" ref={stockDropdownRef}>
                                        <button
                                            onClick={() => setShowStockDropdown(!showStockDropdown)}
                                            className="px-3 py-1.5 text-sm bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 flex items-center gap-2 min-w-[180px] justify-between"
                                        >
                                            <span className="font-medium text-gray-900">{selectedStock?.name || '종목 선택'}</span>
                                            <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                            </svg>
                                        </button>

                                        {/* 종목 드롭다운 메뉴 */}
                                        {showStockDropdown && (
                                            <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                                                {/* 선택 해제 옵션 */}
                                                <button
                                                    onClick={() => {
                                                        setSelectedStock(null);
                                                        setStockCurrentPrice(null);
                                                        setStockMaValue(null);
                                                        setShowStockDropdown(false);
                                                        // 종목 시리즈 제거 (안전하게)
                                                        if (stockSeriesRef.current && chartRef.current) {
                                                            try {
                                                                chartRef.current.removeSeries(stockSeriesRef.current);
                                                            } catch (e) {
                                                                console.log('[선택 해제] 시리즈 제거 중 오류 (무시됨):', e);
                                                            }
                                                            stockSeriesRef.current = null;
                                                        }
                                                    }}
                                                    className="w-full px-4 py-3 text-left hover:bg-gray-50 flex items-center justify-between border-b border-gray-100"
                                                >
                                                    <span className="font-medium text-gray-500 text-sm italic">선택 해제</span>
                                                </button>
                                                {stocks.map((stock) => (
                                                    <button
                                                        key={stock.code}
                                                        onClick={() => {
                                                            setSelectedStock(stock);
                                                            setShowStockDropdown(false);
                                                            fetchStockMaData(stock.code);
                                                        }}
                                                        className={`w-full px-4 py-3 text-left hover:bg-purple-50 flex items-center justify-between border-b border-gray-100 last:border-b-0 ${selectedStock?.code === stock.code ? 'bg-purple-50' : ''
                                                            }`}
                                                    >
                                                        <span className="font-medium text-gray-900 text-sm">{stock.name}</span>
                                                        <span className="text-gray-500 text-xs">{stock.code}</span>
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                )}

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
                                <button
                                    onClick={() => {
                                        // 모든 섹터 데이터 조회 모드로 전환 (REQ-005)
                                        setIsAllSectorsMode(true);
                                        setSectorCd('');
                                        setSectorNm('업종전체');
                                        setSelectedSector(null);
                                        setSelectedStock(null);
                                        setStockCurrentPrice(null);
                                        setStockMaValue(null);

                                        // 개별 섹터 시리즈 제거
                                        if (chartRef.current) {
                                            seriesMapRef.current.forEach(series => {
                                                try {
                                                    chartRef.current?.removeSeries(series);
                                                } catch (e) {
                                                    console.log('[업종전체] 시리즈 제거 중 오류 (무시됨):', e);
                                                }
                                            });
                                            seriesMapRef.current.clear();

                                            if (stockSeriesRef.current) {
                                                try {
                                                    chartRef.current.removeSeries(stockSeriesRef.current);
                                                } catch (e) {
                                                    console.log('[업종전체] 종목 시리즈 제거 중 오류 (무시됨):', e);
                                                }
                                                stockSeriesRef.current = null;
                                            }
                                        }

                                        // 모든 섹터 데이터 조회
                                        fetchAllSectorsData();
                                    }}
                                    className={`ml-2 px-3 py-1 border rounded-lg text-xs font-medium ${
                                        isAllSectorsMode
                                            ? 'bg-purple-600 border-purple-600 text-white'
                                            : 'bg-gray-100 border-gray-300 text-gray-700 hover:bg-gray-200'
                                    }`}
                                >
                                    업종전체
                                </button>
                            </div>
                        </div>

                        {/* 투자자 토글 버튼 - 높이 확대 (REQ-007 UI 통일) */}
                        <div className="flex flex-wrap gap-1.5 items-end py-2">
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
                            오른쪽 Y축: 투자자 순매수 이동평균 수량
                        </p>
                    </div>
                    <div
                        ref={chartContainerRef}
                        className="w-full relative bg-white border border-gray-100 rounded-xl shadow-sm overflow-hidden"
                        style={{ height: 'calc(100vh - 380px)', minHeight: '450px' }}
                    >
                        {/* 빈 상태 메시지 - 차트 컨테이너 내부로 이동 */}
                        {allDataRef.current.length === 0 && !loading && !error && (
                            <div className="absolute inset-0 flex flex-col items-center justify-center text-gray-400 bg-gray-50">
                                <TrendingUp className="w-16 h-16 mb-4" />
                                <p>섹터를 선택해주세요.</p>
                            </div>
                        )}
                    </div>

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
                </div>
            </div >
        </div >
    );
};

export default SectorMovingAverageChart;
