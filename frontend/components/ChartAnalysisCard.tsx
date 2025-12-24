import { ArrowRight, TrendingUp } from 'lucide-react';
import React from 'react';
import { Link } from 'react-router-dom';

const ChartAnalysisCard: React.FC = () => {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex flex-col md:flex-row items-center justify-between gap-4 hover:border-purple-300 transition-colors duration-300">

      {/* Content Section */}
      <div className="flex flex-col md:flex-row md:items-center gap-4 md:gap-8 flex-1">

        {/* Title with icon */}
        <div className="flex items-center gap-2 flex-shrink-0">
          <div className="p-1.5 bg-purple-100 rounded text-purple-600">
            <TrendingUp className="w-5 h-5" />
          </div>
          <h2 className="text-[14px] font-bold text-gray-900 whitespace-nowrap">
            차트 분석
          </h2>
        </div>

        {/* Description */}
        <div className="h-6 w-px bg-gray-200 hidden md:block"></div>
        <p className="text-gray-600 text-sm md:text-base leading-snug">
          투자자별 이동평균 및 수급분석 차트를 확인합니다
        </p>
      </div>

      {/* Link Button */}
      <div className="w-full md:w-auto flex justify-end pt-2 md:pt-0">
        <Link
          to="/moving-chart"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-white border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 hover:text-purple-600 hover:border-purple-300 transition-all duration-200 shadow-sm whitespace-nowrap"
        >
          차트 보기
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>

    </div>
  );
};

export default ChartAnalysisCard;
