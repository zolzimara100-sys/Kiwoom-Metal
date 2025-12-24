
import { createChart, IChartApi, ISeriesApi, LineSeries } from 'lightweight-charts';
import { Clock, Search, TrendingUp } from 'lucide-react';
import React, { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { analysisApi, stockListApi, StockSearchResult } from '../services/api';

// --- Types ---

interface AccumulationData {
    stkCd: string;
    dt: string;
    curPrc: number;
    // Daily Raw
    indInvsr: number;
    frgnrInvsr: number;
    orgn: number;
    // Cumulative Qty
    indInvsrNetBuyQty: number;
    frgnrInvsrNetBuyQty: number;
    orgnNetBuyQty: number;
    frgnrInvsrOrgnNetBuyQty: number;
    // Cumulative Amount
    indInvsrNetBuyAmount: number;
    frgnrInvsrNetBuyAmount: number;
    orgnNetBuyAmount: number;
    frgnrInvsrOrgnNetBuyAmount: number;
    // Dynamic keys for other investors
    [key: string]: any;
}

// 투자자 유형 정의 (MovingAverageChart와 통일)
const INVESTOR_TYPES = [
    { key: 'frgnr', label: '외국인', color: '#EF4444', dataKeyPrefix: 'frgnrInvsr' },
    { key: 'orgn', label: '기관계', color: '#3B82F6', dataKeyPrefix: 'orgn' },
    { key: 'ind', label: '개인', color: '#10B981', dataKeyPrefix: 'indInvsr' },
    { key: 'frgnrOrgn', label: '외인+기관', color: '#8B5CF6', dataKeyPrefix: 'frgnrInvsrOrgn' },
    // 필요 시 추가 (금융투자 등)
    { key: 'fnncInvt', label: '금융투자', color: '#059669', dataKeyPrefix: 'fnncInvt' },
    { key: 'insrnc', label: '보험', color: '#D97706', dataKeyPrefix: 'insrnc' },
    { key: 'invtrt', label: '투신', color: '#4F46E5', dataKeyPrefix: 'invtrt' },
    { key: 'penfndEtc', label: '연기금등', color: '#0D9488', dataKeyPrefix: 'penfndEtc' },
    { key: 'samoFund', label: '사모펀드', color: '#F97316', dataKeyPrefix: 'samoFund' },
    { key: 'natn', label: '국가', color: '#64748B', dataKeyPrefix: 'natn' },
    { key: 'etcCorp', label: '기타법인', color: '#84CC16', dataKeyPrefix: 'etcCorp' },
    { key: 'natfor', label: '내국인', color: '#A855F7', dataKeyPrefix: 'natfor' },
];

const format100M = (val: number) => {
    if (!val) return 0;
    return val / 100000000; // 1억 단위
};

const formatToChartDate = (dt: any) => {
    if (!dt) return '';
    if (Array.isArray(dt)) {
        const y = dt[0];
        const m = ('0' + dt[1]).slice(-2);
        const d = ('0' + dt[2]).slice(-2);
        return `${y}-${m}-${d}`;
    }
    if (typeof dt === 'string') {
        if (dt.includes('-')) return dt;
        if (dt.length === 8) return `${dt.substring(0, 4)}-${dt.substring(4, 6)}-${dt.substring(6, 8)}`;
    }
    return String(dt);
};

interface Props {
    stkCd?: string;
    hideSearch?: boolean;
    isVisible?: boolean;
}

const InvestorSupplyDemandAnalysis: React.FC<Props> = ({ stkCd: propStkCd, hideSearch, isVisible }) => {
    const [searchParams] = useSearchParams();
    const initialStkCd = propStkCd || searchParams.get('stkCd') || '';

    // --- State ---
    const [stkCd, setStkCd] = useState(initialStkCd);
    const [searchKeyword, setSearchKeyword] = useState(initialStkCd);
    const [searchResults, setSearchResults] = useState<StockSearchResult[]>([]);
    const [showDropdown, setShowDropdown] = useState(false);
    const [focusedIndex, setFocusedIndex] = useState(-1);

    // UI Controls
    const [selectedInvestors, setSelectedInvestors] = useState<string[]>(['frgnr', 'orgn']);
    const [tooltip, setTooltip] = useState<any>(null);
    const [showPrice, setShowPrice] = useState(true);

    // Data
    const [data, setData] = useState<AccumulationData[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [startDate, setStartDate] = useState<Date>(new Date(new Date().setFullYear(new Date().getFullYear() - 4))); // Default ~4 years ago

    // REQ-007: 투자자별 거래 비중
    const [investorRatios, setInvestorRatios] = useState<{ [key: string]: number }>({});

    // --- Refs ---
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const seriesMapRef = useRef<Map<string, ISeriesApi<'Line'>>>(new Map()); // Key format: "type-investor" e.g., "qty-frgnr" or "amt-orgn"
    const searchRef = useRef<HTMLDivElement>(null);

    // --- Handlers ---
    const toggleInvestor = (key: string) => {
        setSelectedInvestors(prev =>
            prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key]
        );
    };

    // YYYYMMDD -> Date Object
    const parseDate = (str: string) => {
        const y = parseInt(str.substring(0, 4));
        const m = parseInt(str.substring(4, 6)) - 1;
        const d = parseInt(str.substring(6, 8));
        return new Date(y, m, d);
    };

    // Date -> YYYYMMDD
    const formatDate = (date: Date) => {
        const y = date.getFullYear();
        const m = ('0' + (date.getMonth() + 1)).slice(-2);
        const d = ('0' + date.getDate()).slice(-2);
        return `${y}${m}${d}`;
    };

    const handleLoadMore = () => {
        // Load additional 4 years
        const newStart = new Date(startDate);
        newStart.setFullYear(newStart.getFullYear() - 4);
        setStartDate(newStart);
        // fetchData will be triggered by useEffect dependency on startDate? 
        // Or specific call. Let's make fetchData append logic.
        fetchData(stkCd, newStart, true);
    };

    // --- Sync Prop StkCd ---
    useEffect(() => {
        if (propStkCd) {
            if (propStkCd !== stkCd || data.length === 0) {
                setStkCd(propStkCd);
                setSearchKeyword(propStkCd); // Placeholder

                // Fetch stock name to display "Name (Code)"
                stockListApi.search(propStkCd).then(res => {
                    if (res.data) {
                        const match = res.data.find(s => s.code === propStkCd);
                        if (match) {
                            setSearchKeyword(`${match.name} (${match.code})`);
                        }
                    }
                }).catch(console.error);

                // Reset date to default 4 years when stock changes or reload
                const defDate = new Date();
                defDate.setFullYear(defDate.getFullYear() - 4);
                setStartDate(defDate);
                fetchData(propStkCd, defDate, false);
            }
        }
    }, [propStkCd]);

    // Initial Load
    useEffect(() => {
        if (initialStkCd && !propStkCd && data.length === 0) {
            const defDate = new Date();
            defDate.setFullYear(defDate.getFullYear() - 4);
            setStartDate(defDate);
            fetchData(initialStkCd, defDate, false);
        }
    }, []); // Run once on mount if initialStkCd exists

    // Keyboard Navigation
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!showDropdown || searchResults.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setFocusedIndex(prev => Math.min(prev + 1, searchResults.length - 1));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setFocusedIndex(prev => Math.max(prev - 1, 0));
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (focusedIndex >= 0 && focusedIndex < searchResults.length) {
                handleSelectStock(searchResults[focusedIndex]);
            } else if (searchResults.length === 1) {
                handleSelectStock(searchResults[0]);
            }
        }
    };

    // Search
    const handleSearch = async (keyword: string) => {
        setSearchKeyword(keyword);
        setFocusedIndex(-1);
        if (keyword.length < 1) {
            setSearchResults([]);
            setShowDropdown(false);
            return;
        }
        try {
            const results = await stockListApi.search(keyword);
            setSearchResults(results.data || []);
            setShowDropdown(true);
        } catch (err) { console.error(err); }
    };

    const handleSelectStock = (stock: StockSearchResult) => {
        setStkCd(stock.code);
        setSearchKeyword(`${stock.name} (${stock.code})`);
        setShowDropdown(false);
        const defDate = new Date();
        defDate.setFullYear(defDate.getFullYear() - 4);
        setStartDate(defDate);
        fetchData(stock.code, defDate, false);
    };

    // --- Data Fetching ---
    const fetchData = async (code: string, startDtVal: Date, append: boolean) => {
        if (!code) return;
        setLoading(true);
        setError(null);
        try {
            const startStr = formatDate(startDtVal);
            // endDt should be not specified to get up to latest, OR if append, 
            // maybe we are fetching OLDER data?
            // "Load More" means loading older data.
            // If we fetch range [NewStartDate, Today], we get everything.
            // If append=true, it means we are expanding the range backwards.
            // API returns list. We just replace `data` with result because result will include [NewStartDate ~ Today].
            // Warning: If result is huge, replace might be heavy. But 8 years ~ 4000 rows is fine.

            const res = await analysisApi.getSupplyDemand(code, startStr);
            if (res.data) {
                // Ensure unique and sorted
                const uniqueData = res.data; // API should return ordered by Date ASC
                setData(uniqueData);
                if (uniqueData.length === 0) setError('데이터가 없습니다.');

                // REQ-007: 투자자 비중 조회
                try {
                    const { statisticsApi } = await import('../services/api');
                    const ratioRes = await statisticsApi.getInvestorRatio(code);
                    if (ratioRes.data) {
                        setInvestorRatios({
                            frgnr: ratioRes.data.frgnr || 0,
                            orgn: ratioRes.data.orgn || 0,
                            ind: ratioRes.data.ind || 0,
                            fnncInvt: ratioRes.data.fnncInvt || 0,
                            insrnc: ratioRes.data.insrnc || 0,
                            invtrt: ratioRes.data.invtrt || 0,
                            bank: ratioRes.data.bank || 0,
                            etcFnnc: ratioRes.data.etcFnnc || 0,
                            penfndEtc: ratioRes.data.penfndEtc || 0,
                            samoFund: ratioRes.data.samoFund || 0,
                            natn: ratioRes.data.natn || 0,
                            etcCorp: ratioRes.data.etcCorp || 0,
                            natfor: ratioRes.data.natfor || 0,
                        });
                    }
                } catch (e) {
                    console.warn('투자자 비중 조회 실패:', e);
                }
            } else {
                setError(res.error || '데이터 조회 실패');
            }
        } catch (err) {
            setError('API Error');
        } finally {
            setLoading(false);
        }
    };

    // --- Chart Rendering ---
    useEffect(() => {
        if (!chartContainerRef.current || data.length === 0) return;

        // Cleanup
        if (chartRef.current) {
            chartRef.current.remove();
            chartRef.current = null;
            seriesMapRef.current.clear();
        }

        // Create Chart
        const chart = createChart(chartContainerRef.current, {
            width: chartContainerRef.current.clientWidth || 1000,
            height: 700,
            layout: { background: { color: '#ffffff' }, textColor: '#333' },
            grid: { vertLines: { color: '#f0f0f0' }, horzLines: { color: '#f0f0f0' } },
            // Left Axis: Qty
            leftPriceScale: {
                visible: true,
                borderColor: '#e0e0e0',
                scaleMargins: { top: 0.1, bottom: 0.1 },
            },
            // Right Axis: Amount (100M)
            rightPriceScale: {
                visible: true,
                borderColor: '#e0e0e0',
                scaleMargins: { top: 0.1, bottom: 0.1 },
            },
            handleScale: {
                axisPressedMouseMove: true,
                mouseWheel: true,
                pinch: true,
            },
            timeScale: {
                minBarSpacing: 0.001,
            },
            // Overlay Scale (Current Price) - Hidden but autoscaled
            crosshair: {
                vertLine: { labelVisible: false },
                horzLine: { labelVisible: false },
            },
        });

        chartRef.current = chart;

        // Add Price Series (Overlay)
        if (showPrice) {
            const priceSeries = chart.addSeries(LineSeries, {
                color: '#000000',
                lineWidth: 1,
                title: '현재가',
                priceScaleId: 'left', // Conflict: Left is Qty.
                // If we put it on Left, large Qty (1M) vs Price (50K).
                // They might overlap messily.
                // Ideally use `priceScaleId: 'price-overlay'` (custom ID).
                // Lightweight Charts supports custom ID, but it won't show labels on Y-axis.
            });
            // However, to make it visible without labels, we must use custom scale ID.
            // But let's try mapping to 'right' just to see? No, Amount (1000 100M = 100B) vs Price (50K).
            // We MUST use separate scale.
            priceSeries.applyOptions({
                priceScaleId: 'curr-price-scale'
            });

            // Custom scale configuration can be applied to chart.priceScale('curr-price-scale')
            // But we just want it to auto-scale within the chart area.

            const priceData = data.map(d => ({
                time: formatToChartDate(d.dt),
                value: d.curPrc || 0
            }));
            priceSeries.setData(priceData as any);
            seriesMapRef.current.set('price', priceSeries);
        }

        // Add Selected Investors Series
        // Qty -> Left, Amount -> Right
        selectedInvestors.forEach(invKey => {
            const config = INVESTOR_TYPES.find(t => t.key === invKey);
            if (!config) return;

            // 1. Qty Series (Left) - Dashed or Thin?
            // "누적 순매수량"
            const qtySeries = chart.addSeries(LineSeries, {
                color: config.color,
                lineWidth: 1,
                lineStyle: 2, // Dashed
                title: `${config.label} (수량)`,
                priceScaleId: 'left',
                priceFormat: {
                    type: 'custom',
                    formatter: (val: number) => `${(val / 1000).toFixed(0)}K`,
                },
            });
            const qtyData = data.map(d => ({
                time: formatToChartDate(d.dt),
                value: Number(d[`${config.dataKeyPrefix}NetBuyQty`] || 0)
            }));
            qtySeries.setData(qtyData as any);
            seriesMapRef.current.set(`qty_${invKey}`, qtySeries);

            // 2. Amount Series (Right) - Solid, Thick
            // "누적 순매수 금액"
            const amtSeries = chart.addSeries(LineSeries, {
                color: config.color,
                lineWidth: 2,
                title: `${config.label} (금액)`,
                priceScaleId: 'right',
                priceFormat: {
                    type: 'custom',
                    formatter: (price: number) => {
                        // Already formatted to 100M in data preprocessing? No, doing it here.
                        return price.toFixed(1);
                    }
                }
            });
            const amtData = data.map(d => ({
                time: formatToChartDate(d.dt),
                value: format100M(Number(d[`${config.dataKeyPrefix}NetBuyAmount`] || 0))
            }));
            amtSeries.setData(amtData as any);
            seriesMapRef.current.set(`amt_${invKey}`, amtSeries);
        });

        // Tooltip Handler (Crosshair)
        chart.subscribeCrosshairMove(param => {
            if (
                param.point === undefined ||
                !param.time ||
                param.point.x < 0 ||
                param.point.x > chartContainerRef.current!.clientWidth ||
                param.point.y < 0 ||
                param.point.y > chartContainerRef.current!.clientHeight
            ) {
                setTooltip(null);
            } else {
                const dateStr = param.time as string;
                // Get Price
                const priceSeries = seriesMapRef.current.get('price');
                let price = 0;
                if (priceSeries) {
                    const pData = param.seriesData.get(priceSeries) as any;
                    if (pData) price = pData.value || pData.close || 0;
                }

                // Get Investors
                const investorsData = selectedInvestors.map(key => {
                    const conf = INVESTOR_TYPES.find(t => t.key === key);
                    const qtyS = seriesMapRef.current.get(`qty_${key}`);
                    const amtS = seriesMapRef.current.get(`amt_${key}`);
                    const qData = param.seriesData.get(qtyS) as any;
                    const aData = param.seriesData.get(amtS) as any;
                    return {
                        label: conf?.label,
                        color: conf?.color,
                        qty: qData?.value || 0,
                        amt: aData?.value || 0
                    };
                }).filter(i => i.label); // Filter undefined

                setTooltip({
                    x: param.point.x,
                    y: param.point.y,
                    date: dateStr,
                    price,
                    investors: investorsData
                });
            }
        });

        // Fit Content (Explicit Range)
        if (data.length > 0) {
            try {
                const firstDt = formatToChartDate(data[0].dt);
                const lastDt = formatToChartDate(data[data.length - 1].dt);
                chart.timeScale().setVisibleRange({
                    from: firstDt as any,
                    to: lastDt as any,
                });
            } catch (e) {
                chart.timeScale().fitContent();
            }
        } else {
            chart.timeScale().fitContent();
        }
        // Safety net
        setTimeout(() => chart.timeScale().fitContent(), 100);

        // Resize Observer
        const resizeObserver = new ResizeObserver(() => {
            if (chartContainerRef.current) {
                chart.applyOptions({
                    width: chartContainerRef.current.clientWidth,
                    height: chartContainerRef.current.clientHeight
                });
            }
        });
        resizeObserver.observe(chartContainerRef.current);

        return () => {
            resizeObserver.disconnect();
            chart.remove();
            chartRef.current = null;
            seriesMapRef.current.clear();
        };

    }, [data, selectedInvestors, showPrice]);

    // Refit when becoming visible
    useEffect(() => {
        if (isVisible && chartRef.current && data.length > 0) {
            // Small delay to allow DOM to settle after display:block
            setTimeout(() => {
                try {
                    const firstDt = formatToChartDate(data[0].dt);
                    const lastDt = formatToChartDate(data[data.length - 1].dt);
                    chartRef.current?.timeScale().setVisibleRange({
                        from: firstDt as any,
                        to: lastDt as any,
                    });
                } catch (e) {
                    chartRef.current?.timeScale().fitContent();
                }
            }, 100);
        }
    }, [isVisible, data]);

    return (
        <div className="flex flex-col h-full">
            <div className="flex-1 w-full max-w-[1800px] mx-auto px-4 py-6 flex flex-col gap-6">
                {/* Internal Search Bar (Hidden if prop passed) */}
                {!hideSearch && (
                    <div className="relative z-20" ref={searchRef}>
                        {/* Same Search UI as before */}
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-5 h-5" />
                            <input
                                type="text"
                                className="w-full pl-10 pr-10 py-3 bg-white border rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="종목명 또는 코드 입력..."
                                value={searchKeyword}
                                onChange={(e) => handleSearch(e.target.value)}
                                onKeyDown={handleKeyDown}
                            />
                            {/* ... Dropdown ... */}
                        </div>
                        {showDropdown && searchResults.length > 0 && (
                            <div className="absolute w-full mt-1 bg-white border rounded-lg shadow-xl max-h-96 overflow-y-auto">
                                {searchResults.map((stock, index) => (
                                    <button
                                        key={stock.code}
                                        className={`w-full px-4 py-3 text-left hover:bg-gray-50 flex items-center justify-between border-b last:border-0 ${index === focusedIndex ? 'bg-blue-50' : ''
                                            }`}
                                        onClick={() => handleSelectStock(stock)}
                                    >
                                        <div className="font-medium">{stock.name}</div>
                                        <div className="text-sm text-gray-500">{stock.code}</div>
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* Header Controls - REQ-007 */}
                <div className="bg-white p-2 rounded-lg shadow-sm">
                    <div className="flex items-center gap-4 mb-2">
                        {/* +4 Years Button */}
                        <button
                            onClick={handleLoadMore}
                            disabled={loading}
                            className="px-3 py-1 bg-white border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50 flex items-center gap-1 shadow-sm transition-colors"
                        >
                            <Clock className="w-3 h-3" />
                            과거 +4년
                        </button>
                    </div>

                    {/* Investor Toggles - REQ-007: 넓은 고정 버튼 + 가로 막대그래프 */}
                    <div className="flex flex-wrap gap-1.5 items-end w-full">
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
                                        {inv.key !== 'frgnrOrgn' && (
                                            <>
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
                                            </>
                                        )}
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
                </div>

                {/* Chart Area */}
                <div className="bg-white p-2 rounded-lg shadow-sm flex-1 relative">
                    {loading && (
                        <div className="absolute inset-0 flex items-center justify-center bg-white/80 z-10">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                        </div>
                    )}
                    {error && (
                        <div className="absolute inset-0 flex items-center justify-center text-red-500">
                            {error}
                        </div>
                    )}
                    <div ref={chartContainerRef} className="relative w-full h-[calc(100vh-340px)] min-h-[400px]">
                        {tooltip && (
                            <div
                                className="absolute bg-white/90 border border-gray-200 shadow-lg rounded-lg p-3 text-sm z-50 pointer-events-none backdrop-blur-sm"
                                style={{
                                    left: tooltip.x > (chartContainerRef.current?.clientWidth || 1000) / 2
                                        ? tooltip.x - 240
                                        : tooltip.x + 20,
                                    top: tooltip.y + 20,
                                }}
                            >
                                <div className="font-bold text-gray-900 border-b border-gray-200 pb-1 mb-2">
                                    {tooltip.date}
                                </div>
                                <div className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
                                    <div className="text-gray-600">현재가:</div>
                                    <div className="font-mono text-right font-medium">{Math.round(tooltip.price).toLocaleString()}원</div>

                                    {tooltip.investors.map((inv: any, idx: number) => (
                                        <React.Fragment key={idx}>
                                            <div className="col-span-2 mt-1 font-semibold" style={{ color: inv.color }}>
                                                {inv.label}
                                            </div>
                                            <div className="text-gray-500 pl-2">누적금액:</div>
                                            <div className="font-mono text-right">{Math.round(inv.amt).toLocaleString()}억</div>
                                            <div className="text-gray-500 pl-2">누적수량:</div>
                                            <div className="font-mono text-right">{Math.round(inv.qty).toLocaleString()}주</div>
                                        </React.Fragment>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Manual Legend / Explanation */}
                <div className="text-xs text-gray-500 text-right px-2">
                    * 좌측 Y축: 누적 순매수량 (점선), 우측 Y축: 누적 순매수 금액(1억 단위, 실선)
                </div>
            </div>
        </div>
    );
};

export default InvestorSupplyDemandAnalysis;
