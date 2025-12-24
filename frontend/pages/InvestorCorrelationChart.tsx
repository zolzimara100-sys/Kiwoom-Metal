import { createChart, IChartApi, ISeriesApi, LineData, LineSeries, Time } from 'lightweight-charts';
import { Clock, Info, RotateCcw, ZoomIn, ZoomOut } from 'lucide-react';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { statisticsApi } from '../services/api';

interface CorrelationDataPoint {
    dt: string;
    curPrc: number;
    frgnrCorr: number;
    orgnCorr: number;
    indCorr: number;
    fnncInvtCorr?: number;
    insrncCorr?: number;
    invtrtCorr?: number;
    bankCorr?: number;
    penfndEtcCorr?: number;
    samoFundCorr?: number;
    natnCorr?: number;
    etcCorpCorr?: number;
    natforCorr?: number;
    [key: string]: string | number | undefined;
}

interface ChartResponse {
    stkCd: string;
    sector: string;
    corrDays: number;
    data: CorrelationDataPoint[];
    message: string;
}

interface Props {
    stkCd: string;
    mainChartApi?: IChartApi | null; // 메인 차트 (동기화용 - Optional)
    syncHandler?: (subChart: IChartApi) => void; // 역방향 동기화 등록용 핸들러 - Optional
}

// 투자자 유형 정의 (이동평균과 동일)
const INVESTOR_TYPES = [
    { key: 'frgnrCorr', label: '외국인', color: '#EF4444' },
    { key: 'orgnCorr', label: '기관계', color: '#3B82F6' },
    { key: 'fnncInvtCorr', label: '금융투자', color: '#10B981' },
    { key: 'insrncCorr', label: '보험', color: '#F59E0B' },
    { key: 'invtrtCorr', label: '투신', color: '#6366F1' },
    { key: 'bankCorr', label: '은행', color: '#EC4899' },
    { key: 'penfndEtcCorr', label: '연기금등', color: '#14B8A6' },
    { key: 'samoFundCorr', label: '사모펀드', color: '#F97316' },
    { key: 'natnCorr', label: '국가', color: '#64748B' },
    { key: 'etcCorpCorr', label: '기타법인', color: '#84CC16' },
    { key: 'natforCorr', label: '내국인', color: '#A855F7' },
];

const MA_PERIODS = [5, 10, 20, 60];

// 초기 로드 데이터 양
const CORR_LOAD_DAYS: Record<number, number> = {
    5: 750,
    10: 750,
    20: 1000,
    60: 1000,
};

// 초기 화면 표시 범위
const CORR_VISIBLE_DAYS: Record<number, number> = {
    5: 250,
    10: 375,
    20: 500,
    60: 500,
};

const formatDateForChart = (dateStr: string): string => {
    return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
};

