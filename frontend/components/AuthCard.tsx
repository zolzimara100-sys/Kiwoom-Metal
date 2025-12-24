import React, { useState, useEffect } from 'react';
import { ShieldCheck, ShieldAlert, Server, Loader2, Lock, Activity } from 'lucide-react';
import { ConnectionStatus } from '../types';
import { oauthApi, saveToken } from '../services/api';

const AuthCard: React.FC = () => {
  const [status, setStatus] = useState<ConnectionStatus>(ConnectionStatus.IDLE);
  const [tokenInfo, setTokenInfo] = useState<{ token: string; expiresAt: string } | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');

  // Check token status on mount
  useEffect(() => {
    checkTokenStatus();
  }, []);

  const checkTokenStatus = async () => {
    // localStorage에서 직접 토큰 확인
    const storedToken = localStorage.getItem('oauth_token');
    const storedExpiry = localStorage.getItem('oauth_token_expiry');

    if (storedToken && storedExpiry) {
      const expiryDate = new Date(storedExpiry);
      const now = new Date();

      // 토큰이 아직 유효한지 확인
      if (expiryDate > now) {
        setStatus(ConnectionStatus.CONNECTED);
        setTokenInfo({
          token: storedToken,
          expiresAt: storedExpiry,
        });
        return;
      } else {
        // 토큰이 만료됨
        localStorage.removeItem('oauth_token');
        localStorage.removeItem('oauth_token_expiry');
      }
    }

    // localStorage에 토큰이 없거나 만료된 경우, IDLE 상태로 설정
    // 사용자가 명시적으로 "서버접속" 버튼을 클릭해야만 토큰을 받음
    setStatus(ConnectionStatus.IDLE);
    setTokenInfo(null);
  };

  const handleConnect = async () => {
    setStatus(ConnectionStatus.CONNECTING);
    setErrorMessage('');

    const result = await oauthApi.getToken();

    if (result.error) {
      setStatus(ConnectionStatus.ERROR);
      setErrorMessage(result.error);
    } else if (result.data && result.data.success) {
      const expiryDate = result.data.expiresDt || new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
      setStatus(ConnectionStatus.CONNECTED);
      setTokenInfo({
        token: result.data.token,
        expiresAt: expiryDate,
      });
      // localStorage에 토큰 및 만료 시간 저장
      saveToken(result.data.token);
      localStorage.setItem('oauth_token_expiry', expiryDate);
    } else {
      setStatus(ConnectionStatus.ERROR);
      setErrorMessage(result.data?.message || '토큰 발급 실패');
    }
  };

  const getStatusDisplay = () => {
    switch (status) {
      case ConnectionStatus.CONNECTING:
        return (
          <div className="flex items-center gap-2 text-blue-600 animate-pulse bg-blue-50 px-3 py-1 rounded-md">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span className="font-bold">서버 연결중...</span>
          </div>
        );
      case ConnectionStatus.CONNECTED:
        return (
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-2 text-[#16a34a] bg-green-50 px-3 py-1 rounded-md border border-green-100">
              <ShieldCheck className="w-6 h-6" />
              <span className="font-bold">인증 성공</span>
            </div>
            {tokenInfo && (
              <p className="text-xs text-gray-500 ml-1">
                만료: {new Date(tokenInfo.expiresAt).toLocaleString('ko-KR')}
              </p>
            )}
          </div>
        );
      case ConnectionStatus.ERROR:
        return (
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-2 text-[#dc2626] bg-red-50 px-3 py-1 rounded-md border border-red-100">
              <ShieldAlert className="w-6 h-6" />
              <span className="font-bold">접속 실패</span>
            </div>
            {errorMessage && (
              <p className="text-xs text-red-600 ml-1">{errorMessage}</p>
            )}
          </div>
        );
      case ConnectionStatus.IDLE:
      default:
        return (
          <div className="flex items-center gap-2 text-gray-400 bg-gray-50 px-3 py-1 rounded-md border border-gray-200">
            <Lock className="w-5 h-5" />
            <span className="font-medium">연결 대기</span>
          </div>
        );
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex flex-col md:flex-row items-center justify-between gap-4">
      
      {/* Left Side: Label and Status */}
      <div className="flex flex-col md:flex-row items-center gap-6 w-full md:w-auto">
        {/* Requirement: "인증 상태" font: bold 25 */}
        <div className="flex items-center gap-2">
           <Activity className="w-6 h-6 text-gray-700" />
           <h2 className="text-[25px] font-bold text-gray-900 whitespace-nowrap">
             인증 상태
           </h2>
        </div>

        {/* Requirement: Status Display next to label */}
        <div className="flex-shrink-0">
          {getStatusDisplay()}
        </div>
      </div>

      {/* Right Side: Connect Button */}
      <div className="w-full md:w-auto flex justify-end">
        {/* Requirement: "서버접속" Button */}
        <button
          onClick={handleConnect}
          disabled={status === ConnectionStatus.CONNECTING}
          className={`
            flex items-center justify-center gap-2 px-6 py-3 rounded-lg font-bold text-base shadow-sm transition-all duration-200 min-w-[140px]
            ${status === ConnectionStatus.CONNECTING 
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed' 
              : 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow active:scale-95'}
          `}
        >
          <Server className="w-5 h-5" />
          {status === ConnectionStatus.CONNECTING ? '접속중...' : '서버접속'}
        </button>
      </div>

    </div>
  );
};

export default AuthCard;