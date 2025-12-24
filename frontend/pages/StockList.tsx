import React, { useState } from 'react';
import { ArrowLeft, Database, RefreshCw, CheckCircle, XCircle, Clock, TrendingUp } from 'lucide-react';
import { Link } from 'react-router-dom';
import { stockListApi } from '../services/api';

type FetchStatus = 'idle' | 'fetching' | 'saving' | 'success' | 'error';

interface ProgressState {
  status: FetchStatus;
  receivedCount: number;
  savedCount: number;
  totalCount: number;
  message: string;
  errorDetails?: string; // 상세 오류 메시지
  startTime?: number;
  endTime?: number;
}

const StockList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState<ProgressState>({
    status: 'idle',
    receivedCount: 0,
    savedCount: 0,
    totalCount: 0,
    message: '',
  });

  const getStatusIcon = (status: FetchStatus) => {
    switch (status) {
      case 'fetching':
        return <RefreshCw className="w-5 h-5 text-blue-600 animate-spin" />;
      case 'saving':
        return <Database className="w-5 h-5 text-yellow-600 animate-pulse" />;
      case 'success':
        return <CheckCircle className="w-5 h-5 text-green-600" />;
      case 'error':
        return <XCircle className="w-5 h-5 text-red-600" />;
      default:
        return <Clock className="w-5 h-5 text-gray-400" />;
    }
  };

  const getStatusText = (status: FetchStatus) => {
    switch (status) {
      case 'fetching':
        return '데이터 수신 중...';
      case 'saving':
        return '데이터베이스 저장 중...';
      case 'success':
        return '완료';
      case 'error':
        return '실패';
      default:
        return '대기 중';
    }
  };

  const getStatusColor = (status: FetchStatus) => {
    switch (status) {
      case 'fetching':
        return 'bg-blue-50 border-blue-200 text-blue-800';
      case 'saving':
        return 'bg-yellow-50 border-yellow-200 text-yellow-800';
      case 'success':
        return 'bg-green-50 border-green-200 text-green-800';
      case 'error':
        return 'bg-red-50 border-red-200 text-red-800';
      default:
        return 'bg-gray-50 border-gray-200 text-gray-800';
    }
  };

  const formatElapsedTime = (startTime?: number, endTime?: number) => {
    if (!startTime) return '-';
    const elapsed = (endTime || Date.now()) - startTime;
    const seconds = Math.floor(elapsed / 1000);
    const ms = elapsed % 1000;
    return `${seconds}.${Math.floor(ms / 100)}초`;
  };

  const handleFetchStockList = async () => {
    setLoading(true);
    const startTime = Date.now();

    setProgress({
      status: 'fetching',
      receivedCount: 0,
      savedCount: 0,
      totalCount: 0,
      message: '코스피 종목 리스트를 가져오는 중...',
      startTime,
    });

    try {
      const result = await stockListApi.refreshAll();

      if (result.error) {
        setProgress({
          status: 'error',
          receivedCount: 0,
          savedCount: 0,
          totalCount: 0,
          message: '종목 리스트 갱신 실패',
          errorDetails: result.error,
          startTime,
          endTime: Date.now(),
        });
        setLoading(false); // 버튼 재활성화
      } else {
        // 저장 단계로 전환
        setProgress({
          status: 'saving',
          receivedCount: result.totalCount || 0,
          savedCount: 0,
          totalCount: result.totalCount || 0,
          message: '데이터베이스에 저장 중...',
          startTime,
        });

        // 저장 완료 (실제로는 API 응답이 이미 저장까지 완료됨)
        setTimeout(() => {
          setProgress({
            status: 'success',
            receivedCount: result.totalCount || 0,
            savedCount: result.totalCount || 0,
            totalCount: result.totalCount || 0,
            message: result.message || '종목 리스트 갱신 완료',
            startTime,
            endTime: Date.now(),
          });
          setLoading(false);
        }, 500);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';
      setProgress({
        status: 'error',
        receivedCount: 0,
        savedCount: 0,
        totalCount: 0,
        message: '네트워크 오류 발생',
        errorDetails: `오류 상세: ${errorMessage}\n\n서버 연결을 확인하세요.\nAPI 엔드포인트: /api/v1/stock-list/refresh`,
        startTime,
        endTime: Date.now(),
      });
      setLoading(false); // 버튼 재활성화
    }
  };

  const progressPercentage =
    progress.totalCount > 0 ? Math.round((progress.receivedCount / progress.totalCount) * 100) : 0;

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
            <h1 className="text-3xl font-bold text-gray-900">주식 종목 리스트 관리</h1>
          </div>
          <p className="text-gray-600 mt-2">
            코스피, 코스닥 등 전체 시장의 종목 리스트를 갱신합니다.
          </p>
        </div>

        {/* Action Button */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">종목 리스트 갱신</h2>
          <button
            onClick={handleFetchStockList}
            disabled={loading}
            className="w-full py-4 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <RefreshCw className="w-5 h-5 animate-spin" />
                <span>처리 중...</span>
              </>
            ) : (
              <>
                <RefreshCw className="w-5 h-5" />
                <span>종목리스트받기</span>
              </>
            )}
          </button>
        </div>

        {/* Progress Display */}
        {progress.status !== 'idle' && (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">진행 상황</h2>

            {/* Status Badge */}
            <div className={`mb-4 p-4 rounded-lg border ${getStatusColor(progress.status)}`}>
              <div className="flex items-center gap-3 mb-2">
                {getStatusIcon(progress.status)}
                <span className="font-semibold">{getStatusText(progress.status)}</span>
              </div>
              <p className="text-sm">{progress.message}</p>
            </div>

            {/* Error Details Box (Scrollable) */}
            {progress.status === 'error' && progress.errorDetails && (
              <div className="mb-4 bg-red-50 border border-red-200 rounded-lg p-4">
                <h3 className="text-sm font-semibold text-red-900 mb-2">오류 상세 정보</h3>
                <div className="bg-white border border-red-200 rounded p-3 max-h-48 overflow-y-auto">
                  <pre className="text-xs text-red-800 whitespace-pre-wrap font-mono">
                    {progress.errorDetails}
                  </pre>
                </div>
                <p className="text-xs text-red-600 mt-2">
                  💡 "종목리스트받기" 버튼을 다시 클릭하여 재시도할 수 있습니다.
                </p>
              </div>
            )}

            {/* Progress Bar */}
            {(progress.status === 'fetching' || progress.status === 'saving') && (
              <div className="mb-4">
                <div className="flex justify-between text-sm text-gray-600 mb-2">
                  <span>진행률</span>
                  <span>{progressPercentage}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                  <div
                    className="bg-blue-600 h-3 rounded-full transition-all duration-300"
                    style={{ width: `${progressPercentage}%` }}
                  />
                </div>
              </div>
            )}

            {/* Stats Grid */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-blue-50 rounded-lg p-4">
                <div className="text-xs text-gray-600 mb-1">수신 건수</div>
                <div className="text-2xl font-bold text-blue-600">{progress.receivedCount.toLocaleString()}</div>
              </div>

              <div className="bg-green-50 rounded-lg p-4">
                <div className="text-xs text-gray-600 mb-1">저장 건수</div>
                <div className="text-2xl font-bold text-green-600">{progress.savedCount.toLocaleString()}</div>
              </div>

              <div className="bg-purple-50 rounded-lg p-4">
                <div className="text-xs text-gray-600 mb-1">총 건수</div>
                <div className="text-2xl font-bold text-purple-600">{progress.totalCount.toLocaleString()}</div>
              </div>

              <div className="bg-yellow-50 rounded-lg p-4">
                <div className="text-xs text-gray-600 mb-1">소요 시간</div>
                <div className="text-2xl font-bold text-yellow-600">
                  {formatElapsedTime(progress.startTime, progress.endTime)}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Info Box */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h4 className="font-semibold text-blue-900 mb-2">💡 사용 안내</h4>
          <ul className="text-sm text-blue-800 space-y-1">
            <li>• <strong>종목리스트받기</strong> 버튼을 클릭하면 전체 시장의 종목 리스트를 갱신합니다.</li>
            <li>• 기존 데이터는 삭제되고 최신 데이터로 교체됩니다.</li>
            <li>• 진행 상황은 실시간으로 표시됩니다.</li>
            <li>• 수신, 저장, 완료 단계가 순차적으로 진행됩니다.</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default StockList;
