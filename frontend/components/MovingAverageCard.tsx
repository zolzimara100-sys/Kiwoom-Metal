import { AlertCircle, ArrowRight, BarChart3, CheckCircle, Loader2, Play, TrendingUp } from 'lucide-react';
import React, { useState } from 'react';
import { Link } from 'react-router-dom';

type Status = 'idle' | 'loading' | 'success' | 'error';

const MovingAverageCard: React.FC = () => {
  const [status, setStatus] = useState<Status>('idle');
  const [message, setMessage] = useState<string>('');
  const [rowCount, setRowCount] = useState<number | null>(null);

  const handleCalculate = async () => {
    setStatus('loading');
    setMessage('이동평균 계산 중...');
    setRowCount(null);

    try {
      const response = await fetch('http://localhost:8080/api/statistics/moving-average/calculate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      setStatus('success');
      setRowCount(data.rowCount || null);
      setMessage(data.message || '이동평균 계산이 완료되었습니다.');
    } catch (error) {
      setStatus('error');
      setMessage(`오류 발생: ${error instanceof Error ? error.message : '알 수 없는 오류'}`);
    }
  };

  const getStatusIcon = () => {
    switch (status) {
      case 'loading':
        return <Loader2 className="w-4 h-4 animate-spin text-blue-600" />;
      case 'success':
        return <CheckCircle className="w-4 h-4 text-green-600" />;
      case 'error':
        return <AlertCircle className="w-4 h-4 text-red-600" />;
      default:
        return null;
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case 'loading':
        return 'text-blue-600';
      case 'success':
        return 'text-green-600';
      case 'error':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:border-purple-300 transition-colors duration-300">

      {/* Header Section */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 mb-4">

        {/* Title and Description */}
        <div className="flex flex-col md:flex-row md:items-center gap-4 md:gap-8 flex-1">

          {/* Icon and Title */}
          <div className="flex items-center gap-2 flex-shrink-0">
            <div className="p-1.5 bg-purple-100 rounded text-purple-600">
              <TrendingUp className="w-5 h-5" />
            </div>
            <h2 className="text-[14px] font-bold text-gray-900 whitespace-nowrap">
              투자자 이동평균 분석
            </h2>
          </div>

          {/* Separator */}
          <div className="h-6 w-px bg-gray-200 hidden md:block"></div>

          {/* Description */}
          <p className="text-gray-600 text-sm md:text-base leading-snug">
            12개 투자자별(외국인, 기관, 금융투자 등) 5/10/20/60일 이동평균 일괄 계산
          </p>
        </div>

        {/* Action Buttons */}
        <div className="w-full md:w-auto flex flex-wrap justify-end gap-2">
          {/* Chart Link Button */}
          <Link
            to="/moving-average-chart"
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-purple-300 text-purple-700 rounded-lg font-medium hover:bg-purple-50 transition-all duration-200 shadow-sm whitespace-nowrap"
          >
            <BarChart3 className="w-4 h-4" />
            차트 보기
            <ArrowRight className="w-4 h-4" />
          </Link>

          {/* Calculate Button */}
          <button
            onClick={handleCalculate}
            disabled={status === 'loading'}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 disabled:bg-purple-300 disabled:cursor-not-allowed transition-all duration-200 shadow-sm whitespace-nowrap"
          >
            {status === 'loading' ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                계산 중...
              </>
            ) : (
              <>
                <Play className="w-4 h-4" />
                이동평균 계산 실행
              </>
            )}
          </button>
        </div>
      </div>

      {/* Status Section */}
      {status !== 'idle' && (
        <div className={`flex items-center gap-2 mt-4 pt-4 border-t border-gray-100 ${getStatusColor()}`}>
          {getStatusIcon()}
          <span className="text-sm">{message}</span>
          {rowCount !== null && (
            <span className="text-sm text-gray-500 ml-2">
              (처리 건수: {rowCount.toLocaleString()}건)
            </span>
          )}
        </div>
      )}

    </div>
  );
};

export default MovingAverageCard;