const InvestorCorrelationChart: React.FC<Props> = ({ stkCd, mainChartApi, syncHandler }) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const seriesMapRef = useRef<Map<string, ISeriesApi<'Line'>>>(new Map());
    const priceSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);
    const allDataRef = useRef<CorrelationDataPoint[]>([]);
    const isLoadingMoreRef = useRef(false);
    const oldestDateRef = useRef<string | null>(null);
    const hasMoreRef = useRef(true);

    const [period, setPeriod] = useState(20); // 기본값 20일
    const [loading, setLoading] = useState(false);
    const [activeInvestors, setActiveInvestors] = useState(['frgnrCorr', 'orgnCorr']);
    const [showPrice, setShowPrice] = useState(true);
    const [isSyncing, setIsSyncing] = useState(false);
    const [syncError, setSyncError] = useState<string | null>(null);

    // Add state for tooltip data
    const [tooltipData, setTooltipData] = useState<{ date: string; curPrc: number; netBuyAmount: number; netBuyQty: number } | null>(null);

    // 데이터를 시리즈 형식으로 변환
    const prepareSeriesData = useCallback((investorKey: string): LineData<Time>[] => {
        try {
            let seriesData: LineData<Time>[] = allDataRef.current
                .filter(d => {
                    const value = d[investorKey];
                    if (value === null || value === undefined) return false;
                    if (!d.dt || d.dt.length !== 8) return false;
                    return true;
                })
                .map(d => {
                    const value = d[investorKey] as number;
                    return {
                        time: formatDateForChart(d.dt) as Time,
                        value: typeof value === 'number' ? value : 0,
                    };
                });

            seriesData.sort((a, b) => (a.time as string).localeCompare(b.time as string));

            const uniqueData = seriesData.filter((item, index, self) =>
                index === self.findIndex((t) => t.time === item.time)
            );

            return uniqueData;
        } catch (err) {
            console.error(`[prepareSeriesData] ${investorKey} 데이터 변환 실패:`, err);
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

            seriesData.sort((a, b) => (a.time as string).localeCompare(b.time as string));

            const uniqueData = seriesData.filter((item, index, self) =>
                index === self.findIndex((t) => t.time === item.time)
            );

            return uniqueData;
        } catch (err) {
            console.error(`[preparePriceSeriesData] 현재가 데이터 변환 실패:`, err);
            return [];
        }
    }, []);

    // 시리즈만 업데이트
    const updateSeriesData = useCallback(() => {
        if (!chartRef.current || allDataRef.current.length === 0) return;

        seriesMapRef.current.forEach((series, investorKey) => {
            const seriesData = prepareSeriesData(investorKey);
            try {
                series.setData(seriesData);
            } catch (e) {
                console.error(`시리즈 데이터 업데이트 실패[${investorKey}]:`, e);
            }
        });

        if (priceSeriesRef.current) {
            const priceData = preparePriceSeriesData();
            try {
                priceSeriesRef.current.setData(priceData);
            } catch (e) {
                console.error(`현재가 시리즈 데이터 업데이트 실패:`, e);
            }
        }
    }, [prepareSeriesData, preparePriceSeriesData]);

    // 시리즈 생성/제거
    const recreateSeries = useCallback((shouldSetVisibleRange = true) => {
        if (!chartRef.current) return;

        try {
            // 기존 시리즈 제거
            seriesMapRef.current.forEach((series, key) => {
                try {
                    if (series && chartRef.current) {
                        chartRef.current.removeSeries(series);
                    }
                } catch (e) {
                    // 이미 제거된 시리즈는 무시
                }
            });
            seriesMapRef.current.clear();

            if (allDataRef.current.length === 0) return;

            // 0 기준선 추가
            try {
                const zeroLine = chartRef.current.addSeries(LineSeries, {
                    color: '#e0e0e0',
                    lineWidth: 1,
                    lineStyle: 2,
                    lastValueVisible: false,
                    priceLineVisible: false,
                    crosshairMarkerVisible: false,
                    priceScaleId: 'right', // 기준선은 오른쪽 축
                });

                const zeroData = allDataRef.current.map(d => ({
                    time: formatDateForChart(d.dt) as Time,
                    value: 0
                }));
                zeroLine.setData(zeroData);
            } catch (e) {
                console.warn("기준선 추가 실패:", e);
            }

            // 선택된 투자자 시리즈 추가
            activeInvestors.forEach((investorKey) => {
                try {
                    const investor = INVESTOR_TYPES.find(i => i.key === investorKey);
                    if (!investor) return;

                    const series = chartRef.current!.addSeries(LineSeries, {
                        color: investor.color,
                        lineWidth: 2,
                        title: investor.label,
                        priceFormat: {
                            type: 'custom',
                            formatter: (val: number) => val.toFixed(2),
                        },
                        priceScaleId: 'right', // 투자자는 오른쪽 축
                    });

                    const seriesData = prepareSeriesData(investorKey);

                    if (seriesData.length === 0) {
                        chartRef.current?.removeSeries(series);
                        return;
                    }

                    series.setData(seriesData);
                    seriesMapRef.current.set(investorKey, series);
                } catch (e) {
                    console.error(`투자자 시리즈 추가 실패[${investorKey}]:`, e);
                }
            });

            // 현재가 시리즈
            if (priceSeriesRef.current && chartRef.current) {
                try {
                    chartRef.current.removeSeries(priceSeriesRef.current);
                } catch (e) {
                    // 이미 제거된 시리즈는 무시
                }
                priceSeriesRef.current = null;
            }

            if (showPrice) {
                try {
                    const priceSeries = chartRef.current.addSeries(LineSeries, {
                        color: '#000000',
                        lineWidth: 2,
                        title: '현재가',
                        priceScaleId: 'left',
                        priceFormat: {
                            type: 'custom',
                            formatter: (price: number) => {
                                return price.toLocaleString() + '원';
                            },
                        },
                    });

                    const priceData = preparePriceSeriesData();
                    if (priceData.length > 0) {
                        priceSeries.setData(priceData);
                        priceSeriesRef.current = priceSeries;
                    } else {
                        chartRef.current.removeSeries(priceSeries);
                    }
                } catch (e) {
                    console.error("현재가 시리즈 추가 실패:", e);
                    // 실패 시 시리즈 참조 제거
                    if (priceSeriesRef.current) {
                        try { chartRef.current.removeSeries(priceSeriesRef.current); } catch (_) { }
                        priceSeriesRef.current = null;
                    }
                }
            }

            // 초기 표시 범위 설정
            if (shouldSetVisibleRange && allDataRef.current.length > 0) {
                try {
                    const visibleDays = CORR_VISIBLE_DAYS[period];
                    const totalDataLength = allDataRef.current.length;

                    const startIndex = Math.max(0, totalDataLength - visibleDays);
                    const startDate = allDataRef.current[startIndex].dt;
                    const endDate = allDataRef.current[totalDataLength - 1].dt;

                    const startTime = formatDateForChart(startDate) as string;
                    const endTime = formatDateForChart(endDate) as string;

                    chartRef.current.timeScale().setVisibleRange({
                        from: startTime as Time,
                        to: endTime as Time,
                    });
                } catch (e) {
                    console.error('표시 범위 설정 실패:', e);
                    chartRef.current.timeScale().fitContent();
                }
            }
        } catch (fatalError) {
            console.error("recreateSeries 치명적 오류:", fatalError);
        }
    }, [activeInvestors, prepareSeriesData, preparePriceSeriesData, showPrice, period]);

    // 날짜 비교 (A < B 로직)
    const checkDateDiff = (dateA: string, dateB: string) => {
        if (!dateA || !dateB) return false;
        return parseInt(dateA.replace(/-/g, '')) < parseInt(dateB.replace(/-/g, ''));
    };

    // Guard: remember last raw date we requested a sync for to avoid repeated triggers
    const lastRequestedRawDateRef = useRef<string | null>(null);

    // 데이터 조회
    const fetchChartData = useCallback(async (beforeDate?: string) => {
        if (!stkCd) {
            allDataRef.current = [];
            oldestDateRef.current = null;
            hasMoreRef.current = false;
            return;
        }

        setLoading(true);

        try {
            const loadDays = CORR_LOAD_DAYS[period] || 1000;

            // 1. Initial Fetch
            let response = await statisticsApi.getCorrelationChart(stkCd, period, loadDays, beforeDate);
            let data: ChartResponse = response.data;

            if (data.data && data.data.length > 0) {
                if (beforeDate) {
                    // 과거 데이터 추가
                    const existingDates = new Set(allDataRef.current.map(d => d.dt));
                    const newData = data.data.filter(d => !existingDates.has(d.dt));

                    if (newData.length > 0) {
                        allDataRef.current = [...newData, ...allDataRef.current];
                        oldestDateRef.current = newData[0].dt;
                        updateSeriesData();
                    } else {
                        hasMoreRef.current = false;
                    }
                } else {
                    // 초기 데이터
                    allDataRef.current = data.data;
                    oldestDateRef.current = data.data[0].dt;
                    hasMoreRef.current = true;
                    recreateSeries(true);
                }
            } else {
                if (!beforeDate) {
                    allDataRef.current = [];
                    oldestDateRef.current = null;
                }
                hasMoreRef.current = false;
            }
        } catch (err) {
            console.error('상관계수 차트 데이터 조회 오류:', err);
        } finally {
            setLoading(false);
        }
    }, [stkCd, period, updateSeriesData, recreateSeries, isSyncing]);

    // 과거 데이터 추가 로드
    const loadMoreData = useCallback(async () => {
        if (!stkCd || isLoadingMoreRef.current || !oldestDateRef.current || !hasMoreRef.current) {
            return;
        }

        console.log('[loadMoreData] 과거 4년 치 데이터 로드 시작:', oldestDateRef.current);
        isLoadingMoreRef.current = true;

        try {
            const timeScale = chartRef.current?.timeScale();
            const currentTimeRange = timeScale?.getVisibleRange();

            await fetchChartData(oldestDateRef.current);

            if (timeScale && currentTimeRange) {
                try {
                    timeScale.setVisibleRange(currentTimeRange);
                } catch (e) {
                    console.warn('시간 범위 복원 실패:', e);
                }
            }
        } catch (err) {
            console.error('[loadMoreData] 과거 데이터 로드 실패:', err);
        } finally {
            isLoadingMoreRef.current = false;
        }
    }, [stkCd, fetchChartData]);

    // 차트 초기화
    const initChart = useCallback(() => {
        if (!chartContainerRef.current) return;

        if (chartRef.current) {
            chartRef.current.remove();
            chartRef.current = null;
            seriesMapRef.current.clear();
        }

        const chart = createChart(chartContainerRef.current, {
            width: chartContainerRef.current.clientWidth || 1000,
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
                mode: 1,
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
        if (syncHandler) {
            syncHandler(chart);
        }

        const resizeObserver = new ResizeObserver(() => {
            if (chartContainerRef.current && chartRef.current) {
                chartRef.current.applyOptions({
                    width: chartContainerRef.current.clientWidth,
                    height: chartContainerRef.current.clientHeight,
                });
            }
        });
        resizeObserver.observe(chartContainerRef.current);

        // 무한스크롤
        let lastLogTime = 0;
        let consecutiveDragAtEdge = 0;

        chart.timeScale().subscribeVisibleTimeRangeChange((timeRange) => {
            if (!timeRange || !allDataRef.current.length) return;

            const now = Date.now();
            const shouldLog = now - lastLogTime > 1000;

            const oldestDataTime = allDataRef.current[0]?.dt;
            const newestDataTime = allDataRef.current[allDataRef.current.length - 1]?.dt;
            if (!oldestDataTime || !newestDataTime) return;

            const oldestDate = formatDateForChart(oldestDataTime);
            const newestDate = formatDateForChart(newestDataTime);
            const visibleFrom = timeRange.from as string;

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

            if (percentageFromOldest < 5 && distanceFromOldest >= 0) {
                if (shouldLog) {
                    console.log(`🤖 [자동 로드] 끝에 도달 - 과거 데이터 로드 중...`);
                }
                loadMoreData();
            }

            if (percentageFromOldest < 1) {
                consecutiveDragAtEdge++;
                if (consecutiveDragAtEdge > 3 && shouldLog) {
                    console.log(`👆 [제스처 감지] 끝에서 계속 드래그 - 과거 데이터 로드 중...`);
                    loadMoreData();
                    consecutiveDragAtEdge = 0;
                }
            } else {
                consecutiveDragAtEdge = 0;
            }
        });

        return () => {
            resizeObserver.disconnect();
        };
    }, [syncHandler, loadMoreData]);

    // 초기화
    useEffect(() => {
        initChart();
        return () => {
            if (chartRef.current) {
                chartRef.current.remove();
                chartRef.current = null;
            }
        };
    }, [initChart]);

    // 데이터 조회
    useEffect(() => {
        if (stkCd) {
            fetchChartData();
        }
    }, [stkCd, period, fetchChartData]);

    // 투자자 선택 변경 시
    useEffect(() => {
        if (allDataRef.current.length > 0 && chartRef.current) {
            recreateSeries(false);
        }
    }, [activeInvestors, showPrice, recreateSeries]);

    // Add mousemove event listener to display tooltip
    useEffect(() => {
        if (!chartRef.current) return;

        const handleCrosshairMove = (param: any) => {
            if (!param || !param.time || !param.seriesData) {
                setTooltipData(null);
                return;
            }

            const hoveredDate = param.time;
            const seriesData = param.seriesData;

            const curPrc = seriesData.find((d: any) => d.series === priceSeriesRef.current)?.value || 0;
            const netBuyAmount = activeInvestors.reduce((sum, key) => {
                const investorSeries = seriesMapRef.current.get(key);
                const value = seriesData.find((d: any) => d.series === investorSeries)?.value || 0;
                return sum + value;
            }, 0);
            const netBuyQty = netBuyAmount / curPrc || 0;

            setTooltipData({
                date: hoveredDate,
                curPrc,
                netBuyAmount,
                netBuyQty,
            });
        };

        chartRef.current.subscribeCrosshairMove(handleCrosshairMove);

        return () => {
            chartRef.current?.unsubscribeCrosshairMove(handleCrosshairMove);
        };
    }, [activeInvestors]);

    const handleZoomIn = () => {
        chartRef.current?.timeScale().scrollToPosition(-5, true);
    };

    const handleZoomOut = () => {
        chartRef.current?.timeScale().scrollToPosition(5, true);
    };

    const handleReset = () => {
        chartRef.current?.timeScale().fitContent();
    };

    const toggleInvestor = (key: string) => {
        setActiveInvestors((prev) =>
            prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
        );
    };

    // Modify the initial visible range to fill the chart area
    useEffect(() => {
        // 차트와 데이터가 모두 준비되었는지 확인
        if (!chartRef.current || allDataRef.current.length === 0) return;

        // 시리즈가 하나라도 존재하는지 확인 (시리즈 없이 범위 설정 시 오류 발생)
        if (seriesMapRef.current.size === 0 && !priceSeriesRef.current) return;

        try {
            const startDate = allDataRef.current[0]?.dt;
            const endDate = allDataRef.current[allDataRef.current.length - 1]?.dt;

            // 날짜 유효성 검사
            if (!startDate || !endDate || startDate.length !== 8 || endDate.length !== 8) {
                console.warn('초기 표시 범위 설정 건너뜀: 유효하지 않은 날짜 데이터');
                return;
            }

            const startTime = formatDateForChart(startDate) as string;
            const endTime = formatDateForChart(endDate) as string;

            chartRef.current.timeScale().setVisibleRange({
                from: startTime as Time,
                to: endTime as Time,
            });
        } catch (e) {
            console.warn('초기 표시 범위 설정 실패 - fitContent 사용:', e);
            try {
                chartRef.current?.timeScale().fitContent();
            } catch (fitError) {
                // fitContent도 실패하면 무시
            }
        }
    }, [loading, stkCd]); // Updated dependency to trigger after load

    return (
        <div className="mt-4 p-4 api-chart-container bg-white rounded-xl shadow-lg border border-gray-100">
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                    <h3 className="text-lg font-bold text-gray-800 flex items-center gap-2">
                        📊 투자자 상관분석 (Rolling Correlation)
                    </h3>

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

                    <div className="group relative">
                        <Info className="w-4 h-4 text-gray-400 cursor-help" />
                        <div className="absolute left-0 bottom-full mb-2 w-80 p-3 bg-gray-800 text-white text-xs rounded shadow-lg opacity-0 group-hover:opacity-100 transition-opacity z-10 pointer-events-none">
                            <strong>상관계수 추세 분석</strong><br />
                            주가 등락률과 각 투자자의 순매수 강도 간의 상관관계 추세입니다.<br />
                            • 1.0에 가까울수록: 해당 투자자가 살 때 주가가 오름 (주포).<br />
                            • -1.0에 가까울수록: 해당 투자자가 살 때 주가가 내림 (역상관).<br />
                            • 0 기준선 위면 '양의 상관', 아래면 '음의 상관'입니다.
                        </div>
                    </div>
                </div>

                {/* 차트 컨트롤 버튼 */}
                <div className="flex gap-2">
                    <button
                        onClick={handleZoomIn}
                        className="p-2 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-600"
                        title="확대"
                    >
                        <ZoomIn className="w-4 h-4" />
                    </button>
                    <button
                        onClick={handleZoomOut}
                        className="p-2 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-600"
                        title="축소"
                    >
                        <ZoomOut className="w-4 h-4" />
                    </button>
                    <button
                        onClick={handleReset}
                        className="p-2 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-600"
                        title="전체 보기"
                    >
                        <RotateCcw className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {/* 투자자 토글 버튼 */}
            <div className="mb-6">
                <div className="flex flex-wrap gap-2">
                    {/* 현재가 토글 */}
                    <button
                        onClick={() => setShowPrice(!showPrice)}
                        className={`px-3 py-1.5 rounded-full text-sm font-medium transition-all flex items-center gap-2 border-2 ${showPrice
                            ? 'bg-black text-white border-black'
                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200 border-gray-300'
                            }`}
                    >
                        <span
                            className="w-3 h-3 rounded-full"
                            style={{ backgroundColor: showPrice ? 'white' : '#000' }}
                        />
                        현재가
                    </button>
                    {INVESTOR_TYPES.map((inv) => (
                        <button
                            key={inv.key}
                            onClick={() => toggleInvestor(inv.key)}
                            className={`px-3 py-1.5 rounded-full text-sm font-medium transition-all flex items-center gap-2 ${activeInvestors.includes(inv.key)
                                ? 'text-white'
                                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                }`}
                            style={{
                                backgroundColor: activeInvestors.includes(inv.key) ? inv.color : undefined,
                            }}
                        >
                            <span
                                className="w-3 h-3 rounded-full"
                                style={{ backgroundColor: activeInvestors.includes(inv.key) ? 'white' : inv.color }}
                            />
                            {inv.label}
                        </button>
                    ))}
                </div>
                <p className="text-xs text-gray-500 mt-2">
                    오른쪽 Y축: 상관계수 | 왼쪽 Y축: 현재가(원)
                </p>
            </div>

            <div ref={chartContainerRef} className="w-full relative" style={{ height: 'calc(100vh - 320px)', minHeight: '500px' }}>
                {!stkCd && !loading && (
                    <div className="absolute inset-0 flex flex-col items-center justify-center text-gray-400">
                        <p>상단에서 종목을 검색하고 조회하면</p>
                        <p>해당 종목의 투자자 상관분석 차트가 표시됩니다.</p>
                    </div>
                )}

                {tooltipData && (
                    <div
                        className="absolute bg-white border border-gray-300 shadow-lg p-2 rounded text-sm"
                        style={{
                            left: `${tooltipData.x}px`,
                            top: `${tooltipData.y}px`,
                        }}
                    >
                        <p>날짜: {tooltipData.date}</p>
                        <p>현재가: {tooltipData.curPrc.toLocaleString()}원</p>
                        <p>누적 금액: {tooltipData.netBuyAmount.toLocaleString()}원</p>
                        <p>누적 수량: {tooltipData.netBuyQty.toLocaleString()}주</p>
                    </div>
                )}
            </div>

            {loading && <div className="text-center text-sm text-gray-500 mt-2">상관계수 분석 데이터 로딩 중...</div>}
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

        </div>
    );
};

export default InvestorCorrelationChart;
