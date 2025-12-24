
import { ArrowLeft, Search, X } from 'lucide-react';
import React, { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { stockListApi, StockSearchResult } from '../services/api';
import InvestorCorrelationChart from './InvestorCorrelationChart';
import InvestorSupplyDemandAnalysis from './InvestorSupplyDemandAnalysis';
import MovingAverageChart from './MovingAverageChart';
import SectorMovingAverageChart from './SectorMovingAverageChart';

const UnifiedChartPage: React.FC = () => {
    const [searchParams, setSearchParams] = useSearchParams();
    const initialStkCd = searchParams.get('stkCd') || '';

    // --- Tab State ---
    const [activeTab, setActiveTab] = useState<'ma' | 'correlation' | 'supply_demand' | 'sector_ma'>('ma');

    // --- Search & Stock State ---
    const [stkCd, setStkCd] = useState(initialStkCd);
    const [stockName, setStockName] = useState('');
    const [sector, setSector] = useState('');
    const [marketName, setMarketName] = useState('');

    const [searchKeyword, setSearchKeyword] = useState('');
    const [searchResults, setSearchResults] = useState<StockSearchResult[]>([]);
    const [showDropdown, setShowDropdown] = useState(false);
    const [focusedIndex, setFocusedIndex] = useState(-1);
    const searchRef = useRef<HTMLDivElement>(null);
    const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);

    // Auto-scroll to focused item
    useEffect(() => {
        if (focusedIndex >= 0 && itemRefs.current[focusedIndex]) {
            itemRefs.current[focusedIndex]?.scrollIntoView({
                block: 'nearest',
            });
        }
    }, [focusedIndex]);

    // Initial load if stkCd in URL
    // Initial load if stkCd in URL
    useEffect(() => {
        if (initialStkCd && !stockName) {
            setSearchKeyword(initialStkCd);

            const fetchInfo = async () => {
                try {
                    // 1. Try search API
                    let res = await stockListApi.search(initialStkCd);
                    let list = res.data || [];
                    let match = list.find(s => s.code === initialStkCd);

                    // 2. Fallback: Try KOSPI200 list if not found
                    if (!match) {
                        const kospiRes = await stockListApi.getKospi200Stocks();
                        if (kospiRes.data) {
                            match = kospiRes.data.find(s => s.code === initialStkCd);
                        }
                    }

                    if (match) {
                        setStockName(match.name);
                        setSector(match.sector || '');
                        setMarketName(match.marketName || '');
                        setSearchKeyword(`${match.name} (${match.code})`);
                    }
                } catch (e) {
                    console.error("Failed to recover stock info:", e);
                }
            };

            fetchInfo();
        }
    }, [initialStkCd]);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.nativeEvent.isComposing) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (searchResults.length > 0) {
                setFocusedIndex(prev => Math.min(prev + 1, searchResults.length - 1));
                setShowDropdown(true);
            }
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (searchResults.length > 0) {
                setFocusedIndex(prev => Math.max(prev - 1, 0));
            }
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (searchResults.length === 0) return;

            if (focusedIndex >= 0 && focusedIndex < searchResults.length) {
                handleSelectStock(searchResults[focusedIndex]);
            } else {
                // If no specific focus, select the first result (Best Match)
                handleSelectStock(searchResults[0]);
            }
        }
    };

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
        } catch (err) {
            console.error(err);
        }
    };

    const handleSelectStock = (stock: StockSearchResult) => {
        setStkCd(stock.code);
        setStockName(stock.name);
        setSector(stock.sector || '');
        setMarketName(stock.marketName || '');
        setSearchKeyword(`${stock.name} (${stock.code})`);
        setShowDropdown(false);

        // Update URL
        setSearchParams({ stkCd: stock.code });
    };

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            {/* Header */}
            <header className="bg-white border-b sticky top-0 z-30">
                <div className="w-full px-4 h-16 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <Link to="/" className="p-2 hover:bg-gray-100 rounded-full">
                            <ArrowLeft className="w-6 h-6" />
                        </Link>
                        <h1 className="text-xl font-bold hidden sm:block">차트 분석</h1>

                        {/* Selected Stock Info Badge */}
                        {stkCd && (
                            <div className="flex items-center gap-2 pl-4 border-l ml-2">
                                <span className="text-lg font-bold text-gray-900">{stockName || stkCd}</span>
                                <span className="text-sm text-gray-500">{stkCd}</span>
                                {marketName && <span className="px-2 py-0.5 bg-gray-100 text-xs rounded text-gray-600">{marketName}</span>}
                                {sector && <span className="px-2 py-0.5 bg-purple-100 text-xs rounded text-purple-700">{sector}</span>}
                            </div>
                        )}
                    </div>
                </div>
            </header>

            {/* REQ-002: Common Search Bar & Tabs - 한 줄 배치로 공간 확보 */}
            <div className="bg-white border-b sticky top-16 z-20 shadow-sm">
                <div className="w-full px-4 py-2">
                    <div className="flex items-center gap-6">
                        {/* Search Input - 축소 */}
                        <div className="relative w-80" ref={searchRef}>
                            <div className="relative">
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
                                <input
                                    type="text"
                                    className="w-full pl-9 pr-8 py-2 text-sm bg-gray-50 border border-gray-200 rounded-lg shadow-sm focus:ring-2 focus:ring-purple-500 outline-none transition-all"
                                    placeholder="종목명 또는 코드 입력"
                                    value={searchKeyword}
                                    onChange={(e) => handleSearch(e.target.value)}
                                    onKeyDown={handleKeyDown}
                                    onFocus={() => {
                                        if (searchResults.length > 0) setShowDropdown(true);
                                    }}
                                />
                                {searchKeyword && (
                                    <button
                                        onClick={() => { setSearchKeyword(''); setSearchResults([]); setStkCd(''); setStockName(''); setSearchParams({}); }}
                                        className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                                    >
                                        <X className="w-4 h-4" />
                                    </button>
                                )}
                            </div>

                            {/* Dropdown */}
                            {showDropdown && searchResults.length > 0 && (
                                <div className="absolute w-96 mt-1 bg-white border border-gray-100 rounded-lg shadow-xl max-h-80 overflow-y-auto z-50">
                                    {searchResults.map((stock, index) => (
                                        <button
                                            ref={el => itemRefs.current[index] = el}
                                            key={stock.code}
                                            className={`w-full px-4 py-2.5 text-left hover:bg-gray-50 flex items-center justify-between border-b border-gray-50 last:border-0 ${index === focusedIndex ? 'bg-purple-50' : ''
                                                }`}
                                            onClick={() => handleSelectStock(stock)}
                                        >
                                            <div>
                                                <div className="font-medium text-gray-900 text-sm">{stock.name}</div>
                                                <div className="text-xs text-gray-500">{stock.code} | {stock.marketName}</div>
                                            </div>
                                            {stock.sector && (
                                                <span className="text-xs px-2 py-0.5 bg-gray-100 rounded-full text-gray-600">
                                                    {stock.sector}
                                                </span>
                                            )}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Navigation Tabs - 버튼 스타일 */}
                        <div className="flex gap-1">
                            <button
                                onClick={() => setActiveTab('ma')}
                                className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${activeTab === 'ma'
                                    ? 'bg-purple-600 text-white'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                    }`}
                            >
                                종목 순매수량 이동평균
                            </button>
                            <button
                                onClick={() => setActiveTab('sector_ma')}
                                className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${activeTab === 'sector_ma'
                                    ? 'bg-purple-600 text-white'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                    }`}
                            >
                                업종 순매수금액 이동평균
                            </button>
                            <button
                                onClick={() => setActiveTab('supply_demand')}
                                className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${activeTab === 'supply_demand'
                                    ? 'bg-purple-600 text-white'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                    }`}
                            >
                                종목 누적 순매수금액 분석
                            </button>
                            <span className="text-gray-400 mx-2">|</span>
                            <button
                                onClick={() => setActiveTab('correlation')}
                                className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${activeTab === 'correlation'
                                    ? 'bg-purple-600 text-white'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                    }`}
                            >
                                상관분석
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Content Area */}
            <main className="flex-1 w-full bg-gray-50">
                <div style={{ display: activeTab === 'ma' ? 'block' : 'none' }}>
                    <MovingAverageChart stkCd={stkCd} hideSearch={true} />
                </div>
                <div style={{ display: activeTab === 'correlation' ? 'block' : 'none' }} className="w-full max-w-[1800px] mx-auto px-4 py-6">
                    <InvestorCorrelationChart stkCd={stkCd} />
                </div>
                <div style={{ display: activeTab === 'supply_demand' ? 'block' : 'none' }}>
                    <InvestorSupplyDemandAnalysis stkCd={stkCd} hideSearch={true} isVisible={activeTab === 'supply_demand'} />
                </div>
                <div style={{ display: activeTab === 'sector_ma' ? 'block' : 'none' }}>
                    <SectorMovingAverageChart />
                </div>
            </main>
        </div>
    );
};

export default UnifiedChartPage;
